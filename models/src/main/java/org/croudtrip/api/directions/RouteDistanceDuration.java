package org.croudtrip.api.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Contains the the distance in meters and duration in seconds of a specific two waypoint route.
 */
@Embeddable
public class RouteDistanceDuration {

    @Column(name = "distance_in_meters", nullable = false)
    private long distanceInMeters;

    @Column(name = "duration_in_seconds", nullable = false)
    private long durationInSeconds;

    public RouteDistanceDuration() {
    }

    @JsonCreator
    public RouteDistanceDuration(
        @JsonProperty("distanceInMeters") long distanceInMeters,
        @JsonProperty("durationInSeconds") long durationInSeconds
    ){
        this.distanceInMeters = distanceInMeters;
        this.durationInSeconds = durationInSeconds;
    }

    public long getDistanceInMeters() {
        return distanceInMeters;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteDistanceDuration that = (RouteDistanceDuration) o;

        if (distanceInMeters != that.distanceInMeters) return false;
        return durationInSeconds == that.durationInSeconds;

    }

    @Override
    public int hashCode() {
        int result = (int) (distanceInMeters ^ (distanceInMeters >>> 32));
        result = 31 * result + (int) (durationInSeconds ^ (durationInSeconds >>> 32));
        return result;
    }

}
