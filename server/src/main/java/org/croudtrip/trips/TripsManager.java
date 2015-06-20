/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.SuperTrip;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.SuperTripDAO;
import org.croudtrip.db.SuperTripReservationDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.Assert;
import org.croudtrip.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TripsManager {

    private final TripOfferDAO tripOfferDAO;
    private final SuperTripReservationDAO superTripReservationDAO;
    private final SuperTripDAO superTripDAO;
    private final JoinTripRequestDAO joinTripRequestDAO;
    private final DirectionsManager directionsManager;
    private final VehicleManager vehicleManager;
    private final GcmManager gcmManager;
    private final TripsMatcher tripsMatcher;
    private final SuperTripManager superTripManager;
    private final RunningTripQueriesManager runningTripQueriesManager;
    private final TripsUtils tripsUtils;
    private final LogManager logManager;


    @Inject
    TripsManager(
            TripOfferDAO tripOfferDAO,
            SuperTripReservationDAO superTripReservationDAO,
            SuperTripDAO superTripDAO,
            JoinTripRequestDAO joinTripRequestDAO,
            DirectionsManager directionsManager,
            VehicleManager vehicleManager,
            GcmManager gcmManager,
            TripsMatcher tripsMatcher,
            SuperTripManager superTripManager,
            RunningTripQueriesManager runningTripQueriesManager,
            TripsUtils tripsUtils,
            LogManager logManager) {

        this.tripOfferDAO = tripOfferDAO;
        this.superTripReservationDAO = superTripReservationDAO;
        this.superTripDAO = superTripDAO;
        this.joinTripRequestDAO = joinTripRequestDAO;
        this.directionsManager = directionsManager;
        this.vehicleManager = vehicleManager;
        this.gcmManager = gcmManager;
        this.tripsMatcher = tripsMatcher;
        this.runningTripQueriesManager = runningTripQueriesManager;
        this.superTripManager = superTripManager;
        this.tripsUtils = tripsUtils;
        this.logManager = logManager;

    }

    /**
     * Adds an offer to the database.
     * @param owner The driver that offers the trip
     * @param description the description of the offer.
     * @return the newly created {@link org.croudtrip.api.trips.TripOffer}
     * @throws RouteNotFoundException is thrown, if there was no route from the requested start to
     * the requested destination.
     */
    public TripOffer addOffer(User owner, TripOfferDescription description) throws RouteNotFoundException {
        logManager.d("Searching for routes");

        // check if there is a route
        List<Route> route = directionsManager.getDirections(description.getStart(), description.getEnd());
        if (route.size() == 0) throw new RouteNotFoundException();

        logManager.d("Found " + route.size() + " routes to " + description.getEnd());

        // find vehicle
        Optional<Vehicle>vehicle = vehicleManager.findVehicleById(description.getVehicleId());
        Assert.assertTrue(vehicle.isPresent() && vehicle.get().getOwner().getId() == owner.getId(), "no vehilce for id " + description.getVehicleId());

        // create and store offer
        TripOffer offer = new TripOffer(
                0,
                route.get(0),
                System.currentTimeMillis()/1000+route.get(0).getDurationInSeconds(),
                description.getStart(),
                description.getMaxDiversionInMeters(),
                description.getPricePerKmInCents(),
                owner,
                vehicle.get(),
                TripOfferStatus.ACTIVE,
                System.currentTimeMillis()/1000);
        tripOfferDAO.save(offer);

        // check background search
        runningTripQueriesManager.checkAndUpdateRunningQueries(offer);

        return offer;
    }


    /**
     * Lists all the offers that belong to a specific driver.
     * @param driver The driver that you want to know the offers from.
     * @return A list of {@link org.croudtrip.api.trips.TripOffer} with all the offers of the driver.
     */
    public List<TripOffer> findOffersByDriver(User driver) {
        return tripOfferDAO.findByDriverId(driver.getId());
    }


    /**
     * Finds an offer by its id.
     * @param offerId the id of the offer.
     * @return An {@link com.google.common.base.Optional} that contains the offer if it exists.
     */
    public Optional<TripOffer> findOffer(long offerId) {
        return tripOfferDAO.findById(offerId);
    }


    /**
     * Deletes an offer from the database
     * @param offer the offer that should be deleted.
     */
    public void deleteOffer(TripOffer offer) {
        tripOfferDAO.delete(offer);
    }


    /**
     * Update the status of an offer.
     * @param offer the offer that should be updated.
     * @param offerUpdate the update status that should be applied to the offer.
     * @return The updated offer.
     */
    public TripOffer updateOffer(TripOffer offer, TripOfferUpdate offerUpdate) {
        RouteLocation newStart;
        TripOfferStatus newStatus;

        if (offerUpdate.getFinishOffer()) {
            // finish offer
            newStart = offer.getCurrentLocation();
            newStatus = TripOfferStatus.FINISHED;

            // decline pending passengers
            for (JoinTripRequest request : findAllJoinRequests(offer.getId())) {
                if (request.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED)) {
                    updateJoinRequestAcceptance(request, false);
                }
            }

        } else if (offerUpdate.getCancelOffer()) {
            // cancel trip
            newStart = offer.getCurrentLocation();
            newStatus = TripOfferStatus.CANCELLED;

            for (JoinTripRequest request : findAllJoinRequests(offer.getId())) {
                if (request.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED)) {
                    // decline pending passengers
                    updateJoinRequestAcceptance(request, false);

                } else if (request.getStatus().equals(JoinTripStatus.DRIVER_ACCEPTED)) {
                    // alert accepted passengers
                    JoinTripRequest updatedRequest = new JoinTripRequest(request, JoinTripStatus.DRIVER_CANCELLED);
                    joinTripRequestDAO.update(updatedRequest);
                    gcmManager.sendDriverCancelledTripMsg(offer, request.getSuperTrip().getQuery().getPassenger());
                }
            }


        } else {
            // update start location
            newStart = offerUpdate.getUpdatedStart();
            newStatus = offer.getStatus();
        }

        // update and store offer
        TripOffer updatedOffer = new TripOffer(
                offer.getId(),
                offer.getDriverRoute(),
                offer.getEstimatedArrivalTimeInSeconds(),
                newStart,
                offer.getMaxDiversionInMeters(),
                offer.getPricePerKmInCents(),
                offer.getDriver(),
                offer.getVehicle(),
                newStatus,
                System.currentTimeMillis() / 1000);
        tripOfferDAO.update(updatedOffer);
        return updatedOffer;
    }


    /**
     * Requests a query for offers that match for this passenger.
     * @param passenger The passenger that wants to join a trip.
     * @param queryDescription The description of his query
     * @return A {@link org.croudtrip.api.trips.TripQueryResult} for this request-
     * @throws RouteNotFoundException is thrown, if there is no route from the passenger's starting position to his final destination.
     */
    public TripQueryResult queryOffers(User passenger, TripQueryDescription queryDescription) throws RouteNotFoundException {
        logManager.d("QUERY OFFER: User " + passenger.getId() + " (" + passenger.getFirstName() + " " + passenger.getLastName() + ") from " + queryDescription.getStart() + " to " + queryDescription.getEnd() + " with " + " max waiting time: " + queryDescription.getMaxWaitingTimeInSeconds());

        // compute passenger route
        List<Route> possiblePassengerRoutes = directionsManager.getDirections(queryDescription.getStart(), queryDescription.getEnd());
        if (possiblePassengerRoutes.isEmpty()) throw new RouteNotFoundException();

        // analyse offers
        long queryCreationTimestamp = System.currentTimeMillis() / 1000;
        TripQuery query = new TripQuery(possiblePassengerRoutes.get(0), queryDescription.getStart(), queryDescription.getEnd(), queryDescription.getMaxWaitingTimeInSeconds(), queryCreationTimestamp, passenger);
        List<TripOffer> potentialMatches = tripsMatcher.filterPotentialMatches(tripOfferDAO.findAllActive(), query);

        // find and store reservations
        List<SuperTripReservation> reservations = findCheapestMatch(query, potentialMatches);

        // no reservations were found -> try to find super trips
        if( reservations.isEmpty() ) {
            reservations = superTripManager.findSuperTrips( tripOfferDAO.findAll(), query );
            for( SuperTripReservation reservation : reservations ) {
                logManager.d("Found a super trip: ");
                for( TripReservation res : reservation.getReservations() ) {
                    logManager.d("Driver: " + res.getDriver().getFirstName() );
                }
            }

        }

        // store all reservations in database
        for (SuperTripReservation reservation : reservations) superTripReservationDAO.save(reservation);

        // if no reservations start "background search"
        RunningTripQuery runningQuery = null;
        if (reservations.isEmpty()) {
            runningQuery = runningTripQueriesManager.startRunningQuery(query);
        }

        logManager.d("QUERY OFFER END");

        return new TripQueryResult(reservations, runningQuery);
    }

    /**
     * Get a list of all the reservations that exists.
     * @return returns all trip reservations from the database.
     */
    public List<SuperTripReservation> findAllReservations() {
        return superTripReservationDAO.findAll();
    }

    /**
     * Find a specific reservation by its id.
     * @param reservationId the id of the {@link org.croudtrip.api.trips.SuperTripReservation} that you want to get.
     * @return An {@link com.google.common.base.Optional} that contains the reservation
     */
    public Optional<SuperTripReservation> findReservation(long reservationId) {
        return superTripReservationDAO.findById(reservationId);
    }

    /**
     * Join a specific offer by a {@link org.croudtrip.api.trips.SuperTripReservation}.
     * A {@link SuperTrip} for this query will be created and a
     * notification is sent to the drivers (might be multiple!), that they have to either decline or accept.
     * @param tripReservation The reservation for that the user want to join the trip
     * @return An {@link com.google.common.base.Optional} that contains a join request if the
     * reservation was still valid.
     */
    public Optional<SuperTrip> joinTrip(SuperTripReservation tripReservation) {
        // remove reservation (either it has now been accepted or is can be discarded)
        superTripReservationDAO.delete(tripReservation);

        // find and check trip
        // TODO check all offers that belong to a super trip
        Optional<TripOffer> offerOptional = tripOfferDAO.findById(tripReservation.getReservations().get(0).getOfferId());
        if (!offerOptional.isPresent()) return Optional.absent();
        TripOffer offer = offerOptional.get();

        // check if the offer is still a valid match for this request (newly accepted requests may change this)
        Optional<TripsMatcher.PotentialMatch> potentialMatch = tripsMatcher.isPotentialMatch(offer, tripReservation.getQuery());
        if (!potentialMatch.isPresent()) return Optional.absent();

        // find estimated arrival time in list
        long arrivalTimestamp = 0;
        logManager.d("Potential match has " + potentialMatch.get().userWayPoints.size() + " wps");
        for( UserWayPoint wp : potentialMatch.get().userWayPoints ){
            logManager.d("WP for user " + wp.getUser().getFirstName());
            if( wp.getUser().getId() == tripReservation.getQuery().getPassenger().getId() ) {
                arrivalTimestamp = wp.getArrivalTimestamp();
                break;
            }
        }

        // update join request
        SuperTrip superTrip = new SuperTrip.Builder().setQuery(tripReservation.getQuery()).build();
        JoinTripRequest joinTripRequest = new JoinTripRequest(
                0,
                tripReservation.getReservations().get(0).getTotalPriceInCents(),
                tripReservation.getReservations().get(0).getPricePerKmInCents(),
                arrivalTimestamp,
                offer,
                JoinTripStatus.PASSENGER_ACCEPTED,
                superTrip);

        superTripDAO.save(superTrip);
        joinTripRequestDAO.save(joinTripRequest);

        // send push notification to driver
        // TODO send notification to all drivers!
        gcmManager.sendGcmMessageToUser(offerOptional.get().getDriver(), GcmConstants.GCM_MSG_JOIN_REQUEST,
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST, "There is a new request to join your trip"),
                new Pair<String, String>(GcmConstants.GCM_MSG_USER_MAIL, "" + offerOptional.get().getDriver().getEmail()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + joinTripRequest.getId()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + offerOptional.get().getId()));

        return Optional.of(superTrip);
    }


    /**
     * Returns all {@link JoinTripRequest}s, regardless of their state or user.
     */
    public List<JoinTripRequest> findAllJoinRequests() {
        return joinTripRequestDAO.findAll();
    }


    /**
     * Returns all JoinRequests that are related to a specific user and possibly have as status
     * {@link org.croudtrip.api.trips.JoinTripStatus#PASSENGER_ACCEPTED}.
     * @param passengerOrDriver the user to which all JoinTripRequests should belong.
     * @param showOnlyPassengerAccepted true if you want only get all the requests that are accepted by passenger side.
     * @return a list of JoinTripRequests.
     */
    public List<JoinTripRequest> findAllJoinRequests(User passengerOrDriver, boolean showOnlyPassengerAccepted) {
        if (showOnlyPassengerAccepted) return joinTripRequestDAO.findByUserIdAndStatusPassengerAccepted(passengerOrDriver.getId());
        else return joinTripRequestDAO.findByUserId(passengerOrDriver.getId());
    }


    /**
     * Finds all {@link org.croudtrip.api.trips.JoinTripRequest} that are related to a certain offer.
     * @param offerId the offer id for that all JoinTripRequests should be found.
     * @return A List of JoinTripRequests for this specific offer.
     */
    public List<JoinTripRequest> findAllJoinRequests(long offerId) {
        return joinTripRequestDAO.findByOfferId(offerId);
    }


    /**
     * Find all the {@link org.croudtrip.api.trips.JoinTripRequest} that were accepted by the driver (but are not in the car).
     * @param passengerOrDriver Either the driver or the passenger that are related to the JoinTripRequest
     * @return A list of JoinTripRequests that have as status {@link org.croudtrip.api.trips.JoinTripStatus#DRIVER_ACCEPTED}
     * and are related to the passed user.
     */
    public List<JoinTripRequest> findDriverAcceptedJoinRequests(User passengerOrDriver) {
        return joinTripRequestDAO.findByUserIdAndStatusDriverAccepted(passengerOrDriver.getId());
    }


    /**
     * Finds a {@link org.croudtrip.api.trips.JoinTripRequest} by its id.
     * @param joinRequestId the id of the JoinTripRequest
     * @return An {@link com.google.common.base.Optional} that contains the respective JoinTripRequest
     * if it exists.
     */
    public Optional<JoinTripRequest> findJoinRequest(long joinRequestId) {
        return joinTripRequestDAO.findById(joinRequestId);
    }

    /**
     * Updates a running join request if the driver either accepts or decline the passenger
     * @param joinRequest The join request that needs to be updated.
     * @param passengerAccepted true if the passenger should be accepted, false if not.
     * @return the updated join request.
     */
    public JoinTripRequest updateJoinRequestAcceptance(JoinTripRequest joinRequest, boolean passengerAccepted) {
        Assert.assertTrue(joinRequest.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED), "cannot modify join request");

        // update join request status
        JoinTripStatus newStatus;
        if (passengerAccepted) newStatus = JoinTripStatus.DRIVER_ACCEPTED;
        else newStatus = JoinTripStatus.DRIVER_DECLINED;
        JoinTripRequest updatedRequest = new JoinTripRequest(joinRequest, newStatus);
        joinTripRequestDAO.update(updatedRequest);

        // notify the passenger about status
        User passenger = joinRequest.getSuperTrip().getQuery().getPassenger();
        logManager.d("User " + passenger.getId() + " (" + passenger.getFirstName() + " " + passenger.getLastName() + ") got status update for joinTripRequest.");
        if(passengerAccepted) gcmManager.sendAcceptPassengerMsg(joinRequest);
        else gcmManager.sendDeclinePassengerMsg(joinRequest);

        // TODO: Check if other pending join requests are still valid

        // Send all the passengers an arrival time update
        if(passengerAccepted) {
            tripsUtils.updateArrivalTimesForOffer(joinRequest.getOffer(), passenger);
        }

        return joinTripRequestDAO.findById(joinRequest.getId()).get();
    }


    /**
     * Updates a running join request if the passenger has just entered the car.
     * @param joinRequest The join request that needs to be updated.
     * @return the updated join request.
     */
    public JoinTripRequest updateJoinRequestPassengerEnterCar(JoinTripRequest joinRequest) {
        JoinTripRequest updatedRequest = new JoinTripRequest(joinRequest, JoinTripStatus.PASSENGER_IN_CAR);
        joinTripRequestDAO.update(updatedRequest);

        // Send GCM to the driver to notify him that the passenger entered the car
        gcmManager.sendPassengerEnterCarMsg( joinRequest );

        return updatedRequest;
    }


    /**
     * Updates a running join request if the passenger is at his destination and has left the car.
     * @param joinRequest The join request that needs to be updated.
     * @return the updated join request.
     */
    public JoinTripRequest updateJoinRequestPassengerExitCar(JoinTripRequest joinRequest) {
        JoinTripRequest updatedRequest = new JoinTripRequest(joinRequest, JoinTripStatus.PASSENGER_AT_DESTINATION);
        joinTripRequestDAO.update(updatedRequest);

        // Send GCM to the driver to notify him that the passenger left the car
        gcmManager.sendPassengerExitCarMsg(joinRequest);

        // check background search
        runningTripQueriesManager.checkAndUpdateRunningQueries(joinRequest.getOffer());

        return updatedRequest;
    }


    /**
     * Updates a running join request if the passenger cancels the trip from his device.
     * @param joinRequest The join request that needs to be updated.
     * @return the updated join request.
     */
    public JoinTripRequest updateJoinRequestPassengerCancel(JoinTripRequest joinRequest) {
        JoinTripRequest updatedRequest = new JoinTripRequest(joinRequest, JoinTripStatus.PASSENGER_CANCELLED);
        joinTripRequestDAO.update(updatedRequest);
        gcmManager.sendPassengerCancelledTripMsg(joinRequest);

        // Update all the passenger's arrival time
        tripsUtils.updateArrivalTimesForOffer( joinRequest.getOffer() );

        // check background search
        runningTripQueriesManager.checkAndUpdateRunningQueries(joinRequest.getOffer());

        return joinRequest;
    }


    /**
     * Will compute a list of cheapest {@link org.croudtrip.api.trips.SuperTripReservation} for a
     * specific query out of a list of potential matches
     * @param query the query you want to get a match for
     * @param potentialMatches the list of potential matches for this query
     * @return a list of reservations for this query with the cheapest price
     */
    private List<SuperTripReservation> findCheapestMatch(TripQuery query, List<TripOffer> potentialMatches) {
        if (potentialMatches.isEmpty()) return new ArrayList<>();

        Collections.sort(potentialMatches, new Comparator<TripOffer>() {
            @Override
            public int compare(TripOffer offer1, TripOffer offer2) {
                return Integer.valueOf(offer1.getPricePerKmInCents()).compareTo(offer2.getPricePerKmInCents());
            }
        });

        List<TripOffer> matches = new ArrayList<>();

        // find prices
        int lowestPricePerKmInCents  = potentialMatches.get(0).getPricePerKmInCents(), secondLowestPricePerKmInCents = -1;
        for (TripOffer potentialMatch : potentialMatches) {
            if (potentialMatch.getPricePerKmInCents() == lowestPricePerKmInCents) {
                // all cheapest trips are matches
                matches.add(potentialMatch);

            } else if (potentialMatch.getPricePerKmInCents() != secondLowestPricePerKmInCents) {
                // second cheapest determines price
                secondLowestPricePerKmInCents = potentialMatch.getPricePerKmInCents();
                break;
            }
        }

        // calculate final price
        int pricePerKmInCents = lowestPricePerKmInCents;
        if (secondLowestPricePerKmInCents != -1) pricePerKmInCents = secondLowestPricePerKmInCents;
        int totalPriceInCents = (int) (pricePerKmInCents * query.getPassengerRoute().getDistanceInMeters() / 1000);

        // create price reservations
        List<SuperTripReservation> reservations = new ArrayList<>();
        for (TripOffer match : matches) {
            reservations.add(new SuperTripReservation.Builder()
                    .setQuery(query)
                    .addReservation(new TripReservation(
                                    totalPriceInCents,
                                    match.getPricePerKmInCents(),
                                    match.getId(),
                                    match.getDriver())
                    )
                    .build());
        }

        return reservations;
    }

}

