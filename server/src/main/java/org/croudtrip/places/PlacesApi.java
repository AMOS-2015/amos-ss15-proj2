package org.croudtrip.places;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;

/**
 * (Partial) Retrofit implementation of the Google Places API.
 * https://developers.google.com/places/webservice/
 */
public interface PlacesApi {

    public class QueryMapBuilder {
        private HashMap<String, String> params;

        public QueryMapBuilder() {
            params = new HashMap<String, String>();
        }

        public QueryMapBuilder location( LatLng location ){
            params.put("location", location.toUrlValue());
            System.out.println("location: " + location.toUrlValue());
            return this;
        }

        public QueryMapBuilder radius( long radiusInMeters ) {
            params.put( "radius", String.valueOf(radiusInMeters) );
            return this;
        }

        public QueryMapBuilder rankBy( PlaceRanking ranking ) {
            if( ranking == PlaceRanking.RANK_BY_DISTANCE)
                params.put( "rankby", "distance"  );
            else if( ranking == PlaceRanking.RANK_BY_PROMINENCE)
                params.put("rankby", "prominence" );
            else
                params.remove("rankby");

            return this;
        }

        public Map<String, String> build() {
            return params;
        }
    }

    long
            RADIUS_5_KILOMETERS = 5_000,
            RADIUS_10_KILOMETERS = 10_000,
            RADIUS_20_KILOMETERS = 20_000,
            RADIUS_50_KILOMETERS = 50_000,
            RADIUS_100_KILOMETERS = 100_000;


    @GET("/nearbysearch/json")
    ObjectNode getNearybyPlaces(@Query("location") LatLng location, @Query("radius") long radiusInMeters);

    @GET("/nearbysearch/json")
    ObjectNode getNearybyPlaces(@QueryMap Map<String, String> params);

}
