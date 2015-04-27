package org.croudtrip.directions;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.croudtrip.app.CroudTripConfig;
import org.croudtrip.rest.DirectionsResource;

/**
 *
 * Created by Frederik Simon on 24.04.2015.
 */
public class DirectionsModule extends AbstractModule {
    private final CroudTripConfig config;

    public DirectionsModule( CroudTripConfig config ) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(DirectionsResource.class);
    }

    @Provides
    CroudTripConfig getConfig() {
        return config;
    }
}
