package org.croudtrip.trips;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.RunningTripQueryStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class RunningTripQueriesManagerTest {

	@Mocked RunningTripQueryDAO runningTripQueryDAO;
	@Mocked
	SimpleTripsMatcher simpleTripsMatcher;
	@Mocked GcmManager gcmManager;

	private RunningTripQueriesManager manager;

	@Before
	public void setpuManager() {
		this.manager = new RunningTripQueriesManager(runningTripQueryDAO, simpleTripsMatcher, gcmManager);
	}

	@Test
	@SuppressWarnings("rawTypes")
	public void testCheckAndUpdateRunningQueries() {
		TripQuery oldQuery = new TripQuery.Builder()
				.setCreationTimestamp(0)
				.build();
		final TripQuery newQuery = new TripQuery.Builder()
				.setCreationTimestamp(System.currentTimeMillis() / 1000)
				.setMaxWaitingTimeInSeconds(1000)
				.build();
		final RunningTripQuery runningQuery1 = new RunningTripQuery(0, newQuery, RunningTripQueryStatus.RUNNING);
		final RunningTripQuery runningQuery2 = new RunningTripQuery(0, oldQuery, RunningTripQueryStatus.RUNNING);

		final TripOffer offer = new TripOffer(0, null, 0, null, 0, 0, null, null, null, 0);

		new Expectations() {{
			runningTripQueryDAO.findByStatusRunning();
			result = Lists.newArrayList(runningQuery1, runningQuery2);

			simpleTripsMatcher.isPotentialMatch((TripOffer) any, (TripQuery) any);
			result = Optional.of(new SimpleTripsMatcher.PotentialMatch(offer, newQuery, null));
		}};

		manager.checkAndUpdateRunningQueries(offer);

		new Verifications() {{
			gcmManager.sendGcmMessageToUser(newQuery.getPassenger(), anyString, (Pair) any);

			RunningTripQuery resultQuery;
			runningTripQueryDAO.update(resultQuery = withCapture());
			Assert.assertEquals(RunningTripQueryStatus.FOUND, resultQuery.getStatus());
		}};
	}


}