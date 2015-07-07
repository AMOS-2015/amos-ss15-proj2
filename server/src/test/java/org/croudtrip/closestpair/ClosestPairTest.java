package org.croudtrip.closestpair;

import com.google.common.collect.Lists;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.logs.LogManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class ClosestPairTest {

    @Mocked Route pR;
    @Mocked Route dR;
    @Mocked LogManager logManager;

    private ClosestPair closestPair;

    private User p = new User.Builder().setId(0).build();

    @Before()
    public void setupClosestPair() {
        closestPair = new ClosestPair(logManager);
    }

    @Test
    public void simpleClosestPairTest() {
        final List<RouteLocation> pickUpRoutePoints = Lists.newArrayList(
                    new RouteLocation( 0, 0 ),
                    new RouteLocation( 1, 1),
                    new RouteLocation( 5, 5)
                );

        final List<RouteLocation> dropRoutePoints = Lists.newArrayList(
                new RouteLocation( 9, 9 ),
                new RouteLocation( 6, 6),
                new RouteLocation( 6, 8)
        );

        new Expectations(){
            {
                pR.getPolylineWaypointsForUser( p, null );
                result = pickUpRoutePoints;

                dR.getPolylineWaypointsForUser(p, null);
                result = dropRoutePoints;
            }
        };

        ClosestPairResult result = closestPair.findClosestPair(p, new NavigationResult(pR, null), new NavigationResult(dR, null));

        Assert.assertNotNull(result);
        Assert.assertNotNull( result.getDropLocation() );
        Assert.assertNotNull( result.getPickupLocation() );
        Assert.assertEquals( new RouteLocation(5,5), result.getPickupLocation() );
        Assert.assertEquals( new RouteLocation(6,6), result.getDropLocation() );
    }


}
