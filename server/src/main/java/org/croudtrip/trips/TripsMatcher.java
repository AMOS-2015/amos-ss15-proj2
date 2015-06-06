package org.croudtrip.trips;


import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.logs.LogManager;

import java.util.List;

import javax.inject.Inject;

/**
 * Helper methods for matching offers with queries.
 */
class TripsMatcher {

	private final JoinTripRequestDAO joinTripRequestDAO;
	private final TripOfferDAO tripOfferDAO;
	private final TspSolver tspSolver;
	private final DirectionsManager directionsManager;
	private final TripsUtils tripsUtils;
	private final LogManager logManager;


	@Inject
	TripsMatcher(
			JoinTripRequestDAO joinTripRequestDAO,
			TripOfferDAO tripOfferDAO,
			TspSolver tspSolver,
			DirectionsManager directionsManager,
			TripsUtils tripsUtils,
			LogManager logManager) {

		this.joinTripRequestDAO = joinTripRequestDAO;
		this.tripOfferDAO = tripOfferDAO;
		this.tspSolver = tspSolver;
		this.directionsManager = directionsManager;
		this.tripsUtils = tripsUtils;
		this.logManager = logManager;
	}


	public boolean isPotentialMatch(TripOffer offer, TripQuery query) {
		// check trip status
		if (!offer.getStatus().equals(TripOfferStatus.ACTIVE_NOT_FULL)) return false;

		// check that query has not been declined before
		if (!assertJoinRequestNotDeclined(offer, query)) return false;

		// check current passenger count
		int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
		if (passengerCount >= offer.getVehicle().getCapacity()) return false;

		// early reject based on airline;
		if (!assertWithinAirDistance(offer, query)) return false;

		// get shortest route by air distance
		List<RouteLocation> totalRouteWayPoints = tspSolver.getBestOrder(
				joinTripRequestDAO.findByOfferId(offer.getId()),
				offer,
				query)
				.get(0);

		// trim driver way points
		totalRouteWayPoints.remove(0);
		totalRouteWayPoints.remove(totalRouteWayPoints.size() - 1);

		// check for route including passengers
		List<RouteLocation> driverWayPoints = offer.getDriverRoute().getWayPoints();
		List<Route> possibleRoutes = directionsManager.getDirections(
				driverWayPoints.get(0),
				driverWayPoints.get(1),
				totalRouteWayPoints);

		if (possibleRoutes == null || possibleRoutes.isEmpty()) return false;

		// update driver route on new position update
		assertUpdatedDriverRoute(offer);
		Route driverRoute = offer.getDriverRoute();

		// check if passenger route is within max diversion
		if (possibleRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters() > offer.getMaxDiversionInMeters()) {
			logManager.d("Declined Query due to max diversion: " + (possibleRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters()) + " > " + offer.getMaxDiversionInMeters() );
			return false;
		}

		// check passenger max waiting time
		if (!assertRouteWithinPassengerMaxWaitingTime(possibleRoutes.get(0), query)) return false;

		return true;
	}


	private boolean assertJoinRequestNotDeclined(TripOffer offer, TripQuery query) {
		List<JoinTripRequest> declinedRequests = joinTripRequestDAO.findDeclinedRequests(query.getPassenger().getId());
		for( JoinTripRequest request : declinedRequests ) {
			if( offer.getId() == request.getOffer().getId()) {
				return false;
			}
		}
		return true;
	}


	private boolean assertWithinAirDistance(TripOffer offer, TripQuery query) {
		List<RouteLocation> driverWayPoints = offer.getDriverRoute().getWayPoints();
		double airlineDriverRoute = driverWayPoints.get(0).distanceFrom( driverWayPoints.get( driverWayPoints.size() - 1 ) );
		double airlineTotalRoute = driverWayPoints.get(0).distanceFrom( query.getStartLocation() ) +
				query.getStartLocation().distanceFrom( query.getDestinationLocation() ) +
				query.getDestinationLocation().distanceFrom( driverWayPoints.get( driverWayPoints.size() - 1 ) );

		logManager.d("airlines compared: driverRoute: " + airlineDriverRoute + " totalRoute: " + airlineTotalRoute + " distance: " + (airlineTotalRoute - airlineDriverRoute) );
		if( (airlineTotalRoute - airlineDriverRoute) > offer.getMaxDiversionInMeters() * 10 ) {
			logManager.w("REQUEST REJECTED BY AIRLINE DISTANCES");
			return false;
		}
		return true;
	}


	private void assertUpdatedDriverRoute(TripOffer offer) {
		Route driverRoute = offer.getDriverRoute();
		if( driverRoute.getLastUpdateTimeInSeconds() < offer.getLastPositonUpdateInSeconds() ) {
			logManager.d(offer.getId() + ": driver route is out of date. Updating route...");
			List<Route> updatedDriverRoutes = directionsManager.getDirections(offer.getCurrentLocation(), driverRoute.getWayPoints().get(driverRoute.getWayPoints().size() - 1));
			if( updatedDriverRoutes == null || updatedDriverRoutes.isEmpty() ) {
				// that's quite bad; we will use the old route for matching for now.
				logManager.e("Could not compute a route for the driver after route update.");

			} else {
				driverRoute = updatedDriverRoutes.get(0);
				offer = new TripOffer(
						offer.getId(),
						driverRoute,
						System.currentTimeMillis()/1000+driverRoute.getDurationInSeconds(),
						offer.getCurrentLocation(),
						offer.getMaxDiversionInMeters(),
						offer.getPricePerKmInCents(),
						offer.getDriver(),
						offer.getVehicle(),
						offer.getStatus(),
						offer.getLastPositonUpdateInSeconds()
				);
				tripOfferDAO.update(offer);
			}
		}
	}


	private boolean assertRouteWithinPassengerMaxWaitingTime(Route route, TripQuery query) {
		// TODO: It is not that simple for multiple passengers
		double durationToPassenger = route.getLegDurationsInSeconds().get(0);
		return durationToPassenger <= query.getMaxWaitingTimeInSeconds();

	}

}
