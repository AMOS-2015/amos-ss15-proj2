package org.croudtrip.trips;


import com.google.common.collect.Lists;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.SuperPassengerTrip;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class TripsUtilsTest {

	@Mocked JoinTripRequestDAO joinTripRequestDAO;
    @Mocked TripsNavigationManager tripsNavigationManager;
    @Mocked GcmManager gcmManager;
    @Mocked LogManager logManager;
	private TripsUtils tripsUtils;


	@Before
	public void setupUtils() {
		this.tripsUtils = new TripsUtils(joinTripRequestDAO, tripsNavigationManager, gcmManager, logManager);
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
        final User p1 = new User(0, "1", "", "", "", true, new Date(0), "", "", 0);
        final User p2 = new User(1, "2", "", "", "", true, new Date(0), "", "", 0);
        final User p3 = new User(2, "3", "", "", "", true, new Date(0), "", "", 0);
        final User p4 = new User(3, "4", "", "", "", true, new Date(0), "", "", 0);
        final User p5 = new User(4, "5", "", "", "", true, new Date(0), "", "", 0);

        final List<JoinTripRequest> resultRequests = new ArrayList<>();

        new Expectations(){{
            tripsNavigationManager.getRouteWaypointsForOffer(offer);
            result = Lists.newArrayList(
                    new UserWayPoint( p1, new RouteLocation(0,0), true, 1, 100),
                    new UserWayPoint( p1, new RouteLocation(0,0), false, 2, 200),
                    new UserWayPoint( p3, new RouteLocation(0,0), true, 3, 300),
                    new UserWayPoint( p2, new RouteLocation(0,0), false, 5, 500),
                    new UserWayPoint( p3, new RouteLocation(0,0), false, 6, 600),
                    new UserWayPoint( p4, new RouteLocation(0,0), true, 7, 700),
                    new UserWayPoint( p4, new RouteLocation(0,0), false, 8, 800)
            );

            joinTripRequestDAO.findByOfferId(offer.getId());
            result = Lists.newArrayList(
                    createJoinRequest(p1, offer, JoinTripStatus.DRIVER_ACCEPTED),
                    createJoinRequest(p3, offer, JoinTripStatus.DRIVER_ACCEPTED),
                    createJoinRequest(p4, offer, JoinTripStatus.DRIVER_ACCEPTED),
                    createJoinRequest(p5, offer, JoinTripStatus.PASSENGER_ACCEPTED),
                    createJoinRequest(p5, offer, JoinTripStatus.PASSENGER_AT_DESTINATION),
                    createJoinRequest(p2, offer, JoinTripStatus.PASSENGER_IN_CAR),
                    createJoinRequest(p5, offer, JoinTripStatus.PASSENGER_CANCELLED),
                    createJoinRequest(p5, offer, JoinTripStatus.DRIVER_CANCELLED));

            joinTripRequestDAO.update( (JoinTripRequest)(any) );
            result = new Delegate() {
                void update( JoinTripRequest request ){
                    resultRequests.add(request);
                }
            };
        }};

        tripsUtils.updateArrivalTimesForOffer( offer );

        Assert.assertTrue(resultRequests.size() > 0);

        for( JoinTripRequest request : resultRequests ){
            if( request.getStatus() != JoinTripStatus.DRIVER_ACCEPTED ) {
                Assert.assertEquals( 12345, request.getEstimatedArrivalTimestamp());
            }
            else {
                int userId = (int) request.getSuperPassengerTrip().getQuery().getPassenger().getId();
                switch( userId ) {
                    case 0:
                        Assert.assertEquals( request.getEstimatedArrivalTimestamp(), 1 );
                        break;
                    case 1:
                        Assert.fail("p2 is in the car and not waiting for the driver");
                        break;
                    case 2:
                        Assert.assertEquals( request.getEstimatedArrivalTimestamp(), 3 );
                        break;
                    case 3:
                        Assert.assertEquals( request.getEstimatedArrivalTimestamp(), 7 );
                        break;
                    case 4:
                        Assert.fail("User is not accepted for this trip");
                        break;
                }
            }
        }
    }


	private JoinTripRequest createJoinRequest(TripOffer offer, JoinTripStatus status) {
		return new JoinTripRequest.Builder()
                .setStatus(status)
                .setOffer(offer)
                .build();
	}


    private JoinTripRequest createJoinRequest( User passenger, TripOffer offer, JoinTripStatus status){
        TripQuery query = new TripQuery(null, null, null, 0, 0, passenger);
        return new JoinTripRequest.Builder()
                .setOffer(offer)
                .setStatus(status)
                .setEstimatedArrivalTimestamp(12345)
                .setSuperPassengerTrip(new SuperPassengerTrip.Builder().setQuery(query).build())
                .build();
    }

}