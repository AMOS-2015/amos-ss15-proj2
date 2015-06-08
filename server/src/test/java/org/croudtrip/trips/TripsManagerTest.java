package org.croudtrip.trips;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
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
    @Mocked TripsNavigationManager tripsNavigationManager;
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
                System.currentTimeMillis()/1000+finalRoute.getDurationInSeconds(),
                offerDescription.getStart(),
                offerDescription.getMaxDiversionInMeters(),
                offerDescription.getPricePerKmInCents(),
                driver,
                vehicle,
                TripOfferStatus.ACTIVE_NOT_FULL,
                System.currentTimeMillis()/1000
                );

        new Expectations(){{
            directionsManager.getDirections( tripStart, tripEnd);
            result = Lists.newArrayList( finalRoute );

            vehicleManager.findVehicleById( offerDescription.getVehicleId() );
            result = Optional.of(vehicle);

            runningTripQueryDAO.findByStatusRunning();
            result = Lists.newArrayList();
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

    private void assertEquals( TripOffer expected, TripOffer actual ){
        Assert.assertEquals(expected.getStatus(), actual.getStatus());
        Assert.assertEquals(expected.getDriverRoute(), actual.getDriverRoute());
        Assert.assertEquals(expected.getCurrentLocation(), actual.getCurrentLocation());
        Assert.assertEquals(expected.getVehicle(), expected.getVehicle());
        Assert.assertEquals(expected.getPricePerKmInCents(), actual.getPricePerKmInCents());
        Assert.assertEquals(expected.getMaxDiversionInMeters(), actual.getMaxDiversionInMeters());
    }

}
