package org.croudtrip.trips;

import com.google.common.collect.Lists;

import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.logs.LogManager;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class DisableTripOffersExecutorTest {

	@Mocked TripOfferDAO tripOfferDAO;
	@Mocked TripsUtils tripsUtils;
	@Mocked SessionFactory sessionFactory;
	@Mocked LogManager logManager;

	private DisableTripOffersExecutor executor;

	@Before
	public void setupExecutor() {
		this.executor = new DisableTripOffersExecutor(tripOfferDAO, tripsUtils, sessionFactory, logManager);
	}

	@Test
	public void testDisableOffer() throws Exception {
		final TripOffer offer1 = new TripOffer.Builder()
				.setStatus(TripOfferStatus.FINISHED)
				.build();
		final TripOffer offer2 = new TripOffer.Builder()
				.setStatus(TripOfferStatus.ACTIVE)
				.setLastPositonUpdateInSeconds(0)
				.build();

		new Expectations() {{
			tripOfferDAO.findAll();
			result = Lists.newArrayList(offer1, offer2);
		}};

		executor.doRun();

		new Verifications() {{
			TripOffer updatedOffer;
			tripOfferDAO.update(updatedOffer = withCapture());
			Assert.assertEquals(TripOfferStatus.DISABLED, updatedOffer.getStatus());
		}};
	}

	@Test
	public void testEnableOffer() throws Exception {
		final TripOffer offer1 = new TripOffer.Builder()
				.setStatus(TripOfferStatus.FINISHED)
				.build();
		final TripOffer offer2 = new TripOffer.Builder()
				.setStatus(TripOfferStatus.DISABLED)
				.setLastPositonUpdateInSeconds(System.currentTimeMillis() / 1000)
				.build();

		new Expectations() {{
			tripOfferDAO.findAll();
			result = Lists.newArrayList(offer1, offer2);
		}};

		executor.doRun();

		new Verifications() {{
			TripOffer updatedOffer;
			tripOfferDAO.update(updatedOffer = withCapture());
			Assert.assertEquals(TripOfferStatus.ACTIVE, updatedOffer.getStatus());

			tripsUtils.checkAndUpdateRunningQueries(updatedOffer);
		}};
	}

}