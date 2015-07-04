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
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;

import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteDistanceDuration;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

public class DirectionsManager {

	private final GeoApiContext geoApiContext;
    private final LogManager logManager;

    private static HashMap<CachingRouteKey, List<Route>> cachedRoutes = new HashMap<>();

    private static class CachingRouteKey{
        private List<RouteLocation> waypoints;
        private long creationTimestamp;

        public CachingRouteKey(RouteLocation startLocaction, RouteLocation destinationLocation, List<RouteLocation> waypoints) {
            this.waypoints = new ArrayList<>();
            this.waypoints.add(startLocaction);
            this.waypoints.addAll( waypoints );
            this.waypoints.add(destinationLocation);
            this.creationTimestamp = System.currentTimeMillis();
        }

        public long getCreationTimestamp() {
            return creationTimestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CachingRouteKey that = (CachingRouteKey) o;

            return waypoints.equals(that.waypoints) && Math.abs(creationTimestamp - that.creationTimestamp) < 10 * 60*1000;
        }

        @Override
        public int hashCode() {
            return waypoints.hashCode();
        }
    }

	@Inject
	DirectionsManager(GeoApiContext geoApiContext, LogManager logManager) {
		this.geoApiContext = geoApiContext;
        this.logManager = logManager;
	}

    /**
     * Find a simple route using GoogleDirectionAPI from a starting point to a destination
     * @param startLocation the start of the directions query
     * @param endLocation the destination of the query
     * @return a list of possible routes to get to the given destination
     */
	public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation) {
		return getDirections(startLocation, endLocation, new ArrayList<RouteLocation>());
	}

    /**
     * Find a route using Google's Directions API from a starting point to a destination with serveral
     * given waypoints.
     * @param startLocation the start of the directions query
     * @param endLocation the destination of the query
     * @param waypoints the waypoints that should be visited
     * @return a list of possible routes to the given destination
     */
    public List<Route> getDirections(RouteLocation startLocation, RouteLocation endLocation, List<RouteLocation> waypoints) {
        CachingRouteKey cachingRouteKey = new CachingRouteKey( startLocation, endLocation, waypoints );
        if( cachedRoutes.containsKey( cachingRouteKey ) ){
            logManager.d("DIRECTIONS REQUEST " + startLocation + " to " + endLocation + " --> USED CACHED RESULT");
            return cachedRoutes.get( cachingRouteKey );
        }

        LatLng origin = new LatLng(startLocation.getLat(), startLocation.getLng());
        LatLng destination = new LatLng(endLocation.getLat(), endLocation.getLng());

        logManager.d("DIRECTIONS REQUEST " + startLocation + " to " + endLocation + " (" + waypoints.size() + " wps)");

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

            for (DirectionsRoute googleRoute : googleRoutes) {
                    result.add(createRoute(allWaypoints, googleRoute));
                }

            cachedRoutes.put( cachingRouteKey, result );

            return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    /**
     * Computes distance and duration for a given start and destination location using Google's Distance API Matrix
     * @param startLocation the start of the directions query
     * @param destinationLocation the destination of the query
     * @return A {@link RouteDistanceDuration} that contains the distance and the duration of the trip from start to destination
     */
    public RouteDistanceDuration getDistanceAndDurationForDirection( RouteLocation startLocation, RouteLocation destinationLocation ){
        LatLng origin = new LatLng(startLocation.getLat(), startLocation.getLng());
        LatLng destination = new LatLng(destinationLocation.getLat(), destinationLocation.getLng());

        try {
            DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest( geoApiContext )
                    .origins(origin)
                    .destinations(destination)
                    .await();

            if( distanceMatrix.rows.length == 0 ||
                    distanceMatrix.rows[0].elements.length == 0 )
                throw new RuntimeException("No distance and duration found.");

            return new RouteDistanceDuration( distanceMatrix.rows[0].elements[0].distance.inMeters, distanceMatrix.rows[0].elements[0].duration.inSeconds );
        } catch (Exception e) {
            e.printStackTrace();
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
}
