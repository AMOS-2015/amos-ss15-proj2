package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by Frederik Simon on 24.04.2015.
 */
public class RouteDistance {

    private final long meters;
    private final String readable;

    @JsonCreator
    public RouteDistance(
            @JsonProperty("meters") long meters,
            @JsonProperty("readable") String readable) {

        this.meters = meters;
        this.readable = readable;
    }

    public long getMeters() {
        return meters;
    }

    public String getReadable() {
        return readable;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof RouteDistance)) return false;
        RouteDistance distance = (RouteDistance) other;
        return Objects.equal(meters, distance.meters) && Objects.equal(readable, distance.readable);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(meters, readable);
    }

}
