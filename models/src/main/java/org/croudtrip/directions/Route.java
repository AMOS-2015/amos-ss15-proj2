package org.croudtrip.directions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A rout that is provided by a directions-request
 * Created by Frederik Simon on 24.04.2015.
 */
public class Route {
    public String summary;
    public Leg[] legs;
    public int[] waypointOrder;
    public String polyline;
    /* TODO: add bounds */
    public String copyrights;
    public String[] warnings;
    // note: fares are ignored for now

    @JsonCreator
    public Route( @JsonProperty("summary") String summary,
                  @JsonProperty("legs") Leg[] legs,
                  @JsonProperty("waypointOrder") int[] waypointOrder,
                  @JsonProperty("polyline") String polyline,
                  @JsonProperty("copyrights") String copyrights,
                  @JsonProperty("warnings") String[] warnings ) {
        this.summary = summary;
        this.legs = legs;
        this.waypointOrder = waypointOrder;
        this.polyline = polyline;
        this.copyrights = copyrights;
        this.warnings = warnings;
    }

}
