package org.croudtrip.directions;

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

}
