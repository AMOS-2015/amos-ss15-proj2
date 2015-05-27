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
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.gcm.GcmRegistration;
import org.croudtrip.api.gcm.GcmRegistrationDescription;
import org.croudtrip.gcm.GcmManager;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing GCM registrations (and messages for testing).
 */
@Path("/gcm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GcmRegistrationResource {

    private final GcmManager gcmManager;
    private final UserManager userManager;

    @Inject
    GcmRegistrationResource(GcmManager gcmManager, UserManager userManager) {
        this.gcmManager = gcmManager;
        this.userManager = userManager;
    }


    @PUT
    @UnitOfWork
    public GcmRegistration register(@Auth User user, @Valid GcmRegistrationDescription description) {
        return gcmManager.register(user, description);
    }


    @DELETE
    @UnitOfWork
    public void unregister(@Auth User user) {
        Optional<GcmRegistration> registration = gcmManager.findRegistrationByUser(user);
        if (!registration.isPresent()) throw RestUtils.createNotFoundException();
        gcmManager.unregister(registration.get());
    }


    @GET
    @UnitOfWork
    public GcmRegistration getRegistration(@Auth User user) {
        Optional<GcmRegistration> registration = gcmManager.findRegistrationByUser(user);
        if (registration.isPresent()) return registration.get();
        else throw RestUtils.createNotFoundException();
    }


    @POST
    @UnitOfWork
    public void sendTestMessage(@QueryParam("userId") Long userId, @DefaultValue("hello") @QueryParam("msg") String message) throws IOException {
        if (userId == null) throw RestUtils.createJsonFormattedException("missing query param \"userId\"", 400);

        Optional<User> user = userManager.findUserById(userId);
        if (!user.isPresent()) throw RestUtils.createNotFoundException();
        Optional<GcmRegistration> registration = gcmManager.findRegistrationByUser(user.get());
        if (!registration.isPresent()) throw RestUtils.createNotFoundException();

        gcmManager.sendGcmMessageToUser(user.get(), GcmConstants.GCM_MSG_DUMMY);
    }

}
