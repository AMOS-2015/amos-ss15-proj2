package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * A route between two points.
 */
@Embeddable
public class Route {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "startLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "startLng"))
    })
    private RouteLocation start;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "endLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "endLng"))
    })
    private RouteLocation end;

    @Column(name = "polyLine", nullable = false, length = 65535)
    private String polyline;

    @Column(name = "distance_in_meters", nullable = false)
    private long distanceInMeters;

    @Column(name = "duration_in_seconds", nullable = false)
    private long durationInSeconds;

    @Column(name = "google_copyrights", nullable = true)
    private String googleCopyrights;

    @Column(name = "google_warnings", nullable = true)
    private String googleWarnings;


    public Route() { }

    @JsonCreator
    public Route(
            @JsonProperty("start") RouteLocation start,
            @JsonProperty("end") RouteLocation end,
            @JsonProperty("polyline") String polyline,
            @JsonProperty("distanceInMeters") long distanceInMeters,
            @JsonProperty("durationInSeconds") long durationInSeconds,
            @JsonProperty("copyrights") String googleCopyrights,
            @JsonProperty("warnings") String googleWarnings) {

        this.start = start;
        this.end = end;
        this.polyline = polyline;
        this.distanceInMeters = distanceInMeters;
        this.durationInSeconds = durationInSeconds;
        this.googleCopyrights = googleCopyrights;
        this.googleWarnings = googleWarnings;
    }

    public String getPolyline() {
        return polyline;
    }

    public long getDistanceInMeters() {
        return distanceInMeters;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public String getGoogleCopyrights() {
        return googleCopyrights;
    }

    public RouteLocation getStart() {
        return start;
    }

    public RouteLocation getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Route)) return false;
        Route route = (Route) other;
        return Objects.equal(polyline, route.polyline)
                && Objects.equal(googleCopyrights, route.googleCopyrights)
                && Objects.equal(durationInSeconds, route.durationInSeconds)
                && Objects.equal(distanceInMeters, route.distanceInMeters)
                && Objects.equal(googleWarnings, route.googleWarnings)
                && Objects.equal(start, route.start)
                && Objects.equal(end, route.end);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(polyline, googleCopyrights, durationInSeconds, distanceInMeters, googleWarnings, start, end);
    }

}
