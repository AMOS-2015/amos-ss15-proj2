package org.croudtrip.api.directions;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.maps.GeoApiContext;

import org.croudtrip.app.CroudTripConfig;


public final class DirectionsModule extends AbstractModule {


    private final GeoApiContext geoApiContext;


    public DirectionsModule(CroudTripConfig config) {
        this.geoApiContext = new GeoApiContext();
        this.geoApiContext.setApiKey(config.getGoogleAPIKey());
    }


    @Override
    protected void configure() {
        // nothing to do for now
    }


    @Provides
    public GeoApiContext provideGeoApiContext() {
        return geoApiContext;
    }

}
