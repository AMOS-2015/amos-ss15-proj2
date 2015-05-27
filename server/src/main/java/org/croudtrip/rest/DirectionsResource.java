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
