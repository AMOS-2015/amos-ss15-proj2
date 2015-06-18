package org.croudtrip.trips;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.SuperPassengerTrip;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class TripsNavigationManagerTest extends TestCase {

	private static final User
			passenger = new User.Builder().setId(0).build(),
			driver = new User.Builder().setId(1).build();

	private static final Route driverRoute = new Route.Builder()
			.wayPoints(Lists.newArrayList(new RouteLocation(45, 45), new RouteLocation(50, 50)))
			.build();
	private static final TripOffer offer = new TripOffer( 0, driverRoute, 0, null, 1000, 0, driver, null, null, 0);

	private static final TripQuery query = new TripQuery(
			null,
			new RouteLocation(45, 45),
			new RouteLocation(50, 50),
			10,
			System.currentTimeMillis() / 1000,
			passenger);
	private static final JoinTripRequest joinTripRequest = new JoinTripRequest.Builder()
			.setOffer(offer)
			.setSuperPassengerTrip(new SuperPassengerTrip.Builder().setQuery(query).build())
			.build();


	@Mocked TspSolver tspSolver;
	@Mocked JoinTripRequestDAO joinTripRequestDAO;
	@Mocked DirectionsManager directionsManager;

	TripsNavigationManager tripsNavigationManager;

	@Before
	public void setupManager() {
		tripsNavigationManager = new TripsNavigationManager(tspSolver, joinTripRequestDAO, directionsManager);
	}


	@Test
	@SuppressWarnings("unchecked")
	public void testGetRouteWaypointsForOffer() {
		new Expectations() {{
			joinTripRequestDAO.findByOfferId(offer.getId());
			result = Lists.newArrayList(joinTripRequest);

			directionsManager.getDirections(
					driverRoute.getWayPoints().get(0),
					driverRoute.getWayPoints().get(1),
					(List<RouteLocation>) any);
			result = Lists.newArrayList(new Route.Builder()
					.wayPoints(driverRoute.getWayPoints())
					.legDurationInSeconds(Lists.newArrayList(1l, 1l, 1l))
					.legDistancesInMeters(Lists.newArrayList(1l, 1l, 1l))
					.build());

			tspSolver.getBestOrder((List<JoinTripRequest>) any, offer);
			List<List<TspSolver.TspWayPoint>> routesList = new ArrayList<>();
			routesList.add(Lists.newArrayList(
					new TspSolver.TspWayPoint(driver, driverRoute.getWayPoints().get(0), true),
					new TspSolver.TspWayPoint(passenger, query.getStartLocation(), true),
					new TspSolver.TspWayPoint(passenger, query.getDestinationLocation(), false),
					new TspSolver.TspWayPoint(driver, driverRoute.getWayPoints().get(1), false)));
			result = routesList;
		}};

		List<UserWayPoint> userWayPoints = tripsNavigationManager.getRouteWaypointsForOffer(offer);

		Assert.assertEquals(4, userWayPoints.size());
		Assert.assertEquals(driver, userWayPoints.get(0).getUser());
		Assert.assertEquals(passenger, userWayPoints.get(1).getUser());
		Assert.assertEquals(passenger, userWayPoints.get(2).getUser());
		Assert.assertEquals(driver, userWayPoints.get(3).getUser());
		long arrivalTime = Long.MIN_VALUE;
		long distanceToDriver = Long.MIN_VALUE;
		for (UserWayPoint wayPoint : userWayPoints) {
			Assert.assertTrue(wayPoint.getArrivalTimestamp() > arrivalTime);
			arrivalTime = wayPoint.getArrivalTimestamp();

			Assert.assertTrue(wayPoint.getDistanceToDriverInMeters() > distanceToDriver);
			distanceToDriver = wayPoint.getDistanceToDriverInMeters();
		}

	}

    @Test
    public void getRouteForOffer() {
        new Expectations() {{
            joinTripRequestDAO.findByOfferId(offer.getId());
            result = Lists.newArrayList(joinTripRequest);

            final TspSolver.TspWayPoint wp0 = new TspSolver.TspWayPoint(driver, driverRoute.getWayPoints().get(0), true);
            final TspSolver.TspWayPoint wp1 = new TspSolver.TspWayPoint(passenger, query.getStartLocation(), true);
            final TspSolver.TspWayPoint wp2 = new TspSolver.TspWayPoint(passenger, query.getDestinationLocation(), false);
            final TspSolver.TspWayPoint wp3 = new TspSolver.TspWayPoint(driver, driverRoute.getWayPoints().get(1), false);


            directionsManager.getDirections(
                    driverRoute.getWayPoints().get(0),
                    driverRoute.getWayPoints().get(1),
                    (List<RouteLocation>) any);
            result = new Delegate() {
                public List<Route> getDirections( RouteLocation start, RouteLocation dest, List<RouteLocation> wps ) {

                    Assert.assertTrue( wps.size() == 2 );
                    Assert.assertEquals(wp0.getLocation(), start);
                    Assert.assertEquals(wp3.getLocation(), dest);
                    Assert.assertEquals(wp1.getLocation(), wps.get(0));
                    Assert.assertEquals(wp2.getLocation(), wps.get(1));

                    return Lists.newArrayList(new Route.Builder()
                                .wayPoints(driverRoute.getWayPoints())
                                .durationInSeconds(33)
                                .distanceInMeters(33)
                                .legDurationInSeconds(Lists.newArrayList(1l, 1l, 1l))
                                .legDistancesInMeters(Lists.newArrayList(1l, 1l, 1l))
                                .build());
                }
            };

            tspSolver.getBestOrder((List<JoinTripRequest>) any, offer);
            List<List<TspSolver.TspWayPoint>> routesList = new ArrayList<>();
            routesList.add(Lists.newArrayList(wp0, wp1, wp2, wp3));
            result = routesList;
        }};

        NavigationResult navigationResult = null;
        try {
            navigationResult = tripsNavigationManager.getNavigationResultForOffer(offer);
        } catch (RouteNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull( navigationResult );
        Assert.assertNotNull( navigationResult.getRoute() );
        Assert.assertNotNull( navigationResult.getUserWayPoints() );

        Assert.assertEquals(33, navigationResult.getRoute().getDurationInSeconds());
        Assert.assertEquals(33, navigationResult.getRoute().getDistanceInMeters());
        Assert.assertEquals(4, navigationResult.getUserWayPoints().size());

    }
}