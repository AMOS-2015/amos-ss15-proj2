package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.gcm.GcmRegistration;
import org.croudtrip.api.gcm.GcmRegistrationDescription;
import org.croudtrip.gcm.GcmManager;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing user vehicles.
 */
@Path("/gcm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GcmRegistrationResource {

    private final GcmManager gcmManager;

    @Inject
    GcmRegistrationResource(GcmManager gcmManager) {
        this.gcmManager = gcmManager;
    }


    @PUT
    @UnitOfWork
    public GcmRegistration register(@Auth User user, @Valid GcmRegistrationDescription description) {
        return gcmManager.register(user, description);
    }


    @GET
    @UnitOfWork
    public GcmRegistration getRegistration(@Auth User user) {
        Optional<GcmRegistration> registration = gcmManager.findRegistrationByUser(user);
        if (registration.isPresent()) return registration.get();
        else throw RestUtils.createNotFoundException();
    }

}
