package org.croudtrip.closestpair;

import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

public class ClosestPairLocation {
    private RouteLocation location;
    private Route route;

    public ClosestPairLocation(RouteLocation location, Route route) {
        this.location = location;
        this.route = route;
    }

    public RouteLocation getLocation() {
        return location;
    }

    public Route getRoute() {
        return route;
    }
}
