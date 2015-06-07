/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.api.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;

/**
 * A route between two points.
 */
@Embeddable
public class Route {

    @Column(name = "way_points", nullable = false)
    @JsonIgnore
    private String wayPointsString;

    @Column(name = "polyLine", nullable = false, length =((1<<20)-1))
    private String polyline;

    @Column(name = "distance_in_meters", nullable = false)
    private long distanceInMeters;

    @Column(name = "duration_in_seconds", nullable = false)
    private long durationInSeconds;

    @Column(name = "google_copyrights", nullable = true)
    private String googleCopyrights;

    @Column(name = "google_warnings", nullable = true)
    private String googleWarnings;

    @Column(name="last_update_time_in_seconds", nullable = false)
    private long lastUpdateTimeInSeconds;

    @ElementCollection
    @Column(name="leg_durations")
    private List<Long> legDurationsInSeconds;

    @ElementCollection
    @Column(name="leg_distances")
    private List<Long> legDistancesInMeters;

    public Route() { }

    @JsonCreator
    public Route(
            @JsonProperty("wayPoints") List<RouteLocation> wayPoints,
            @JsonProperty("polyline") String polyline,
            @JsonProperty("distanceInMeters") long distanceInMeters,
            @JsonProperty("durationInSeconds") long durationInSeconds,
            @JsonProperty("legDurationsInSeconds") List<Long> legDurationsInSeconds,
            @JsonProperty("legDistancesInMeters") List<Long> legDistancesInMeters,
            @JsonProperty("copyrights") String googleCopyrights,
            @JsonProperty("warnings") String googleWarnings,
            @JsonProperty("lastUpdateTime") long lastUpdateTimeInSeconds) {

        // convert JSON fields to string for persistence
        StringBuilder wayPointsBuilder = new StringBuilder();
        boolean firstPoint = true;
        for (RouteLocation location : wayPoints) {
            if (firstPoint) firstPoint = false;
            else wayPointsBuilder.append("#");
            wayPointsBuilder.append(location.getLat()).append(":").append(location.getLng());
        }
        this.wayPointsString = wayPointsBuilder.toString();

        this.polyline = polyline;
        this.distanceInMeters = distanceInMeters;
        this.durationInSeconds = durationInSeconds;
        this.googleCopyrights = googleCopyrights;
        this.googleWarnings = googleWarnings;
        this.lastUpdateTimeInSeconds = lastUpdateTimeInSeconds;
        this.legDurationsInSeconds = legDurationsInSeconds;
        this.legDistancesInMeters= legDistancesInMeters;
    }

    @JsonProperty("wayPoints")
    public List<RouteLocation> getWayPoints() {
        // parse string to list of way points
        List<RouteLocation> result = new ArrayList<>();
        String[] points = wayPointsString.split("#");
        for (String point : points) {
            String[] parts = point.split(":");
            result.add(new RouteLocation(Float.valueOf(parts[0]), Float.valueOf(parts[1])));
        }
        return result;
    }

    public String getPolyline() {
        return polyline;
    }

    public long getDistanceInMeters() {
        return distanceInMeters;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public String getGoogleCopyrights() {
        return googleCopyrights;
    }

    public long getLastUpdateTimeInSeconds() { return lastUpdateTimeInSeconds; }

    public List<Long> getLegDurationsInSeconds() { return legDurationsInSeconds; }

    public List<Long> getLegDistancesInMeters() {
        return legDistancesInMeters;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Route)) return false;
        Route route = (Route) other;
        return Objects.equal(polyline, route.polyline)
                && Objects.equal(googleCopyrights, route.googleCopyrights)
                && Objects.equal(durationInSeconds, route.durationInSeconds)
                && Objects.equal(distanceInMeters, route.distanceInMeters)
                && Objects.equal(googleWarnings, route.googleWarnings)
                && Objects.equal(wayPointsString, route.wayPointsString)
                && Objects.equal(legDurationsInSeconds, route.legDurationsInSeconds)
                && Objects.equal(legDistancesInMeters, route.legDistancesInMeters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(polyline, googleCopyrights, durationInSeconds, distanceInMeters,
                googleWarnings, wayPointsString, legDistancesInMeters, legDurationsInSeconds);
    }


    public static class Builder {

        private List<RouteLocation> wayPoints;
        private String polyline;
        private long distanceInMeters;
        private long durationInSeconds;
        private List<Long> legDurationsInSeconds;
        private List<Long> legDistancesInMeters;
        private String googleCopyrights;
        private String googleWarnings;
        private long lastUpdateTimeInSeconds;

        public Builder wayPoints(List<RouteLocation> wayPoints) {
            this.wayPoints = wayPoints;
            return this;
        }

        public Builder polyline(String polyline) {
            this.polyline = polyline;
            return this;
        }

        public Builder distanceInMeters(long distanceInMeters) {
            this.distanceInMeters = distanceInMeters;
            return this;
        }

        public Builder durationInSeconds(long durationInSeconds) {
            this.durationInSeconds = durationInSeconds;
            return this;
        }

        public Builder legDurationInSeconds(List<Long> legDurationsInSeconds) {
            this.legDurationsInSeconds = legDurationsInSeconds;
            return this;
        }

        public Builder legDistancesInMeters(List<Long> legDistancesInMeters) {
            this.legDistancesInMeters = legDistancesInMeters;
            return this;
        }

        public Builder googleCopyrights(String googleCopyrights) {
            this.googleCopyrights = googleCopyrights;
            return this;
        }

        public Builder googleWarnings(String googleWarnings) {
            this.googleWarnings = googleWarnings;
            return this;
        }

        public Builder lastUpdateTimeInSeconds(long lastUpdateTimeInSeconds) {
            this.lastUpdateTimeInSeconds = lastUpdateTimeInSeconds;
            return this;
        }

        public Route build() {
            return new Route(
                    wayPoints,
                    polyline,
                    distanceInMeters,
                    durationInSeconds,
                    legDurationsInSeconds,
                    legDistancesInMeters,
                    googleCopyrights,
                    googleWarnings,
                    lastUpdateTimeInSeconds);
        }
    }

}
