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
