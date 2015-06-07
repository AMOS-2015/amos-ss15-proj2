package org.croudtrip.trips;


import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.logs.LogManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Helper methods for matching offers with queries.
 */
class TripsMatcher {

	private final JoinTripRequestDAO joinTripRequestDAO;
	private final TripOfferDAO tripOfferDAO;
	private final TripsNavigationManager tripsNavigationManager;
	private final DirectionsManager directionsManager;
	private final TripsUtils tripsUtils;
	private final LogManager logManager;


	@Inject
	TripsMatcher(
			JoinTripRequestDAO joinTripRequestDAO,
			TripOfferDAO tripOfferDAO,
			TripsNavigationManager tripsNavigationManager,
			DirectionsManager directionsManager,
			TripsUtils tripsUtils,
			LogManager logManager) {

		this.joinTripRequestDAO = joinTripRequestDAO;
		this.tripOfferDAO = tripOfferDAO;
		this.tripsNavigationManager = tripsNavigationManager;
		this.directionsManager = directionsManager;
		this.tripsUtils = tripsUtils;
		this.logManager = logManager;
	}


	/**
	 * Checks the passed in offers for potential matches.
	 * @return a filtered list of potential matches (which can be empty).
	 */
	public List<TripOffer> filterPotentialMatches(List<TripOffer> offers, TripQuery query) {
		List<TripOffer> potentialMatches = new ArrayList<>();
		for (TripOffer offer : offers) {
			if (isPotentialMatch(offer, query)) {
				potentialMatches.add(offer);
			}
		}

		return potentialMatches;
	}


	/**
	 * Checks if a query matches with a given offer (is a potential match). This
	 * includes max diversion and max waiting time of both driver and passenger.
	 */
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

		// update driver route on new position update
		assertUpdatedDriverRoute(offer);

		// get complete new route
		List<UserWayPoint> userWayPoints = tripsNavigationManager.getRouteWaypointsForOffer(offer, query);
		if (userWayPoints.isEmpty()) return false;

		// check passenger max waiting time
		if (!assertRouteWithinPassengerMaxWaitingTime(offer, query, userWayPoints)) return false;

		/*
		// TODO check this for all passengers!!
		// check if passenger route is within max diversion
		if (possibleRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters() > offer.getMaxDiversionInMeters()) {
			logManager.d("Declined Query due to max diversion: " + (possibleRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters()) + " > " + offer.getMaxDiversionInMeters() );
			return false;
		}
		*/

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


	private boolean assertRouteWithinPassengerMaxWaitingTime(
			TripOffer offer,
			TripQuery query,
			List<UserWayPoint> userWayPoints) {

		// check max waiting time for each passenger
		long currentTimestamp = System.currentTimeMillis() / 1000; // unix timestamp
		for (UserWayPoint userWayPoint : userWayPoints) {
			if (!userWayPoint.isStartOfTrip()) continue;
			if (userWayPoint.getUser().equals(offer.getDriver())) continue;

			double passengerMaxWaitingTime = 0;
			if (userWayPoint.getUser().equals(query.getPassenger())) {
				passengerMaxWaitingTime = query.getMaxWaitingTimeInSeconds();
			} else {
				for (JoinTripRequest joinTripRequest : joinTripRequestDAO.findByOfferId(offer.getId())) {
					if (userWayPoint.getUser().equals(joinTripRequest.getQuery().getPassenger())) {
						passengerMaxWaitingTime = joinTripRequest.getQuery().getMaxWaitingTimeInSeconds();
						break;
					}
				}
			}
			if (userWayPoint.getArrivalTimestamp() - currentTimestamp > passengerMaxWaitingTime) return false;
		}
		return true;
	}

}
