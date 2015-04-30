package org.croudtrip.rest;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.NotFoundException;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;

import org.croudtrip.directions.Location;
import org.croudtrip.directions.RouteNavigation;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
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

    private final GeoApiContext geoApiContext;

    @Inject
    DirectionsResource(GeoApiContext geoApiContext) {
        this.geoApiContext = geoApiContext;
    }


    @GET
    @UnitOfWork
    public List<RouteNavigation> getDirections(
            @QueryParam("fromLat") double fromLat, @QueryParam("fromLng") double fromLng,
            @NotEmpty @QueryParam("toLat") double toLat, @NotEmpty @QueryParam("toLng") double toLng) throws Exception {

        List<RouteNavigation> result = new ArrayList<>();
        try {
            DirectionsRoute[] googleRoutes = DirectionsApi.newRequest(geoApiContext)
                    .origin(new LatLng(fromLat, fromLng))
                    .destination(new LatLng(toLat, toLng))
                    .await();

            for (DirectionsRoute googleRoute : googleRoutes) {
                result.add(createRoute(new Location(fromLat, fromLng), new Location(toLat, toLng), googleRoute));
            }
            return result;

        } catch (NotFoundException nfe) {
            throw RestUtils.createJsonFormattedException("location not found", 404);
        }
    }


    private RouteNavigation createRoute(Location startLocation, Location endLocation, DirectionsRoute googleRoute) {

        List<LatLng> points = new ArrayList<>();
        for (DirectionsLeg leg : googleRoute.legs) {
            for (DirectionsStep step : leg.steps) {
                points.addAll(step.polyline.decodePath());
            }
        }

        EncodedPolyline polyline = new EncodedPolyline(points);
        String warnings;
        if (googleRoute.warnings.length > 0) {
            boolean firstIter = true;
            warnings = "";
            for (String warning : googleRoute.warnings) {
                if (firstIter) {
                    warnings += "\n";
                    firstIter = false;
                }
                warnings += warning;
            }
        } else {
            warnings = null;
        }

        return new RouteNavigation(startLocation, endLocation, polyline.getEncodedPath(), googleRoute.copyrights, warnings);
    }


}
