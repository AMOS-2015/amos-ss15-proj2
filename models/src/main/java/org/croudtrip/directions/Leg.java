package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes one section (leg) of the given route
 * Created by Frederik Simon on 24.04.2015.
 */
public class Leg {
    public Step[] steps;
    public RouteDistance distance;
    public RouteDuration duration;
    public RouteDuration durationInTraffic;
    public Location startLocation;
    public Location endLocation;
    public String startAddress;
    public String endAddress;

    @JsonCreator
    public Leg( @JsonProperty("steps") Step[] steps,
                @JsonProperty("distance") RouteDistance distance,
                @JsonProperty("duration") RouteDuration duration,
                @JsonProperty("durationInTraffic") RouteDuration durationInTraffic,
                @JsonProperty("startLocation") Location startLocation,
                @JsonProperty("endLocation") Location endLocation,
                @JsonProperty("startAddress") String startAddress,
                @JsonProperty("endAddress") String endAddress) {
        this.steps = steps;
        this.distance = distance;
        this.duration = duration;
        this.durationInTraffic = durationInTraffic;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }

}
