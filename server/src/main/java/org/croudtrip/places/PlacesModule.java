package org.croudtrip.places;

import com.google.inject.AbstractModule;

import org.croudtrip.app.CroudTripConfig;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

public class PlacesModule extends AbstractModule {

    private final String googleApiKey;

    public PlacesModule(CroudTripConfig config) {
        this.googleApiKey = config.getGoogleAPIKey();
    }


    @Override
    protected void configure() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("https://maps.googleapis.com/maps/api/place/")
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam("key", googleApiKey);
                    }
                })
                .setConverter(new JacksonConverter())
                .build();

        bind(PlacesApi.class).toInstance(adapter.create(PlacesApi.class));
    }

}
