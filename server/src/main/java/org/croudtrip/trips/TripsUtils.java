package org.croudtrip.trips;

import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;

import java.util.List;

import javax.inject.Inject;

/**
 * Helper methods for dealing with offers, join requests, etc.
 */
public class TripsUtils {

	private final JoinTripRequestDAO joinTripRequestDAO;
    private final TripsNavigationManager tripsNavigationManager;
    private final GcmManager gcmManager;
    private final LogManager logManager;

	@Inject
	TripsUtils(
            JoinTripRequestDAO joinTripRequestDAO,
            TripsNavigationManager tripsNavigationManager,
            GcmManager gcmManager,
            LogManager logManager) {

        this.joinTripRequestDAO = joinTripRequestDAO;
        this.tripsNavigationManager = tripsNavigationManager;
        this.gcmManager = gcmManager;
        this.logManager = logManager;
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


    /**
     * Updates all the arrival times for the passengers that are waiting for the driver in this offer
     * and inform the passengers via GCM.
     * @param offer The offer that should be updated.
     */
    public void updateArrivalTimesForOffer(TripOffer offer) {
        updateArrivalTimesForOffer(offer, null);
    }


    /**
     * Updates all the arrival times for the passengers that are waiting for the driver in this offer
     * and inform the passengers via GCM. This method is used if a new passenger was accepted. The
     * newly accepted passenger will not be informed if he is passed as an additional parameter.
     * @param offer the offer that should be updated
     * @param noReceiver a certain user that will not receive a GCM
     */
    public void updateArrivalTimesForOffer(TripOffer offer, User noReceiver) {
        List<UserWayPoint> userWayPoints = tripsNavigationManager.getRouteWaypointsForOffer(offer);
        List<JoinTripRequest> joinRequests = joinTripRequestDAO.findByOfferId( offer.getId() );

        for( JoinTripRequest request : joinRequests ) {
            // only DRIVER_ACCEPTED requests matter, otherwise they are not active anymore or will be
            // DRIVER_ACCEPTED in future and therefore updated later
            if( !request.getStatus().equals( JoinTripStatus.DRIVER_ACCEPTED ) )
                continue;

            if( noReceiver != null && request.getSuperTrip().getQuery().getPassenger().getId() == noReceiver.getId())
                continue;

            // find estimated arrival time in list
            long arrivalTimestamp = 0;
            logManager.d("Potential match has " + userWayPoints.size() + " wps");
            for( UserWayPoint wp : userWayPoints ){
                logManager.d("WP for user " + wp.getUser().getFirstName());
                if( wp.getUser().getId() == request.getSuperTrip().getQuery().getPassenger().getId() ) {
                    arrivalTimestamp = wp.getArrivalTimestamp();
                    break;
                }
            }

            request = new JoinTripRequest(
                    request.getId(),
                    request.getTotalPriceInCents(),
                    request.getPricePerKmInCents(),
                    arrivalTimestamp,
                    request.getOffer(),
                    request.getStatus(),
                    request.getSuperTrip()
            );
            joinTripRequestDAO.update( request );

            gcmManager.sendArrivalTimeUpdate( request );
        }
    }

}
