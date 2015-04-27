package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;

/**
 * A rout that is provided by a directions-request
 * Created by Frederik Simon on 24.04.2015.
 */
public class Route {

    private final String summary;
    private final List<Leg> legs;
    private final List<Integer> waypointOrder;
    private final String polyline;
    private final String copyrights;
    private final List<String> warnings;

    @JsonCreator
    public Route(
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
        if (other == null || !(other instanceof Route)) return false;
        Route route = (Route) other;
        return Objects.equal(summary, route.summary)
                && Objects.equal(legs, route.legs)
                && Objects.equal(waypointOrder, route.waypointOrder)
                && Objects.equal(polyline, route.polyline)
                && Objects.equal(copyrights, route.copyrights)
                && Objects.equal(warnings, route.warnings);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(summary, legs, waypointOrder, polyline, copyrights, warnings);
    }

}
