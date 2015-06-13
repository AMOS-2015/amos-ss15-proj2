package org.croudtrip.trips;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.RunningTripQueryStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.Pair;

import java.util.List;

import javax.inject.Inject;

/**
 * Helper methods for dealing with offers, join requests, etc.
 */
public class TripsUtils {

	private final JoinTripRequestDAO joinTripRequestDAO;
    private final RunningTripQueryDAO runningTripQueryDAO;
    private final TripsMatcher tripsMatcher;
    private final TripsNavigationManager tripsNavigationManager;
    private final GcmManager gcmManager;
    private final LogManager logManager;

	@Inject
	TripsUtils(
            JoinTripRequestDAO joinTripRequestDAO,
            RunningTripQueryDAO runningTripQueryDAO,
            TripsMatcher tripsMatcher,
            TripsNavigationManager tripsNavigationManager,
            GcmManager gcmManager,
            LogManager logManager) {

        this.joinTripRequestDAO = joinTripRequestDAO;
        this.runningTripQueryDAO = runningTripQueryDAO;
        this.tripsMatcher = tripsMatcher;
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

            if( noReceiver != null && request.getQuery().getPassenger().getId() == noReceiver.getId())
                continue;

            // find estimated arrival time in list
            long arrivalTimestamp = 0;
            logManager.d("Potential match has " + userWayPoints.size() + " wps");
            for( UserWayPoint wp : userWayPoints ){
                logManager.d("WP for user " + wp.getUser().getFirstName());
                if( wp.getUser().getId() == request.getQuery().getPassenger().getId() ) {
                    arrivalTimestamp = wp.getArrivalTimestamp();
                    break;
                }
            }

            request = new JoinTripRequest(
                    request.getId(),
                    request.getQuery(),
                    request.getTotalPriceInCents(),
                    request.getPricePerKmInCents(),
                    arrivalTimestamp,
                    request.getOffer(),
                    request.getStatus()
            );
            joinTripRequestDAO.update( request );

            gcmManager.sendArrivalTimeUpdate( request );
        }
    }

    /**
     * Checks if the passed in offer is a match for one of the running background searches
     * and alerts the passenger if that is the case.
     */
    public void checkAndUpdateRunningQueries(TripOffer offer) {
        for (RunningTripQuery runningQuery : runningTripQueryDAO.findByStatusRunning()) {
            // check max waiting time
            if (runningQuery.getQuery().getCreationTimestamp() + runningQuery.getQuery().getMaxWaitingTimeInSeconds() < System.currentTimeMillis() / 1000) continue;

            // check if the newly offered trip matches to a running trip query
            TripQuery query = runningQuery.getQuery();
            Optional<TripsMatcher.PotentialMatch> potentialMatch = tripsMatcher.isPotentialMatch(offer, query);

            // notify passenger about potential match
            if (potentialMatch.isPresent()) {
                gcmManager.sendGcmMessageToUser(
                        query.getPassenger(),
                        GcmConstants.GCM_MSG_FOUND_MATCHES,
                        new Pair<>(GcmConstants.GCM_MSG_FOUND_MATCHES_QUERY_ID, "" + runningQuery.getId()));
                RunningTripQuery updatedRunningQuery = new RunningTripQuery(
                        runningQuery.getId(),
                        runningQuery.getQuery(),
                        RunningTripQueryStatus.FOUND);
                runningTripQueryDAO.update(updatedRunningQuery);
            }
        }
    }

}
