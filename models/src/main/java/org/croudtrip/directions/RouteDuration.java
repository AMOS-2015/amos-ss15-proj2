package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The duration that is needed for a rout or a step on the route.
 * Created by Frederik Simon on 24.04.2015.
 */
public class RouteDuration {
    public long inSeconds;
    public String humanReadable;

    @JsonCreator
    public RouteDuration( @JsonProperty("seconds") long inSeconds,
                          @JsonProperty("readable") String humanReadable ) {
        this.inSeconds = inSeconds;
        this.humanReadable = humanReadable;
    }
}
