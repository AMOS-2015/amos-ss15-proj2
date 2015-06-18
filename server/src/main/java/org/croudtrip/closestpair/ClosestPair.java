package org.croudtrip.closestpair;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ClosestPair {

    /**
     * Find the closest pair of two routes that pick up and drop one specific passenger.
     * @param passenger The passenger that will be taken.
     * @param pickupNavigationResult A navigation result for the rout that picks up the passenger at his starting point
     * @param dropNavigationResult A navigation result for the route that drops the passenger at his destination
     * @return A {@link org.croudtrip.closestpair.ClosestPairResult} for the solved Closest pair problem.
     */
    public ClosestPairResult findClosestPair( User passenger, NavigationResult pickupNavigationResult, NavigationResult dropNavigationResult ) {
        // subdivide both routes into multiple points
        // this will get us a list of locations from the passenger pickup until the end of the trips
        // since we are creating a super trip there will be only one starting waypoint for this user
        List<RouteLocation> pickupLocations = pickupNavigationResult.getRoute().getPolylineWaypointsForUser(passenger, pickupNavigationResult.getUserWayPoints());

        // for the drop route only one end waypoint will exist and we will simple get all the points from the
        // beginning of the trip. So this is exactly what we want, since we don't need points if the
        // passenger is not in the car.
        List<RouteLocation> dropLocations = dropNavigationResult.getRoute().getPolylineWaypointsForUser(passenger, dropNavigationResult.getUserWayPoints());

        return findClosestPair( pickupLocations, dropLocations );
    }

    /**
     * First simple O(n*m) approach for the closest pair problem.
     */
    private ClosestPairResult findClosestPair( List<RouteLocation> pickupLocations, List<RouteLocation> dropLocations  ) {
        double minDistance = Double.MAX_VALUE;
        RouteLocation pickUp = null, drop = null;
        for( RouteLocation pickupLoc : pickupLocations ) {
            for( RouteLocation dropLoc : dropLocations ) {
                double dist = pickupLoc.distanceFrom(dropLoc);
                if( dist < minDistance ) {
                    minDistance = dist;
                    pickUp = pickupLoc;
                    drop = dropLoc;
                }
            }
        }

        return new ClosestPairResult( pickUp, drop );

    }

}
