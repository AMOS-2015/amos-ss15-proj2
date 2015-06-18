package org.croudtrip.trips;

import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Cancels {@link org.croudtrip.api.trips.JoinTripRequest}s after their
 * max waiting time has passed.
 */
public class ExpireJoinTripRequestsExecutor extends AbstractScheduledTaskExecutor {

    private TripsManager tripsManager;
    private GcmManager gcmManager;

    @Inject
    ExpireJoinTripRequestsExecutor(TripsManager tripsManager, GcmManager gcmManager, SessionFactory sessionFactory, LogManager logManager) {
        super( sessionFactory, logManager, 15, TimeUnit.MINUTES);
        this.tripsManager = tripsManager;
        this.gcmManager = gcmManager;
    }

    @Override
    protected void doRun() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        for (JoinTripRequest joinTripRequest : tripsManager.findAllJoinRequests()) {
            if (!joinTripRequest.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED)) continue;

            TripQuery query = joinTripRequest.getSuperPassengerTrip().getQuery();
            if (currentTimestamp > query.getCreationTimestamp() + query.getMaxWaitingTimeInSeconds()) {
                tripsManager.updateJoinRequestPassengerCancel(joinTripRequest);
                gcmManager.sendJoinTripRequestExpiredToPassenger(joinTripRequest);
            }
        }
    }

}
