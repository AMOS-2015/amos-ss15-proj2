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
class SuperTripsMatcher extends SimpleTripsMatcher {

    private static final int MAX_DEPTH = 3;

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

    private static class PotentialRecursiveSuperTrip {
        private PotentialSuperTripMatch pickUpMatch;
        private PotentialSuperTripMatch dropMatch;
        private ClosestPairResult closestPairResult;

        public PotentialRecursiveSuperTrip(PotentialSuperTripMatch pickUpMatch, PotentialSuperTripMatch dropMatch, ClosestPairResult closestPairResult) {
            this.pickUpMatch = pickUpMatch;
            this.dropMatch = dropMatch;
            this.closestPairResult = closestPairResult;
        }

        public PotentialSuperTripMatch getPickUpMatch() {
            return pickUpMatch;
        }

        public PotentialSuperTripMatch getDropMatch() {
            return dropMatch;
        }

        public ClosestPairResult getClosestPairResult() {
            return closestPairResult;
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

    @Override
    public List<SuperTripReservation> findPotentialTrips(List<TripOffer> offers, TripQuery query) {
        return findPotentialTrips(offers, query, 0);
    }

    private List<SuperTripReservation> findPotentialTrips(List<TripOffer> offers, TripQuery query, int currentDepth) {
        if( currentDepth > MAX_DEPTH )
            return new ArrayList<>();

        List<SuperTripReservation> simpleReservations = super.findPotentialTrips(offers, query);
        if (!simpleReservations.isEmpty()) return simpleReservations;

        logManager.d("STARTED SEARCHING FOR SUPER TRIPS");

        // compute all offers that would be able to pickup or drop the passenger along their route
        List<TripOffer> potentialSuperTripPickUpMatches = new ArrayList<>();
        List<TripOffer> potentialSuperTripDropMatches = new ArrayList<>();
        for( TripOffer offer : offers ) {
            boolean pickupMatch = isRoughPotentialSuperTripMatchForOneWaypoint(offer, query, true);
            if( pickupMatch )   potentialSuperTripPickUpMatches.add( offer );

            boolean dropMatch = isRoughPotentialSuperTripMatchForOneWaypoint(offer, query, false);
            if( dropMatch )   potentialSuperTripDropMatches.add( offer );
        }

        logManager.d("found " + potentialSuperTripPickUpMatches.size() + " rough pickUp matches." );
        logManager.d("found " + potentialSuperTripDropMatches.size() + " rough drop matches." );


        // compute the closest pair of those trip offers. As soon as we find a matching pair, we return the super trip
        List<SuperTripReservation> reservations = new ArrayList<>();
        List<PotentialRecursiveSuperTrip> potentialRecursiveSuperTrips = new ArrayList<>();
        pickUp: for( TripOffer pickUpOffer : potentialSuperTripPickUpMatches ) {

            // now do the real check if the trip offer is a potential match
            Optional<PotentialSuperTripMatch> tripMatchOptional = isPotentialSuperTripMatchForOneWaypoint( pickUpOffer, query, true );
            if( !tripMatchOptional.isPresent() )
                continue;

            PotentialSuperTripMatch pickUpMatch = tripMatchOptional.get();

            for( TripOffer dropOffer : potentialSuperTripDropMatches ) {
                // don't match the same offer to itself
                if( dropOffer.getId() == pickUpOffer.getId() )
                    continue;

                // now do the real check if the trip offer is a potential match
                tripMatchOptional = isPotentialSuperTripMatchForOneWaypoint( dropOffer, query, false );
                if( !tripMatchOptional.isPresent() )
                    continue;

                PotentialSuperTripMatch dropMatch = tripMatchOptional.get();

                // find the closest pair for both routes
                ClosestPairResult closestPairResult = closestPair.findClosestPair(query.getPassenger(), pickUpMatch.getDiversionNavigationResult(), dropMatch.getDiversionNavigationResult());
                if( closestPairResult.getDropLocation() == null || closestPairResult.getPickupLocation() == null )
                    continue;

                // check if one of the drivers may take the diversion to drive to the connection point
                Optional<SuperTripReservation> reservation = isValidReservation( pickUpMatch, dropMatch, query, closestPairResult );
                if( reservation.isPresent() ){
                    reservations.add( reservation.get() );
                    break pickUp;
                }

                // no reservation has been found, but we can try to find a recursive super trip for this match later on.
                potentialRecursiveSuperTrips.add( new PotentialRecursiveSuperTrip( pickUpMatch, dropMatch, closestPairResult ) );
            }
        }

        // if we have not found a regular super trip, we may find one using recursive search
        if( reservations.isEmpty() && currentDepth < MAX_DEPTH ) {
            for( PotentialRecursiveSuperTrip recursiveSuperTrip : potentialRecursiveSuperTrips ){

                // adjust passenger route from pickup to drop location
                Route passengerRoute = directionsManager.getDirections( recursiveSuperTrip.getClosestPairResult().getPickupLocation(), recursiveSuperTrip.getClosestPairResult().getDropLocation() ).get(0);

                TripQuery adaptedQuery = new TripQuery.Builder().setPassengerRoute( passengerRoute )
                        .setPassenger( query.getPassenger() )
                        .setStartLocation( recursiveSuperTrip.getClosestPairResult().getPickupLocation() )
                        .setDestinationLocation( recursiveSuperTrip.getClosestPairResult().getDropLocation() )
                        .setCreationTimestamp( query.getCreationTimestamp() ) /* TODO: Just set this value to the arrival timestamp of the driver at the pickup location*/
                        .setMaxWaitingTimeInSeconds( TripQuery.IGNORE_MAX_WAITING_TIME ) /* TODO: Adjust the correct max waiting time */
                        .build();

                List<SuperTripReservation> recursiveReservations = findPotentialTrips( offers, adaptedQuery, currentDepth + 1 );

                if( recursiveReservations.isEmpty() ) continue;

                // pickup query
                TripQuery pickupQuery = new TripQuery.Builder().setPassengerRoute( null )
                        .setPassenger( query.getPassenger() )
                        .setStartLocation(query.getStartLocation())
                        .setDestinationLocation(recursiveSuperTrip.getClosestPairResult().getPickupLocation())
                        .setCreationTimestamp( query.getCreationTimestamp() )
                        .setMaxWaitingTimeInSeconds(query.getMaxWaitingTimeInSeconds())
                        .build();

                NavigationResult pickupNavigationResult = null;
                try {
                    pickupNavigationResult = tripsNavigationManager.getNavigationResultForOffer( recursiveSuperTrip.getPickUpMatch().getOffer(), pickupQuery );
                } catch (RouteNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }

                for( SuperTripReservation res : recursiveReservations ) {

                    SuperTripReservation.Builder reservationBuilder = new SuperTripReservation.Builder()
                            .setQuery(query)
                            .addReservation(new TripReservation(
                                    new SuperTripSubQuery.Builder()
                                            .setStartLocation(query.getStartLocation())
                                            .setDestinationLocation(recursiveSuperTrip.getClosestPairResult().getPickupLocation())
                                            .build(),
                                    (int)( recursiveSuperTrip.getPickUpMatch().getOffer().getPricePerKmInCents() * (pickupNavigationResult.getEstimatedTripDistanceInMetersForUser(query.getPassenger())/1000) ),
                                    recursiveSuperTrip.getPickUpMatch().getOffer().getPricePerKmInCents(),
                                    recursiveSuperTrip.getPickUpMatch().getOffer().getId(),
                                    pickupNavigationResult.getEstimatedTripDurationInSecondsForUser( query.getPassenger() ),
                                    recursiveSuperTrip.pickUpMatch.getOffer().getDriver()));

                    for( TripReservation tripReservation : res.getReservations() ){
                        reservationBuilder.addReservation( tripReservation );
                    }

                    // pickup query
                    TripQuery dropQuery = new TripQuery.Builder().setPassengerRoute( null )
                            .setPassenger( query.getPassenger() )
                            .setStartLocation( recursiveSuperTrip.getClosestPairResult().getDropLocation())
                            .setDestinationLocation(query.getDestinationLocation())
                            .setCreationTimestamp( query.getCreationTimestamp() ) /*TODO: Insert estimated arrival time at this point here */
                            .setMaxWaitingTimeInSeconds(query.getMaxWaitingTimeInSeconds())
                            .build();

                    NavigationResult dropNavigationResult = null;
                    try {
                        dropNavigationResult = tripsNavigationManager.getNavigationResultForOffer( recursiveSuperTrip.getDropMatch().getOffer(), dropQuery );
                    } catch (RouteNotFoundException e) {
                        e.printStackTrace();
                        continue;
                    }
                    reservationBuilder.addReservation(new TripReservation(
                            new SuperTripSubQuery.Builder()
                                    .setStartLocation(adaptedQuery.getDestinationLocation())
                                    .setDestinationLocation(query.getDestinationLocation())
                                    .build(),
                            (int)( recursiveSuperTrip.getPickUpMatch().getOffer().getPricePerKmInCents() * (dropNavigationResult.getEstimatedTripDistanceInMetersForUser(query.getPassenger())/1000) ),
                            recursiveSuperTrip.dropMatch.getOffer().getPricePerKmInCents(),
                            recursiveSuperTrip.dropMatch.getOffer().getId(),
                            pickupNavigationResult.getEstimatedTripDurationInSecondsForUser( query.getPassenger() ),
                            recursiveSuperTrip.dropMatch.getOffer().getDriver()));

                    reservations.add( reservationBuilder.build());
                }

            }


        }

        logManager.d("STOPPED SEARCHING FOR SUPER TRIPS");

        return reservations;
    }

    private boolean isRoughPotentialSuperTripMatchForOneWaypoint(TripOffer offer, TripQuery query, boolean useStartWaypoint) {
        // check trip status
        if (!offer.getStatus().equals(TripOfferStatus.ACTIVE)) return false;

        // check that query has not been declined before
        if (!assertJoinRequestNotDeclined(offer, query)) return false;

        // check current passenger count
        int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
        if (passengerCount >= offer.getVehicle().getCapacity()) return false;

        // create a fake query with only one point to compute matches
        TripQuery onePointQuery;
        if( useStartWaypoint)
            onePointQuery = new TripQuery.Builder().setPassenger(query.getPassenger()).setStartLocation( query.getStartLocation() ).build();
        else
            onePointQuery = new TripQuery.Builder().setPassenger(query.getPassenger()).setDestinationLocation(query.getDestinationLocation()).build();

        // early reject based on airline;
        if (!assertWithinAirDistance(offer, onePointQuery)) return false;

        return true;
    }

    private Optional<SuperTripReservation> isValidReservation(PotentialSuperTripMatch pickUpMatch, PotentialSuperTripMatch dropMatch, TripQuery query, ClosestPairResult closestPairResult) {

        // first check, if the pickUp-driver can drop the passenger at the closest point of the drop-Driver
        TripQuery adaptedQuery =  new TripQuery.Builder().setPassenger( query.getPassenger() )
                .setStartLocation( query.getStartLocation() )
                .setCreationTimestamp( query.getCreationTimestamp() )
                .setDestinationLocation(closestPairResult.getDropLocation())
                .setMaxWaitingTimeInSeconds( query.getMaxWaitingTimeInSeconds() )
                .build();

        Optional<SimpleTripsMatcher.PotentialMatch> potentialMatch = isPotentialMatch(pickUpMatch.getOffer(), adaptedQuery);
        if( potentialMatch.isPresent() ) {
            // expensive directions call but necessary, we need the adapted passenger routes to compute the total price per driver
            Route passengerpickUpRoute = directionsManager.getDirections( query.getStartLocation(), adaptedQuery.getDestinationLocation() ).get(0);
            Route passengerDropRoute = directionsManager.getDirections( adaptedQuery.getDestinationLocation(), query.getDestinationLocation() ).get(0);
            int totalPickUpPriceInCents = (int) (pickUpMatch.getOffer().getPricePerKmInCents() * passengerpickUpRoute.getDistanceInMeters() / 1000);
            int totalDropPriceInCents = (int) (dropMatch.getOffer().getPricePerKmInCents() * passengerDropRoute.getDistanceInMeters() / 1000);

            long estimatedPickupDuration = potentialMatch.get().getTotalRouteNavigationResult().getEstimatedTripDurationInSecondsForUser( query.getPassenger() );
            long estimatedDropDuration = dropMatch.getDiversionNavigationResult().getEstimatedTripDurationInSecondsForUser( query.getPassenger() );

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
                            estimatedPickupDuration,
                            pickUpMatch.getOffer().getDriver() ) )
                    .addReservation( new TripReservation(
                            new SuperTripSubQuery.Builder()
                                    .setStartLocation( adaptedQuery.getDestinationLocation() )
                                    .setDestinationLocation( query.getDestinationLocation() )
                                    .build(),
                            totalDropPriceInCents,
                            dropMatch.getOffer().getPricePerKmInCents(),
                            dropMatch.getOffer().getId(),
                            estimatedDropDuration,
                            dropMatch.getOffer().getDriver()) )
                    .build();

            return Optional.of( reservation );
        }

        // check if the dop driver can take the diversion to the waypoint of the pickup driver
        adaptedQuery =  new TripQuery.Builder().setPassenger( query.getPassenger() )
                .setStartLocation( closestPairResult.getPickupLocation() )
                .setCreationTimestamp( query.getCreationTimestamp() )
                .setDestinationLocation(query.getDestinationLocation() )
                .setMaxWaitingTimeInSeconds( TripQuery.IGNORE_MAX_WAITING_TIME ) // TODO: We are ignoring time for now
                .build();

        potentialMatch = isPotentialMatch(dropMatch.getOffer(), adaptedQuery);
        if( potentialMatch.isPresent() ) {
            // expensive directions call but necessary, we need the adapted passenger routes to compute the total price per driver
            Route passengerpickUpRoute = directionsManager.getDirections( query.getStartLocation(), adaptedQuery.getStartLocation() ).get(0);
            Route passengerDropRoute = directionsManager.getDirections( adaptedQuery.getStartLocation(), query.getDestinationLocation() ).get(0);
            int totalPickUpPriceInCents = (int) (pickUpMatch.getOffer().getPricePerKmInCents() * passengerpickUpRoute.getDistanceInMeters() / 1000);
            int totalDropPriceInCents = (int) (dropMatch.getOffer().getPricePerKmInCents() * passengerDropRoute.getDistanceInMeters() / 1000);

            long estimatedPickupDuration = pickUpMatch.getDiversionNavigationResult().getEstimatedTripDurationInSecondsForUser( query.getPassenger() );
            long estimatedDropDuration = potentialMatch.get().getTotalRouteNavigationResult().getEstimatedTripDurationInSecondsForUser( query.getPassenger() );

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
                            estimatedPickupDuration,
                            pickUpMatch.getOffer().getDriver() ) )
                    .addReservation( new TripReservation(
                            new SuperTripSubQuery.Builder()
                                    .setStartLocation( adaptedQuery.getStartLocation() )
                                    .setDestinationLocation( query.getDestinationLocation() )
                                    .build(),
                            totalDropPriceInCents,
                            dropMatch.getOffer().getPricePerKmInCents(),
                            dropMatch.getOffer().getId(),
                            estimatedDropDuration,
                            dropMatch.getOffer().getDriver()) )
                    .build();

            return Optional.of( reservation );
        }

        // TODO: Nothing was found, now we have recursive problem that could be solved using the trips matcher again from the beginning

        return Optional.absent();
    }

    /**
     * Checks if the given offer is a potential match for the given query for one particular waypoint
     * It will be used in super trips to compute offers that are able to pick up or drop passenger.
     * @param offer The offer that should be checked
     * @param query The query that should be checked
     * @return If the trip is a potential match a {@link SuperTripsMatcher.PotentialSuperTripMatch} will be returned.
     */
    private Optional<SuperTripsMatcher.PotentialSuperTripMatch> isPotentialSuperTripMatchForOneWaypoint( TripOffer offer, TripQuery query, boolean useStartWaypoint ) {
        if( !isRoughPotentialSuperTripMatchForOneWaypoint( offer, query, useStartWaypoint ) )
            return Optional.absent();

        // update driver route on new position update
        assertUpdatedDriverRoute(offer);

        // create a fake query with only one point to compute matches
        TripQuery onePointQuery;
        if( useStartWaypoint)
            onePointQuery = new TripQuery.Builder().setPassenger(query.getPassenger()).setStartLocation( query.getStartLocation() ).build();
        else
            onePointQuery = new TripQuery.Builder().setPassenger(query.getPassenger()).setDestinationLocation(query.getDestinationLocation()).build();

        // get complete new route
        NavigationResult navigationResult = null;
        try {
            navigationResult = tripsNavigationManager.getNavigationResultForOffer(offer, onePointQuery);
            if (navigationResult.getUserWayPoints().isEmpty()) return Optional.absent();
        } catch (RouteNotFoundException e) {
            return Optional.absent();
        }

        // TODO: Currently we are ignoring time completely
        //if (!assertRouteWithinPassengerMaxWaitingTime(offer, query, navigationResult.getUserWayPoints())) return Optional.absent();

        // check if passenger route is within max diversion
        long distanceToDriverInMeters = navigationResult.getUserWayPoints().get(navigationResult.getUserWayPoints().size() - 1).getDistanceToDriverInMeters();
        if (distanceToDriverInMeters - offer.getDriverRoute().getDistanceInMeters() > offer.getMaxDiversionInMeters()) return Optional.absent();

        return Optional.of( new SuperTripsMatcher.PotentialSuperTripMatch( offer, query, useStartWaypoint ? query.getStartLocation() : query.getDestinationLocation(), navigationResult ));
    }


}
