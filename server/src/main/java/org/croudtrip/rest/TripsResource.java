package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.trips.TripsManager;

import java.io.IOException;
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

    @Inject
    TripsResource(TripsManager tripsManager, VehicleManager vehicleManager) {
        this.tripsManager = tripsManager;
        this.vehicleManager = vehicleManager;
    }


    @POST
    @UnitOfWork
    @Path(PATH_OFFERS)
    public TripOffer addOffer(@Auth User user, @Valid TripOfferDescription offerDescription) throws Exception {
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
    public TripOffer updateOffer(@Auth User driver, @PathParam("offerId") long offerId, @Valid TripOfferUpdate offerUpdate) throws Exception {
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
    public TripQueryResult queryOffers(@Auth User passenger, @Valid TripQueryDescription requestDescription) throws Exception {
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
    public JoinTripRequest joinTrip(@PathParam("reservationId") long reservationId, @Auth User passenger) throws Exception {
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
            JoinTripRequestUpdate update) throws IOException {

        System.out.println("Update join request start");

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
