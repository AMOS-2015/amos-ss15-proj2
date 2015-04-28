package org.croudtrip.rest;

import com.google.common.collect.Lists;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.NotFoundException;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.LatLng;

import org.croudtrip.directions.Leg;
import org.croudtrip.directions.Location;
import org.croudtrip.directions.Route;
import org.croudtrip.directions.RouteDistance;
import org.croudtrip.directions.RouteDuration;
import org.croudtrip.directions.Step;
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
    public List<Route> getDirections(
            @QueryParam("from") String fromLocation,
            @NotEmpty @QueryParam("to") String toLocation) throws Exception {

        List<Route> resultRoutes = new ArrayList<>();
        try {
            DirectionsRoute[] googleRoutes = DirectionsApi.getDirections(
                    geoApiContext,
                    assertValidLocationParam("from", fromLocation),
                    assertValidLocationParam("to", toLocation))
                    .await();

            for (DirectionsRoute googleRoute : googleRoutes) {
                resultRoutes.add(createRoute(googleRoute));
            }
            return resultRoutes;
        } catch (NotFoundException nfe) {
            throw RestUtils.createJsonFormattedException("location not found", 404);
        }
    }

    @GET
    @Path("/loc")
    @UnitOfWork
    public List<Route> getDirections(
            @QueryParam("fromLat") double fromLat, @QueryParam("fromLng") double fromLng,
            @NotEmpty @QueryParam("toLat") double toLat, @NotEmpty @QueryParam("toLng") double toLng) throws Exception {

        List<Route> resultRoutes = new ArrayList<>();
        try {
            DirectionsRoute[] googleRoutes = DirectionsApi.newRequest(geoApiContext)
                                                          .origin( new LatLng( fromLat, fromLng ))
                                                          .destination( new LatLng( toLat, toLng ))
                                                          .await();

            for (DirectionsRoute googleRoute : googleRoutes) {
                resultRoutes.add(createRoute(googleRoute));
            }
            return resultRoutes;
        } catch (NotFoundException nfe) {
            throw RestUtils.createJsonFormattedException("location not found", 404);
        }
    }


    private String assertValidLocationParam(String paramName, String location) {
        if (location == null || location.length() == 0) {
            throw RestUtils.createJsonFormattedException("query param \"" + paramName + "\" cannot be emtpy", 400);
        }
        return location;
    }

    private LatLng assertValidLocationParam(String paramName, Location location) {
        if ( location == null ) {
            throw RestUtils.createJsonFormattedException("query param \"" + paramName + "\" cannot be emtpy", 400);
        }
        return new LatLng( location.getLat(), location.getLng() );
    }

    /**
     * Creates a Route that can be exported to JSON and is readable
     * for the client from the given Route coming from the Google Directions server.
     * @param googleRoute the route that is downloaded from the google server
     * @return an own JSON convertible route
     */
    private Route createRoute(DirectionsRoute googleRoute) {
        List<Leg> legs = new ArrayList<>();
        for (DirectionsLeg googleLeg : googleRoute.legs) {

            List<Step> steps = new ArrayList<>();
            for (DirectionsStep googleStep : googleLeg.steps) {

                // every step has a distance, duration, startLocation and endLocation -> create objects for all of theses
                RouteDistance distance = new RouteDistance(googleStep.distance.inMeters, googleStep.distance.humanReadable);
                RouteDuration duration = new RouteDuration(googleStep.duration.inSeconds, googleStep.duration.humanReadable);
                Location startLocation = new Location(googleStep.startLocation.lat, googleStep.startLocation.lng);
                Location endLocation = new Location(googleStep.endLocation.lat, googleStep.endLocation.lng);
                steps.add(new Step(googleStep.htmlInstructions, distance, duration, startLocation, endLocation, googleStep.polyline.getEncodedPath()));
            }

            // create distance, duration, durationInTraffic, startLocation, endLocation for the current leg of the route
            RouteDistance distance = new RouteDistance(googleLeg.distance.inMeters, googleLeg.distance.humanReadable);

            RouteDuration duration = new RouteDuration(googleLeg.duration.inSeconds, googleLeg.duration.humanReadable);
            RouteDuration durationInTraffic = new RouteDuration(0, "NO_TRAFFIC_INFORMATION");
            if (googleLeg.durationInTraffic != null)
                durationInTraffic = new RouteDuration(googleLeg.durationInTraffic.inSeconds, googleLeg.durationInTraffic.humanReadable);

            Location startLocation = new Location(googleLeg.startLocation.lat, googleLeg.startLocation.lng);
            Location endLocation = new Location(googleLeg.endLocation.lat, googleLeg.endLocation.lng);
            legs.add(new Leg(steps, distance, duration, durationInTraffic, startLocation, endLocation, googleLeg.startAddress, googleLeg.endAddress));
        }

        List<Integer> wayPointOrder = new ArrayList<>();
        for (int order : googleRoute.waypointOrder) wayPointOrder.add(order);

        return new Route(
                googleRoute.summary,
                legs,
                wayPointOrder,
                googleRoute.overviewPolyline.getEncodedPath(),
                googleRoute.copyrights,
                Lists.newArrayList(googleRoute.warnings));
    }


}
