/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.UserManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;

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
        if (description.getEmail() == null || description.getPassword() == null
                || description.getFirstName() == null || description.getLastName() == null) {
            throw RestUtils.createJsonFormattedException("email, password, first name and last name may not be empty", 400);
        }
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
    public User updateUser(@Auth User user, UserDescription updatedUser) {
        // email must be unique
        Optional<User> oldUser = userManager.findUserByEmail(updatedUser.getEmail());
        if (oldUser.isPresent() && oldUser.get().getId() != user.getId()) {
            throw RestUtils.createJsonFormattedException("user with email " + updatedUser.getEmail() + " already registered", 400);
        }
        return userManager.updateUser(user, updatedUser);
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
