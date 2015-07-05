package org.croudtrip.places;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.maps.model.LatLng;

import retrofit.http.GET;
import retrofit.http.Query;

/**
 * (Partial) Retrofit implementation of the Google Places API.
 * https://developers.google.com/places/webservice/
 */
public interface PlacesApi {

    long
            RADIUS_5_KILOMETERS = 5_000,
            RADIUS_10_KILOMETERS = 10_000,
            RADIUS_20_KILOMETERS = 20_000,
            RADIUS_50_KILOMETERS = 50_000,
            RADIUS_100_KILOMETERS = 100_000;


    @GET("/nearbysearch/json")
    ObjectNode getNearybyPlaces(@Query("location") LatLng location, @Query("radius") long radiusInMeters);

}
