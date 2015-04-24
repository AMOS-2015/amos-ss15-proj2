package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes the location of a geo position by latitude and logitude
 * Created by Frederik Simon on 24.04.2015.
 */
public class Location {
    public double lat;
    public double lng;

    @JsonCreator
    public Location( @JsonProperty("lat") double lat, @JsonProperty("lng") double lng ) {
        this.lat = lat;
        this.lng = lng;
    }
}
