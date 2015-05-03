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

        /* For testing purposes. What shoud we do if we cant get a location?*/
        if (lastLocation == null) {
            lastLocation = new Location("test");
            lastLocation.setLatitude(49.5891772);
            lastLocation.setLongitude(10.9844836);
        }
        return this.lastLocation;
    }
}
