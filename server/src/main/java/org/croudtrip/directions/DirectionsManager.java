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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class DirectionsManager {

	private final GeoApiContext geoApiContext;

	@Inject
	DirectionsManager(GeoApiContext geoApiContext) {
		this.geoApiContext = geoApiContext;
	}


	public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation) {
		return getDirections(startLocation, endLocation, new ArrayList<RouteLocation>());
	}

    public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation, List<RouteLocation> waypoints) {
		try {
			String[] stringWaypoints = new String[waypoints.size()];
			for (int i = 0; i < stringWaypoints.length; ++i) {
				RouteLocation loc = waypoints.get(i);
				GeocodingResult[] result = GeocodingApi.newRequest(geoApiContext).latlng(new LatLng(loc.getLat(), loc.getLng())).await();

				// no route found ...
				if (result == null || result.length == 0) return new ArrayList<>();

				stringWaypoints[i] = result[0].formattedAddress;
			}

			List<Route> result = new ArrayList<>();
			DirectionsRoute[] googleRoutes = DirectionsApi.newRequest(geoApiContext)
					.origin(new LatLng(startLocation.getLat(), startLocation.getLng()))
					.destination(new LatLng(endLocation.getLat(), endLocation.getLng()))
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
			distanceInMeters += leg.distance.inMeters;
			durationInSeconds += leg.duration.inSeconds;
			for (DirectionsStep step : leg.steps) {
				points.addAll(step.polyline.decodePath());
			}
		}

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
