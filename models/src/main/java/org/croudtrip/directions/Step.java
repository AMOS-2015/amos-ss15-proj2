package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes a step that has to be done on a specific route
 * Created by Frederik Simon on 24.04.2015.
 */
public class Step {
    public String htmlInstructions;
    public RouteDistance distance;
    public RouteDuration duration;
    public Location startLocation;
    public Location endLocation;
    public String polyline;
    // note: Transit settings currently not supported

    @JsonCreator
    public Step( @JsonProperty("htmlInstructions") String htmlInstructions,
                 @JsonProperty("distance") RouteDistance distance,
                 @JsonProperty("duration") RouteDuration duration,
                 @JsonProperty("startLocation") Location startLocation,
                 @JsonProperty("endLocation") Location endLocation,
                 @JsonProperty("polyline") String polyline ) {
        this.htmlInstructions = htmlInstructions;
        this.distance = distance;
        this.duration = duration;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.polyline = polyline;
    }

}
