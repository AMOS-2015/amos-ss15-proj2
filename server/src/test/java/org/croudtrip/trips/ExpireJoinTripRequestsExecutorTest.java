package org.croudtrip.trips;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.SuperTrip;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class ExpireJoinTripRequestsExecutorTest extends TestCase {

	private static final long currentTimestamp = System.currentTimeMillis() / 1000;

	private static final TripQuery query1 = new TripQuery.Builder()
			.setCreationTimestamp(currentTimestamp)
			.setMaxWaitingTimeInSeconds(100)
			.build();
	private static final TripQuery query2 = new TripQuery.Builder()
			.setCreationTimestamp(currentTimestamp - 10)
			.setMaxWaitingTimeInSeconds(1)
			.build();

	private static final JoinTripRequest
			request1 = new JoinTripRequest.Builder()
					.setStatus(JoinTripStatus.PASSENGER_ACCEPTED)
					.setSuperTrip(new SuperTrip.Builder().setQuery(query1).build())
					.build(),
			request2 = new JoinTripRequest.Builder()
					.setStatus(JoinTripStatus.PASSENGER_ACCEPTED)
					.setSuperTrip(new SuperTrip.Builder().setQuery(query2).build())
					.build(),
			request3 = new JoinTripRequest.Builder()
					.setStatus(JoinTripStatus.DRIVER_ACCEPTED)
					.setSuperTrip(new SuperTrip.Builder().setQuery(query2).build())
					.build();


	@Mocked TripsManager tripsManager;
	@Mocked GcmManager gcmManager;
	@Mocked SessionFactory sessionFactory;
	@Mocked LogManager logManager;
	private ExpireJoinTripRequestsExecutor executor;

	@Before
	public void setupExecutor() {
		executor = new ExpireJoinTripRequestsExecutor(tripsManager, gcmManager, sessionFactory, logManager);
	}

	@Test
	public void testExpireJoinRequests() throws Exception {
		new Expectations() {{
			tripsManager.findAllJoinRequests();
			result = Lists.newArrayList(request1, request2);
		}};

		executor.doRun();

		new Verifications() {{
			tripsManager.updateJoinRequestPassengerCancel(request1); times = 0;
			tripsManager.updateJoinRequestPassengerCancel(request2); times = 1;
			tripsManager.updateJoinRequestPassengerCancel(request3); times = 0;

			gcmManager.sendJoinTripRequestExpiredToPassenger(request2);
		}};
	}
}