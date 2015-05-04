package org.croudtrip.rest;

import org.croudtrip.api.account.User;
import org.croudtrip.api.gcm.GcmRegistration;
import org.croudtrip.api.gcm.GcmRegistrationDescription;
import org.croudtrip.db.GcmRegistrationDAO;

import java.util.List;

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

    private final GcmRegistrationDAO gcmRegistrationDAO;

    @Inject
    GcmRegistrationResource(GcmRegistrationDAO gcmRegistrationDAO) {
        this.gcmRegistrationDAO = gcmRegistrationDAO;
    }


    @PUT
    @UnitOfWork
    public GcmRegistration register(@Auth User user, @Valid GcmRegistrationDescription description) {
        GcmRegistration registration = new GcmRegistration(0, description.getGcmId(), user);
        gcmRegistrationDAO.save(registration);
        return registration;
    }


    @GET
    @UnitOfWork
    public List<GcmRegistration> getRegistrations() {
        return gcmRegistrationDAO.findAll();
    }

}
