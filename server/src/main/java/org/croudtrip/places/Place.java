package org.croudtrip.places;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.croudtrip.api.directions.RouteLocation;

public class Place {
    private final String name;
    private final RouteLocation location;

    @JsonCreator
    Place( @JsonProperty("name") String name,
           @JsonProperty("location") RouteLocation location  ){
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public RouteLocation getLocation() {
        return location;
    }
}
