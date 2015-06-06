package org.croudtrip.trips;


import com.google.common.collect.Lists;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.logs.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class TripsMatcherTest {

	private static final User
			passenger = new User(0, null, null, null, null, null, null, null, null, 0),
			driver = new User(1, null, null, null, null, null, null, null, null, 0);

	private static final TripQuery query = new TripQuery(
			null,
			new RouteLocation(45, 45),
			new RouteLocation(50, 50),
			10,
			passenger);

	private static final Vehicle vehicle = new Vehicle(0, null, null, null, 4, driver);
	private static final Route driverRoute = new Route.Builder()
			.wayPoints(Lists.newArrayList(new RouteLocation(45, 45), new RouteLocation(50, 50)))
			.distanceInMeters(1000)
			.build();
	private static final Route totalRoute = new Route.Builder()
			.wayPoints(Lists.newArrayList(driverRoute.getWayPoints().get(0), query.getStartLocation(), query.getDestinationLocation(), driverRoute.getWayPoints().get(1)))
			.build();
	private static final TripOffer offer = new TripOffer( 0, driverRoute, 0, null, 1000, 0, driver, vehicle, TripOfferStatus.ACTIVE_NOT_FULL, 0);


	@Mocked JoinTripRequestDAO joinTripRequestDAO;
	@Mocked TripOfferDAO tripOfferDAO;
	@Mocked TspSolver tspSolver;
	@Mocked DirectionsManager directionsManager;
	@Mocked TripsUtils tripsUtils;
	@Mocked LogManager logManager;

	private TripsMatcher tripsMatcher;

	@Before
	public void setupTripsMatcher() {
		tripsMatcher = new TripsMatcher(joinTripRequestDAO, tripOfferDAO, tspSolver, directionsManager, tripsUtils, logManager);
	}


	@Test
	public void testPotentialMatchStatus() {
		TripOffer offer = new TripOffer(0, null, 0, null, 0, 0, null, null, TripOfferStatus.ACTIVE_FULL, 0);
		Assert.assertFalse(tripsMatcher.isPotentialMatch(offer, query));
	}


	@Test
	public void testPotentialMatchDeclined() {
		new Expectations() {{
			joinTripRequestDAO.findDeclinedRequests(passenger.getId());
			result = Lists.newArrayList(
					new JoinTripRequest(0, query, 0, 0, offer, JoinTripStatus.DRIVER_DECLINED));
		}};

		Assert.assertFalse(tripsMatcher.isPotentialMatch(offer, query));
	}


	@Test
	public void testPotentialMatchFullCar() {
		new Expectations() {{
			tripsUtils.getActivePassengerCountForOffer(offer);
			result = vehicle.getCapacity();
		}};

		Assert.assertFalse(tripsMatcher.isPotentialMatch(offer, query));
	}


	@Test
	public void testPotentialMatchWithinAirDistance() {
		TripQuery query = new TripQuery(null, new RouteLocation(0, 0), new RouteLocation(1, 1), 10, passenger);
		Assert.assertFalse(tripsMatcher.isPotentialMatch(offer, query));
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testPotentialMatchMaxDiversion() {
		new Expectations() {{
			tspSolver.getBestOrder((List<JoinTripRequest>) any, offer, query);
			List<List<RouteLocation>> sortedRoutes = new ArrayList<>();
			sortedRoutes.add(Lists.newArrayList(new RouteLocation(0, 0), new RouteLocation(0, 0)));
			result = sortedRoutes;

			directionsManager.getDirections(
					driverRoute.getWayPoints().get(0),
					driverRoute.getWayPoints().get(1),
					(List<RouteLocation>) any);
			result = Lists.newArrayList(new Route.Builder().wayPoints(new ArrayList<RouteLocation>()).distanceInMeters(10000).build());
		}};

		Assert.assertFalse(tripsMatcher.isPotentialMatch(offer, query));
	}

	// TODO test max waiting time and updating routes

}