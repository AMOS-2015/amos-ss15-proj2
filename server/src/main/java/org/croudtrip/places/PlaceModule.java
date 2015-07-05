package org.croudtrip.places;

import com.google.inject.AbstractModule;

import org.croudtrip.app.CroudTripConfig;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

/**
 * Simple Module to provide {@link PlacesApiContext} via dependency injection
 */
public class PlaceModule extends AbstractModule {

    private final PlacesApiContext placesApiContext;

    public PlaceModule(CroudTripConfig config ) {
        placesApiContext = new PlacesApiContext( config.getGoogleAPIKey() );
    }


    @Override
    protected void configure() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("https://maps.googleapis.com/maps/api/place/")
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam("key", placesApiContext.getGoogleApiKey());
                    }
                })
                .setConverter(new JacksonConverter())
                .build();

        bind(PlacesApi.class).toInstance(adapter.create(PlacesApi.class));
    }

}
