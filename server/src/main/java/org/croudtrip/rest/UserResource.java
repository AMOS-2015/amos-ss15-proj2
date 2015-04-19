package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.auth.User;
import org.croudtrip.auth.UserDescription;
import org.croudtrip.auth.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.dropwizard.hibernate.UnitOfWork;

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
    @UnitOfWork
    public User registerUser(@Valid UserDescription description) {
        if (userManager.findUserByEmail(description.getEmail()).isPresent()) {
            throw RestUtils.createJsonFormattedException("email already registered", 409);
        }

        return userManager.addUser(description);
    }


    @GET
    @Path("/{userId}")
    @UnitOfWork
    public User getUser(@PathParam("userId") long userId) {
        return assertUserExists(userId);
    }


    @GET
    @UnitOfWork
    public List<User> getAllUsers() {
        return userManager.getAllUsers();
    }


    @DELETE
    @Path("/{userId}")
    @UnitOfWork
    public void removeUser(@PathParam("userId") long userId) {
        userManager.deleteUser(assertUserExists(userId));
    }


    private User assertUserExists(long userId) {
        Optional<User> user = userManager.getUser(userId);
        if (user.isPresent()) return user.get();
        else throw RestUtils.createNotFoundException();
    }
}
