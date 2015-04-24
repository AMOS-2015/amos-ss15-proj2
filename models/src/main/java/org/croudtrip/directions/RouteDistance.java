package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Frederik Simon on 24.04.2015.
 */
public class RouteDistance {
    public long inMeters;
    public String humanReadable;

    @JsonCreator
    public RouteDistance( @JsonProperty("meters") long inMeters,
                          @JsonProperty("readable") String humanReadable ) {
        this.inMeters = inMeters;
        this.humanReadable = humanReadable;
    }
}
