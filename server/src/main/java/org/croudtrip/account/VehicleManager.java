package org.croudtrip.account;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;
import org.croudtrip.db.VehicleDAO;

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


	public Vehicle setVehicle(User owner, VehicleDescription vehicleDescription) {
		Optional<Vehicle> vehicleOptional  = vehicleDAO.findByUserId(owner.getId());
		long vehicleId = 0;
		if (vehicleOptional.isPresent()) vehicleId = vehicleOptional.get().getId();

		Vehicle vehicle = new Vehicle(
				vehicleId,
				vehicleDescription.getLicensePlate(),
				vehicleDescription.getColor(),
				vehicleDescription.getType(),
				vehicleDescription.getCapacity(),
				owner);

		if (!vehicleOptional.isPresent()) vehicleDAO.save(vehicle);
		else vehicleDAO.update(vehicle);

		return vehicle;
	}


	public Optional<Vehicle> getVehicle(User owner) {
		return vehicleDAO.findByUserId(owner.getId());
	}


	public void deleteVehicle(Vehicle vehicle) {
		vehicleDAO.delete(vehicle);
	}

}
