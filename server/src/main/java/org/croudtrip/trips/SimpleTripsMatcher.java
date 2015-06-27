package org.croudtrip.trips;


import com.google.common.base.Optional;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.LatLng;

import org.croudtrip.api.directions.NavigationResult;
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
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.logs.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
		List<PotentialMatch> potentialMatches = new ArrayList<>();
		for (TripOffer offer : offers) {
			Optional<PotentialMatch> potentialMatch = isPotentialMatch(offer, query);
			if ( potentialMatch.isPresent() ) {
				potentialMatches.add(potentialMatch.get());
			}
		}

		return findCheapestMatch(query, potentialMatches);
	}


	@Override
	public Optional<PotentialMatch> isPotentialMatch(TripOffer offer, TripQuery query) {
		// check trip status
		if (!offer.getStatus().equals(TripOfferStatus.ACTIVE)){
			return Optional.absent();
		}

		// check that query has not been declined before
		if (!assertJoinRequestNotDeclined(offer, query)){
			logManager.d("Offer " + offer.getId() + " is no potential match to query because passenger has been already declined.");
			return Optional.absent();
		}

		// check current passenger count
		int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
		if (passengerCount >= offer.getVehicle().getCapacity()){
			logManager.d("Offer " + offer.getId() + " is no potential match to query because the offer is full");
			return Optional.absent();
		}

		// early reject based on airline;
		if (!assertWithinAirDistance(offer, query)){
			logManager.d("Offer " + offer.getId() + " is no potential match to query due to its airline comparison");
			return Optional.absent();
		}

		// TODO: Early reject based on time

		// update driver route on new position update
		assertUpdatedDriverRoute(offer);

		// get complete new route
		NavigationResult totalRouteNavigationResult = null;
		try {
			totalRouteNavigationResult = tripsNavigationManager.getNavigationResultForOffer(offer, query);
		} catch (RouteNotFoundException e) {
			logManager.e(e.toString());
			return Optional.absent();
		}
		List<UserWayPoint> userWayPoints = totalRouteNavigationResult.getUserWayPoints();
		if (userWayPoints.isEmpty()) return Optional.absent();

		// check passenger max waiting time
		if (!assertRouteWithinPassengerMaxWaitingTime(offer, query, userWayPoints)){
			logManager.d("Offer " + offer.getId() + " is no potential match to query due to the waiting times");
			return Optional.absent();
		}

		// check if passenger route is within max diversion
		long distanceToDriverInMeters = userWayPoints.get(userWayPoints.size() - 1).getDistanceToDriverInMeters();
		if (distanceToDriverInMeters - offer.getDriverRoute().getDistanceInMeters() > offer.getMaxDiversionInMeters()){
			logManager.d("Offer " + offer.getId() + " is no potential match to query due to the maximum diversions of the passenger");
			return Optional.absent();
		}

		return Optional.of( new PotentialMatch( offer, query, totalRouteNavigationResult ));
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

		// ignore may waiting time if is -1. Use it, if you want to ignore max waiting times
		if( query.getMaxWaitingTimeInSeconds() == TripQuery.IGNORE_MAX_WAITING_TIME ) {
			logManager.d("Ignore Max waiting time");
			return true;
		}

		// check max waiting time for each passenger
		for (UserWayPoint userWayPoint : userWayPoints) {
			if (!userWayPoint.isStartOfTrip()) continue;
			if (userWayPoint.getUser().equals(offer.getDriver())) continue;

			long passengerMaxWaitingTimestamp = 0;
			long passengerAtStartingPoint = 0;
			if (userWayPoint.getUser().equals(query.getPassenger())) {
				passengerMaxWaitingTimestamp = query.getCreationTimestamp() + query.getMaxWaitingTimeInSeconds();
				passengerAtStartingPoint = query.getCreationTimestamp();
			} else {
				for (JoinTripRequest joinTripRequest : joinTripRequestDAO.findByOfferId(offer.getId())) {
					TripQuery foundQuery = joinTripRequest.getSuperTrip().getQuery();
					if (userWayPoint.getUser().equals(foundQuery.getPassenger())) {
						passengerMaxWaitingTimestamp = foundQuery.getCreationTimestamp() + foundQuery.getMaxWaitingTimeInSeconds();
						passengerAtStartingPoint = foundQuery.getCreationTimestamp();
						break;
					}
				}
			}

			logManager.d("Passenger would have to wait " + (userWayPoint.getArrivalTimestamp() - passengerAtStartingPoint) + "s. His max waiting time is: " + query.getMaxWaitingTimeInSeconds() + " -- driver will arrive at:" + new Date(userWayPoint.getArrivalTimestamp() * 1000).toLocaleString() + ", passenger will start his trip at: " + new Date(query.getCreationTimestamp() * 1000).toLocaleString());

			// driver may not come before passenger is at his starting position (a small bias for the case that driver and passenger are at the same place)
			if( userWayPoint.getArrivalTimestamp() - passengerAtStartingPoint < -20 ) return false;

			// passenger may not wait longer than his max waiting time
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
	private List<SuperTripReservation> findCheapestMatch(TripQuery query, List<PotentialMatch> potentialMatches) {
		if (potentialMatches.isEmpty()) return new ArrayList<>();

		// sort by price per km
		Collections.sort(potentialMatches, new Comparator<PotentialMatch>() {
			@Override
			public int compare(PotentialMatch pm1, PotentialMatch pm2) {
				return Integer.valueOf(pm1.getOffer().getPricePerKmInCents()).compareTo(pm2.getOffer().getPricePerKmInCents());
			}
		});

		List<PotentialMatch> matches = new ArrayList<>();

		// find prices
		int lowestPricePerKmInCents  = potentialMatches.get(0).getOffer().getPricePerKmInCents(), secondLowestPricePerKmInCents = -1;
		for (PotentialMatch potentialMatch : potentialMatches) {
			if (potentialMatch.getOffer().getPricePerKmInCents() == lowestPricePerKmInCents) {
				// all cheapest trips are matches
				matches.add(potentialMatch);

			} else if (potentialMatch.getOffer().getPricePerKmInCents() != secondLowestPricePerKmInCents) {
				// second cheapest determines price
				secondLowestPricePerKmInCents = potentialMatch.getOffer().getPricePerKmInCents();
				break;
			}
		}

		// calculate final price
		int pricePerKmInCents = lowestPricePerKmInCents;
		if (secondLowestPricePerKmInCents != -1) pricePerKmInCents = secondLowestPricePerKmInCents;
		int totalPriceInCents = (int) (pricePerKmInCents * query.getPassengerRoute().getDistanceInMeters() / 1000);



		// create price reservations
		List<SuperTripReservation> reservations = new ArrayList<>();
		for (PotentialMatch match : matches) {
			reservations.add(new SuperTripReservation.Builder()
					.setQuery(query)
					.addReservation(new TripReservation(
									new SuperTripSubQuery(query),
									totalPriceInCents,
									match.getOffer().getPricePerKmInCents(),
									match.getOffer().getId(),
									match.getTotalRouteNavigationResult().getEstimatedTripDurationInSecondsForUser( query.getPassenger() ),
									match.getOffer().getDriver())
					)
					.build());
		}

		return reservations;
	}

}
