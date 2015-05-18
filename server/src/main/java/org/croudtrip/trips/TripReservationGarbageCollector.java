package org.croudtrip.trips;

import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Removes unused {@link org.croudtrip.api.trips.TripReservation} after they have expired.
 */
public class TripReservationGarbageCollector extends AbstractScheduledTaskExecutor {

	private final TripReservationDAO tripReservationDAO;

	@Inject
	TripReservationGarbageCollector(
			TripReservationDAO tripReservationDAO,
			SessionFactory sessionFactory,
			LogManager logManager) {

		super(sessionFactory, logManager, 60*60, TimeUnit.SECONDS);
		this.tripReservationDAO = tripReservationDAO;
	}

	@Override
	protected void doRun() {
		long currentTimestamp = System.currentTimeMillis() / 1000;
		for (TripReservation reservation : tripReservationDAO.findAll()) {
			if (currentTimestamp > reservation.getCreationTimestamp() + reservation.getQuery().getMaxWaitingTimeInSeconds()) {
				logManager.d("Removing reservation " + reservation.getId());
				tripReservationDAO.delete(reservation);
			}
		}
	}

}
