package org.croudtrip.rest;

import org.croudtrip.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Class that represents the user resource within our REST-API.
 * Created by Frederik Simon on 17.04.2015.
 */
@Path("/registerUser")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterUserResource {

    @POST
    public void registerUser( User user ) {

        System.out.println( user.getFirstName() + " " + user.getLastName() + " registered." );

    }
}
