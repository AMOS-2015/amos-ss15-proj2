package org.croudtrip.trips;

import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.SuperTripSubQuery;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.SuperTripReservationDAO;
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

	@Mocked
	SuperTripReservationDAO superTripReservationDAO;
	@Mocked SessionFactory sessionFactory;
	@Mocked LogManager logManager;

	private TripReservationGarbageCollectionExecutor collector;

	@Before
	public void setupCollector() {
		collector = new TripReservationGarbageCollectionExecutor(superTripReservationDAO, sessionFactory, logManager);
	}


	@Test
	public void testDoRun() {
		long currentTimestamp = System.currentTimeMillis() / 1000;
		final List<SuperTripReservation> reservations = new ArrayList<>();
		final SuperTripReservation newReservation = createReservation(currentTimestamp - 10, 1000);
		final SuperTripReservation oldReservation = createReservation(currentTimestamp - 10000, 10);
		reservations.add(newReservation);
		reservations.add(oldReservation);

		new Expectations() {{
			superTripReservationDAO.findAll();
			result = reservations;
		}};

		collector.doRun();

		new Verifications() {{
			superTripReservationDAO.delete(oldReservation); times = 1;
			superTripReservationDAO.delete(newReservation); times = 0;
		}};
	}


	private SuperTripReservation createReservation(long creationTimestamp, long maxWaitingTime) {
        TripQuery query = new TripQuery.Builder().setMaxWaitingTimeInSeconds(maxWaitingTime).setCreationTimestamp(creationTimestamp).build();
		return new SuperTripReservation.Builder()
				.setQuery(query)
				.addReservation(new TripReservation( new SuperTripSubQuery(query), 0, 0, 0, null))
				.build();
	}
}
