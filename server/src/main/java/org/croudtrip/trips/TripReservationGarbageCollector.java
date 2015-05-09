package org.croudtrip.trips;

import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.logs.LogManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.dropwizard.lifecycle.Managed;

/**
 * Removes unused {@link org.croudtrip.api.trips.TripReservation} after they have expired.
 */
public class TripReservationGarbageCollector implements Managed, Runnable {

	private final TripReservationDAO tripReservationDAO;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final SessionFactory sessionFactory;
	private final LogManager logManager;

	private ScheduledFuture<?> gcTask = null;

	@Inject
	TripReservationGarbageCollector(
			TripReservationDAO tripReservationDAO,
			SessionFactory sessionFactory,
			LogManager logManager) {

		this.tripReservationDAO = tripReservationDAO;
		this.sessionFactory = sessionFactory;
		this.logManager = logManager;
	}

	@Override
	public void start() throws Exception {
		gcTask = scheduler.scheduleAtFixedRate(this, 0, 1, TimeUnit.HOURS);
	}

	@Override
	public void stop() throws Exception {
		gcTask.cancel(true);
	}

	@Override
	public void run() {
		logManager.d("Running trip reservations GC");
		Session session = null;
		try {
			session = sessionFactory.openSession();
			ManagedSessionContext.bind(session);

			long currentTimestamp = System.currentTimeMillis() / 1000;
			for (TripReservation reservation : tripReservationDAO.findAll()) {
				if (currentTimestamp > reservation.getCreationTimestamp() + reservation.getQuery().getMaxWaitingTimeInSeconds()) {
					tripReservationDAO.delete(reservation);
				}
			}
		} catch (Exception e) {
			logManager.e(e, "failed to run reservations GC");
		} finally {
			if (session != null) {
				session.flush();
				session.close();
			}
		}

	}

}
