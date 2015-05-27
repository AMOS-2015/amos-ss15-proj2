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


    public Route() { }

    @JsonCreator
    public Route(
            @JsonProperty("wayPoints") List<RouteLocation> wayPoints,
            @JsonProperty("polyline") String polyline,
            @JsonProperty("distanceInMeters") long distanceInMeters,
            @JsonProperty("durationInSeconds") long durationInSeconds,
            @JsonProperty("copyrights") String googleCopyrights,
            @JsonProperty("warnings") String googleWarnings) {

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

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Route)) return false;
        Route route = (Route) other;
        return Objects.equal(polyline, route.polyline)
                && Objects.equal(googleCopyrights, route.googleCopyrights)
                && Objects.equal(durationInSeconds, route.durationInSeconds)
                && Objects.equal(distanceInMeters, route.distanceInMeters)
                && Objects.equal(googleWarnings, route.googleWarnings)
                && Objects.equal(wayPointsString, route.wayPointsString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(polyline, googleCopyrights, durationInSeconds, distanceInMeters, googleWarnings, wayPointsString);
    }

}
