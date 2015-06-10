package org.croudtrip.api.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.UserWayPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * A result for a navigation request that is given for a certain offer.
 */
public class NavigationResult {
    private Route route;
    private List<UserWayPoint> userWayPoints;

    public static NavigationResult createNavigationResultForDriverRoute( TripOffer offer ) {
        User driver = offer.getDriver();
        Route route = offer.getDriverRoute();

        if( route.getWayPoints().size() > 2 ) {
            throw new IllegalArgumentException("Your route has more than two waypoints." +
                    " So this is no basic driver route and you should already have a NavigationResult-Object for your route.");
        }

        List<RouteLocation> waypoints = route.getWayPoints();
        List<UserWayPoint> userWaypoints = new ArrayList<>();
        userWaypoints.add( new UserWayPoint( driver, waypoints.get(0), true, 0, 0 ) );
        userWaypoints.add( new UserWayPoint( driver, waypoints.get(1), false, route.getLastUpdateTimeInSeconds() + route.getDurationInSeconds(), route.getDistanceInMeters() ) );

        return new NavigationResult( route, userWaypoints );
    }

    @JsonCreator
    public NavigationResult(
            @JsonProperty("route") Route route,
            @JsonProperty("userWaypoints") List<UserWayPoint> userWaypoints){
        this.route = route;
        this.userWayPoints = userWaypoints;
    }

    /** Returns the actual route for the navigation request
     * @return The Route for the navigation request
     */
    public Route getRoute() {
        return route;
    }

    /**
     * Returns as list of {@link org.croudtrip.api.trips.UserWayPoint} for every passenger that
     * participates at this trip in the best order that was found solving the TSP.
     * @return a list of waypoints in the best TSP order
     */
    public List<UserWayPoint> getUserWayPoints() {
        return userWayPoints;
    }
}
