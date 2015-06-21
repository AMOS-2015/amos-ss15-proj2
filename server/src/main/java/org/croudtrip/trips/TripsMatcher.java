package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;

import java.util.List;

/**
 * Helper methods for matching offers with queries.
 */
interface TripsMatcher {

	class PotentialMatch {

		private final TripOffer offer;
		private final TripQuery query;
		private final NavigationResult totalRouteNavigationResult;

		public PotentialMatch( TripOffer offer, TripQuery query, NavigationResult totalRouteNavigationResult ) {
			this.offer = offer;
			this.query = query;
			this.totalRouteNavigationResult = totalRouteNavigationResult;
		}

		public TripOffer getOffer() {
			return offer;
		}

		public TripQuery getQuery() {
			return query;
		}

		public List<UserWayPoint> getUserWayPoints() {
			return totalRouteNavigationResult.getUserWayPoints();
		}

		public Route getTotalRoute() {
			return totalRouteNavigationResult.getRoute();
		}

		public NavigationResult getTotalRouteNavigationResult() {
			return totalRouteNavigationResult;
		}
	}


	/**
	 * Checks the passed in offers for potential matches.
	 * @return a filtered list of potential matches (which can be empty).
	 */
	List<SuperTripReservation> findPotentialTrips(List<TripOffer> offers, TripQuery query);


	/**
	 * Checks if a query matches with a given offer (is a potential match). This
	 * includes max diversion and max waiting time of both driver and passenger.
	 */
	Optional<PotentialMatch> isPotentialMatch(TripOffer offer, TripQuery query);

}
