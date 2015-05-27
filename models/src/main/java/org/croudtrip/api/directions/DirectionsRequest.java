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
