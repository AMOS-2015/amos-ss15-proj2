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

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;

import java.util.Collections;
import java.util.Comparator;
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
    public Vehicle getVehicle(@Auth User owner, @PathParam("vehicleId") long vehicleId) {
        return assertValidVehicleId(owner, vehicleId);
    }


    @GET
    @UnitOfWork
    public List<Vehicle> getVehicles(@Auth User owner) {
        // sort by id
        List<Vehicle> vehicles = vehicleManager.findAllVehicles(owner);
        Collections.sort(vehicles, new Comparator<Vehicle>() {
            @Override
            public int compare(Vehicle v1, Vehicle v2) {
                return Long.valueOf(v1.getId()).compareTo(v2.getId());
            }
        });
        return vehicles;
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
