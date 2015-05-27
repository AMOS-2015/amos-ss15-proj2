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

package org.croudtrip.account;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;
import org.croudtrip.db.VehicleDAO;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates, stores and finds registered users.
 */
@Singleton
public class VehicleManager {

	private final VehicleDAO vehicleDAO;


	@Inject
	VehicleManager(VehicleDAO vehicleDAO) {
		this.vehicleDAO = vehicleDAO;
	}


	public Vehicle addVehicle(User owner, VehicleDescription vehicleDescription) {
		Vehicle vehicle = new Vehicle(
				0,
				vehicleDescription.getLicensePlate(),
				vehicleDescription.getColor(),
				vehicleDescription.getType(),
				vehicleDescription.getCapacity(),
				owner);

		vehicleDAO.save(vehicle);
		return vehicle;
	}


	public Vehicle updateVehicle(User owner, Vehicle oldVehicle, VehicleDescription newVehicle) {
		Vehicle updatedVehicle = new Vehicle(
				oldVehicle.getId(),
				getNonNull(newVehicle.getLicensePlate(), oldVehicle.getLicensePlate()),
				getNonNull(newVehicle.getColor(), oldVehicle.getColor()),
				getNonNull(newVehicle.getType(), oldVehicle.getType()),
				getNonNull(newVehicle.getCapacity(), oldVehicle.getCapacity()),
				owner);

		vehicleDAO.update(updatedVehicle);
		return updatedVehicle;
	}


	public Optional<Vehicle> findVehicleById(long vehicleId) {
		return vehicleDAO.findById(vehicleId);
	}


	public List<Vehicle> findAllVehicles(User owner) {
		return vehicleDAO.findByUserId(owner.getId());
	}


	public void deleteVehicle(Vehicle vehicle) {
		vehicleDAO.delete(vehicle);
	}


	private <T> T getNonNull(T value1, T value2) {
		if (value1 == null) return value2;
		else return value1;
	}
}
