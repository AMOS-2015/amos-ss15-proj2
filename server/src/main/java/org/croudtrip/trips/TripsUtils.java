package org.croudtrip.trips;

import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.db.JoinTripRequestDAO;

import java.util.List;

import javax.inject.Inject;

/**
 * Helper methods for dealing with offers, join requests, etc.
 */
public class TripsUtils {

	private final JoinTripRequestDAO joinTripRequestDAO;

	@Inject
	TripsUtils(JoinTripRequestDAO joinTripRequestDAO) {
		this.joinTripRequestDAO = joinTripRequestDAO;
	}


	/**
	 * Computes how many passengers actually will be picked up or sit in the car
	 * @param offer the offer for which you want to compute the passenger count
	 * @return the count of passengers that are still actively related to this offer
	 */
	public int getActivePassengerCountForOffer(TripOffer offer) {
		int passengerCount = 0;
		List<JoinTripRequest> joinRequests = joinTripRequestDAO.findByOfferId(offer.getId());
		for (JoinTripRequest request : joinRequests) {
			JoinTripStatus status = request.getStatus();
			if (!status.equals(JoinTripStatus.DRIVER_DECLINED)
					&& !status.equals(JoinTripStatus.PASSENGER_AT_DESTINATION)
					&& !status.equals(JoinTripStatus.PASSENGER_CANCELLED)
					&& !status.equals(JoinTripStatus.DRIVER_CANCELLED)) {

				++passengerCount;
			}
		}
		return passengerCount;
	}



}
