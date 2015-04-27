package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * The duration that is needed for a rout or a step on the route.
 * Created by Frederik Simon on 24.04.2015.
 */
public class RouteDuration {

    private final long seconds;
    private final String readable;

    @JsonCreator
    public RouteDuration(
            @JsonProperty("seconds") long seconds,
            @JsonProperty("readable") String readable) {

        this.seconds = seconds;
        this.readable = readable;
    }

    public long getSeconds() {
        return seconds;
    }

    public String getReadable() {
        return readable;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof RouteDuration)) return false;
        RouteDuration duration = (RouteDuration) other;
        return Objects.equal(seconds, duration.seconds) && Objects.equal(readable, duration.readable);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(seconds, readable);
    }

}
