package org.croudtrip.trips;

import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;

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


	public List<UserWayPoint> getRouteWaypointsForOffer(TripOffer offer) {
		return getRouteWaypointsForOffer(offer, null);
	}


	/**
	 * Returns the complete route waypoints for a given {@link TripOffer}
	 * and one {@link TripQuery} including all passenger way points and their expected
	 * arrival time.
	 * @param offer the offer which includes the driver route and which is used to find
	 *              the pending {@link org.croudtrip.api.trips.JoinTripRequest}.
	 * @param query an optional query which should also be considered during the matching
	 *              process.
	 */
	public List<UserWayPoint> getRouteWaypointsForOffer(TripOffer offer, TripQuery query) {
		List<JoinTripRequest> joinTripRequests = joinTripRequestDAO.findByOfferId(offer.getId());

		List<TspSolver.TspWayPoint> tspWayPoints;
		if (query == null) {
			tspWayPoints = tspSolver.getBestOrder(joinTripRequests, offer) .get(0);
		} else {
			tspWayPoints = tspSolver.getBestOrder(joinTripRequests, offer, query) .get(0);
		}

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
		long distanceToDriverInMeters = 0;
		for (int i = 0; i < tspWayPoints.size(); ++i) {
			if (i != 0) {
				arrivalTimestamp += routes.get(0).getLegDurationsInSeconds().get(i - 1);
				distanceToDriverInMeters += routes.get(0).getLegDistancesInMeters().get(i - 1);
			}

			TspSolver.TspWayPoint tspWayPoint = tspWayPoints.get(i);
			userWayPoints.add(new UserWayPoint(
					tspWayPoint.getUser(),
					tspWayPoint.getLocation(),
					tspWayPoint.isStart(),
					arrivalTimestamp,
					distanceToDriverInMeters));
		}

		return userWayPoints;
	}

    /**
     * Returns the {@link org.croudtrip.api.directions.NavigationResult}
     * for a given {@link TripOffer} with an optimal solution for visiting all the passengers
     * @param offer the offer you want to do the navigation request for.
     * @return the navigation result for this offer.
     * @throws RouteNotFoundException If there exists no route for the driver.
     */
    public NavigationResult getNavigationResultForOffer(TripOffer offer) throws RouteNotFoundException {
        List<TspSolver.TspWayPoint> tspWayPoints = tspSolver.getBestOrder(
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

        if (routes == null || routes.isEmpty()) throw new RouteNotFoundException();

        List<UserWayPoint> userWayPoints = new ArrayList<>();
        long arrivalTimestamp = System.currentTimeMillis() / 1000;
        long distanceToDriverInMeters = 0;
        for (int i = 0; i < tspWayPoints.size(); ++i) {
            if (i != 0) {
                arrivalTimestamp += routes.get(0).getLegDurationsInSeconds().get(i - 1);
                distanceToDriverInMeters += routes.get(0).getLegDistancesInMeters().get(i - 1);
            }

            TspSolver.TspWayPoint tspWayPoint = tspWayPoints.get(i);
            userWayPoints.add(new UserWayPoint(
                    tspWayPoint.getUser(),
                    tspWayPoint.getLocation(),
                    tspWayPoint.isStart(),
                    arrivalTimestamp,
                    distanceToDriverInMeters));
        }

        return new NavigationResult( routes.get(0), userWayPoints );
    }
}
