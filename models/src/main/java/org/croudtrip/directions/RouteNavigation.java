package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * A route between two points.
 */
public class RouteNavigation {

    private final Location start, end;
    private final String polyline;
    private final String googleCopyrights;
    private final String googleWarnings;

    @JsonCreator
    public RouteNavigation(
            @JsonProperty("start") Location start,
            @JsonProperty("end") Location end,
            @JsonProperty("polyline") String polyline,
            @JsonProperty("copyrights") String googleCopyrights,
            @JsonProperty("warnings") String googleWarnings) {

        this.start = start;
        this.end = end;
        this.polyline = polyline;
        this.googleCopyrights = googleCopyrights;
        this.googleWarnings = googleWarnings;
    }

    public String getPolyline() {
        return polyline;
    }

    public String getGoogleCopyrights() {
        return googleCopyrights;
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof RouteNavigation)) return false;
        RouteNavigation routeNavigation = (RouteNavigation) other;
        return Objects.equal(polyline, routeNavigation.polyline)
                && Objects.equal(googleCopyrights, routeNavigation.googleCopyrights)
                && Objects.equal(googleWarnings, routeNavigation.googleWarnings)
                && Objects.equal(start, routeNavigation.start)
                && Objects.equal(end, routeNavigation.end);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(polyline, googleCopyrights, googleWarnings, start, end);
    }

}
