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

package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripRequestUpdateType;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.logs.LogManager;
import org.croudtrip.trips.TripsManager;
import org.croudtrip.trips.TripsNavigationManager;
import org.croudtrip.trips.TripsUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing trips.
 */
@Path("/trips")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TripsResource {

    private static final String
            PATH_OFFERS = "/offers",
            PATH_JOINS = "/joins",
            PATH_ACCEPTED_JOINS = "/accepted",
            PATH_QUERIES = "/queries",
            PATH_RESERVATIONS = "/reservations";

    private final TripsManager tripsManager;
    private final TripsNavigationManager tripsNavigationManager;
    private final TripsUtils tripsUtils;
    private final VehicleManager vehicleManager;
    private final LogManager logManager;

    @Inject
    TripsResource(
            TripsManager tripsManager,
            TripsNavigationManager tripsNavigationManager,
            TripsUtils tripsUtils,
            VehicleManager vehicleManager,
            LogManager logManager) {

        this.tripsManager = tripsManager;
        this.tripsNavigationManager = tripsNavigationManager;
        this.tripsUtils = tripsUtils;
        this.vehicleManager = vehicleManager;
        this.logManager = logManager;
    }


    /**
     * Adds a new {@link TripOffer} which passengers can query for.
     * @return the newly created offer (which also includes the offer id!).
     */
    @POST
    @UnitOfWork
    @Path(PATH_OFFERS)
    public TripOffer addOffer(@Auth User driver, @Valid TripOfferDescription offerDescription) throws RouteNotFoundException {
        logManager.d("Add offer");
        Optional<Vehicle> vehicle = vehicleManager.findVehicleById(offerDescription.getVehicleId());
        if (!vehicle.isPresent() || vehicle.get().getOwner().getId() != driver.getId()) {
            throw RestUtils.createNotFoundException("not vehicle with id " + offerDescription.getVehicleId() + " found");
        }

        return tripsManager.addOffer(driver, offerDescription);
    }


    /**
     * Find an offer by id.
     */
    @GET
    @Path(PATH_OFFERS + "/{offerId}")
    @UnitOfWork
    public TripOffer getOffer(@PathParam("offerId") long offerId) {
        return assertIsValidOfferId(offerId);
    }

    /**
     * Get the complete route for an offer
     */
    @GET
    @Path(PATH_OFFERS + "/{offerId}/route")
    @UnitOfWork
    public Route getRouteForOffer(@PathParam("offerId") long offerId) throws RouteNotFoundException {
        TripOffer offer = assertIsValidOfferId( offerId );
        return tripsNavigationManager.getRouteForOffer( offer );
    }


    /**
     * Find all offers that belong to a particular driver.
     * @param showActiveAndNotFullOnly if false this method will return all offers that belong to a driver,
     *                                 otherwise only those which are active (not disable due to timeouts etc.)
     *                                 and are not full (passengers can still join). Default is true.
     */
    @GET
    @Path(PATH_OFFERS)
    @UnitOfWork
    public List<TripOffer> getOffers(@Auth User driver, @DefaultValue("true") @QueryParam("active") boolean showActiveAndNotFullOnly) {
        List<TripOffer> offers = new ArrayList<>(tripsManager.findOffersByDriver(driver));
        if (!showActiveAndNotFullOnly) return offers;

        // filter by active status
        Iterator<TripOffer> iterator = offers.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getStatus().equals(TripOfferStatus.ACTIVE_NOT_FULL)) {
                iterator.remove();
            }
        }

        return offers;
    }

    /**
     * Find all active offers that belong to a particular driver.
     * Even offers with a full car will be shown. Call this method if you want to get active running
     * offers of a driver.
     */
    @GET
    @Path("active_offers")
    @UnitOfWork
    public List<TripOffer> getActiveOffers(@Auth User driver ) {
        List<TripOffer> offers = new ArrayList<>(tripsManager.findOffersByDriver(driver));

        // filter by active status
        Iterator<TripOffer> iterator = offers.iterator();
        while (iterator.hasNext()) {
            TripOffer offer = iterator.next();
            if (!offer.getStatus().equals(TripOfferStatus.ACTIVE_NOT_FULL) &&
                !offer.getStatus().equals(TripOfferStatus.ACTIVE_FULL)) {
                iterator.remove();
            }
        }

        return offers;
    }


    /**
     * Allows drivers to update their offers, e.g. update the drivers current location (which changes
     * what passengers can join a trip).
     */
    @PUT
    @Path(PATH_OFFERS + "/{offerId}")
    @UnitOfWork
    public TripOffer updateOffer(@Auth User driver, @PathParam("offerId") long offerId, @Valid TripOfferUpdate offerUpdate) throws RouteNotFoundException {
        // find offer
        TripOffer offer = assertIsValidOfferId(offerId);
        if (offer.getDriver().getId() != driver.getId()) throw RestUtils.createUnauthorizedException();

        // check passenger status
        if (offerUpdate.getFinishOffer()) {
            if (tripsUtils.getActivePassengerCountForOffer(offer) > 0) {
                throw RestUtils.createJsonFormattedException("there are still passengers that need to be taken care of first!", 400);
            }

        } else if (offerUpdate.getCancelOffer()) {
            for (JoinTripRequest request : tripsManager.findAllJoinRequests(offerId)) {
                JoinTripStatus status = request.getStatus();
                if (status.equals(JoinTripStatus.PASSENGER_IN_CAR)) {
                    throw RestUtils.createJsonFormattedException("are you looking to the eject passenger button?", 400);
                }
            }
        }


        return tripsManager.updateOffer(offer, offerUpdate);
    }


    /**
     * Removes an offer. You should probably NOT use this method, please consider finishing or canceling
     * the trip instead.
     */
    @DELETE
    @UnitOfWork
    @Path(PATH_OFFERS + "/{offerId}")
    public void deleteOffer(@PathParam("offerId") long offerId) {
        tripsManager.deleteOffer(assertIsValidOfferId(offerId));
    }


    /**
     * Allows passengers to query for offers.
     * @return the result will EITHER contain a list of {@link TripReservation} which indicate that
     * trips have been found, OR a {@link RunningTripQuery} which means that a background search has
     * been started. In the latter case passengers will receive a GCM notification once an offer
     * has been found.
     */
    @POST
    @UnitOfWork
    @Path(PATH_QUERIES)
    public TripQueryResult queryOffers(@Auth User passenger, @Valid TripQueryDescription requestDescription) throws RouteNotFoundException {
        return tripsManager.queryOffers(passenger, requestDescription);
    }


    /**
     * Returns all running background queries for a passenger.
     * @param showOnlyRunning if true this method will return only running background queries but not
     *                        those which have been cancelled, finished, etc. Default is false.
     */
    @GET
    @UnitOfWork
    @Path(PATH_QUERIES)
    public List<RunningTripQuery> getQueries(@Auth User passenger, @DefaultValue("false") @QueryParam("running") boolean showOnlyRunning) {
        return tripsManager.getRunningQueries(passenger, showOnlyRunning);
    }


    /**
     * Returns a single running background query for a passenger.
     */
    @GET
    @UnitOfWork
    @Path(PATH_QUERIES + "/{queryId}")
    public RunningTripQuery getQuery(@Auth User passenger, @PathParam("queryId") long queryId) {
        Optional<RunningTripQuery> query = tripsManager.getRunningQuery(queryId);
        if (!query.isPresent()) throw RestUtils.createNotFoundException();
        if (query.get().getQuery().getPassenger().getId() != passenger.getId()) throw RestUtils.createUnauthorizedException();
        return query.get();
    }


    /**
     * Stops one particular background query for a passenger.
     */
    @DELETE
    @UnitOfWork
    @Path(PATH_QUERIES + "/{queryId}")
    public void deleteQuery(@Auth User passenger, @PathParam("queryId") long queryId) {
        tripsManager.deleteRunningQuery(getQuery(passenger, queryId));
    }


    /**
     * Returns all trip reservations ever made.
     */
    @GET
    @UnitOfWork
    @Path(PATH_RESERVATIONS)
    public List<TripReservation> getReservations() {
        return tripsManager.findAllReservations();
    }


    /**
     * Returns one trip price reservation.
     */
    @GET
    @UnitOfWork
    @Path(PATH_RESERVATIONS + "/{reservationId}")
    public TripReservation getReservation(@PathParam("reservationId") long reservationId) {
        Optional<TripReservation> reservation = tripsManager.findReservation(reservationId);
        if (!reservation.isPresent()) throw RestUtils.createNotFoundException();
        return reservation.get();
    }


    /**
     * Allows passengers to join a trip by "accepting" a previously created {@link TripReservation}.
     * @return the resulting join requests which indicates all further stages of a trip.
     */
    @PUT
    @UnitOfWork
    @Path(PATH_RESERVATIONS + "/{reservationId}")
    public JoinTripRequest joinTrip(@Auth User passenger, @PathParam("reservationId") long reservationId) {
        Optional<TripReservation> reservation = tripsManager.findReservation(reservationId);
        if (!reservation.isPresent()) throw RestUtils.createNotFoundException("reservation does not exist");

        Optional<JoinTripRequest> joinTripRequest = tripsManager.joinTrip(reservation.get());
        if (!joinTripRequest.isPresent()) throw RestUtils.createNotFoundException("offer does not exist");
        return joinTripRequest.get();
    }


    /**
     * Returns a list of join requests for either passengers or drivers.
     * @param showOnlyPassengerAccepted if true this method will only return those join requests with a
     *                                  status of {@link JoinTripStatus#PASSENGER_ACCEPTED}.
     *                                  Default is false.
     */
    @GET
    @UnitOfWork
    @Path(PATH_JOINS)
    public List<JoinTripRequest> getJoinRequests(@Auth User passengerOrDriver, @DefaultValue("false") @QueryParam("open") boolean showOnlyPassengerAccepted) {
        return tripsManager.findAllJoinRequests(passengerOrDriver, showOnlyPassengerAccepted);
    }


    /**
     * Returns a list of join requests for either passengers or drivers where the status of the join
     * request if {@link JoinTripStatus#DRIVER_ACCEPTED}.
     */
    @GET
    @UnitOfWork
    @Path(PATH_ACCEPTED_JOINS)
    public List<JoinTripRequest> getDriverAcceptedJoinRequests(@Auth User passengerOrDriver ) {
        return tripsManager.findDriverAcceptedJoinRequests(passengerOrDriver);
    }


    /**
     * Gets one particular join request.
     */
    @GET
    @UnitOfWork
    @Path(PATH_JOINS + "/{joinRequestId}")
    public JoinTripRequest getJoinRequest(@Auth User driverOrPassenger, @PathParam("joinRequestId") long joinRequestId) {
        Optional<JoinTripRequest> request = tripsManager.findJoinRequest(joinRequestId);
        if (!request.isPresent()) throw RestUtils.createNotFoundException();
        else return request.get();
    }


    /**
     * Updates a join request. Use this method if you want to advance the state of a join request,
     * e.g. passenger enters / leaves car.
     */
    @PUT
    @UnitOfWork
    @Path(PATH_JOINS + "/{joinRequestId}")
    public JoinTripRequest updateJoinRequest(
            @Auth User driverOrPassenger,
            @PathParam("joinRequestId") long joinRequestId,
            JoinTripRequestUpdate update) {

        Optional<JoinTripRequest> joinRequest = tripsManager.findJoinRequest(joinRequestId);
        if (!joinRequest.isPresent()) throw RestUtils.createNotFoundException();

        JoinTripStatus status = joinRequest.get().getStatus();
        switch(update.getType()) {
            case ACCEPT_PASSENGER:
            case DECLINE_PASSENGER:
                if (!status.equals(JoinTripStatus.PASSENGER_ACCEPTED))
                    throw RestUtils.createJsonFormattedException("status must be " + JoinTripStatus.PASSENGER_ACCEPTED, 409);
                if (driverOrPassenger.getId() != joinRequest.get().getOffer().getDriver().getId()) {
                    throw RestUtils.createJsonFormattedException("only driver can take this action", 400);
                }
                return tripsManager.updateJoinRequestAcceptance(joinRequest.get(), update.getType().equals(JoinTripRequestUpdateType.ACCEPT_PASSENGER));

            case ENTER_CAR:
                if (!status.equals(JoinTripStatus.DRIVER_ACCEPTED))
                    throw RestUtils.createJsonFormattedException("status must be " + JoinTripStatus.DRIVER_ACCEPTED, 409);
                assertUserIsPassenger(joinRequest.get(), driverOrPassenger);

                return tripsManager.updateJoinRequestPassengerEnterCar(joinRequest.get());

            case LEAVE_CAR:
                if (!status.equals(JoinTripStatus.PASSENGER_IN_CAR))
                    throw RestUtils.createJsonFormattedException("status must be " + JoinTripStatus.PASSENGER_IN_CAR, 409);
                assertUserIsPassenger(joinRequest.get(), driverOrPassenger);

                return tripsManager.updateJoinRequestPassengerExitCar(joinRequest.get());

            case CANCEL:
                if (status.equals(JoinTripStatus.PASSENGER_IN_CAR) || status.equals(JoinTripStatus.PASSENGER_AT_DESTINATION))
                    throw RestUtils.createJsonFormattedException("cannot cancel when in car or at destination", 409);
                assertUserIsPassenger(joinRequest.get(), driverOrPassenger);

                return tripsManager.updateJoinRequestPassengerCancel(joinRequest.get());
        }

        throw RestUtils.createJsonFormattedException("unknown update type " + update.getType(), 400);
    }


    private void assertUserIsPassenger(JoinTripRequest request, User user) {
        if (user.getId() != request.getQuery().getPassenger().getId()) {
            throw RestUtils.createJsonFormattedException("only passenger can take this action", 400);
        }
    }


    private TripOffer assertIsValidOfferId(long offerId) {
        Optional<TripOffer> offer = tripsManager.findOffer(offerId);
        if (offer.isPresent()) return offer.get();
        else throw RestUtils.createNotFoundException();
    }

}
