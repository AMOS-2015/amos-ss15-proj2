package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Describes the location of a geo position by latitude and logitude
 * Created by Frederik Simon on 24.04.2015.
 */
public class Location {

    private final double lat, lng;

    @JsonCreator
    public Location(@JsonProperty("lat") double lat, @JsonProperty("lng") double lng ) {
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
        if (other == null || !(other instanceof Location)) return false;
        Location location = (Location) other;
        return Objects.equal(lat, location.lat) && Objects.equal(lng, location.lng);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lat, lng);
    }

}
