package org.croudtrip.rest;

import com.google.common.collect.Lists;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;

import org.croudtrip.directions.Leg;
import org.croudtrip.directions.Location;
import org.croudtrip.directions.Route;
import org.croudtrip.directions.RouteDistance;
import org.croudtrip.directions.RouteDuration;
import org.croudtrip.directions.Step;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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

    private final GeoApiContext geoApiContext;

    @Inject
    DirectionsResource(GeoApiContext geoApiContext) {
        this.geoApiContext = geoApiContext;
    }


    @GET
    @UnitOfWork
    public List<Route> getDirections() throws Exception {
        DirectionsRoute[] googleRoutes = DirectionsApi.getDirections(geoApiContext, "Nuremberg, DE", "Erlangen, DE").await();
        List<Route> resultRoutes = new ArrayList<>();

        for (DirectionsRoute googleRoute : googleRoutes) {
            resultRoutes.add(createRoute(googleRoute));
        }

        return resultRoutes;
    }


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
