package org.croudtrip.places;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.maps.model.LatLng;

import org.croudtrip.api.directions.RouteLocation;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles calls to the Google Places API.
 */
public class PlacesManager {

	private final PlacesApi placesApi;

	@Inject
	PlacesManager(PlacesApi placesApi) {
		this.placesApi = placesApi;
	}


	public List<Place> getNearbyPlaces(LatLng location, long radiusInMeters, int maxResults) {
		ObjectNode jsonResult = placesApi.getNearybyPlaces(location, radiusInMeters);
		JsonNode jsonPlaces = jsonResult.path("results");

		int count = 0;
		List<Place> places = new ArrayList<>();
		for (JsonNode jsonPlace : jsonPlaces) {
			if (count >= maxResults) break;
			++count;

			JsonNode jsonLocation = jsonPlace.path("geometry").path("location");
			Place place = new Place(
					jsonPlace.path("name").asText(),
					new RouteLocation(
							jsonLocation.path("lat").asLong(),
							jsonLocation.path("lng").asLong()));
			places.add(place);
		}

		return places;
	}

}
