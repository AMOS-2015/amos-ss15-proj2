package org.croudtrip.trips;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

/**
 * "Solver" for traveling salesman problem (TSP). Be careful though,
 * it's using brute force ...
 */
public class TspSolver {

	@Inject
	TspSolver() {  }


	/**
	 * @param joinTripRequests all join requests that should be considered, regardless of what state they are in
	 * @param tripOffer route information for the driver
	 * @param tripQuery additional way points that should be considered
	 * @return all possible routes sorted by their air distance (shortes first)
	 */
	public List<List<RouteLocation>> getBestOrder(
			List<JoinTripRequest> joinTripRequests,
			TripOffer tripOffer,
			TripQuery tripQuery) {

		List<TripRequest> passengerTripRequests = joinTripRequestsToTripRequests(joinTripRequests);
		passengerTripRequests.add(new TripRequest(
				tripQuery.getPassenger(),
				tripQuery.getStartLocation(),
				tripQuery.getDestinationLocation()));
		TripRequest driverTripRequest = new TripRequest(
				tripOffer.getDriver(),
				tripOffer.getDriverRoute().getWayPoints().get(0),
				tripOffer.getDriverRoute().getWayPoints().get(1));

		return getBestOrder(passengerTripRequests, driverTripRequest);
	}


	public List<List<RouteLocation>> getBestOrder(
			List<JoinTripRequest> joinTripRequests,
			TripOffer tripOffer) {

		List<TripRequest> passengerTripRequests = joinTripRequestsToTripRequests(joinTripRequests);
		TripRequest driverTripRequest = new TripRequest(
				tripOffer.getDriver(),
				tripOffer.getDriverRoute().getWayPoints().get(0),
				tripOffer.getDriverRoute().getWayPoints().get(1));

		return getBestOrder(passengerTripRequests, driverTripRequest);
	}


	public List<List<RouteLocation>> getBestOrder(
			List<TripRequest> passengerTripRequests,
			TripRequest driverTripRequest) {

		// get possible passenger routes (not including driver start / end)
		List<List<RouteLocation>> passengerPermutations = new ArrayList<>();
		findAllPassengerPermutations(
				passengerTripRequests,
				new LinkedList<RouteLocation>(),
				passengerPermutations);

		// compute distances of routes (including driver start / end)
		Map<Long, List<RouteLocation>> sortedRoutes = new TreeMap<>(); // distance <--> route
		for (List<RouteLocation> passengerPermutation : passengerPermutations) {
			long totalDistance = 0;
			passengerPermutation.add(0, driverTripRequest.start);
			passengerPermutation.add(driverTripRequest.end);
			for (int i = 0; i < passengerPermutation.size() - 1; ++i) {
				totalDistance += passengerPermutation.get(i).distanceFrom(passengerPermutation.get(i+1));
			}
			sortedRoutes.put(totalDistance, passengerPermutation);
		}

		return new ArrayList<>(sortedRoutes.values());
	}


	private void findAllPassengerPermutations(
			List<TripRequest> passengerTripRequests,
			LinkedList<RouteLocation> routeBuilder,
			List<List<RouteLocation>> resultRoutes) {

		boolean isRouteComplete = true;
		for (int passenger = 0; passenger < passengerTripRequests.size(); ++passenger) {
			TripRequest tripRequest = passengerTripRequests.get(passenger);

			if (tripRequest.start != null) {
				// a start location has not yet been included in route
				isRouteComplete = false;
				RouteLocation nextLocation = tripRequest.start;

				routeBuilder.addLast(nextLocation);
				tripRequest.start = null;
				findAllPassengerPermutations(passengerTripRequests, routeBuilder, resultRoutes);
				routeBuilder.removeLast();
				tripRequest.start = nextLocation;

			} else if (tripRequest.end != null) {
				// an end location has not yet been included in route
				isRouteComplete = false;
				RouteLocation nextLocation = tripRequest.end;

				routeBuilder.addLast(nextLocation);
				tripRequest.end = null;
				findAllPassengerPermutations(passengerTripRequests, routeBuilder, resultRoutes);
				routeBuilder.removeLast();
				tripRequest.end = nextLocation;
			}
		}

		if (isRouteComplete) {
			resultRoutes.add(new ArrayList<>(routeBuilder));
		}
	}


	private List<TripRequest> joinTripRequestsToTripRequests(List<JoinTripRequest> joinTripRequests) {
		List<TripRequest> tripRequests = new ArrayList<>();
		for (JoinTripRequest joinTripRequest : joinTripRequests) {
			TripRequest tripRequest = new TripRequest(joinTripRequest.getQuery().getPassenger());
			switch (joinTripRequest.getStatus()) {
				case DRIVER_ACCEPTED:
					tripRequest.setStart(joinTripRequest.getQuery().getStartLocation());
				case PASSENGER_IN_CAR:
					tripRequest.setEnd(joinTripRequest.getQuery().getDestinationLocation());
			}
		}
		return tripRequests;
	}


	public static class TripRequest {

		private final User user;
		private RouteLocation start, end;

		public TripRequest(User user) {
			this.user = user;
		}

		public TripRequest(User user, RouteLocation start, RouteLocation end) {
			this.user = user;
			this.start = start;
			this.end = end;
		}

		public User getUser() {
			return user;
		}

		public RouteLocation getStart() {
			return start;
		}

		public void setStart(RouteLocation start) {
			this.start = start;
		}

		public RouteLocation getEnd() {
			return end;
		}

		public void setEnd(RouteLocation end) {
			this.end = end;
		}
	}

}