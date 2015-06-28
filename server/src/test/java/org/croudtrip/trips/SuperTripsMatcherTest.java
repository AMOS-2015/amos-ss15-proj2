package org.croudtrip.trips;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.closestpair.ClosestPair;
import org.croudtrip.closestpair.ClosestPairResult;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.logs.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class SuperTripsMatcherTest extends TestCase {

	/*
	What those annotations do:
	@Mocked: mock all methods of SimpleTripsMatcher
	@Tested: have JMockit call the constructor of SimpleTripsMatcher with parameters that are annotated with @Injectable
	(if not those fields will be null causing NPEs in SuperTripsMatcher).
	==> SuperTripsMatcher can be tested while all super methods are mocked.
	 */

	@Tested @Mocked SimpleTripsMatcher simpleTripsMatcher;
	private SuperTripsMatcher superTripsMatcher;

	@Injectable @Mocked JoinTripRequestDAO joinTripRequestDAO;
	@Injectable @Mocked TripOfferDAO tripOfferDAO;
	@Injectable @Mocked TripsNavigationManager tripsNavigationManager;
	@Injectable @Mocked DirectionsManager directionsManager;
	@Injectable @Mocked TripsUtils tripsUtils;
	@Injectable @Mocked LogManager logManager;
	@Mocked ClosestPair closestPair;

	@Before
	public void setupMatcher() {
		superTripsMatcher = new SuperTripsMatcher(joinTripRequestDAO, tripOfferDAO, tripsNavigationManager, directionsManager,
				tripsUtils, closestPair, logManager);
	}


	@Test
	public void testSimpleTrips() {
		final List<SuperTripReservation> reservations = Lists.newArrayList(new SuperTripReservation());
		new Expectations() {{
			simpleTripsMatcher.findPotentialTrips(null, null);
			result = reservations;
		}};

		List<SuperTripReservation> resultReservations = superTripsMatcher.findPotentialTrips(null, null);
		Assert.assertEquals(reservations, resultReservations);
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testSimpleTwoOffersTrip() throws Exception {
		final User passenger = new User.Builder().build();
		final TripQuery query = createQuery(passenger);
		final TripOffer offer1 = createOffer(42);
		final TripOffer offer2 = createOffer(43);

		new Expectations() {{
			simpleTripsMatcher.findPotentialTrips((List<TripOffer>) any, (TripQuery) any); result = Lists.newArrayList();
			simpleTripsMatcher.assertJoinRequestNotDeclined((TripOffer) any, query); result = true;
			tripsUtils.getActivePassengerCountForOffer((TripOffer) any); result = 2;
			simpleTripsMatcher.assertWithinAirDistance(offer1, (TripQuery) any); result = true;
			simpleTripsMatcher.assertWithinAirDistance(offer2, (TripQuery) any); result = true;

			tripsNavigationManager.getNavigationResultForOffer((TripOffer) any, (TripQuery) any);
			result = new NavigationResult.Builder()
					.addUserWayPoint(new UserWayPoint.Builder().setDistanceToDriverInMeters(0).build())
					.addUserWayPoint(new UserWayPoint.Builder().setDistanceToDriverInMeters(0).build())
					.build();
			simpleTripsMatcher.assertRouteWithinPassengerMaxWaitingTime((TripOffer) any, (TripQuery) any, (List<UserWayPoint>) any);
			result = true;

			closestPair.findClosestPair((User) any, (NavigationResult) any, (NavigationResult) any);
			result = new ClosestPairResult(new RouteLocation(0, 0), new RouteLocation(0, 0));

			simpleTripsMatcher.isPotentialMatch((TripOffer) any, (TripQuery) any);
			result = Optional.of(new TripsMatcher.PotentialMatch(
					offer1,
					query,
					new NavigationResult.Builder()
							.addUserWayPoint(new UserWayPoint.Builder().setUser(passenger).build())
							.addUserWayPoint(new UserWayPoint.Builder().setUser(passenger).build())
							.build()));

			directionsManager.getDirections((RouteLocation) any, (RouteLocation) any);
			result = Lists.newArrayList(new Route.Builder().build());
		}};


		List<SuperTripReservation> reservations = superTripsMatcher.findPotentialTrips(Lists.newArrayList(offer1, offer2), query);
		Assert.assertEquals(1, reservations.size());
	}


	@Test
	public void testIsRoughPotentialSuperTripMatchForOneWaypointSuccess() {
		final TripOffer offer = createOffer(42);
		final TripQuery query = createQuery(new User.Builder().setId(42).build());

		new Expectations() {{
			simpleTripsMatcher.assertJoinRequestNotDeclined(offer, query); result = true;
			tripsUtils.getActivePassengerCountForOffer(offer); result = 2;
			simpleTripsMatcher.assertWithinAirDistance(offer, (TripQuery) any); result = true;
		}};

		boolean isMatch = superTripsMatcher.isRoughPotentialSuperTripMatchForOneWaypoint(offer, query, true);
		Assert.assertEquals(true, isMatch);
	}


	@Test
	public void testIsRoughPotentialSuperTripMatchForOneWaypointFailure() {
		final TripOffer offer = createOffer(42);
		final TripQuery query = createQuery(new User.Builder().setId(42).build());

		new Expectations() {{
			simpleTripsMatcher.assertJoinRequestNotDeclined(offer, query); result = true;
			tripsUtils.getActivePassengerCountForOffer(offer); result = 2;
			simpleTripsMatcher.assertWithinAirDistance(offer, (TripQuery) any); result = false;
		}};

		boolean isMatch = superTripsMatcher.isRoughPotentialSuperTripMatchForOneWaypoint(offer, query, true);
		Assert.assertEquals(false, isMatch);
	}


	private TripOffer createOffer(long id) {
		return new TripOffer.Builder()
				.setId(id)
				.setStatus(TripOfferStatus.ACTIVE)
				.setVehicle(new Vehicle.Builder().setCapacity(4).build())
				.setMaxDiversionInMeters(1000)
				.setDriverRoute(new Route.Builder().distanceInMeters(1000).build())
				.build();
	}


	private TripQuery createQuery(User passenger) {
		return new TripQuery.Builder()
				.setPassenger(passenger)
				.setStartLocation(new RouteLocation(42, 42))
				.setStartLocation(new RouteLocation(43, 43))
				.build();
	}

}