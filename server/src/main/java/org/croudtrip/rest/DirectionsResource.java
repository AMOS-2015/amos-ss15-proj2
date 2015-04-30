package org.croudtrip.rest;

import com.google.maps.errors.NotFoundException;

import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteLocation;
import org.croudtrip.directions.Route;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.dropwizard.hibernate.UnitOfWork;

/**
 * Handles direction requests over the REST-API by the server
 */
@Path("/directions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DirectionsResource {

    private final DirectionsManager directionsManager;

    @Inject
    DirectionsResource(DirectionsManager directionsManager) {
        this.directionsManager = directionsManager;
    }


    @GET
    @UnitOfWork
    public List<Route> getDirections(
            @QueryParam("fromLat") double fromLat, @QueryParam("fromLng") double fromLng,
            @NotEmpty @QueryParam("toLat") double toLat, @NotEmpty @QueryParam("toLng") double toLng) throws Exception {

        try {
            return directionsManager.getDirections(new RouteLocation(fromLat, fromLng), new RouteLocation(toLat, toLng));

        } catch (NotFoundException nfe) {
            throw RestUtils.createJsonFormattedException("location not found", 404);
        }
    }

}
