/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.directions;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;
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
    private int directionCalls;

	@Inject
	DirectionsManager(GeoApiContext geoApiContext, LogManager logManager) {
		this.geoApiContext = geoApiContext;
        this.logManager = logManager;
        this.directionCalls = 0;
	}

	public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation) {
		return getDirections(startLocation, endLocation, new ArrayList<RouteLocation>());
	}

    public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation, List<RouteLocation> waypoints) {
        LatLng origin = new LatLng(startLocation.getLat(), startLocation.getLng());
        LatLng destination = new LatLng(endLocation.getLat(), endLocation.getLng());

        logManager.d("DIRECTIONS REQUEST (" + waypoints.size() + " wps)");

        List<LatLng> llWaypoints = new ArrayList<LatLng>();

        if( waypoints.size() > 0 ) {
            for (RouteLocation loc : waypoints) {
                LatLng waypoint = new LatLng(loc.getLat(), loc.getLng());
                llWaypoints.add(waypoint);
            }
        }

        String[] stringWaypoints = new String[llWaypoints.size()];
        for (int i = 0; i < stringWaypoints.length; ++i) {
            /*GeocodingResult[] result = GeocodingApi.newRequest(geoApiContext).latlng(new LatLng(loc.getLat(), loc.getLng())).await();

            // no route found ...
            if (result == null || result.length == 0) return new ArrayList<>();

            stringWaypoints[i] = result[0].formattedAddress;*/

            LatLng waypoint = llWaypoints.get(i);
            stringWaypoints[i] = waypoint.toUrlValue();
        }

        // create a list containing all the waypoints (also start and destination)
        // don't modify given waypoints list.
        List<RouteLocation> allWaypoints = new ArrayList<RouteLocation>();
        allWaypoints.add( startLocation );
        for( RouteLocation loc : waypoints )
            allWaypoints.add( loc );
        allWaypoints.add( endLocation );

		List<Route> result = new ArrayList<>();
        DirectionsRoute[] googleRoutes = new DirectionsRoute[0];
        try {
            googleRoutes = DirectionsApi.newRequest(geoApiContext)
                    .origin(origin)
                    .destination(destination)
                    .waypoints(stringWaypoints)
                    .await();
            directionCalls++;

            for (DirectionsRoute googleRoute : googleRoutes) {
                    result.add(createRoute(allWaypoints, googleRoute));
                }
            return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }


	private Route createRoute(List<RouteLocation> waypoints, DirectionsRoute googleRoute) {

		long distanceInMeters = 0;
		long durationInSeconds = 0;

        List<Long> legDurationsInSeconds = new ArrayList<>();
		List<Long> legDistancesInMeters = new ArrayList<>();

        List<Integer> waypointIndices = new ArrayList<>();

		List<LatLng> points = new ArrayList<>();
		for (DirectionsLeg leg : googleRoute.legs) {
            //logManager.d("Leg: " + leg.distance.inMeters);
			distanceInMeters += leg.distance.inMeters;
			durationInSeconds += leg.duration.inSeconds;
            legDurationsInSeconds.add( leg.duration.inSeconds );
			legDistancesInMeters.add(leg.distance.inMeters);

            waypointIndices.add( points.size() );

			for (DirectionsStep step : leg.steps) {
				points.addAll(step.polyline.decodePath());
			}
		}

		Polyline polyline = PolylineEncoder.encode( points, waypointIndices );

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

		return new Route(
				waypoints,
				polyline.getPolyline(),
				distanceInMeters,
				durationInSeconds,
				legDurationsInSeconds,
				legDistancesInMeters,
				googleRoute.copyrights,
				warnings,
				System.currentTimeMillis()/1000,
                polyline.getPolylineStringIndices());
	}

    public int getDirectionCalls() {
        return directionCalls;
    }

    public void resetDirectionCalls() {
        this.directionCalls = 0;
    }
}
