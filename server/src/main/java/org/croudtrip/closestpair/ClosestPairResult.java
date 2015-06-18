package org.croudtrip.closestpair;

import org.croudtrip.api.directions.RouteLocation;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ClosestPairResult {
    private RouteLocation pickupLocation;
    private RouteLocation dropLocation;
    private double distance;

    public ClosestPairResult(RouteLocation pickupLocation, RouteLocation dropLocation) {
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;

        this.distance = this.pickupLocation.distanceFrom( this.dropLocation );
    }

    public RouteLocation getPickupLocation() {
        return pickupLocation;
    }

    public RouteLocation getDropLocation() {
        return dropLocation;
    }

    public double getDistance() {
        return distance;
    }
}
