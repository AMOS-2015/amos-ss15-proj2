package org.croudtrip.api.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * A request for directions along multiple way points (min. 2!).
 */
public class DirectionsRequest {

    @NotNull private final List<RouteLocation> wayPoints;

    @JsonCreator
    public DirectionsRequest(
            @JsonProperty("wayPoints") List<RouteLocation> wayPoints) {

        this.wayPoints = wayPoints;
    }

    public List<RouteLocation> getWayPoints() {
        return wayPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DirectionsRequest that = (DirectionsRequest) o;
        return Objects.equal(wayPoints, that.wayPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(wayPoints);
    }


    public static class Builder {

        private final RouteLocation startLocation, endLocation;
        private final List<RouteLocation> intermediateLocations = new ArrayList<>();

        public Builder(RouteLocation startLocation, RouteLocation endLocation) {
            this.startLocation = startLocation;
            this.endLocation = endLocation;
        }

        public Builder addIntermediateLocation(RouteLocation location) {
            this.intermediateLocations.add(location);
            return this;
        }

        public DirectionsRequest build() {
            List<RouteLocation> wayPoints = new ArrayList<>();
            wayPoints.add(startLocation);
            wayPoints.addAll(intermediateLocations);
            wayPoints.add(endLocation);
            return new DirectionsRequest(wayPoints);
        }

    }
}
