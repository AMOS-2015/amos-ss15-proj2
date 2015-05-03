package org.croudtrip.rest;

import org.croudtrip.api.account.User;
import org.croudtrip.account.UserManager;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

/**
 * Part of the "regular" {@link UsersResource} class but separated to alllow
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsersHeadResource {

    private final UserManager userManager;

    @Inject
    UsersHeadResource(UserManager userManager) {
        this.userManager = userManager;
    }


    @HEAD
    @UnitOfWork
    @Path("/me")
    public Response getLastModified(@Auth User user) {
        return Response.status(200).header(HttpHeaders.LAST_MODIFIED, user.getLastModified()).build();
    }

}
