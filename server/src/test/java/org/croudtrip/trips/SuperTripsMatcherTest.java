package org.croudtrip.trips;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.closestpair.ClosestPair;
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
	public void testIsRoughPotentialSuperTripMatchForOneWaypointSuccess() {
		final TripOffer offer = new TripOffer.Builder().setStatus(TripOfferStatus.ACTIVE)
				.setVehicle(new Vehicle.Builder().setCapacity(4).build())
				.build();

		final TripQuery query = new TripQuery.Builder()
				.setPassenger(new User.Builder().setId(42).build())
				.setStartLocation(new RouteLocation(42, 42))
				.build();

		new Expectations() {{
			simpleTripsMatcher.assertJoinRequestNotDeclined(offer, query);
			result = true;

			tripsUtils.getActivePassengerCountForOffer(offer);
			result = 2;

			simpleTripsMatcher.assertWithinAirDistance(offer, (TripQuery) any);
			result = true;
		}};

		tripsUtils.getActivePassengerCountForOffer(offer);
		boolean isMatch = superTripsMatcher.isRoughPotentialSuperTripMatchForOneWaypoint(offer, query, true);
		Assert.assertEquals(true, isMatch);
	}

}