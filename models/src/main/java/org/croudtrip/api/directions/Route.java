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

import org.croudtrip.api.account.User;
import org.croudtrip.api.trips.UserWayPoint;

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

    @Column(name="last_update_time_in_seconds", nullable = false)
    private long lastUpdateTimeInSeconds;

    @Column(name="leg_durations")
    private String legDurationsInSeconds;

    @Column(name="leg_distances")
    private String legDistancesInMeters;

    @Column(name="waypointPolylineIndices")
    @JsonIgnore
    private String waypointPolylineIndicesString;

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
            @JsonProperty("lastUpdateTime") long lastUpdateTimeInSeconds,
            @JsonProperty("waypointPolylineIndices") List<Integer> waypointPolylineIndices ) {

        // convert JSON fields to string for persistence
        if (wayPoints != null) {
            StringBuilder wayPointsBuilder = new StringBuilder();
            boolean firstPoint = true;
            for (RouteLocation location : wayPoints) {
                if (firstPoint) firstPoint = false;
                else wayPointsBuilder.append("#");
                wayPointsBuilder.append(location.getLat()).append(":").append(location.getLng());
            }
            this.wayPointsString = wayPointsBuilder.toString();
        }

        this.polyline = polyline;
        this.distanceInMeters = distanceInMeters;
        this.durationInSeconds = durationInSeconds;
        this.googleCopyrights = googleCopyrights;
        this.googleWarnings = googleWarnings;
        this.lastUpdateTimeInSeconds = lastUpdateTimeInSeconds;

        this.legDurationsInSeconds = "";
        if( legDurationsInSeconds != null ) {
            StringBuilder legDurationBuilder = new StringBuilder();
            boolean firstPoint = true;
            for (Long value : legDurationsInSeconds) {
                if (firstPoint) firstPoint = false;
                else legDurationBuilder.append("#");
                legDurationBuilder.append(value);
            }
            this.legDurationsInSeconds = legDurationBuilder.toString();
        }

        this.legDistancesInMeters = "";
        if( legDistancesInMeters != null ){
            StringBuilder legDistanceBuilder = new StringBuilder();
            boolean firstPoint = true;
            for( Long value : legDistancesInMeters ){
                if( firstPoint ) firstPoint = false;
                else legDistanceBuilder.append("#");
                legDistanceBuilder.append(value);
            }
            this.legDistancesInMeters= legDistanceBuilder.toString();
        }

        this.waypointPolylineIndicesString = "";
        if( waypointPolylineIndices != null ) {
            StringBuilder wpPlIdxBuiler = new StringBuilder();
            boolean firstPoint = true;
            for( Integer value : waypointPolylineIndices ){
                if( firstPoint ) firstPoint = false;
                else wpPlIdxBuiler.append("#");
                wpPlIdxBuiler.append(value);
            }

            this.waypointPolylineIndicesString = wpPlIdxBuiler.toString();
        }

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

    public List<Long> getLegDurationsInSeconds() {
        List<Long> result = new ArrayList<>();
        String[] durations = legDurationsInSeconds.split("#");
        for( String value : durations ) result.add(Long.valueOf(value));
        return result;
    }

    public List<Long> getLegDistancesInMeters() {
        List<Long> result = new ArrayList<>();
        String[] distances = legDistancesInMeters.split("#");
        for( String value : distances ) result.add(Long.valueOf(value));
        return result;
    }

    /**
     * Gets you the mapping from waypoints (that are visited on the route) into an index value
     * of where the polyline does begin for this particular waypoint. So use
     * getWaypointPolylineIndices.get(1) to get the polyline start index for the first waypoint.
     * Therefore you can create route passages by simply using {@link java.lang.String#substring(int, int)}
     * on the polyline.
     *
     * @return
     */
    @JsonProperty("waypointPolylineIndices")
    private List<Integer> getWaypointPolylineIndices() {
        List<Integer> result = new ArrayList<>();

        String[] polylineIndices = waypointPolylineIndicesString.split("#");
        for( String value : polylineIndices ) result.add(Integer.valueOf(value));

        return result;
    }

    /**
     * Gets you the polyline for the whole trip-
     * @return The polyline as a string for the whole trip
     */
    public String getPolyline() {
        return polyline;
    }

    /**
     * Gets you the waypoints for a polyline from a specific waypoint index on the route
     * to a certain waypoint index on the route.
     * @param fromWaypoint index of the waypoint the polyline should start
     * @param toWaypoint index of the waypoint where the polyline should end
     * @return a list of waypoints that are part of the polyline
     * @throws java.lang.IllegalArgumentException if toWaypoint is smaller than fromWaypoint
     */
    public List<RouteLocation> getPolylineWaypoints( int fromWaypoint, int toWaypoint ) {
        if( toWaypoint < fromWaypoint)
            throw new IllegalArgumentException("toWaypoint must be greater than fromWaypoint");

        // compute the string index from the given waypoint indices
        List<Integer> wayppointPolylineIndices = getWaypointPolylineIndices();
        int fromStringIdx = wayppointPolylineIndices.get(fromWaypoint);
        int toStringIdx = wayppointPolylineIndices.get(toWaypoint);

        return PolylineDecoder.decode( polyline, fromStringIdx, toStringIdx );
    }

    /**
     * Gets you subsection of the polyline for a specific user that should have waypoints accross the total route.
     * If the user is already picked up and therefore has no starting waypoint anymore the starting waypoint of
     * this route is used instead.
     * @param user A user that has according waypoints in the userWaypoint
     * @param userWayPoints A list of user waypoints that are visited during the trip.
     * @return a list of waypoints that are part of the polyline
     * @throws java.lang.IllegalArgumentException if the given user has no corresponding waypoints
     */
    public List<RouteLocation> getPolylineWaypointsForUser( User user, List<UserWayPoint> userWayPoints ) {

        // extract the waypoint id for the users starting point and destination point
        int userFromWaypoint = -1;
        int userToWaypoint = -1;
        for( int i = 0; i < userWayPoints.size(); ++i ) {
            UserWayPoint uwp = userWayPoints.get(i);
            if( uwp.getUser().getId() == user.getId() && uwp.isStartOfTrip() )
                userFromWaypoint = i;
            else if( uwp.getUser().getId() == user.getId() ){
                userToWaypoint = i;
            }
        }

        // we cannot do anything if the user is not at all in the given waypoints list
        if( userToWaypoint == -1 && userFromWaypoint == -1)
            throw new IllegalArgumentException("User has no waypoints in the given user waypoints list");

        // if the user has no destination, we will simply take the route until the end of the trip
        if( userToWaypoint == -1)
            userToWaypoint = userWayPoints.size() - 1;

        // if the user has no starting point, we will show the route from the current starting point
        // this could happen, if the passenger already is in the car, so that's not really an error
        if( userFromWaypoint == -1)
            userFromWaypoint = 0;

        return getPolylineWaypoints( userFromWaypoint, userToWaypoint );
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
                && Objects.equal(legDistancesInMeters, route.legDistancesInMeters)
                && Objects.equal(waypointPolylineIndicesString, route.waypointPolylineIndicesString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(polyline, googleCopyrights, durationInSeconds, distanceInMeters,
                googleWarnings, wayPointsString, legDistancesInMeters, legDurationsInSeconds, waypointPolylineIndicesString);
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
        private List<Integer> waypointPolylineIndices;

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

        public Builder waypointPolylineIndices(List<Integer> waypointPolylineIndices ) {
            this.waypointPolylineIndices = waypointPolylineIndices;
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
                    lastUpdateTimeInSeconds,
                    waypointPolylineIndices);
        }
    }

}
