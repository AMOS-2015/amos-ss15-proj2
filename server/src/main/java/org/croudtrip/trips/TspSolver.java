package org.croudtrip.trips;

import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
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
	TspSolver() {}


	/**
     * Find the best waypoint order using TSP based on a list of participating {@link org.croudtrip.api.trips.JoinTripRequest}
     * for an {@link org.croudtrip.api.trips.TripOffer} and a {@link org.croudtrip.api.trips.TripQuery}
     * from a passenger that wants to join this trip.
	 * @param joinTripRequests all join requests that should be considered, regardless of what state they are in
	 * @param tripOffer route information for the driver
	 * @param tripQuery additional way points that should be considered
	 * @return all possible routes sorted by their air distance (shortes first)
	 */
	public List<List<TspWayPoint>> getBestOrder(
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


    /**
     * Find the best waypoint order using TSP based on a list of participating {@link org.croudtrip.api.trips.JoinTripRequest}
     * for an {@link org.croudtrip.api.trips.TripOffer}.
     * @param joinTripRequests all join requests that should be considered, regardless of what state they are in
     * @param tripOffer route information for the driver
     * @return all possible routes sorted by their air distance (shortes first)
     */
	public List<List<TspWayPoint>> getBestOrder(
			List<JoinTripRequest> joinTripRequests,
			TripOffer tripOffer) {

		List<TripRequest> passengerTripRequests = joinTripRequestsToTripRequests(joinTripRequests);
		TripRequest driverTripRequest = new TripRequest(
				tripOffer.getDriver(),
				tripOffer.getDriverRoute().getWayPoints().get(0),
				tripOffer.getDriverRoute().getWayPoints().get(1));

		return getBestOrder(passengerTripRequests, driverTripRequest);
	}


	public List<List<TspWayPoint>> getBestOrder(
			List<TripRequest> passengerTripRequests,
			TripRequest driverTripRequest) {

		// get possible passenger routes (not including driver start / end)
		List<List<TspWayPoint>> passengerPermutations = new ArrayList<>();
		findAllPassengerPermutations(
				passengerTripRequests,
				new LinkedList<TspWayPoint>(),
				passengerPermutations);

		// compute distances of routes (including driver start / end)
		Map<Long, List<TspWayPoint>> sortedRoutes = new TreeMap<>(); // distance <--> route
		for (List<TspWayPoint> passengerPermutation : passengerPermutations) {
			long totalDistance = 0;
			passengerPermutation.add(0, new TspWayPoint(driverTripRequest.getUser(), driverTripRequest.getStart(), true));
			passengerPermutation.add(new TspWayPoint(driverTripRequest.getUser(), driverTripRequest.getEnd(), false));
			for (int i = 0; i < passengerPermutation.size() - 1; ++i) {
				totalDistance += passengerPermutation.get(i).getLocation().distanceFrom(passengerPermutation.get(i+1).getLocation());
			}
			sortedRoutes.put(totalDistance, passengerPermutation);
		}

		return new ArrayList<>(sortedRoutes.values());
	}


	private void findAllPassengerPermutations(
			List<TripRequest> passengerTripRequests,
			LinkedList<TspWayPoint> routeBuilder,
			List<List<TspWayPoint>> resultRoutes) {

		boolean isRouteComplete = true;
		for (int passenger = 0; passenger < passengerTripRequests.size(); ++passenger) {
			TripRequest tripRequest = passengerTripRequests.get(passenger);

			if (tripRequest.start != null) {
				// a start location has not yet been included in route
				isRouteComplete = false;
				RouteLocation nextLocation = tripRequest.start;

				routeBuilder.addLast(new TspWayPoint(tripRequest.getUser(), nextLocation, true));
				tripRequest.start = null;
				findAllPassengerPermutations(passengerTripRequests, routeBuilder, resultRoutes);
				routeBuilder.removeLast();
				tripRequest.start = nextLocation;

			} else if (tripRequest.end != null) {
				// an end location has not yet been included in route
				isRouteComplete = false;
				RouteLocation nextLocation = tripRequest.end;

				routeBuilder.addLast(new TspWayPoint(tripRequest.getUser(), nextLocation, false));
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
			TripQuery query = joinTripRequest.getSuperJoinTripRequest().getQuery();
			TripRequest tripRequest = new TripRequest(query.getPassenger());
			switch (joinTripRequest.getStatus()) {
				case DRIVER_ACCEPTED:
					tripRequest.setStart(query.getStartLocation());
				case PASSENGER_IN_CAR:
					tripRequest.setEnd(query.getDestinationLocation());
			}

            // add the trip request to the final list.
            if( joinTripRequest.getStatus() == JoinTripStatus.DRIVER_ACCEPTED ||
                    joinTripRequest.getStatus() == JoinTripStatus.PASSENGER_IN_CAR)
                tripRequests.add(tripRequest);
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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TripRequest that = (TripRequest) o;
			return Objects.equal(user, that.user) &&
					Objects.equal(start, that.start) &&
					Objects.equal(end, that.end);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(user, start, end);
		}
	}


	public static class TspWayPoint {

		private final User user;
		private final RouteLocation location;
		private final boolean isStart;

		public TspWayPoint(User user, RouteLocation location, boolean isStart) {
			this.user = user;
			this.location = location;
			this.isStart = isStart;
		}

		public User getUser() {
			return user;
		}

		public RouteLocation getLocation() {
			return location;
		}

		public boolean isStart() {
			return isStart;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TspWayPoint tspWayPoint = (TspWayPoint) o;
			return Objects.equal(isStart, tspWayPoint.isStart) &&
					Objects.equal(user, tspWayPoint.user) &&
					Objects.equal(location, tspWayPoint.location);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(user, location, isStart);
		}
	}

}
