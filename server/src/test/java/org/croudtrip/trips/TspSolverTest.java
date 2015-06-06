package org.croudtrip.trips;

import com.google.common.collect.Lists;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
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
	public void getBestOrder() {
		List<TspSolver.TripRequest> passengerTripRequests = Lists.newArrayList(
				new TspSolver.TripRequest(p1, p1Start, p1End),
				new TspSolver.TripRequest(p2, p2Start, p2End),
				new TspSolver.TripRequest(p3, p3Start, p3End));
		TspSolver.TripRequest driverTripRequest = new TspSolver.TripRequest(driver, dStart, dEnd);

		List<List<RouteLocation>> sortedRoutes = solver.getBestOrder(passengerTripRequests, driverTripRequest);

		// for 3 passengers there should be 90 routes
		Assert.assertEquals(90, sortedRoutes.size());

		// assert list is sorted
		long lastDistance = Long.MIN_VALUE;
		for (List<RouteLocation> route : sortedRoutes) {
			long distance = getDistance(route);
			Assert.assertTrue(distance > lastDistance);
			lastDistance = distance;
		}

		// check order of shortest route
		List<RouteLocation> shortestRoute = sortedRoutes.get(0);
		List<RouteLocation> routeSolution = Lists.newArrayList(dStart, p1Start, p2Start, p1End, p2End, p3Start, p3End, dEnd);
		Assert.assertEquals(routeSolution, shortestRoute);
	}


	private long getDistance(List<RouteLocation> route) {
		long distance = 0;
		for (int i = 0; i < route.size() - 1; ++i) {
			distance += route.get(i).distanceFrom(route.get(i+1));
		}
		return distance;
	}

}