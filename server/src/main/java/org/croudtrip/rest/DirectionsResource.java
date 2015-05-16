package org.croudtrip.rest;

import org.croudtrip.api.directions.DirectionsRequest;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.directions.DirectionsManager;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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


    @POST
    @UnitOfWork
    public List<Route> getDirections(@Valid DirectionsRequest directionsRequest) throws Exception {
        if (directionsRequest.getWayPoints().size() < 2) throw RestUtils.createJsonFormattedException("must contain at least 2 way points", 400);
		LinkedList<RouteLocation> wayPoints = new LinkedList<>(directionsRequest.getWayPoints());
		RouteLocation start = wayPoints.remove(0);
		RouteLocation end = wayPoints.remove(wayPoints.size() - 1);
		return directionsManager.getDirections(start, end, wayPoints);
    }

}
