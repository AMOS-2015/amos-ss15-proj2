package org.croudtrip.directions;

import com.google.maps.model.LatLng;

import java.util.List;

/**
 * A simple adapted polyline class that contains also waypoint information and string indices for that
 * particular polyline.
 */
public class Polyline {
    private String polyline;
    private List<Integer> polylineStringIndices;
    private List<LatLng> path;
    private List<Integer> pathWaypointIndices;

    public Polyline(String polyline, List<Integer> polylineStringIndices, List<LatLng> path, List<Integer> pathWaypointIndices) {
        this.polyline = polyline;
        this.polylineStringIndices = polylineStringIndices;
        this.path = path;
        this.pathWaypointIndices = pathWaypointIndices;
    }

    public String getPolyline() {
        return polyline;
    }

    public List<Integer> getPolylineStringIndices() {
        return polylineStringIndices;
    }

    public List<LatLng> getPath() {
        return path;
    }

    public List<Integer> getPathWaypointIndices() {
        return pathWaypointIndices;
    }
}
