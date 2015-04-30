package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.User;
import org.croudtrip.trips.TripMatch;
import org.croudtrip.trips.TripOffer;
import org.croudtrip.trips.TripOfferDescription;
import org.croudtrip.trips.TripRequestDescription;
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

    private static final String
            PATH_OFFERS = "/offers",
            PATH_MATCHES = "/matches";

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
        return assertIsValidId(offerId);
    }


    @GET
    @Path(PATH_OFFERS)
    @UnitOfWork
    public List<TripOffer> getAllOffers() {
        return tripsManager.findAllOffers();
    }


    @DELETE
    @UnitOfWork
    @Path(PATH_OFFERS + "/{offerId}")
    public void deleteOff(@PathParam("offerId") long offerId) {
        tripsManager.deleteOffer(assertIsValidId(offerId));
    }


    @POST
    @UnitOfWork
    @Path(PATH_MATCHES)
    public List<TripMatch> findMatches(@Auth User passenger, @Valid TripRequestDescription requestDescription) {
        return tripsManager.findMatches(passenger, requestDescription);
    }


    private TripOffer assertIsValidId(long offerId) {
        Optional<TripOffer> offer = tripsManager.findOffer(offerId);
        if (offer.isPresent()) return offer.get();
        else throw RestUtils.createNotFoundException();
    }

}
