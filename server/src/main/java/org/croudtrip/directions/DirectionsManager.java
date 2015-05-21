package org.croudtrip.directions;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.logs.LogManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class DirectionsManager {

	private final GeoApiContext geoApiContext;
    private final LogManager logManager;

	@Inject
	DirectionsManager(GeoApiContext geoApiContext, LogManager logManager) {
		this.geoApiContext = geoApiContext;
        this.logManager = logManager;
	}


	public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation) {
		return getDirections(startLocation, endLocation, new ArrayList<RouteLocation>());
	}

    public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation, List<RouteLocation> waypoints) {
        LatLng origin = new LatLng(startLocation.getLat(), startLocation.getLng());
        LatLng destination = new LatLng(endLocation.getLat(), endLocation.getLng());

        List<LatLng> llWaypoints = new ArrayList<LatLng>();

        if( waypoints.size() > 0 ) {
            for (RouteLocation loc : waypoints) {
                LatLng waypoint = new LatLng(loc.getLat(), loc.getLng());

                // don't add same start as waypoint
                if (llWaypoints.isEmpty()) {
                    if (waypoint.toString().equals(origin.toString()))
                        continue;
                }

                llWaypoints.add(waypoint);
            }

            // remove same destination
            if (llWaypoints.get(llWaypoints.size() - 1).toString().equals(destination.toString()))
                llWaypoints.remove(llWaypoints.size() - 1);
        }

        String[] stringWaypoints = new String[llWaypoints.size()];
        logManager.d("RouteRequest with Waypoint: " + origin.toString() );
        for (int i = 0; i < stringWaypoints.length; ++i) {
            /*GeocodingResult[] result = GeocodingApi.newRequest(geoApiContext).latlng(new LatLng(loc.getLat(), loc.getLng())).await();

            // no route found ...
            if (result == null || result.length == 0) return new ArrayList<>();

            stringWaypoints[i] = result[0].formattedAddress;*/

            LatLng waypoint = llWaypoints.get(i);
            stringWaypoints[i] = waypoint.toUrlValue();
            logManager.d("RouteRequest with Waypoint: " + stringWaypoints[i]);
        }

        logManager.d("RouteRequest with Waypoint: " + destination.toString() );

		List<Route> result = new ArrayList<>();
        DirectionsRoute[] googleRoutes = new DirectionsRoute[0];
        try {
            googleRoutes = DirectionsApi.newRequest(geoApiContext)
                    .origin(origin)
                    .destination(destination)
                    .waypoints(stringWaypoints)
                    .await();

            for (DirectionsRoute googleRoute : googleRoutes) {
                    result.add(createRoute(startLocation, endLocation, googleRoute));
                }
            return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }


	private Route createRoute(RouteLocation startLocation, RouteLocation endLocation, DirectionsRoute googleRoute) {

		long distanceInMeters = 0;
		long durationInSeconds = 0;

		List<LatLng> points = new ArrayList<>();
		for (DirectionsLeg leg : googleRoute.legs) {
            logManager.d("Leg: " + leg.distance.inMeters);
			distanceInMeters += leg.distance.inMeters;
			durationInSeconds += leg.duration.inSeconds;
			for (DirectionsStep step : leg.steps) {
				points.addAll(step.polyline.decodePath());
			}
		}

        logManager.d("Total Distance: " + distanceInMeters);

		EncodedPolyline polyline = new EncodedPolyline(points);
		String warnings;
		if (googleRoute.warnings.length > 0) {
			boolean firstIter = true;
			warnings = "";
			for (String warning : googleRoute.warnings) {
				if (firstIter) {
					warnings += "\n";
					firstIter = false;
				}
				warnings += warning;
			}
		} else {
			warnings = null;
		}

		List<RouteLocation> wayPoints = new ArrayList<>();
		wayPoints.add(startLocation);
		wayPoints.add(endLocation);
		return new Route(wayPoints, polyline.getEncodedPath(), distanceInMeters, durationInSeconds, googleRoute.copyrights, warnings);
	}

}
