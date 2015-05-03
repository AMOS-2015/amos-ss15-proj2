package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.account.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing users.
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsersResource {

    private final UserManager userManager;

    @Inject
    UsersResource(UserManager userManager) {
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
    @Path("/me")
    @UnitOfWork
    public User getUser(@Auth User user) {
        return user;
    }


    @GET
    @UnitOfWork
    public List<User> getAllUsers() {
        return userManager.findAllUsers();
    }


    @PUT
    @Path("/me")
    @UnitOfWork
    public User updateUser(@Auth User user, User updatedUser) {
        if (user.getId() != updatedUser.getId()) {
            throw RestUtils.createUnauthorizedException();
        }
        if (!user.getEmail().equals(updatedUser.getEmail())) {
            throw RestUtils.createJsonFormattedException("cannot change email", 400);
        }
        return userManager.updateUser(updatedUser);
    }


    @DELETE
    @Path("/{userId}")
    @UnitOfWork
    public void removeUser(@Auth User user, @PathParam("userId") long userId) {
        if (user.getId() != userId) throw RestUtils.createUnauthorizedException();
        userManager.deleteUser(assertUserExists(userId));
    }


    private User assertUserExists(long userId) {
        Optional<User> user = userManager.findUserById(userId);
        if (user.isPresent()) return user.get();
        else throw RestUtils.createNotFoundException();
    }
}
