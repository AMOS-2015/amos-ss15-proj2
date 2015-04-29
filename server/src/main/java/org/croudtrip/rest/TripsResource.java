package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.trips.TripOffer;
import org.croudtrip.trips.TripOfferDescription;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing users.
 */
@Path("/trips")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TripsResource {

    private final TripOfferDAO tripOfferDAO;

    @Inject
    TripsResource(TripOfferDAO tripOfferDAO) {
        this.tripOfferDAO = tripOfferDAO;
    }


    @POST
    @UnitOfWork
    public TripOffer addOffer(@Valid TripOfferDescription offerDescription) {
        TripOffer offer = new TripOffer(0, offerDescription.getStart(), offerDescription.getEnd(), offerDescription.getMaxDiversionInKm());
        tripOfferDAO.save(offer);
        return offer;
    }


    @GET
    @Path("/{offerId}")
    @UnitOfWork
    public TripOffer getOffer(@PathParam("offerId") long offerId) {
        Optional<TripOffer> offer = tripOfferDAO.findById(offerId);
        if (offer.isPresent()) return offer.get();
        else throw RestUtils.createNotFoundException();
    }


    @GET
    @UnitOfWork
    public List<TripOffer> getAllOffers() {
        return tripOfferDAO.findAll();
    }

}
