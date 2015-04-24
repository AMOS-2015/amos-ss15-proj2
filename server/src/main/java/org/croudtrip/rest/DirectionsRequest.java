package org.croudtrip.rest;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsRoute;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.dropwizard.hibernate.UnitOfWork;

/**
 * Handles direction requests over the REST-API by the server
 * Created by Frederik Simon on 24.04.2015.
 */
@Path("/directions")
public class DirectionsRequest {

    @GET
    @UnitOfWork
    public void directions() { //TODO: add parameters to customize directions request

        //TODO: Insert API-Key (already created, but not commited.
        GeoApiContext context = new GeoApiContext().setApiKey("");
        try {
            DirectionsRoute[] routes = DirectionsApi.getDirections(context, "Nuremberg, DE", "Erlangen, DE").await();
            System.out.println(routes[0].summary);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO: Return some result
    }

}
