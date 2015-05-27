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

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Describes the location of a geo position by latitude and longitude
 * Created by Frederik Simon on 24.04.2015.
 */
@Embeddable
public class RouteLocation {

    public static final String
            COLUMN_LAT = "lat",
            COLUMN_LNG = "lng";


    @Column(name = COLUMN_LAT)
    private double lat;

    @Column(name = COLUMN_LNG)
    private double lng;


    public RouteLocation() { }

    @JsonCreator
    public RouteLocation(@JsonProperty("lat") double lat, @JsonProperty("lng") double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return  lng;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof RouteLocation)) return false;
        RouteLocation location = (RouteLocation) other;
        return Objects.equal(lat, location.lat) && Objects.equal(lng, location.lng);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lat, lng);
    }

    @Override
    public String toString() {
        return "lat: " + lat + " ; " + " long: " + lng;
    }

}
