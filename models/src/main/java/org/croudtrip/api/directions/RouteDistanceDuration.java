package org.croudtrip.api.directions;

/**
 * Contains the the distance in meters and duration in seconds of a specific two waypoint route.
 */
public class RouteDistanceDuration {
    private final long distanceInMeters;
    private final long durationInSeconds;

    public RouteDistanceDuration(long distanceInMeters, long durationInSeconds) {
        this.distanceInMeters = distanceInMeters;
        this.durationInSeconds = durationInSeconds;
    }

    public long getDistanceInMeters() {
        return distanceInMeters;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }
}
