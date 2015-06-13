package org.croudtrip.trips;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class TripsManagerTest {

    @Mocked JoinTripRequestDAO joinTripRequestDAO;
    @Mocked TripOfferDAO tripOfferDAO;
    @Mocked DirectionsManager directionsManager;
    @Mocked LogManager logManager;
    @Mocked TripsUtils tripsUtils;
    @Mocked TripReservationDAO tripReservationDAO;
    @Mocked VehicleManager vehicleManager;
    @Mocked TripsMatcher tripsMatcher;
    @Mocked GcmManager gcmManager;
    @Mocked RunningTripQueryDAO runningTripQueryDAO;

    private TripsManager tripsManager;

    RouteLocation tripStart = new RouteLocation(0,0);
    RouteLocation tripEnd = new RouteLocation(1,1);

    @Before
    public void setupTripsManager() {
        tripsManager = new TripsManager( tripOfferDAO, runningTripQueryDAO, tripReservationDAO, joinTripRequestDAO, directionsManager, vehicleManager, gcmManager, tripsMatcher, tripsUtils, logManager );
    }

    @Test
    public void testAddOffer() {
        final TripOfferDescription offerDescription = new TripOfferDescription(tripStart, tripEnd, 100, 10, 0);
        final User driver = new User(0, "", "", "", "", true, new Date(0), "", "", 0);
        final Route finalRoute = new Route( Lists.newArrayList(tripStart, tripEnd), "", 12345, 12345, Lists.newArrayList(12345L), Lists.newArrayList(12345L), null, null, 0  );
        final Vehicle vehicle = new Vehicle(0, "abc", "", "", 1, driver);

        final TripOffer offer = new TripOffer(0,
                finalRoute,
                System.currentTimeMillis() / 1000 + finalRoute.getDurationInSeconds(),
                offerDescription.getStart(),
                offerDescription.getMaxDiversionInMeters(),
                offerDescription.getPricePerKmInCents(),
                driver,
                vehicle,
                TripOfferStatus.ACTIVE,
                System.currentTimeMillis() / 1000);

        new Expectations(){{
            directionsManager.getDirections( tripStart, tripEnd);
            result = Lists.newArrayList( finalRoute );

            vehicleManager.findVehicleById( offerDescription.getVehicleId() );
            result = Optional.of(vehicle);
        }};

        TripOffer addedOffer = null;
        try {
            addedOffer = tripsManager.addOffer( driver, offerDescription );
        } catch (RouteNotFoundException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(addedOffer);
        assertEquals( offer, addedOffer );
    }

    @Test
    public void testSimplePositionUpdateOffer() {
        // test update position
        RouteLocation updateLocation = new RouteLocation(1,1);
        TripOffer offer = new TripOffer( 0, null, 0, null, 0, 0, null, null, TripOfferStatus.ACTIVE, 0 );
        TripOfferUpdate positionUpdate = TripOfferUpdate.createNewStartUpdate( updateLocation );

        TripOffer updatedOffer = tripsManager.updateOffer( offer, positionUpdate );

        Assert.assertEquals( TripOfferStatus.ACTIVE, updatedOffer.getStatus() );
        Assert.assertEquals( updateLocation, updatedOffer.getCurrentLocation() );
    }

    @Test
    public void testSimpleCancelUpdateOffer() {
        // test cancel trip
        TripOffer offer = new TripOffer( 0, null, 0, null, 0, 0, null, null, TripOfferStatus.ACTIVE, 0 );
        TripOfferUpdate cancelUpdate = TripOfferUpdate.createCancelUpdate();

        TripOffer updatedOffer = tripsManager.updateOffer( offer, cancelUpdate );

        Assert.assertEquals( TripOfferStatus.CANCELLED, updatedOffer.getStatus());
    }

    @Test
    public void testQueryOffers() {
        final User d1 = new User(1, "", "", "", "", true, new Date(0), "", "", 0);
        final User d2 = new User(2, "", "", "", "", true, new Date(0), "", "", 0);
        final User d3 = new User(3, "", "", "", "", true, new Date(0), "", "", 0);
        final User d4 = new User(4, "", "", "", "", true, new Date(0), "", "", 0);
        final User p = new User(10, "", "", "", "", true, new Date(0), "", "", 0);

        final Route passengerRoute = new Route( Lists.newArrayList(tripStart, tripEnd), "", 12345, 12345, Lists.newArrayList(12345L), Lists.newArrayList(12345L), null, null, 0  );
        final TripQuery query = new TripQuery( passengerRoute, tripStart, tripEnd, 0, 0, p);

        new Expectations(){{
            directionsManager.getDirections( tripStart, tripEnd );
            result = Lists.newArrayList( passengerRoute );

            tripOfferDAO.findAllActive();
            result = Lists.newArrayList(
                    new TripOffer(0, null, 0, null, 0, 0, d1, null, TripOfferStatus.ACTIVE, 0 ),
                    new TripOffer(0, null, 0, null, 0, 0, d2, null, TripOfferStatus.ACTIVE, 0 ),
                    new TripOffer(0, null, 0, null, 0, 0, d3, null, TripOfferStatus.ACTIVE, 0 ),
                    new TripOffer(0, null, 0, null, 0, 0, d4, null, TripOfferStatus.ACTIVE, 0 )
            );

            tripsMatcher.filterPotentialMatches( (List<TripOffer>)(any), (TripQuery)(any) );
            result = Lists.newArrayList(
                    new TripOffer(0, null, 0, null, 0, 4, d1, null, TripOfferStatus.ACTIVE, 0 ),
                    new TripOffer(0, null, 0, null, 0, 14, d2, null, TripOfferStatus.ACTIVE, 0 ),
                    new TripOffer(0, null, 0, null, 0, 12, d3, null, TripOfferStatus.ACTIVE, 0 ),
                    new TripOffer(0, null, 0, null, 0, 3, d4, null, TripOfferStatus.ACTIVE, 0 )
            );

        }};

        TripQueryResult result = null;
        try {
             result = tripsManager.queryOffers(p, new TripQueryDescription(tripStart, tripEnd, 0));
        } catch (RouteNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull(result);

        // running query should be null since there were results for it
        RunningTripQuery runningQuery = result.getRunningQuery();
        Assert.assertNull( runningQuery );

        List<TripReservation> reservations = result.getReservations();
        Assert.assertEquals( "Wrong reservations count", 1, reservations.size() );
        Assert.assertEquals( "Wrong driver", d4, reservations.get(0).getDriver());
        Assert.assertEquals( "Wrong query", query.getPassenger(), reservations.get(0).getQuery().getPassenger());
        Assert.assertEquals( "Wrong query", query.getStartLocation(), reservations.get(0).getQuery().getStartLocation());
        Assert.assertEquals( "Wrong query", query.getDestinationLocation(), reservations.get(0).getQuery().getDestinationLocation());
        Assert.assertEquals( "Wrong price per kilometer", 3, reservations.get(0).getPricePerKmInCents());
        Assert.assertEquals( "Wrong total price", 4*12345/1000, reservations.get(0).getTotalPriceInCents());
    }

    @Test
    public void testJoinTrip() {
        final User d = new User(1, "", "", "", "", true, new Date(0), "", "", 0);
        final User p = new User(10, "", "", "", "", true, new Date(0), "", "", 0);

        final RouteLocation passengerStart = new RouteLocation(2,2);
        final RouteLocation passengerEnd = new RouteLocation(3,3);

        final Route passengerRoute = new Route( Lists.newArrayList(passengerStart, passengerEnd), "", 12345, 12345, Lists.newArrayList(12345L), Lists.newArrayList(12345L), null, null, 0  );
        final TripQuery query = new TripQuery( passengerRoute, passengerStart, passengerEnd, 0, 0, p);

        final TripOffer offer = new TripOffer(0, null, 0, tripStart, 10, 10, d, null, TripOfferStatus.ACTIVE, 0);

        TripReservation reservation = new TripReservation( 0, query, 12345, 10, 0, d );

        new Expectations(){{

            tripOfferDAO.findById( anyLong );
            result = Optional.of(offer);

            tripsMatcher.isPotentialMatch( offer, query );
            result = Optional.of( new TripsMatcher.PotentialMatch( offer, query, Lists.newArrayList(
                    new UserWayPoint(d, tripStart, true, 0, 0 ),
                    new UserWayPoint(p, passengerStart, true, 1, 1  ),
                    new UserWayPoint(p, passengerEnd, false, 2, 2  ),
                    new UserWayPoint(d, tripEnd, false, 3, 3  )
            ) ));
        }};

        Optional<JoinTripRequest> requestOptional = tripsManager.joinTrip( reservation );

        Assert.assertTrue( requestOptional.isPresent() );
        Assert.assertEquals( query, requestOptional.get().getQuery() );
        Assert.assertEquals( offer, requestOptional.get().getOffer() );
        Assert.assertEquals( reservation.getTotalPriceInCents(), requestOptional.get().getTotalPriceInCents());
        Assert.assertEquals( reservation.getPricePerKmInCents(), requestOptional.get().getPricePerKmInCents());
        Assert.assertEquals( 1, requestOptional.get().getEstimatedArrivalTimestamp());
    }

    @Test
    public void testUpdateJoinRequestAcceptance() {
        // TODO: add test code here
    }

    @Test
    public void testUpdateJoinRequestAcceptanceAlreadyModifiedFails() {
        JoinTripRequest joinRequest = new JoinTripRequest( 0, null, 0 ,0 , 0, null, JoinTripStatus.DRIVER_ACCEPTED );
        testFailingJoinRequestAcceptanceUpdate(joinRequest);

        joinRequest = new JoinTripRequest( 0, null, 0 ,0 , 0, null, JoinTripStatus.DRIVER_CANCELLED );
        testFailingJoinRequestAcceptanceUpdate(joinRequest);

        joinRequest = new JoinTripRequest( 0, null, 0 ,0 , 0, null, JoinTripStatus.PASSENGER_AT_DESTINATION );
        testFailingJoinRequestAcceptanceUpdate(joinRequest);

        joinRequest = new JoinTripRequest( 0, null, 0 ,0 , 0, null, JoinTripStatus.PASSENGER_IN_CAR );
        testFailingJoinRequestAcceptanceUpdate(joinRequest);

        joinRequest = new JoinTripRequest( 0, null, 0 ,0 , 0, null, JoinTripStatus.DRIVER_DECLINED );
        testFailingJoinRequestAcceptanceUpdate(joinRequest);

        joinRequest = new JoinTripRequest( 0, null, 0 ,0 , 0, null, JoinTripStatus.PASSENGER_CANCELLED );
        testFailingJoinRequestAcceptanceUpdate(joinRequest);
    }

    private void testFailingJoinRequestAcceptanceUpdate(JoinTripRequest joinRequest) {
        boolean exception = false;
        try {
            tripsManager.updateJoinRequestAcceptance(joinRequest, true);
        } catch( IllegalArgumentException e){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testUpdateJoinRequestPassengerExitCar() {
        // TODO: add test code here
    }

    @Test
    public void testUpdateJoinRequestPassengerCancel() {
        // TODO: add test code here
    }

    @Test
    public void testSimpleFinishUpdateOffer() {
        // test finish trip
        TripOffer offer = new TripOffer( 0, null, 0, null, 0, 0, null, null, TripOfferStatus.ACTIVE, 0 );
        TripOfferUpdate finishUpdate = TripOfferUpdate.createFinishUpdate();

        TripOffer updatedOffer = tripsManager.updateOffer( offer, finishUpdate );

        Assert.assertEquals(TripOfferStatus.FINISHED, updatedOffer.getStatus());
    }

    private void assertEquals( TripOffer expected, TripOffer actual ){
        Assert.assertEquals(expected.getStatus(), actual.getStatus());
        Assert.assertEquals(expected.getDriverRoute(), actual.getDriverRoute());
        Assert.assertEquals(expected.getCurrentLocation(), actual.getCurrentLocation());
        Assert.assertEquals(expected.getVehicle(), expected.getVehicle());
        Assert.assertEquals(expected.getPricePerKmInCents(), actual.getPricePerKmInCents());
        Assert.assertEquals(expected.getMaxDiversionInMeters(), actual.getMaxDiversionInMeters());
    }

}
