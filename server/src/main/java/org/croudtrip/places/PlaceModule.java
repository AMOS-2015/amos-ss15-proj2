package org.croudtrip.places;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.croudtrip.app.CroudTripConfig;

/**
 * Simple Module to provide {@link PlacesApiContext} via dependency injection
 */
public class PlaceModule extends AbstractModule {
    private final PlacesApiContext placesApiContext;

    public PlaceModule( CroudTripConfig config ) {
        placesApiContext = new PlacesApiContext( config.getGoogleAPIKey() );
    }


    @Override
    protected void configure() {
    }

    @Provides
    public PlacesApiContext getPlacesApiContext(){
        return placesApiContext;
    }

}
