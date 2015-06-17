package org.croudtrip.directions;

import com.google.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PolylineEncoder {

    /**
     * Encodes a sequence of LatLngs into an encoded path string.
     * The basic encoding algorithm is copied from {@link com.google.maps.internal.PolylineEncoding},
     * but this method is slightly adapted to our own needs. You can pass a list of waypoint indices
     * that will contain indices of the path list that start a new route leg. And this method will also
     * compute corresponding string indices for that particular waypoints.
     */
    public static Polyline encode(final List<LatLng> path, final List<Integer> waypointIndices ) {
        long lastLat = 0;
        long lastLng = 0;

        final StringBuffer result = new StringBuffer();

        List<Integer> stringIndices = new ArrayList<Integer>();

        for (int i = 0; i < path.size(); ++i) {
            LatLng point = path.get(i);

            long lat = Math.round(point.lat * 1e5);
            long lng = Math.round(point.lng * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encode(dLat, result);
            encode(dLng, result);

            // if we match a leg starting point we add the current index (buffer length) to the
            // string indices.
            // first condition checks if not all waypointIndices have been processed yet
            // second condition checks the waypoint index that should be converted to a string index next
            if( stringIndices.size() < waypointIndices.size() && i == waypointIndices.get( stringIndices.size() )){
                stringIndices.add( result.length() );
            }

            lastLat = lat;
            lastLng = lng;
        }

        // also add the last waypoint
        stringIndices.add( result.length() );

        return new Polyline( result.toString(), stringIndices, path, waypointIndices );
    }

    private static void encode(long v, StringBuffer result) {
        v = v < 0 ? ~(v << 1) : v << 1;
        while (v >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        result.append(Character.toChars((int) (v + 63)));
    }
}
