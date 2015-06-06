package org.croudtrip.trips;

import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.directions.DirectionsManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles all navigation / time of arrival tasks which are directly
 * related to a {@link org.croudtrip.api.trips.TripOffer}.
 * Unlike {@link org.croudtrip.directions.DirectionsManager}, this
 * class is not responsible for any low level navigation.
 */
public class TripsNavigationManager {

	private final TspSolver tspSolver;
	private final JoinTripRequestDAO joinTripRequestDAO;
	private final DirectionsManager directionsManager;

	@Inject
	public TripsNavigationManager(
			TspSolver tspSolver,
			JoinTripRequestDAO joinTripRequestDAO,
			DirectionsManager directionsManager) {

		this.tspSolver = tspSolver;
		this.joinTripRequestDAO = joinTripRequestDAO;
		this.directionsManager = directionsManager;
	}


	/**
	 * Returns the complete route for a given {@link TripOffer}
	 * including all passenger way points and their expected
	 * arrival time.
	 */
	public List<UserWayPoint> getRouteForOffer(TripOffer offer) {
		List<TspSolver.WayPoint> tspWayPoints = tspSolver.getBestOrder(
				joinTripRequestDAO.findByOfferId(offer.getId()),
				offer)
				.get(0);

		List<RouteLocation> passengerLocations = new ArrayList<>();
		for (int i = 1; i < tspWayPoints.size() - 1; ++i) {
			passengerLocations.add(tspWayPoints.get(i).getLocation());
		}

		List<Route> routes = directionsManager.getDirections(
				offer.getDriverRoute().getWayPoints().get(0),
				offer.getDriverRoute().getWayPoints().get(1),
				passengerLocations);

		if (routes == null || routes.isEmpty()) return new ArrayList<>();

		List<UserWayPoint> userWayPoints = new ArrayList<>();
		long arrivalTimestamp = System.currentTimeMillis() / 1000;
		for (int i = 0; i < tspWayPoints.size(); ++i) {
			if (i != 0) {
				arrivalTimestamp += routes.get(0).getLegDurationsInSeconds().get(i - 1);
			}

			TspSolver.WayPoint tspWayPoint = tspWayPoints.get(i);
			userWayPoints.add(new UserWayPoint(
					tspWayPoint.getUser(),
					tspWayPoint.getLocation(),
					tspWayPoint.isStart(),
					arrivalTimestamp));
		}

		return userWayPoints;
	}

}
