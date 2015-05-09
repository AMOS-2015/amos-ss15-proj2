package org.croudtrip.rest;

import com.google.common.base.Optional;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;

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


    @POST
    @UnitOfWork
    public Vehicle addVehicle(@Auth User owner, @Valid VehicleDescription vehicleDescription) {
        return vehicleManager.addVehicle(owner, vehicleDescription);
    }


    @GET
    @UnitOfWork
    @Path("/{vehicleId}")
    public Optional<Vehicle> getVehicle(@Auth User owner, @PathParam("vehicleId") long vehicleId) {
        return vehicleManager.findVehicleById(vehicleId);
    }


    @GET
    @UnitOfWork
    public List<Vehicle> getVehicles(@Auth User owner) {
        return vehicleManager.findAllVehicles(owner);
    }


    @DELETE
    @UnitOfWork
    @Path("/{vehicleId}")
    public void removeVehicle(@Auth User owner, @PathParam("vehicleId") long vehicleId) {
        vehicleManager.deleteVehicle(assertValidVehicleId(owner, vehicleId));
    }


    @PUT
    @UnitOfWork
    @Path("/{vehicleId}")
    public Vehicle updateVehicle(@Auth User owner, @PathParam("vehicleId") long vehicleId, VehicleDescription description) {
        return vehicleManager.updateVehicle(owner, assertValidVehicleId(owner, vehicleId), description);
    }


    private Vehicle assertValidVehicleId(User owner, long vehicleId) {
        Optional<Vehicle> vehicle = vehicleManager.findVehicleById(vehicleId);
        if (!vehicle.isPresent()) RestUtils.createNotFoundException();
        if (vehicle.get().getOwner().getId() != owner.getId()) throw RestUtils.createUnauthorizedException();
        return vehicle.get();
    }

}
