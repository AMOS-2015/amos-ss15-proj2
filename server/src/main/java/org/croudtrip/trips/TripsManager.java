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
import org.croudtrip.api.trips.RunningTripQueryStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.db.TripReservationDAO;
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
    private final RunningTripQueryDAO runningTripQueryDAO;
    private final TripReservationDAO tripReservationDAO;
    private final JoinTripRequestDAO joinTripRequestDAO;
    private final DirectionsManager directionsManager;
    private final VehicleManager vehicleManager;
    private final GcmManager gcmManager;
    private final TripsMatcher tripsMatcher;
    private final TripsUtils tripsUtils;
    private final LogManager logManager;


    @Inject
    TripsManager(
            TripOfferDAO tripOfferDAO,
            RunningTripQueryDAO runningTripQueryDAO,
            TripReservationDAO tripReservationDAO,
            JoinTripRequestDAO joinTripRequestDAO,
            DirectionsManager directionsManager,
            VehicleManager vehicleManager,
            GcmManager gcmManager,
            TripsMatcher tripsMatcher,
            TripsUtils tripsUtils,
            LogManager logManager) {

        this.tripOfferDAO = tripOfferDAO;
        this.runningTripQueryDAO = runningTripQueryDAO;
        this.tripReservationDAO = tripReservationDAO;
        this.joinTripRequestDAO = joinTripRequestDAO;
        this.directionsManager = directionsManager;
        this.vehicleManager = vehicleManager;
        this.gcmManager = gcmManager;
        this.tripsMatcher = tripsMatcher;
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
                TripOfferStatus.ACTIVE_NOT_FULL,
                System.currentTimeMillis()/1000);
        tripOfferDAO.save(offer);

        // compare offer with running queries
        for (RunningTripQuery runningQuery : runningTripQueryDAO.findByStatusRunning()) {
            if (!runningQuery.getStatus().equals(RunningTripQueryStatus.RUNNING)) continue;
            if (runningQuery.getCreationTimestamp() + runningQuery.getQuery().getMaxWaitingTimeInSeconds() < System.currentTimeMillis() / 1000) continue;

            TripQuery query = runningQuery.getQuery();
            boolean isPotentialMatch = tripsMatcher.isPotentialMatch(offer, query);

            // notify passenger about potential match
            if (isPotentialMatch) {
                logManager.d("Found a potential match: send gcm message to user " + query.getPassenger().getFirstName() + " " + query.getPassenger().getLastName());
                gcmManager.sendGcmMessageToUser(
                        query.getPassenger(),
                        GcmConstants.GCM_MSG_FOUND_MATCHES,
                        new Pair<>(GcmConstants.GCM_MSG_FOUND_MATCHES_QUERY_ID, "" + runningQuery.getId()));
                RunningTripQuery updatedRunningQuery = new RunningTripQuery(
                        runningQuery.getId(),
                        runningQuery.getQuery(),
                        runningQuery.getCreationTimestamp(),
                        RunningTripQueryStatus.FOUND);
                runningTripQueryDAO.update(updatedRunningQuery);
            }
        }
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
     * @throws RouteNotFoundException is thrown, if there is no route from the starting point to the destination of the trip.
     */
    public TripOffer updateOffer(TripOffer offer, TripOfferUpdate offerUpdate) throws RouteNotFoundException {
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
                    gcmManager.sendDriverCancelledTripMsg(offer, request.getQuery().getPassenger());
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
        TripQuery query = new TripQuery(possiblePassengerRoutes.get(0), queryDescription.getStart(), queryDescription.getEnd(), queryDescription.getMaxWaitingTimeInSeconds(), passenger);
        List<TripOffer> potentialMatches = findPotentialMatches(tripOfferDAO.findAllActive(), query);

        // find and store reservations
        List<TripReservation> reservations = findCheapestMatch(query, potentialMatches);
        for (TripReservation reservation : reservations) tripReservationDAO.save(reservation);

        // if no reservations start "background search"
        RunningTripQuery runningQuery = null;
        if (reservations.isEmpty()) {
            runningQuery = new RunningTripQuery(
                    0,
                    query,
                    System.currentTimeMillis() / 1000,
                    RunningTripQueryStatus.RUNNING);
            runningTripQueryDAO.save(runningQuery);
        }

        logManager.d("QUERY OFFER END");

        return new TripQueryResult(reservations, runningQuery);
    }


    /**
     * Find all the queries by a certain passenger.
     * @param passenger The passenger you want to get the queries for
     * @param showOnlyRunning true, if you want to get only the running queries, false otherwise
     * @return A list of {@link org.croudtrip.api.trips.RunningTripQuery} by this passenger.
     */
    public List<RunningTripQuery> getRunningQueries(User passenger, boolean showOnlyRunning) {
        if (showOnlyRunning) return runningTripQueryDAO.findByPassengerIdAndSatusRunning(passenger.getId());
        else return runningTripQueryDAO.findByPassengerId(passenger.getId());
    }

    /**
     * Find a running query by its id.
     * @param queryId the id of the query you want to get.
     * @return An {@link com.google.common.base.Optional} that contains the {@link org.croudtrip.api.trips.RunningTripQuery}
     * if it exists in the database.
     */
    public Optional<RunningTripQuery> getRunningQuery(long queryId) {
        return runningTripQueryDAO.findById(queryId);
    }

    /**
     * Delete a running query from the database
     * @param runningTripQuery the query that should be deleted.
     */
    public void deleteRunningQuery(RunningTripQuery runningTripQuery) {
        runningTripQueryDAO.delete(runningTripQuery);
    }

    /**
     * Get a list of all the reservations that exists.
     * @return returns all trip reservations from the database.
     */
    public List<TripReservation> findAllReservations() {
        return tripReservationDAO.findAll();
    }

    /**
     * Find a specific reservation by its id.
     * @param reservationId the id of the {@link org.croudtrip.api.trips.TripReservation} that you want to get.
     * @return An {@link com.google.common.base.Optional} that contains the TripReservation if it exists.
     */
    public Optional<TripReservation> findReservation(long reservationId) {
        return tripReservationDAO.findById(reservationId);
    }


    /**
     * Join a specific offer by a {@link org.croudtrip.api.trips.TripReservation}.
     * A {@link org.croudtrip.api.trips.JoinTripRequest} for this query will be created and a
     * notification is sent to the driver, that he has either to decline or to accept this new
     * passenger.
     * @param tripReservation The reservation for that the user want to join the trip
     * @return An {@link com.google.common.base.Optional} that contains a JoinTripRequest if the
     * reservation was still valid.
     */
    public Optional<JoinTripRequest> joinTrip(TripReservation tripReservation) {
        // remove reservation (either it has now been accepted or is can be discarded)
        tripReservationDAO.delete(tripReservation);

        // find and check trip
        Optional<TripOffer> offerOptional = tripOfferDAO.findById(tripReservation.getOfferId());
        if (!offerOptional.isPresent()) return Optional.absent();
        TripOffer offer = offerOptional.get();
        if (!tripsMatcher.isPotentialMatch(offer, tripReservation.getQuery())) return Optional.absent();

        // update join request
        JoinTripRequest joinTripRequest = new JoinTripRequest(
                0,
                tripReservation.getQuery(),
                tripReservation.getTotalPriceInCents(),
                tripReservation.getPricePerKmInCents(),
                offer,
                JoinTripStatus.PASSENGER_ACCEPTED);

        joinTripRequestDAO.save(joinTripRequest);

        // send push notification to driver
        gcmManager.sendGcmMessageToUser(offerOptional.get().getDriver(), GcmConstants.GCM_MSG_JOIN_REQUEST,
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST, "There is a new request to join your trip"),
                new Pair<String, String>(GcmConstants.GCM_MSG_USER_MAIL, "" + offerOptional.get().getDriver().getEmail()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + joinTripRequest.getId()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + offerOptional.get().getId()));

        return Optional.of(joinTripRequest);
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

        // update offer if vehicle is full
        TripOffer offer = joinRequest.getOffer();
        int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
        if (passengerCount >= offer.getVehicle().getCapacity()) {
            TripOffer updatedOffer = new TripOffer(
                    offer.getId(),
                    offer.getDriverRoute(),
                    offer.getEstimatedArrivalTimeInSeconds(),
                    offer.getCurrentLocation(),
                    offer.getMaxDiversionInMeters(),
                    offer.getPricePerKmInCents(),
                    offer.getDriver(),
                    offer.getVehicle(),
                    TripOfferStatus.ACTIVE_FULL,
                    offer.getLastPositonUpdateInSeconds() );
            tripOfferDAO.update(updatedOffer);
        }

        // notify the passenger about status
        logManager.d("User " + joinRequest.getQuery().getPassenger().getId() + " (" + joinRequest.getQuery().getPassenger().getFirstName() + " " + joinRequest.getQuery().getPassenger().getLastName() + ") got status update for joinTripRequest.");
        if(passengerAccepted) gcmManager.sendAcceptPassengerMsg(joinRequest);
        else gcmManager.sendDeclinePassengerMsg(joinRequest);

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
        return joinRequest;
    }


    private List<TripOffer> findPotentialMatches(List<TripOffer> offers, TripQuery query) {
        // analyse offers
        List<TripOffer> potentialMatches = new ArrayList<>();
        for (TripOffer offer : offers) {
            if (tripsMatcher.isPotentialMatch(offer, query)) {
                potentialMatches.add(offer);
            }
        }

        return potentialMatches;
    }


    /**
     * Checks if a specific {@link org.croudtrip.api.trips.TripOffer} matches to a specific
     * {@link org.croudtrip.api.trips.TripQuery}. That means that the additional route the driver
     * has to take into account for this query will not exceed his maximum diversion
     * @param offer The offer you want to check
     * @param query the query you search a potential match for
     * @return true, if the offer is a potential match for this query; false, if it is not.
     */

    /**
     * Will compute a list of cheapest @link{TripReservation} for a specific query out of a list of potential matches
     * @param query the query you want to get a match for
     * @param potentialMatches the list of potential matches for this query
     * @return a list of TripReservations for this query with the cheapest price
     */
    private List<TripReservation> findCheapestMatch(TripQuery query, List<TripOffer> potentialMatches) {
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
        List<TripReservation> reservations = new ArrayList<>();
        for (TripOffer match : matches) {
            reservations.add(new TripReservation(
                    0,
                    query,
                    totalPriceInCents,
                    match.getPricePerKmInCents(),
                    match.getId(),
                    match.getDriver(),
                    System.currentTimeMillis() / 1000));

        }

        return reservations;
    }

}

