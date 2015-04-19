package org.croudtrip.rest;

import org.croudtrip.auth.User;
import org.croudtrip.auth.UserDescription;
import org.croudtrip.auth.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resource for managing users.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserManager userManager;

    @Inject
    UserResource(UserManager userManager) {
        this.userManager = userManager;
    }


    @POST
    public User registerUser(UserDescription description) {
        if (userManager.findUserByEmail(description.getEmail()) != null) {
            throw RestUtils.createJsonFormattedException("email already registered", 409);
        }

        return userManager.addUser(description);
    }


    @GET
    @Path("/{userId}")
    public User getUser(@PathParam("userId") String userId) {
        return assertUserExists(userId);
    }


    @GET
    public List<User> getAllUsers() {
        return userManager.getAllUsers();
    }


    @DELETE
    @Path("/{userId}")
    public void removeUser(@PathParam("userId") String userId) {
        userManager.removeUser(assertUserExists(userId));
    }


    private User assertUserExists(String userId) {
        User user = userManager.getUser(userId);
        if (user == null) throw RestUtils.createNotFoundException();
        else return user;
    }
}
