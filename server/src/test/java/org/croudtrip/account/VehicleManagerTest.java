package org.croudtrip.account;


import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;
import org.croudtrip.db.VehicleDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class VehicleManagerTest {

	@Mocked VehicleDAO vehicleDAO;

	private VehicleManager vehicleManager;
	private final User owner = new User(0, "owner", null, null, null, false, null, null, null, 0);
	private final VehicleDescription vehicleDescription = new VehicleDescription("plate", "color", "type", 5);


	@Before
	public void setupManager() {
		vehicleManager = new VehicleManager(vehicleDAO);
	}


	@Test
	public void testAddVehicle() {
		final Vehicle vehicle = vehicleManager.addVehicle(owner, vehicleDescription);
		assertEquals(vehicle, vehicleDescription);

		new Verifications() {{
			vehicleDAO.save(vehicle);
		}};
	}


	@Test
	public void testUpdateVehicle() {
		final Vehicle oldVehicle = new Vehicle(0, "", "", "", 1, owner);
		final Vehicle updatedVehicle = vehicleManager.updateVehicle(owner, oldVehicle, vehicleDescription);
		assertEquals(updatedVehicle, vehicleDescription);

		new Verifications() {{
			vehicleDAO.update(updatedVehicle);
		}};
	}


	@Test
	public void testFindAllVehicles() {
		vehicleManager.findAllVehicles(owner);
		new Verifications() {{
			vehicleDAO.findByUserId(owner.getId());
		}};
	}


	private void assertEquals(Vehicle vehicle, VehicleDescription vehicleDescription) {
		Assert.assertEquals(vehicle.getLicensePlate(), vehicleDescription.getLicensePlate());
		Assert.assertEquals(vehicle.getColor(), vehicleDescription.getColor());
		Assert.assertEquals(vehicle.getType(), vehicleDescription.getType());
		Assert.assertEquals(vehicle.getCapacity(), vehicleDescription.getCapacity());
	}

}