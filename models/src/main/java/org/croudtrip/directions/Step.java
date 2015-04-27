package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Describes a step that has to be done on a specific route
 * Created by Frederik Simon on 24.04.2015.
 */
public class Step {

    private final String htmlInstructions;
    private final RouteDistance distance;
    private final RouteDuration duration;
    private final Location startLocation, endLocation;
    private final String polyline;

    @JsonCreator
    public Step(
            @JsonProperty("htmlInstructions") String htmlInstructions,
            @JsonProperty("distance") RouteDistance distance,
            @JsonProperty("duration") RouteDuration duration,
            @JsonProperty("startLocation") Location startLocation,
            @JsonProperty("endLocation") Location endLocation,
            @JsonProperty("polyline") String polyline) {

        this.htmlInstructions = htmlInstructions;
        this.distance = distance;
        this.duration = duration;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.polyline = polyline;
    }

    public String getHtmlInstructions() {
        return htmlInstructions;
    }

    public RouteDistance getDistance() {
        return distance;
    }

    public RouteDuration getDuration() {
        return duration;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public String getPolyline() {
        return polyline;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Step)) return false;
        Step step = (Step) other;
        return Objects.equal(htmlInstructions, step.htmlInstructions)
                && Objects.equal(distance, step.distance)
                && Objects.equal(duration, step.duration)
                && Objects.equal(startLocation, step.startLocation)
                && Objects.equal(endLocation, step.endLocation)
                && Objects.equal(polyline, step.polyline);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(htmlInstructions, distance, duration, startLocation, endLocation, polyline);
    }

}
