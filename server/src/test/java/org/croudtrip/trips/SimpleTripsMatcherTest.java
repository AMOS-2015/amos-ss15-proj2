package org.croudtrip.trips;


import com.google.common.collect.Lists;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteDistanceDuration;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.SuperTrip;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.logs.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class SimpleTripsMatcherTest {

	// TODO test updating routes

	private static final User
			passenger = new User.Builder().setId(0).build(),
			driver = new User.Builder().setId(1).build();

	private static final TripQuery query = new TripQuery(
			new RouteDistanceDuration(2345, 2345 ),
			new RouteLocation(45, 45),
			new RouteLocation(50, 50),
			100,
			System.currentTimeMillis() / 1000,
			passenger);

	private static final Vehicle vehicle = new Vehicle(0, null, null, null, 4, driver);

	private static final Route driverRoute = new Route.Builder()
			.wayPoints(Lists.newArrayList(new RouteLocation(45, 45), new RouteLocation(50, 50)))
			.distanceInMeters(1000)
			.build();

	private static final TripOffer offer = new TripOffer(0, driverRoute, 0, null, 1000, 0, driver, vehicle, TripOfferStatus.ACTIVE, 0);


	@Mocked JoinTripRequestDAO joinTripRequestDAO;
	@Mocked TripOfferDAO tripOfferDAO;
	@Mocked TripsNavigationManager tripsNavigationManager;
	@Mocked DirectionsManager directionsManager;
	@Mocked TripsUtils tripsUtils;
	@Mocked LogManager logManager;

	private SimpleTripsMatcher simpleTripsMatcher;

	@Before
	public void setupTripsMatcher() {
		simpleTripsMatcher = new SimpleTripsMatcher(joinTripRequestDAO, tripOfferDAO, tripsNavigationManager, directionsManager, tripsUtils, logManager);
	}


	@Test
	public void testPotentialMatchStatus() {
		TripOffer offer = new TripOffer(0, null, 0, null, 0, 0, null, null, TripOfferStatus.DISABLED, 0);
		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	public void testPotentialMatchDeclined() {
		new Expectations() {{
			joinTripRequestDAO.findDeclinedRequests(passenger.getId());
			result = Lists.newArrayList(
					new JoinTripRequest.Builder()
							.setOffer(offer)
							.setStatus(JoinTripStatus.DRIVER_DECLINED)
							.setSuperTrip(new SuperTrip.Builder().setQuery(query).build())
							.build());
		}};

		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	public void testPotentialMatchFullCar() {
		new Expectations() {{
			tripsUtils.getActivePassengerCountForOffer(offer);
			result = vehicle.getCapacity();
		}};

		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	public void testPotentialMatchWithinAirDistance() {
		TripQuery query = new TripQuery(
				null,
				new RouteLocation(0, 0),
				new RouteLocation(1, 1),
				10,
				System.currentTimeMillis() / 1000,
				passenger);
		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	/*
	@Test
	@SuppressWarnings("unchecked")
	public void testPotentialMatchMaxDiversion() {
		new TspExpectations(wayPoints);
		new Expectations() {{
			directionsManager.getDirections(
					driverRoute.getWayPoints().get(0),
					driverRoute.getWayPoints().get(1),
					(List<RouteLocation>) any);
			result = Lists.newArrayList(new Route.Builder()
					.wayPoints(totalRoute.getWayPoints())
					.distanceInMeters(10000)
					.build());
		}};

		Assert.assertFalse(tripsMatcher.isPotentialMatch(offer, query));
	}
	*/


	@Test
	@SuppressWarnings("unchecked")
	public void testPotentialMatchMaxWaitingTimeQuery() throws RouteNotFoundException {
		// tests that max waiting time of query is honored
		new Expectations() {{
			tripsNavigationManager.getNavigationResultForOffer(offer, query);
			result = new NavigationResult( null, Lists.newArrayList(
					new UserWayPoint(driver, null, true, 0, 0),
					new UserWayPoint(passenger, null, true, query.getCreationTimestamp() + query.getMaxWaitingTimeInSeconds() + 100, 1),
					new UserWayPoint(passenger, null, false, 0, 2),
					new UserWayPoint(driver, null, false, 0, 3)));
		}};

		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testPotentialMatchMaxWaitingTimeJoinRequests() throws RouteNotFoundException {
		// tests that max waiting time of already existing join requests is honored
		final User anotherPassenger = new User.Builder().setId(2).build();

		new Expectations() {{
			tripsNavigationManager.getNavigationResultForOffer(offer, query);
			result = new NavigationResult( null, Lists.newArrayList(
					new UserWayPoint(driver, null, true, 0, 0),
					new UserWayPoint(passenger, null, true, 0, 0),
					new UserWayPoint(passenger, null, false, 0, 0),
					new UserWayPoint(anotherPassenger, null, true, query.getCreationTimestamp() + query.getMaxWaitingTimeInSeconds() + 100, 0),
					new UserWayPoint(anotherPassenger, null, false, 0, 0),
					new UserWayPoint(driver, null, false, 0, 3)));
		}};

		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testPotentialMatchMaxDiversion() throws RouteNotFoundException {
		final long currentTimestamp = System.currentTimeMillis() / 1000;
		new Expectations() {{
			tripsNavigationManager.getNavigationResultForOffer(offer, query);
			result = new NavigationResult( null, Lists.newArrayList(
					new UserWayPoint(driver, null, true, currentTimestamp, 0),
					new UserWayPoint(passenger, null, true, currentTimestamp, 0),
					new UserWayPoint(passenger, null, false, currentTimestamp, 0),
					new UserWayPoint(driver, null, false, currentTimestamp, offer.getDriverRoute().getDistanceInMeters() + offer.getMaxDiversionInMeters() + 100)));
		}};

		Assert.assertFalse(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testPotentialMatchSuccess() throws RouteNotFoundException {
		final long currentTimestamp = System.currentTimeMillis() / 1000;
		new Expectations() {{
			tripsNavigationManager.getNavigationResultForOffer(offer, query);
			result = new NavigationResult( null, Lists.newArrayList(
					new UserWayPoint(driver, null, true, currentTimestamp, 0),
					new UserWayPoint(passenger, null, true, currentTimestamp + 1, 1),
					new UserWayPoint(passenger, null, false, currentTimestamp + query.getMaxWaitingTimeInSeconds() / 2, 2),
					new UserWayPoint(driver, null, false, currentTimestamp, 3)));
		}};

		Assert.assertTrue(simpleTripsMatcher.isPotentialMatch(offer, query).isPresent());
	}


	@Test
	public void testFindCheapestMatch() {
		TripOffer offer1 = new TripOffer.Builder().setPricePerKmInCents(12).build();
		TripOffer offer2 = new TripOffer.Builder().setPricePerKmInCents(12).build();
		TripOffer offer3 = new TripOffer.Builder().setPricePerKmInCents(13).build();
		TripOffer offer4 = new TripOffer.Builder().setPricePerKmInCents(14).build();

		final long currentTimestamp = System.currentTimeMillis() / 1000;
		NavigationResult navRes = new NavigationResult( null, Lists.newArrayList(
				new UserWayPoint(driver, null, true, currentTimestamp, 0),
				new UserWayPoint(passenger, null, true, currentTimestamp + 1, 1),
				new UserWayPoint(passenger, null, false, currentTimestamp + query.getMaxWaitingTimeInSeconds() / 2, 2),
				new UserWayPoint(driver, null, false, currentTimestamp, 3)));

		TripsMatcher.PotentialMatch pm1 = new TripsMatcher.PotentialMatch( offer1, query, navRes );
		TripsMatcher.PotentialMatch pm2 = new TripsMatcher.PotentialMatch( offer2, query, navRes );
		TripsMatcher.PotentialMatch pm3 = new TripsMatcher.PotentialMatch( offer3, query, navRes );
		TripsMatcher.PotentialMatch pm4 = new TripsMatcher.PotentialMatch( offer4, query, navRes );


		List<SuperTripReservation> reservations = Deencapsulation.invoke(
				simpleTripsMatcher,
				"findCheapestMatch",
				query,
				Lists.newArrayList(pm1, pm2, pm3, pm4));

		Assert.assertEquals(2, reservations.size());
		long totalPrice = offer3.getPricePerKmInCents() * query.getRouteDistanceDuration().getDistanceInMeters() / 1000;
		Assert.assertEquals(totalPrice, reservations.get(0).getReservations().get(0).getTotalPriceInCents());
		Assert.assertEquals(totalPrice, reservations.get(1).getReservations().get(0).getTotalPriceInCents());
	}

}