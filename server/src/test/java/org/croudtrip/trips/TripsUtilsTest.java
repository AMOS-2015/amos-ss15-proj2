package org.croudtrip.trips;


import com.google.common.collect.Lists;

import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.gcm.GcmManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class TripsUtilsTest {

	@Mocked JoinTripRequestDAO joinTripRequestDAO;
    @Mocked TripsNavigationManager tripsNavigationManager;
    @Mocked GcmManager gcmManager;
	private TripsUtils tripsUtils;


	@Before
	public void setupUtils() {
		this.tripsUtils = new TripsUtils(joinTripRequestDAO, tripsNavigationManager, gcmManager);
	}


	@Test
	public void testGetActivePassengerCountForOffer() {
		final TripOffer offer = new TripOffer(0, null, 0, null, 0, 0, null, null, null, 0);

		new Expectations() {{
			joinTripRequestDAO.findByOfferId(offer.getId());
			result = Lists.newArrayList(
					createJoinRequest(offer, JoinTripStatus.DRIVER_ACCEPTED),
					createJoinRequest(offer, JoinTripStatus.DRIVER_DECLINED),
					createJoinRequest(offer, JoinTripStatus.PASSENGER_ACCEPTED),
					createJoinRequest(offer, JoinTripStatus.PASSENGER_AT_DESTINATION),
					createJoinRequest(offer, JoinTripStatus.PASSENGER_IN_CAR),
					createJoinRequest(offer, JoinTripStatus.PASSENGER_CANCELLED),
					createJoinRequest(offer, JoinTripStatus.DRIVER_CANCELLED));
		}};

		int passengerCount = tripsUtils.getActivePassengerCountForOffer(offer);
		Assert.assertEquals(3, passengerCount);
	}

    @Test
    public void testUpdateArrivalTimesForOffer() {
        final TripOffer offer = new TripOffer(0, null, 0, null, 0, 0, null, null, null, 0);

        // TODO: write some test here
    }


	private JoinTripRequest createJoinRequest(TripOffer offer, JoinTripStatus status) {
		return new JoinTripRequest(0, null, 0, 0, 0, offer, status);
	}

}