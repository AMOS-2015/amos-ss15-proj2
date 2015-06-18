package org.croudtrip.trips;

import com.google.common.collect.Lists;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.SuperPassengerTrip;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TspSolverTest {

	private static final User
			driver = new User(0, null, null, null, null, null, null, null, null, 0),
			p1 = new User(1, null, null, null, null, null, null, null, null, 0),
			p2 = new User(2, null, null, null, null, null, null, null, null, 0),
			p3 = new User(3, null, null, null, null, null, null, null, null, 0);

	private static final RouteLocation
			dStart = new RouteLocation(45, 45),
			dEnd = new RouteLocation(50, 50),
			p1Start = new RouteLocation(46, 46),
			p1End = new RouteLocation(48, 48),
			p2Start = new RouteLocation(47, 47),
			p2End = new RouteLocation(49, 49),
			p3Start = new RouteLocation(49.1, 49.1),
			p3End = new RouteLocation(49.2, 49.2);


	private TspSolver solver;

	@Before
	public void setupSolver() {
		solver = new TspSolver();
	}


	@Test
	public void testGetBestOrder() {
		List<TspSolver.TripRequest> passengerTripRequests = Lists.newArrayList(
				new TspSolver.TripRequest(p1, p1Start, p1End),
				new TspSolver.TripRequest(p2, p2Start, p2End),
				new TspSolver.TripRequest(p3, p3Start, p3End));
		TspSolver.TripRequest driverTripRequest = new TspSolver.TripRequest(driver, dStart, dEnd);

		List<List<TspSolver.TspWayPoint>> sortedRoutes = solver.getBestOrder(passengerTripRequests, driverTripRequest);

		// for 3 passengers there should be 90 routes
		Assert.assertEquals(90, sortedRoutes.size());

		// assert list is sorted
		long lastDistance = Long.MIN_VALUE;
		for (List<TspSolver.TspWayPoint> route : sortedRoutes) {
			long distance = getDistance(route);
			Assert.assertTrue(distance > lastDistance);
			lastDistance = distance;
		}

		// check order of shortest route
		List<TspSolver.TspWayPoint> shortestRoute = sortedRoutes.get(0);
		List<TspSolver.TspWayPoint> routeSolution = Lists.newArrayList(
				new TspSolver.TspWayPoint(driver, dStart, true),
				new TspSolver.TspWayPoint(p1, p1Start, true),
				new TspSolver.TspWayPoint(p2, p2Start, true),
				new TspSolver.TspWayPoint(p1, p1End, false),
				new TspSolver.TspWayPoint(p2, p2End, false),
				new TspSolver.TspWayPoint(p3, p3Start, true),
				new TspSolver.TspWayPoint(p3, p3End, false),
				new TspSolver.TspWayPoint(driver, dEnd, false));
		Assert.assertEquals(routeSolution, shortestRoute);
	}

    @Test
    public void testGetBestOrderJoinTripRequests() {
        TripQuery q1 = new TripQuery(null, p1Start, p1End, 0, 0, p1);
        TripQuery q2 = new TripQuery(null, p2Start, p2End, 0, 0, p2);
        TripQuery q3 = new TripQuery(null, p3Start, p3End, 0, 0, p3);
        TripQuery q4 = new TripQuery(null, null, null, 0, 0, p1);
        TripOffer offer = new TripOffer( 0, new Route.Builder().wayPoints( Lists.newArrayList( dStart, dEnd ) ).build(), 0, dStart, 0, 0, driver, null, null, 0 );

        // TODO: maybe finde some route with one passenger in car
        List<JoinTripRequest> passengerTripRequests = Lists.newArrayList(
				createJoinTripRequest(offer, q1, JoinTripStatus.DRIVER_ACCEPTED),
                createJoinTripRequest(offer, q2, JoinTripStatus.DRIVER_ACCEPTED),
                createJoinTripRequest(offer, q3, JoinTripStatus.DRIVER_ACCEPTED),
                createJoinTripRequest(offer, q4, JoinTripStatus.DRIVER_DECLINED),
                createJoinTripRequest(offer, q4, JoinTripStatus.DRIVER_CANCELLED),
                createJoinTripRequest(offer, q4, JoinTripStatus.PASSENGER_AT_DESTINATION),
                createJoinTripRequest(offer, q4, JoinTripStatus.PASSENGER_ACCEPTED));

        List<List<TspSolver.TspWayPoint>> sortedRoutes = solver.getBestOrder(passengerTripRequests, offer);

        // for 3 passengers there should be 90 routes
        Assert.assertEquals(90, sortedRoutes.size());

        // assert list is sorted
        long lastDistance = Long.MIN_VALUE;
        for (List<TspSolver.TspWayPoint> route : sortedRoutes) {
            long distance = getDistance(route);
            Assert.assertTrue(distance > lastDistance);
            lastDistance = distance;
        }

        // check order of shortest route
        List<TspSolver.TspWayPoint> shortestRoute = sortedRoutes.get(0);
        List<TspSolver.TspWayPoint> routeSolution = Lists.newArrayList(
                new TspSolver.TspWayPoint(driver, dStart, true),
                new TspSolver.TspWayPoint(p1, p1Start, true),
                new TspSolver.TspWayPoint(p2, p2Start, true),
                new TspSolver.TspWayPoint(p1, p1End, false),
                new TspSolver.TspWayPoint(p2, p2End, false),
                new TspSolver.TspWayPoint(p3, p3Start, true),
                new TspSolver.TspWayPoint(p3, p3End, false),
                new TspSolver.TspWayPoint(driver, dEnd, false));
        Assert.assertEquals(routeSolution, shortestRoute);
    }


	private long getDistance(List<TspSolver.TspWayPoint> route) {
		long distance = 0;
		for (int i = 0; i < route.size() - 1; ++i) {
			distance += route.get(i).getLocation().distanceFrom(route.get(i + 1).getLocation());
		}
		return distance;
	}


	private JoinTripRequest createJoinTripRequest(TripOffer offer, TripQuery query, JoinTripStatus status) {
		return new JoinTripRequest.Builder()
				.setOffer(offer)
				.setStatus(status)
				.setSuperPassengerTrip(new SuperPassengerTrip.Builder().setQuery(query).build())
				.build();
	}

}