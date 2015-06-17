package org.croudtrip.api.directions;

import java.util.ArrayList;
import java.util.List;

public class PolylineDecoder {

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    static List<RouteLocation> decode(final String encodedPath, final int startIdx, final int stopIdx) {

        int len = encodedPath.length();

        final List<RouteLocation> path = new ArrayList<RouteLocation>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            if( index >= startIdx && index <= stopIdx )
                path.add(new RouteLocation(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }
}
