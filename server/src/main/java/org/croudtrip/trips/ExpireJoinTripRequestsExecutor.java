package org.croudtrip.trips;

import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
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

    @Inject
    ExpireJoinTripRequestsExecutor(TripsManager tripsManager, SessionFactory sessionFactory, LogManager logManager) {
        super( sessionFactory, logManager, 15, TimeUnit.MINUTES);
        this.tripsManager = tripsManager;
    }

    @Override
    protected void doRun() throws Exception {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        for (JoinTripRequest joinTripRequest : tripsManager.findAllJoinRequests()) {
            if (!joinTripRequest.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED)) continue;

            if (currentTimestamp > joinTripRequest.getQuery().getCreationTimestamp() + joinTripRequest.getQuery().getMaxWaitingTimeInSeconds()) {
                tripsManager.updateJoinRequestPassengerCancel(joinTripRequest);
            }
        }
    }

}
