package org.croudtrip.trips;

import com.google.common.base.Optional;
import com.google.maps.model.LatLng;

import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.RouteDistanceDuration;
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
import org.croudtrip.places.Place;
import org.croudtrip.places.PlaceRanking;
import org.croudtrip.places.PlacesApi;
import org.croudtrip.places.PlacesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        private final PotentialSuperTripMatch pickUpMatch;
        private final PotentialSuperTripMatch dropMatch;
        private final ClosestPairResult closestPairResult;

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

    private final PlacesManager placesManager;

    @Inject
    SuperTripsMatcher(
            JoinTripRequestDAO joinTripRequestDAO,
            TripOfferDAO tripOfferDAO,
            TripsNavigationManager tripsNavigationManager,
            DirectionsManager directionsManager,
            TripsUtils tripsUtils,
            ClosestPair closestPair,
            PlacesManager placesManager,
            LogManager logManager) {

        super(joinTripRequestDAO, tripOfferDAO, tripsNavigationManager, directionsManager,  tripsUtils, logManager);
        this.closestPair = closestPair;
        this.placesManager = placesManager;
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

                // TODO: If closest pair distance is greater thant the max diversion of the driver we don't have to check if it is a valid reserveration since it simply cannot be.

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
                RouteDistanceDuration passengerDistanceDuration = directionsManager.getDistanceAndDurationForDirection(recursiveSuperTrip.getClosestPairResult().getPickupLocation(), recursiveSuperTrip.getClosestPairResult().getDropLocation());

                // pickup query
                TripQuery pickupQuery = new TripQuery.Builder()
                        .setPassenger( query.getPassenger() )
                        .setStartLocation(query.getStartLocation())
                        .setDestinationLocation(recursiveSuperTrip.getClosestPairResult().getPickupLocation())
                        .setCreationTimestamp( query.getCreationTimestamp() )
                        .setMaxWaitingTimeInSeconds(query.getMaxWaitingTimeInSeconds())
                        .build();

                // compute route from passenger start point to passenger destination
                NavigationResult pickupNavigationResult;
                try {
                    pickupNavigationResult = tripsNavigationManager.getNavigationResultForOffer( recursiveSuperTrip.getPickUpMatch().getOffer(), pickupQuery );
                } catch (RouteNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }

                //
                long tripDurationInSeconds = pickupNavigationResult.getEstimatedTripDurationInSecondsForUser(pickupQuery.getPassenger());

                // compute results from closest pickup point to closes drop point
                TripQuery adaptedQuery = new TripQuery.Builder().setPasengerRouteDistanceDuration( passengerDistanceDuration )
                        .setPassenger( query.getPassenger() )
                        .setStartLocation( recursiveSuperTrip.getClosestPairResult().getPickupLocation() )
                        .setDestinationLocation( recursiveSuperTrip.getClosestPairResult().getDropLocation() )
                        .setCreationTimestamp( query.getCreationTimestamp() + tripDurationInSeconds ) /* Just set this value to the arrival timestamp of the driver at the pickup location*/
                        .setMaxWaitingTimeInSeconds(query.getMaxWaitingTimeInSeconds())
                        .build();

                // temporarily remove found offers from offers list
                offers.remove(recursiveSuperTrip.pickUpMatch.getOffer());
                offers.remove(recursiveSuperTrip.dropMatch.getOffer());
                List<SuperTripReservation> recursiveReservations = findPotentialTrips( offers, adaptedQuery, currentDepth + 1 );
                offers.add(recursiveSuperTrip.pickUpMatch.getOffer());
                offers.add(recursiveSuperTrip.dropMatch.getOffer());

                // if no recursive solution was found we can stop further searching
                if( recursiveReservations.isEmpty() ) continue;

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
                        tripDurationInSeconds += tripReservation.getEstimatedTripDurationInSeconds();
                    }

                    // drop query
                    TripQuery dropQuery = new TripQuery.Builder()
                            .setPassenger( query.getPassenger() )
                            .setStartLocation( recursiveSuperTrip.getClosestPairResult().getDropLocation())
                            .setDestinationLocation(query.getDestinationLocation())
                            .setCreationTimestamp( query.getCreationTimestamp() + tripDurationInSeconds ) /*estimated arrival time at closest drop point*/
                            .setMaxWaitingTimeInSeconds(query.getMaxWaitingTimeInSeconds())
                            .build();

                    NavigationResult dropNavigationResult;
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

    protected boolean isRoughPotentialSuperTripMatchForOneWaypoint(TripOffer offer, TripQuery query, boolean useStartWaypoint) {
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
        return assertWithinAirDistance(offer, onePointQuery);
    }

    protected Optional<SuperTripReservation> isValidReservation(PotentialSuperTripMatch pickUpMatch, PotentialSuperTripMatch dropMatch, TripQuery query, ClosestPairResult closestPairResult) {

        Map queryMap = new PlacesApi.QueryMapBuilder().location(new LatLng(closestPairResult.getPickupLocation().getLat(), closestPairResult.getPickupLocation().getLng()))
                .rankBy(PlaceRanking.RANK_BY_DISTANCE)
                .build();

        List<Place> places = placesManager.getNearbyPlaces( queryMap, 3);

        queryMap = new PlacesApi.QueryMapBuilder().location(new LatLng( closestPairResult.getDropLocation().getLat(),closestPairResult.getDropLocation().getLng()))
                .rankBy(PlaceRanking.RANK_BY_DISTANCE)
                .build();
        places.addAll(placesManager.getNearbyPlaces(queryMap, 3));

        logManager.d("Got Places next to: " + closestPairResult.getPickupLocation());
        logManager.d("Got Places next to: " + closestPairResult.getDropLocation());

        for( Place place : places ) {
            logManager.d("Check place: " + place.getName() + " at " + place.getLocation());

            Optional<SuperTripReservation> reservationOptional = isValidReservationForConnectionPoint(query, place.getLocation(), pickUpMatch, dropMatch);
            if( reservationOptional.isPresent() ) {
                logManager.d( "Found a connection point at a place: " + place.getName() + " at location: " + place.getLocation() );
                return reservationOptional;
            }
        }

        Optional<SuperTripReservation> reservationOptional = isValidReservationForConnectionPoint(query, closestPairResult.getDropLocation(), pickUpMatch, dropMatch);
        if( reservationOptional.isPresent() ) {
            logManager.d( "Found a connection point at drop location: " + closestPairResult.getDropLocation() );
            return reservationOptional;
        }

        return isValidReservationForConnectionPoint(query, closestPairResult.getPickupLocation(), pickUpMatch, dropMatch);
    }

    private Optional<SuperTripReservation> isValidReservationForConnectionPoint(TripQuery query, RouteLocation connectionPoint, PotentialSuperTripMatch pickUpMatch, PotentialSuperTripMatch dropMatch) {
        // compute the time, when the first driver will be at his connection point and check if it's a valid match
        TripQuery adaptedQuery =  new TripQuery.Builder().setPassenger(query.getPassenger())
                .setStartLocation(query.getStartLocation())
                .setDestinationLocation(connectionPoint)
                .setCreationTimestamp(query.getCreationTimestamp())
                .setMaxWaitingTimeInSeconds(query.getMaxWaitingTimeInSeconds())
                .build();
        Optional<PotentialMatch> potentialMatch = isPotentialMatch(pickUpMatch.getOffer(), adaptedQuery);

        if( !potentialMatch.isPresent() )
            return Optional.absent();

        long estimatedPickupDuration = potentialMatch.get().getTotalRouteNavigationResult().getEstimatedTripDurationInSecondsForUser( query.getPassenger() );

        // compute the time, when the second driver will be at the connection point and check if it's in range and a valid match
        adaptedQuery =  new TripQuery.Builder().setPassenger(query.getPassenger())
                .setStartLocation(connectionPoint)
                .setDestinationLocation(query.getDestinationLocation())
                .setCreationTimestamp( query.getCreationTimestamp() + estimatedPickupDuration )
                .setMaxWaitingTimeInSeconds( query.getMaxWaitingTimeInSeconds() )
                .build();

        potentialMatch = isPotentialMatch(dropMatch.getOffer(), adaptedQuery);
        if( !potentialMatch.isPresent() )
            return Optional.absent();

        long estimatedDropDuration = potentialMatch.get().getTotalRouteNavigationResult().getEstimatedTripDurationInSecondsForUser(query.getPassenger());

        // don't use direction calls here, but use distance matrix calls
        RouteDistanceDuration passengerPickUp = directionsManager.getDistanceAndDurationForDirection(query.getStartLocation(), adaptedQuery.getStartLocation());
        RouteDistanceDuration passengerDrop = directionsManager.getDistanceAndDurationForDirection(adaptedQuery.getStartLocation(), query.getDestinationLocation());
        int totalPickUpPriceInCents = (int) (pickUpMatch.getOffer().getPricePerKmInCents() * passengerPickUp.getDistanceInMeters() / 1000);
        int totalDropPriceInCents = (int) (dropMatch.getOffer().getPricePerKmInCents() * passengerDrop.getDistanceInMeters() / 1000);

        logManager.d("SuperTripReservation: " + totalPickUpPriceInCents + "ct, " + totalDropPriceInCents + "ct");

        return Optional.of(
                createSuperTripReservation( query,
                        connectionPoint,
                        pickUpMatch.getOffer(),
                        dropMatch.getOffer(),
                        totalPickUpPriceInCents,
                        estimatedPickupDuration,
                        totalDropPriceInCents,
                        estimatedDropDuration)
        );
    }

    private SuperTripReservation createSuperTripReservation(
            TripQuery query,
            RouteLocation connectionPoint,
            TripOffer pickUpOffer,
            TripOffer dropOffer,
            int totalPickUpPriceInCents,
            long estimatedPickupDuration,
            int totalDropPriceInCents,
            long estimatedDropDuration) {

        return new SuperTripReservation.Builder()
                .setQuery(query)
                .addReservation(new TripReservation.Builder()
                        .setSubQuery(new SuperTripSubQuery.Builder()
                                .setStartLocation(query.getStartLocation())
                                .setDestinationLocation(connectionPoint)
                                .build())
                        .setTotalPriceInCents(totalPickUpPriceInCents)
                        .setPricePerKmInCents(pickUpOffer.getPricePerKmInCents())
                        .setOfferId(pickUpOffer.getId())
                        .setEstimatedTripDurationInSeconds(estimatedPickupDuration)
                        .setDriver(pickUpOffer.getDriver())
                        .build())
                .addReservation(new TripReservation.Builder()
                        .setSubQuery(new SuperTripSubQuery.Builder()
                                .setStartLocation(connectionPoint)
                                .setDestinationLocation(query.getDestinationLocation())
                                .build())
                        .setTotalPriceInCents(totalDropPriceInCents)
                        .setPricePerKmInCents(dropOffer.getPricePerKmInCents())
                        .setOfferId(dropOffer.getId())
                        .setEstimatedTripDurationInSeconds(estimatedDropDuration)
                        .setDriver(dropOffer.getDriver())
                        .build())
                .build();
    }

    /**
     * Checks if the given offer is a potential match for the given query for one particular waypoint
     * It will be used in super trips to compute offers that are able to pick up or drop passenger.
     * @param offer The offer that should be checked
     * @param query The query that should be checked
     * @return If the trip is a potential match a {@link SuperTripsMatcher.PotentialSuperTripMatch} will be returned.
     */
    protected Optional<SuperTripsMatcher.PotentialSuperTripMatch> isPotentialSuperTripMatchForOneWaypoint( TripOffer offer, TripQuery query, boolean useStartWaypoint ) {
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
        NavigationResult navigationResult;
        try {
            navigationResult = tripsNavigationManager.getNavigationResultForOffer(offer, onePointQuery);
            if (navigationResult.getUserWayPoints().isEmpty()) return Optional.absent();
        } catch (RouteNotFoundException e) {
            return Optional.absent();
        }

        // check if the user is picked up in time
        if( useStartWaypoint)
            if (!assertRouteWithinPassengerMaxWaitingTime(offer, query, navigationResult.getUserWayPoints())) return Optional.absent();

        // check if passenger route is within max diversion
        long distanceToDriverInMeters = navigationResult.getUserWayPoints().get(navigationResult.getUserWayPoints().size() - 1).getDistanceToDriverInMeters();
        if (distanceToDriverInMeters - offer.getDriverRoute().getDistanceInMeters() > offer.getMaxDiversionInMeters()) return Optional.absent();

        return Optional.of( new SuperTripsMatcher.PotentialSuperTripMatch( offer, query, useStartWaypoint ? query.getStartLocation() : query.getDestinationLocation(), navigationResult ));
    }


}
