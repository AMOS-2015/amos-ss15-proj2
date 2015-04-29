package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.User;
import org.croudtrip.account.Vehicle;
import org.croudtrip.account.VehicleDescription;
import org.croudtrip.account.VehicleManager;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehicleResource {

    private final VehicleManager vehicleManager;

    @Inject
    VehicleResource(VehicleManager vehicleManager) {
        this.vehicleManager = vehicleManager;
    }


    @PUT
    @UnitOfWork
    public Vehicle setVehicle(@Auth User owner, @Valid VehicleDescription vehicleDescription) {
        return vehicleManager.setVehicle(owner, vehicleDescription);
    }


    @GET
    @UnitOfWork
    public Vehicle getVehicle(@Auth User owner) {
        return assertHasVehicle(owner);
    }


    @DELETE
    @UnitOfWork
    public void removeVehicle(@Auth User owner) {
        vehicleManager.deleteVehicle(assertHasVehicle(owner));
    }


    private Vehicle assertHasVehicle(User owner) {
        Optional<Vehicle> vehicle = vehicleManager.getVehicle(owner);
        if (vehicle.isPresent()) return vehicle.get();
        else throw RestUtils.createNotFoundException();
    }

}
