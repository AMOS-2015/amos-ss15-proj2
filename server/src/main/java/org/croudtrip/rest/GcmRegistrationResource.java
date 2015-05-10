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
