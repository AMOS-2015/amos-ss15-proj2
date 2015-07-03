package org.croudtrip.places;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.maps.model.LatLng;

import org.croudtrip.api.directions.RouteLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dropwizard.jackson.Jackson;

public class PlacesApiRequest {
    private final static String baseURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private String apiKey;

    ObjectMapper objectMapper;

    HashMap<String, String> params = new HashMap<String, String>();

    public PlacesApiRequest( String apiKey ) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    public PlacesApiRequest location( LatLng location ){
        params.put( "location", location.toString() );
        return this;
    }

    public PlacesApiRequest location( String location ){
        params.put( "location", location );
        return this;
    }

    public PlacesApiRequest radius( long radiusInMeters ){
        params.put( "radius", String.valueOf(radiusInMeters) );
        return this;
    }

    public PlacesApiRequest keyword( String keyword ){
        params.put( "keyword", keyword );
        return this;
    }

    public PlacesApiRequest name( String name ){
        params.put( "name", name );
        return this;
    }

    public PlacesApiRequest rankby( PlaceRanking rankby ){
        if( rankby == PlaceRanking.NO_RANKING )
            params.remove( "rankby" );
        else if( rankby == PlaceRanking.RANK_BY_DISTANCE )
            params.put( "rankby", "distance" );
        else if( rankby == PlaceRanking.RANK_BY_PROMINENCE )
            params.put( "rankby", "prominence");
        return this;
    }

    public PlacesApiRequest types( PlaceType...types ) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i != 0) {
                result.append("|");
            }
            result.append(types[i]);
        }
        params.put("types", result.toString());

        return this;
    }


    public List<Place> await() {
        List<Place> places = new ArrayList<>();
        StringBuilder query = new StringBuilder();

        query.append(baseURL);
        query.append("key=" + apiKey);
        for (Map.Entry<String, String> param : params.entrySet()) {
            query.append('&').append(param.getKey()).append("=");
            try {
                query.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return places;
            }
        }

        StringBuilder jsonResult = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            URL url = new URL( query.toString() );
            connection = (HttpURLConnection) url.openConnection();

            InputStreamReader in = new InputStreamReader(connection.getInputStream());

            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResult.append(buff, 0, read);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return places;
        } catch (IOException e) {
            e.printStackTrace();
            return places;
        } finally {
            if( connection != null )
                connection.disconnect();
        }

        try {
            JsonNode resultNodes = null;
            resultNodes = objectMapper.readTree(jsonResult.toString()).get("results");

            for( JsonNode resultNode : resultNodes ) {
                String name = resultNode.get("name").toString();
                JsonNode location = resultNode.get("geometry").get("location");
                RouteLocation loc = objectMapper.readValue( location.toString(), RouteLocation.class );

                places.add( new Place( name, loc ) );
            }

        } catch (IOException e) {
            e.printStackTrace();
            return places;
        }

        return places;
    }
}
