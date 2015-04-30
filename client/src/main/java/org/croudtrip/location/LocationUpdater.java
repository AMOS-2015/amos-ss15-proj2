package org.croudtrip.location;

import android.location.Location;

import javax.inject.Singleton;


/**
 * A class that handles the last retrieved location by the application.
 * Created by Frederik Simon on 30.04.2015.
 */
@Singleton
public class LocationUpdater {

    Location lastLocation;

    public void setLastLocation( Location lastLocation ) {
        this.lastLocation = lastLocation;
    }

    public Location getLastLocation () {
        return this.lastLocation;
    }
}
