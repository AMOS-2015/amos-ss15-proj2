package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.User;
import org.croudtrip.trips.TripOffer;
import org.croudtrip.trips.TripOfferDescription;
import org.croudtrip.trips.TripsManager;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

    private final TripsManager tripsManager;

    @Inject
    TripsResource(TripsManager tripsManager) {
        this.tripsManager = tripsManager;
    }


    @POST
    @UnitOfWork
    public TripOffer addOffer(@Auth User user, @Valid TripOfferDescription offerDescription) {
        return tripsManager.addOffer(user, offerDescription);
    }


    @GET
    @Path("/{offerId}")
    @UnitOfWork
    public TripOffer getOffer(@PathParam("offerId") long offerId) {
        return assertIsValidId(offerId);
    }


    @GET
    @UnitOfWork
    public List<TripOffer> getAllOffers() {
        return tripsManager.findAllOffers();
    }


    @DELETE
    @UnitOfWork
    @Path("/{offerId}")
    public void deleteOff(@PathParam("offerId") long offerId) {
        tripsManager.deleteOffer(assertIsValidId(offerId));
    }


    private TripOffer assertIsValidId(long offerId) {
        Optional<TripOffer> offer = tripsManager.findOffer(offerId);
        if (offer.isPresent()) return offer.get();
        else throw RestUtils.createNotFoundException();
    }

}
