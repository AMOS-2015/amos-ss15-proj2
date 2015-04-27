package org.croudtrip.rest;

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

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.dropwizard.hibernate.UnitOfWork;

/**
 * Handles direction requests over the REST-API by the server
 * Created by Frederik Simon on 24.04.2015.
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
    public Route directions() { //TODO: add parameters to customize directions request
        DirectionsRoute[] routes = new DirectionsRoute[0];

        try {
            routes = DirectionsApi.getDirections(geoApiContext, "Nuremberg, DE", "Erlangen, DE").await();
            //System.out.println("ROUTE COMPUTED: " + routes.length + " " + routes[0].summary+ " " + routes[0].legs.length + " " + routes[0].waypointOrder.length);
        } catch (Exception e) {
            e.printStackTrace();
            return new Route(e.getMessage(), null, null, "ploy", "copy", null);
        }

        if( routes.length == 0)
            return new Route("Test", null, null, "ploy", "copy", null);

        Route[] resultRoutes = new Route[routes.length];

        // step through all routes, legs and steps and create JSON output for these information
        for( int i = 0; i < resultRoutes.length; ++i )
        {

            Leg[] legs = new Leg[routes[i].legs.length];
            for( int j = 0; j < legs.length; ++j ) {

                DirectionsLeg currentLeg = routes[i].legs[j];

                Step[] steps = new Step[currentLeg.steps.length];
                for( int k = 0; k < steps.length; ++k ) {
                    DirectionsStep currentStep = currentLeg.steps[k];

                    // every step has a distance, duration, startLocation and endLocation -> create objects for all of theses
                    RouteDistance distance = new RouteDistance( currentStep.distance.inMeters, currentStep.distance.humanReadable );
                    RouteDuration duration = new RouteDuration( currentStep.duration.inSeconds, currentStep.duration.humanReadable );
                    Location startLocation = new Location( currentStep.startLocation.lat, currentStep.startLocation.lng );
                    Location endLocation = new Location( currentStep.endLocation.lat, currentStep.endLocation.lng );
                    steps[k] = new Step( currentStep.htmlInstructions, distance, duration, startLocation, endLocation, currentStep.polyline.getEncodedPath() );
                }

                // create distance, duration, durationInTraffic, startLocation, endLocation for the current leg of the route
                RouteDistance distance = new RouteDistance( currentLeg.distance.inMeters, currentLeg.distance.humanReadable );

                RouteDuration duration = new RouteDuration( currentLeg.duration.inSeconds, currentLeg.duration.humanReadable );
                RouteDuration durationInTraffic = new RouteDuration( 0, "NO_TRAFFIC_INFORMATION" );
                if( currentLeg.durationInTraffic != null )
                     durationInTraffic = new RouteDuration( currentLeg.durationInTraffic.inSeconds, currentLeg.durationInTraffic.humanReadable );

                Location startLocation = new Location( currentLeg.startLocation.lat, currentLeg.startLocation.lng );
                Location endLocation = new Location( currentLeg.endLocation.lat, currentLeg.endLocation.lng );
                legs[j] = new Leg( steps, distance, duration, durationInTraffic, startLocation, endLocation, currentLeg.startAddress, currentLeg.endAddress );

            }

            DirectionsRoute route = routes[i];
            resultRoutes[i] = new Route(route.summary, legs, route.waypointOrder, route.overviewPolyline.getEncodedPath(), route.copyrights, route.warnings);
        }

        return resultRoutes[0];
    }

}
