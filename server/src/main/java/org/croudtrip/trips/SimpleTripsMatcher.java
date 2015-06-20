package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.SuperTripSubQuery;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.logs.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles simple trips (those which have max one driver).
 */
class SimpleTripsMatcher implements TripsMatcher {

	protected final JoinTripRequestDAO joinTripRequestDAO;
	protected final TripOfferDAO tripOfferDAO;
	protected final TripsNavigationManager tripsNavigationManager;
	protected final DirectionsManager directionsManager;
	protected final TripsUtils tripsUtils;
	protected final LogManager logManager;


	@Inject
	SimpleTripsMatcher(
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


	@Override
	public List<SuperTripReservation> findPotentialTrips(List<TripOffer> offers, TripQuery query) {
		List<TripOffer> potentialMatches = new ArrayList<>();
		for (TripOffer offer : offers) {
			if (isPotentialMatch(offer, query).isPresent()) {
				potentialMatches.add(offer);
			}
		}

		return findCheapestMatch(query, potentialMatches);
	}


	@Override
	public Optional<PotentialMatch> isPotentialMatch(TripOffer offer, TripQuery query) {
		// check trip status
		if (!offer.getStatus().equals(TripOfferStatus.ACTIVE)) return Optional.absent();

		// check that query has not been declined before
		if (!assertJoinRequestNotDeclined(offer, query)) return Optional.absent();

		// check current passenger count
		int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
		if (passengerCount >= offer.getVehicle().getCapacity()) return Optional.absent();

		// early reject based on airline;
		if (!assertWithinAirDistance(offer, query)) return Optional.absent();

		// update driver route on new position update
		assertUpdatedDriverRoute(offer);

		// get complete new route
		List<UserWayPoint> userWayPoints = tripsNavigationManager.getRouteWaypointsForOffer(offer, query);
		if (userWayPoints.isEmpty()) return Optional.absent();

		// check passenger max waiting time
		if (!assertRouteWithinPassengerMaxWaitingTime(offer, query, userWayPoints)) return Optional.absent();

		// check if passenger route is within max diversion
		long distanceToDriverInMeters = userWayPoints.get(userWayPoints.size() - 1).getDistanceToDriverInMeters();
		if (distanceToDriverInMeters - offer.getDriverRoute().getDistanceInMeters() > offer.getMaxDiversionInMeters()) return Optional.absent();

		return Optional.of( new PotentialMatch( offer, query, userWayPoints ));
	}

	protected boolean assertJoinRequestNotDeclined(TripOffer offer, TripQuery query) {
		List<JoinTripRequest> declinedRequests = joinTripRequestDAO.findDeclinedRequests(query.getPassenger().getId());
		for( JoinTripRequest request : declinedRequests ) {
			if( offer.getId() == request.getOffer().getId()) {
				return false;
			}
		}
		return true;
	}


	protected boolean assertWithinAirDistance(TripOffer offer, TripQuery query) {
		List<RouteLocation> driverWayPoints = offer.getDriverRoute().getWayPoints();
		double airlineDriverRoute = driverWayPoints.get(0).distanceFrom( driverWayPoints.get( driverWayPoints.size() - 1 ) );

		// compute airline if both waypoints are valid and also if only one waypoint ist valid
		double airlineTotalRoute = 0;
		if( query.getDestinationLocation() != null && query.getStartLocation() != null) {
			airlineTotalRoute = driverWayPoints.get(0).distanceFrom(query.getStartLocation()) +
					query.getStartLocation().distanceFrom(query.getDestinationLocation()) +
					query.getDestinationLocation().distanceFrom(driverWayPoints.get(driverWayPoints.size() - 1));
		}
		else if( query.getDestinationLocation() != null )
			airlineTotalRoute = driverWayPoints.get(0).distanceFrom(query.getDestinationLocation()) +
					query.getDestinationLocation().distanceFrom(driverWayPoints.get(driverWayPoints.size() - 1));
		else {
			airlineTotalRoute = driverWayPoints.get(0).distanceFrom(query.getStartLocation()) +
					query.getStartLocation().distanceFrom(driverWayPoints.get(driverWayPoints.size() - 1));
		}

		logManager.d("airlines compared: driverRoute: " + airlineDriverRoute + " totalRoute: " + airlineTotalRoute + " distance: " + (airlineTotalRoute - airlineDriverRoute) );
		if( (airlineTotalRoute - airlineDriverRoute) > offer.getMaxDiversionInMeters() * 10 ) {
			logManager.w("REQUEST REJECTED BY AIRLINE DISTANCES");
			return false;
		}
		return true;
	}


	protected void assertUpdatedDriverRoute(TripOffer offer) {
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


	protected boolean assertRouteWithinPassengerMaxWaitingTime(
			TripOffer offer,
			TripQuery query,
			List<UserWayPoint> userWayPoints) {

		// TODO: max waiting time is not valid for super trips

		// check max waiting time for each passenger
		for (UserWayPoint userWayPoint : userWayPoints) {
			if (!userWayPoint.isStartOfTrip()) continue;
			if (userWayPoint.getUser().equals(offer.getDriver())) continue;

			long passengerMaxWaitingTimestamp = 0;
			if (userWayPoint.getUser().equals(query.getPassenger())) {
				passengerMaxWaitingTimestamp = query.getCreationTimestamp() + query.getMaxWaitingTimeInSeconds();
			} else {
				for (JoinTripRequest joinTripRequest : joinTripRequestDAO.findByOfferId(offer.getId())) {
					TripQuery foundQuery = joinTripRequest.getSuperTrip().getQuery();
					if (userWayPoint.getUser().equals(foundQuery.getPassenger())) {
						passengerMaxWaitingTimestamp = foundQuery.getCreationTimestamp() + foundQuery.getMaxWaitingTimeInSeconds();
						break;
					}
				}
			}
			if (userWayPoint.getArrivalTimestamp() > passengerMaxWaitingTimestamp) return false;
		}
		return true;
	}


	/**
	 * Will compute a list of cheapest {@link org.croudtrip.api.trips.SuperTripReservation} for a
	 * specific query out of a list of potential matches
	 * @param query the query you want to get a match for
	 * @param potentialMatches the list of potential matches for this query
	 * @return a list of reservations for this query with the cheapest price
	 */
	private List<SuperTripReservation> findCheapestMatch(TripQuery query, List<TripOffer> potentialMatches) {
		if (potentialMatches.isEmpty()) return new ArrayList<>();

		// sort by price per km
		Collections.sort(potentialMatches, new Comparator<TripOffer>() {
			@Override
			public int compare(TripOffer offer1, TripOffer offer2) {
				return Integer.valueOf(offer1.getPricePerKmInCents()).compareTo(offer2.getPricePerKmInCents());
			}
		});

		List<TripOffer> matches = new ArrayList<>();

		// find prices
		int lowestPricePerKmInCents  = potentialMatches.get(0).getPricePerKmInCents(), secondLowestPricePerKmInCents = -1;
		for (TripOffer potentialMatch : potentialMatches) {
			if (potentialMatch.getPricePerKmInCents() == lowestPricePerKmInCents) {
				// all cheapest trips are matches
				matches.add(potentialMatch);

			} else if (potentialMatch.getPricePerKmInCents() != secondLowestPricePerKmInCents) {
				// second cheapest determines price
				secondLowestPricePerKmInCents = potentialMatch.getPricePerKmInCents();
				break;
			}
		}

		// calculate final price
		int pricePerKmInCents = lowestPricePerKmInCents;
		if (secondLowestPricePerKmInCents != -1) pricePerKmInCents = secondLowestPricePerKmInCents;
		int totalPriceInCents = (int) (pricePerKmInCents * query.getPassengerRoute().getDistanceInMeters() / 1000);

		// create price reservations
		List<SuperTripReservation> reservations = new ArrayList<>();
		for (TripOffer match : matches) {
			reservations.add(new SuperTripReservation.Builder()
					.setQuery(query)
					.addReservation(new TripReservation(
									new SuperTripSubQuery(query),
									totalPriceInCents,
									match.getPricePerKmInCents(),
									match.getId(),
									match.getDriver())
					)
					.build());
		}

		return reservations;
	}

}
