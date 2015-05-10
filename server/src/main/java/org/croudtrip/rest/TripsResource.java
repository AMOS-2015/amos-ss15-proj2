package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.trips.TripsManager;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
            PATH_JOINS = "/offers/{offerId}/joins",
            PATH_QUERIES = "/queries",
            PATH_RESERVATIONS = "/reservations";

    private final TripsManager tripsManager;

    @Inject
    TripsResource(TripsManager tripsManager) {
        this.tripsManager = tripsManager;
    }


    @POST
    @UnitOfWork
    @Path(PATH_OFFERS)
    public TripOffer addOffer(@Auth User user, @Valid TripOfferDescription offerDescription) throws Exception {
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
    public List<TripOffer> getOffers(@Auth User driver) {
        return tripsManager.findOffersByDriver(driver);
    }


    @DELETE
    @UnitOfWork
    @Path(PATH_OFFERS + "/{offerId}")
    public void deleteOff(@PathParam("offerId") long offerId) {
        tripsManager.deleteOffer(assertIsValidOfferId(offerId));
    }


    @POST
    @UnitOfWork
    @Path(PATH_QUERIES)
    public List<TripReservation> queryOffers(@Auth User passenger, @Valid TripQueryDescription requestDescription) throws Exception {
        return tripsManager.createReservations(passenger, requestDescription);
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
    public JoinTripRequest joinTrip(@PathParam("reservationId") long reservationId, @Auth User passenger) throws IOException {
        Optional<TripReservation> reservation = tripsManager.findReservation(reservationId);
        if (!reservation.isPresent()) throw RestUtils.createNotFoundException("reservation does not exist");

        Optional<JoinTripRequest> joinTripRequest = tripsManager.joinTrip(reservation.get());
        if (!joinTripRequest.isPresent()) throw RestUtils.createNotFoundException("offer does not exist");
        return joinTripRequest.get();
    }


    @GET
    @UnitOfWork
    @Path(PATH_JOINS)
    public List<JoinTripRequest> getJoinRequests(@Auth User driver, @PathParam("offerId") long offerId) {
        TripOffer offer = assertIsValidOfferId(offerId);
        return tripsManager.findAllJoinRequests(offer);
    }


    @GET
    @UnitOfWork
    @Path(PATH_JOINS + "/{joinRequestId}")
    public JoinTripRequest getJoinRequest(@Auth User driver, @PathParam("offerId") long offerId, @PathParam("joinRequestId") long joinRequestId) {
        assertIsValidOfferId(offerId);
        Optional<JoinTripRequest> request = tripsManager.findJoinRequest(joinRequestId);
        if (!request.isPresent()) throw RestUtils.createNotFoundException();
        else return request.get();
    }


    @PUT
    @UnitOfWork
    @Path(PATH_JOINS + "/{joinRequestId}")
    public JoinTripRequest updateJoinRequest(
            @Auth User driver,
            @PathParam("offerId") long offerId,
            @PathParam("joinRequestId") long joinRequestId,
            JoinTripRequestUpdate update) throws IOException {

        System.out.println("Update join request start");

        assertIsValidOfferId(offerId);
        Optional<JoinTripRequest> joinRequest = tripsManager.findJoinRequest(joinRequestId);
        if (!joinRequest.isPresent()) throw RestUtils.createNotFoundException();
        if (!joinRequest.get().getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED)) {
            throw RestUtils.createJsonFormattedException("driver action already taken", 409);
        }

        return tripsManager.updateJoinRequest(joinRequest.get(), update.getAcceptPassenger());
    }


    private TripOffer assertIsValidOfferId(long offerId) {
        Optional<TripOffer> offer = tripsManager.findOffer(offerId);
        if (offer.isPresent()) return offer.get();
        else throw RestUtils.createNotFoundException();
    }

}
