package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;

/**
 * Describes one section (leg) of the given route
 * Created by Frederik Simon on 24.04.2015.
 */
public class Leg {

    private final List<Step> steps;
    private final RouteDistance distance;
    private final RouteDuration duration, durationInTraffic;
    private final Location startLocation, endLocation;
    private final String startAddress, endAddress;

    @JsonCreator
    public Leg(
            @JsonProperty("steps") List<Step> steps,
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

    public String getEndAddress() {
        return endAddress;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public RouteDistance getDistance() {
        return distance;
    }

    public RouteDuration getDuration() {
        return duration;
    }

    public RouteDuration getDurationInTraffic() {
        return durationInTraffic;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public String getStartAddress() {
        return startAddress;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Leg)) return false;
        Leg leg = (Leg) other;
        return Objects.equal(steps, leg.steps)
                && Objects.equal(startAddress, leg.startAddress)
                && Objects.equal(endAddress, leg.endAddress)
                && Objects.equal(duration, leg.duration)
                && Objects.equal(durationInTraffic, leg.durationInTraffic)
                && Objects.equal(startLocation, leg.startLocation)
                && Objects.equal(endLocation, leg.endLocation)
                && Objects.equal(distance, leg.distance);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(steps, startAddress, endAddress, duration, durationInTraffic,
                startLocation, endLocation, distance);
    }

}
