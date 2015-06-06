package org.croudtrip.trips;

import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.logs.LogManager;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class TripReservationGarbageCollectionExecutorTest {

	@Mocked TripReservationDAO tripReservationDAO;
	@Mocked SessionFactory sessionFactory;
	@Mocked LogManager logManager;

	private TripReservationGarbageCollectionExecutor collector;

	@Before
	public void setupCollector() {
		collector = new TripReservationGarbageCollectionExecutor(tripReservationDAO, sessionFactory, logManager);
	}


	@Test
	public void testDoRun() {
		long currentTimestamp = System.currentTimeMillis() / 1000;
		final List<TripReservation> reservations = new ArrayList<>();
		final TripReservation newReservation = createReservation(currentTimestamp - 10, 1000);
		final TripReservation oldReservation = createReservation(currentTimestamp - 10000, 10);
		reservations.add(newReservation);
		reservations.add(oldReservation);

		new Expectations() {{
			tripReservationDAO.findAll();
			result = reservations;
		}};

		collector.doRun();

		new Verifications() {{
			tripReservationDAO.delete(oldReservation); times = 1;
			tripReservationDAO.delete(newReservation); times = 0;
		}};
	}


	private TripReservation createReservation(long creationTimestamp, long maxWaitingTime) {
		return new TripReservation(0,
				new TripQuery(null, null, null, maxWaitingTime, null),
				0,
				0,
				0,
				null,
				creationTimestamp);
	}
}
