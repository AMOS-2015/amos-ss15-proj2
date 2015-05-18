package org.croudtrip.location;

import android.location.Location;

import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferUpdate;

import java.util.List;
import java.util.TimerTask;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by Frederik Simon on 17.05.2015.
 */
public class LocationUploadTimerTask extends TimerTask {

    private final float MIN_ACCURACY = 200.0f;

    private LocationUpdater locationUpdater;
    private TripsResource tripsResource;

    public LocationUploadTimerTask(LocationUpdater locationUpdater, TripsResource tripsResource) {
        this.locationUpdater = locationUpdater;
        this.tripsResource = tripsResource;
    }

    @Override
    public void run() {
        tripsResource.getOffers(false)
                .subscribe(new Action1<List<TripOffer>>() {
                    @Override
                    public void call(List<TripOffer> tripOffers) {

                        Location location = locationUpdater.getLastLocation();

                        if( location == null ) {
                            Timber.e("No Update of location was possible, since location was null");
                            return;
                        }

                        Timber.d("Your location accuracy is " + location.getAccuracy());

                        if( location.getAccuracy() > MIN_ACCURACY ) {
                            Timber.e("Your location is not accurate enough: " + location.getAccuracy());
                            return;
                        }

                        RouteLocation routeLocation = new RouteLocation( location.getLatitude(), location.getLongitude() );

                        for (final TripOffer offer : tripOffers) {

                            TripOfferUpdate offerUpdate = new TripOfferUpdate( routeLocation );
                            tripsResource.updateOffer(offer.getId(), offerUpdate)
                                .subscribe( new Action1<TripOffer>() {
                                    @Override
                                    public void call(TripOffer tripOffer) {
                                        Timber.d("Updated your location on the server for offer " + tripOffer.getId());
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Timber.e("Was not able to update your location on the server " + offer.getId() + " : " + throwable.getMessage());
                                    }
                                });
                        }
                    }
                });
    }
}
