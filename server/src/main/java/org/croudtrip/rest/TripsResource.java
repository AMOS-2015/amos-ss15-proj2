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
    private final VehicleManager vehicleManager;
    private final LogManager logManager;

    @Inject
    TripsResource(TripsManager tripsManager, VehicleManager vehicleManager, LogManager logManager) {
        this.tripsManager = tripsManager;
        this.vehicleManager = vehicleManager;
        this.logManager = logManager;
    }


    @POST
    @UnitOfWork
    @Path(PATH_OFFERS)
    public TripOffer addOffer(@Auth User user, @Valid TripOfferDescription offerDescription) throws RouteNotFoundException {
        logManager.d("Add offer");
        Optional<Vehicle> vehicle = vehicleManager.findVehicleById(offerDescription.getVehicleId());
        if (!vehicle.isPresent() || vehicle.get().getOwner().getId() != user.getId()) {
            throw RestUtils.createNotFoundException("not vehicle with id " + offerDescription.getVehicleId() + " found");
        }

        return tripsManager.addOffer(user, offerDescription);
    }


    @GET
    @Path(PATH_OFFERS + "/{offerId}")
    @UnitOfWork
    public TripOffer getOffer(@PathParam("offerId") long offerId) {
        return assertIsValidOfferId(offerId);
    }


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


    @PUT
    @Path(PATH_OFFERS + "/{offerId}")
    @UnitOfWork
    public TripOffer updateOffer(@Auth User driver, @PathParam("offerId") long offerId, @Valid TripOfferUpdate offerUpdate) throws RouteNotFoundException {
        TripOffer offer = assertIsValidOfferId(offerId);
        if (offer.getDriver().getId() != driver.getId()) throw RestUtils.createUnauthorizedException();
        return tripsManager.updateOffer(offer, offerUpdate);
    }


    @DELETE
    @UnitOfWork
    @Path(PATH_OFFERS + "/{offerId}")
    public void deleteOffer(@PathParam("offerId") long offerId) {
        tripsManager.deleteOffer(assertIsValidOfferId(offerId));
    }


    @POST
    @UnitOfWork
    @Path(PATH_QUERIES)
    public TripQueryResult queryOffers(@Auth User passenger, @Valid TripQueryDescription requestDescription) throws RouteNotFoundException {
        return tripsManager.queryOffers(passenger, requestDescription);
    }


    @GET
    @UnitOfWork
    @Path(PATH_QUERIES)
    public List<RunningTripQuery> getQueries(@Auth User passenger, @DefaultValue("false") @QueryParam("running") boolean showOnlyRunning) {
        return tripsManager.getRunningQueries(passenger, showOnlyRunning);
    }


    @GET
    @UnitOfWork
    @Path(PATH_QUERIES + "/{queryId}")
    public RunningTripQuery getQuery(@Auth User passenger, @PathParam("queryId") long queryId) {
        Optional<RunningTripQuery> query = tripsManager.getRunningQuery(queryId);
        if (!query.isPresent()) throw RestUtils.createNotFoundException();
        if (query.get().getQuery().getPassenger().getId() != passenger.getId()) throw RestUtils.createUnauthorizedException();
        return query.get();
    }


    @DELETE
    @UnitOfWork
    @Path(PATH_QUERIES + "/{queryId}")
    public void deleteQuery(@Auth User passenger, @PathParam("queryId") long queryId) {
        tripsManager.deleteRunningQuery(getQuery(passenger, queryId));
    }


    @GET
    @UnitOfWork
    @Path(PATH_RESERVATIONS)
    public List<TripReservation> getReservations() {
        return tripsManager.findAllReservations();
    }


    @GET
    @UnitOfWork
    @Path(PATH_RESERVATIONS + "/{reservationId}")
    public TripReservation getReservation(@PathParam("reservationId") long reservationId) {
        Optional<TripReservation> reservation = tripsManager.findReservation(reservationId);
        if (!reservation.isPresent()) throw RestUtils.createNotFoundException();
        return reservation.get();
    }


    @PUT
    @UnitOfWork
    @Path(PATH_RESERVATIONS + "/{reservationId}")
    public JoinTripRequest joinTrip(@PathParam("reservationId") long reservationId, @Auth User passenger) {
        Optional<TripReservation> reservation = tripsManager.findReservation(reservationId);
        if (!reservation.isPresent()) throw RestUtils.createNotFoundException("reservation does not exist");

        Optional<JoinTripRequest> joinTripRequest = tripsManager.joinTrip(reservation.get());
        if (!joinTripRequest.isPresent()) throw RestUtils.createNotFoundException("offer does not exist");
        return joinTripRequest.get();
    }


    @GET
    @UnitOfWork
    @Path(PATH_JOINS)
    public List<JoinTripRequest> getJoinRequests(@Auth User passengerOrDriver, @DefaultValue("false") @QueryParam("open") boolean showOnlyPassengerAccepted) {
        return tripsManager.findAllJoinRequests(passengerOrDriver, showOnlyPassengerAccepted);
    }

    @GET
    @UnitOfWork
    @Path(PATH_ACCEPTED_JOINS)
    public List<JoinTripRequest> getDriverAcceptedJoinRequests(@Auth User passengerOrDriver ) {
        return tripsManager.findDriverAcceptedJoinRequests(passengerOrDriver);
    }

    @GET
    @UnitOfWork
    @Path(PATH_JOINS + "/{joinRequestId}")
    public JoinTripRequest getJoinRequest(@Auth User driver, @PathParam("joinRequestId") long joinRequestId) {
        Optional<JoinTripRequest> request = tripsManager.findJoinRequest(joinRequestId);
        if (!request.isPresent()) throw RestUtils.createNotFoundException();
        else return request.get();
    }


    @PUT
    @UnitOfWork
    @Path(PATH_JOINS + "/{joinRequestId}")
    public JoinTripRequest updateJoinRequest(
            @Auth User driver,
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
                return tripsManager.updateJoinRequestAcceptance(joinRequest.get(), update.getType().equals(JoinTripRequestUpdateType.ACCEPT_PASSENGER));

            case ENTER_CAR:
                if (!status.equals(JoinTripStatus.DRIVER_ACCEPTED))
                    throw RestUtils.createJsonFormattedException("status must be " + JoinTripStatus.DRIVER_ACCEPTED, 409);
                return tripsManager.updateJoinRequestPassengerEnterCar(joinRequest.get());

            case LEAVE_CAR:
                if (!status.equals(JoinTripStatus.PASSENGER_IN_CAR))
                    throw RestUtils.createJsonFormattedException("status must be " + JoinTripStatus.PASSENGER_IN_CAR, 409);
                return tripsManager.updateJoinRequestPassengerExitCar(joinRequest.get());
        }

        throw RestUtils.createJsonFormattedException("unknown update type " + update.getType(), 400);

    }


    private TripOffer assertIsValidOfferId(long offerId) {
        Optional<TripOffer> offer = tripsManager.findOffer(offerId);
        if (offer.isPresent()) return offer.get();
        else throw RestUtils.createNotFoundException();
    }

}
