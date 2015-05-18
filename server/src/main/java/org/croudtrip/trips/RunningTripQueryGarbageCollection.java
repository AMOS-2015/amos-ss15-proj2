package org.croudtrip.trips;

import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Removes unused {@link RunningTripQuery} after they have expired.
 */
public class RunningTripQueryGarbageCollection extends AbstractScheduledTaskExecutor {

	/**
	 * Keeps {@link RunningTripQuery} around a little longer to give clients a chance to download results.
	 */
	private static final int ADDITIONAL_LIFETIME_IN_SECONDS = 60 * 60 * 12; // 12 hours

	private final RunningTripQueryDAO runningTripQueryDAO;

	@Inject
    RunningTripQueryGarbageCollection(
            RunningTripQueryDAO runningTripQueryDAO,
            SessionFactory sessionFactory,
            LogManager logManager) {

		super(sessionFactory, logManager, 15, TimeUnit.SECONDS);
		this.runningTripQueryDAO = runningTripQueryDAO;
	}

	@Override
	protected void doRun() {
		long currentTimestamp = System.currentTimeMillis() / 1000;
		for (RunningTripQuery query : runningTripQueryDAO.findAll()) {
			if (currentTimestamp > query.getCreationTimestamp() + query.getQuery().getMaxWaitingTimeInSeconds() + ADDITIONAL_LIFETIME_IN_SECONDS) {
				logManager.d("Removing RunningQuery " + query.getId());
				runningTripQueryDAO.delete(query);
			}

		}
	}

}
