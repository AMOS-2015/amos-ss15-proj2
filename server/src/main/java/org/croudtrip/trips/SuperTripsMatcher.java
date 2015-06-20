package org.croudtrip.trips;

import com.google.common.base.Optional;

import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.SuperTripSubQuery;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.closestpair.ClosestPair;
import org.croudtrip.closestpair.ClosestPairResult;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.logs.LogManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Responsible for finding and creating super trips.
 */
class SuperTripsMatcher extends TripsMatcher {

    public static class PotentialSuperTripMatch {
        private final TripOffer offer;
        private final TripQuery query;
        private final RouteLocation singleWaypoint;
        private final NavigationResult diversionNavigationResult;

        public PotentialSuperTripMatch(TripOffer offer, TripQuery query, RouteLocation singleWaypoint, NavigationResult diversionNavigationResult) {
            this.offer = offer;
            this.query = query;
            this.singleWaypoint = singleWaypoint;
            this.diversionNavigationResult = diversionNavigationResult;
        }

        public TripOffer getOffer() {
            return offer;
        }

        public TripQuery getQuery() {
            return query;
        }

        public RouteLocation getSingleWaypoint() {
            return singleWaypoint;
        }

        public NavigationResult getDiversionNavigationResult() {
            return diversionNavigationResult;
        }
    }


    private final ClosestPair closestPair;


    @Inject
    SuperTripsMatcher(
            JoinTripRequestDAO joinTripRequestDAO,
            TripOfferDAO tripOfferDAO,
            TripsNavigationManager tripsNavigationManager,
            DirectionsManager directionsManager,
            TripsUtils tripsUtils,
            ClosestPair closestPair,
            LogManager logManager) {

        super(joinTripRequestDAO, tripOfferDAO, tripsNavigationManager, directionsManager,  tripsUtils, logManager);
        this.closestPair = closestPair;
    }

    public List<SuperTripReservation> findSuperTrips( List<TripOffer> offers, TripQuery query  ) throws RouteNotFoundException {
        logManager.d("STARTED SEARCHING FOR SUPER TRIPS");

        // compute all offers that would be able to pickup or drop the passenger along their route
        List<PotentialSuperTripMatch> potentialSuperTripPickUpMatches = new ArrayList<>();
        List<PotentialSuperTripMatch> potentialSuperTripDropMatches = new ArrayList<>();
        for( TripOffer offer : offers ) {
            Optional<PotentialSuperTripMatch> pickupMatch = isPotentialSuperTripMatchForOneWaypoint(offer, query, true);
            if( pickupMatch.isPresent() )   potentialSuperTripPickUpMatches.add( pickupMatch.get());

            Optional<PotentialSuperTripMatch> dropMatch = isPotentialSuperTripMatchForOneWaypoint(offer, query, false);
            if( dropMatch.isPresent() )   potentialSuperTripDropMatches.add( dropMatch.get());
        }

        logManager.d("found " + potentialSuperTripPickUpMatches.size() + " pickUp matches." );
        logManager.d("found " + potentialSuperTripDropMatches.size() + " drop matches." );


        // compute the closest pair of those trip offers. As soon as we find a matching pair, we return the super trip
        List<SuperTripReservation> reservations = new ArrayList<>();
pickUp: for( PotentialSuperTripMatch pickUpMatch : potentialSuperTripPickUpMatches ) {
            for( PotentialSuperTripMatch dropMatch : potentialSuperTripDropMatches ) {
                ClosestPairResult closestPairResult = closestPair.findClosestPair(query.getPassenger(), pickUpMatch.getDiversionNavigationResult(), dropMatch.getDiversionNavigationResult());
                if( closestPairResult.getDropLocation() == null || closestPairResult.getPickupLocation() == null )
                    continue;

                // check if one of the drivers may take the diversion to drive to the connection point
                Optional<SuperTripReservation> reservation = isValidReservation( pickUpMatch, dropMatch, query, closestPairResult );
                if( reservation.isPresent() ){
                    reservations.add( reservation.get() );
                    break pickUp;
                }
            }
        }

        logManager.d("STOPPED SEARCHING FOR SUPER TRIPS");

        return reservations;
    }

    private Optional<SuperTripReservation> isValidReservation(PotentialSuperTripMatch pickUpMatch, PotentialSuperTripMatch dropMatch, TripQuery query, ClosestPairResult closestPairResult) {

        // first check, if the pickUp-driver can drop the passenger at the closest point of the drop-Driver
        TripQuery adaptedQuery =  new TripQuery.Builder().setPassenger( query.getPassenger() )
                .setStartLocation( query.getStartLocation() )
                .setCreationTimestamp( query.getCreationTimestamp() )
                .setDestinationLocation(closestPairResult.getDropLocation())
                .setMaxWaitingTimeInSeconds( query.getMaxWaitingTimeInSeconds() )
                .build();

        Optional<TripsMatcher.PotentialMatch> potentialMatch = isPotentialMatch(pickUpMatch.getOffer(), adaptedQuery);
        if( potentialMatch.isPresent() ) {
            // expensive directions call but necessary, we need the adapted passenger routes to compute the total price per driver
            Route passengerpickUpRoute = directionsManager.getDirections( query.getStartLocation(), adaptedQuery.getDestinationLocation() ).get(0);
            Route passengerDropRoute = directionsManager.getDirections( adaptedQuery.getDestinationLocation(), query.getDestinationLocation() ).get(0);
            int totalPickUpPriceInCents = (int) (pickUpMatch.getOffer().getPricePerKmInCents() * passengerpickUpRoute.getDistanceInMeters() / 1000);
            int totalDropPriceInCents = (int) (dropMatch.getOffer().getPricePerKmInCents() * passengerDropRoute.getDistanceInMeters() / 1000);

            SuperTripReservation reservation = new SuperTripReservation.Builder()
                    .setQuery(query)
                    .addReservation( new TripReservation(
                            new SuperTripSubQuery.Builder()
                                    .setStartLocation( query.getStartLocation() )
                                    .setDestinationLocation( adaptedQuery.getDestinationLocation() )
                            .build(),
                            totalPickUpPriceInCents,
                            pickUpMatch.getOffer().getPricePerKmInCents(),
                            pickUpMatch.getOffer().getId(),
                            pickUpMatch.getOffer().getDriver() ) )
                    .addReservation( new TripReservation(
                            new SuperTripSubQuery.Builder()
                                    .setStartLocation( adaptedQuery.getDestinationLocation() )
                                    .setDestinationLocation( query.getDestinationLocation() )
                            .build(),
                            totalDropPriceInCents,
                            dropMatch.getOffer().getPricePerKmInCents(),
                            dropMatch.getOffer().getId(),
                            dropMatch.getOffer().getDriver()) )
                    .build();

            return Optional.of( reservation );
        }

        // check if the dop driver can take the diversion to the waypoint of the pickup driver
        adaptedQuery =  new TripQuery.Builder().setPassenger( query.getPassenger() )
                .setStartLocation( closestPairResult.getPickupLocation() )
                .setCreationTimestamp( query.getCreationTimestamp() )
                .setDestinationLocation(query.getDestinationLocation() )
                .setMaxWaitingTimeInSeconds( Integer.MAX_VALUE ) // TODO: We are ignoring time for now
                .build();

        potentialMatch = isPotentialMatch(pickUpMatch.getOffer(), adaptedQuery);
        if( potentialMatch.isPresent() ) {
            // expensive directions call but necessary, we need the adapted passenger routes to compute the total price per driver
            Route passengerpickUpRoute = directionsManager.getDirections( query.getStartLocation(), adaptedQuery.getStartLocation() ).get(0);
            Route passengerDropRoute = directionsManager.getDirections( adaptedQuery.getStartLocation(), query.getDestinationLocation() ).get(0);
            int totalPickUpPriceInCents = (int) (pickUpMatch.getOffer().getPricePerKmInCents() * passengerpickUpRoute.getDistanceInMeters() / 1000);
            int totalDropPriceInCents = (int) (dropMatch.getOffer().getPricePerKmInCents() * passengerDropRoute.getDistanceInMeters() / 1000);

            SuperTripReservation reservation = new SuperTripReservation.Builder()
                    .setQuery(query)
                    .addReservation( new TripReservation(
                            new SuperTripSubQuery.Builder()
                                .setStartLocation( query.getStartLocation() )
                                .setDestinationLocation( adaptedQuery.getStartLocation() )
                            .build(),
                            totalPickUpPriceInCents,
                            pickUpMatch.getOffer().getPricePerKmInCents(),
                            pickUpMatch.getOffer().getId(),
                            pickUpMatch.getOffer().getDriver() ) )
                    .addReservation( new TripReservation(
                            new SuperTripSubQuery.Builder()
                                    .setStartLocation( adaptedQuery.getStartLocation() )
                                    .setDestinationLocation( query.getDestinationLocation() )
                            .build(),
                            totalDropPriceInCents,
                            dropMatch.getOffer().getPricePerKmInCents(),
                            dropMatch.getOffer().getId(),
                            dropMatch.getOffer().getDriver()) )
                    .build();

            return Optional.of( reservation );
        }

        return Optional.absent();
    }

    /**
     * Checks if the given offer is a potential match for the given query for one particular waypoint
     * It will be used in super trips to compute offers that are able to pick up or drop passenger.
     * @param offer The offer that should be checked
     * @param query The query that should be checked
     * @return If the trip is a potential match a {@link SuperTripsMatcher.PotentialSuperTripMatch} will be returned.
     */
    private Optional<SuperTripsMatcher.PotentialSuperTripMatch> isPotentialSuperTripMatchForOneWaypoint( TripOffer offer, TripQuery query, boolean useStartWaypoint ) throws RouteNotFoundException {
        // check trip status
        if (!offer.getStatus().equals(TripOfferStatus.ACTIVE)) return Optional.absent();

        // check that query has not been declined before
        if (!assertJoinRequestNotDeclined(offer, query)) return Optional.absent();

        // check current passenger count
        int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
        if (passengerCount >= offer.getVehicle().getCapacity()) return Optional.absent();

        // create a fake query with only one point to compute matches
        TripQuery onePointQuery;
        if( useStartWaypoint)
            onePointQuery = new TripQuery.Builder().setPassenger(query.getPassenger()).setStartLocation( query.getStartLocation() ).build();
        else
            onePointQuery = new TripQuery.Builder().setPassenger(query.getPassenger()).setDestinationLocation(query.getDestinationLocation()).build();

        // early reject based on airline;
        if (!assertWithinAirDistance(offer, onePointQuery)) return Optional.absent();

        // update driver route on new position update
        assertUpdatedDriverRoute(offer);

        // get complete new route
        NavigationResult navigationResult = tripsNavigationManager.getNavigationResultForOffer(offer, onePointQuery);
        if (navigationResult.getUserWayPoints().isEmpty()) return Optional.absent();

        // TODO: Currently we are ignoring time completely
        //if (!assertRouteWithinPassengerMaxWaitingTime(offer, query, navigationResult.getUserWayPoints())) return Optional.absent();

        // check if passenger route is within max diversion
        long distanceToDriverInMeters = navigationResult.getUserWayPoints().get(navigationResult.getUserWayPoints().size() - 1).getDistanceToDriverInMeters();
        if (distanceToDriverInMeters - offer.getDriverRoute().getDistanceInMeters() > offer.getMaxDiversionInMeters()) return Optional.absent();

        return Optional.of( new SuperTripsMatcher.PotentialSuperTripMatch( offer, query, useStartWaypoint ? query.getStartLocation() : query.getDestinationLocation(), navigationResult ));
    }


}
