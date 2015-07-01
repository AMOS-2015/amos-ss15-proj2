package org.croudtrip;


import org.croudtrip.api.TripsResource;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.account.VehicleDescription;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class SuperTripsTest {

	private ApiFactory apiFactory = new ApiFactory();

	@Test
	public void testAddUser() {
		// register user
		UserDescription user = new UserDescription(UUID.randomUUID().toString() + "@foobar.de", "foo", "bar", "foobar");
		apiFactory.getUsersResource().registerUserSynchronously(user);
		apiFactory.setUser(user);

		// add vehicle
		Vehicle vehicle = apiFactory.getVehicleResource().addVehicleSynchronously(new VehicleDescription("foo", "bar", "foobar", 4));

		// add offers
		TripsResource tripsResource = apiFactory.getTripsResource();

		// Nuernberg -> Leipzig
		tripsResource.addOfferSynchronously(new TripOfferDescription(
				new RouteLocation(49.4360936, 11.1011232),
				new RouteLocation(51.3417825 ,12.3936349),
				25000,
				42,
				vehicle.getId()));

		// Hannover -> Dresden
		tripsResource.addOfferSynchronously(new TripOfferDescription(
				new RouteLocation(52.3796664, 9.7614715),
				new RouteLocation(51.0768337, 13.7725857),
				25000,
				42,
				vehicle.getId()));

		// Berlin -> Prag
		tripsResource.addOfferSynchronously(new TripOfferDescription(
				new RouteLocation(52.5075419, 13.4251364),
				new RouteLocation(50.0596696, 14.4656239),
				25000,
				42,
				vehicle.getId()));

		// query offers
		TripQueryResult queryResult = tripsResource.queryOffersSynchronously(new TripQueryDescription(
				new RouteLocation(49.4360936, 11.1011232),
				new RouteLocation(50.0596696, 14.4656239),
				60 * 60));

		Assert.assertNotNull(queryResult.getReservations());
		Assert.assertFalse(queryResult.getReservations().isEmpty());
		Assert.assertEquals(3, queryResult.getReservations().get(0).getReservations().size());
	}

}
