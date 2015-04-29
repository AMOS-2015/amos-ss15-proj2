package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;

/**
 * A rout that is provided by a directions-request
 * Created by Frederik Simon on 24.04.2015.
 */
public class RouteNavigation {

    private final String summary;
    private final List<Leg> legs;
    private final List<Integer> waypointOrder;
    private final String polyline;
    private final String copyrights;
    private final List<String> warnings;

    @JsonCreator
    public RouteNavigation(
            @JsonProperty("summary") String summary,
            @JsonProperty("legs") List<Leg> legs,
            @JsonProperty("waypointOrder") List<Integer> waypointOrder,
            @JsonProperty("polyline") String polyline,
            @JsonProperty("copyrights") String copyrights,
            @JsonProperty("warnings") List<String> warnings) {

        this.summary = summary;
        this.legs = legs;
        this.waypointOrder = waypointOrder;
        this.polyline = polyline;
        this.copyrights = copyrights;
        this.warnings = warnings;
    }

    public String getSummary() {
        return summary;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public List<Integer> getWaypointOrder() {
        return waypointOrder;
    }

    public String getPolyline() {
        return polyline;
    }

    public String getCopyrights() {
        return copyrights;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof RouteNavigation)) return false;
        RouteNavigation routeNavigation = (RouteNavigation) other;
        return Objects.equal(summary, routeNavigation.summary)
                && Objects.equal(legs, routeNavigation.legs)
                && Objects.equal(waypointOrder, routeNavigation.waypointOrder)
                && Objects.equal(polyline, routeNavigation.polyline)
                && Objects.equal(copyrights, routeNavigation.copyrights)
                && Objects.equal(warnings, routeNavigation.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(summary, legs, waypointOrder, polyline, copyrights, warnings);
    }

}
