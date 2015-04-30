package org.croudtrip.activities;

import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import org.croudtrip.R;
import org.croudtrip.TripsResource;
import org.croudtrip.trips.TripOffer;
import org.croudtrip.trips.TripOfferDescription;

import javax.inject.Inject;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * This activity is shown to the user as soon as he offers a trip to our passengers
 * Created by Frederik Simon on 30.04.2015.
 */
@ContentView(R.layout.fragment_maps)
public class DriverActivity extends RoboActivity {
    @Inject TripsResource tripsResource;
    @InjectView(R.id.location_map) private MapFragment mapFragment;

    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        googleMap = mapFragment.getMap();

        Bundle b = getIntent().getExtras();
        int maxDiversion = b.getInt( "maxDiversion" );

        double fromLat = b.getDouble("fromLat");
        double fromLng = b.getDouble("fromLng");

        double toLat = b.getDouble("toLat");
        double toLng = b.getDouble("toLng");

        TripOfferDescription tripOffer = new TripOfferDescription(
                new org.croudtrip.directions.Location( fromLat, fromLng ),
                new org.croudtrip.directions.Location( toLat, toLng ),
                maxDiversion );

        tripsResource.addOffer( tripOffer ).subscribe(new Action1<TripOffer>() {
            @Override
            public void call(TripOffer routeNavigations) {
                Timber.d("Your offer was successfully sent to the server");
                

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                // on main thread; something went wrong
                Timber.e(throwable.getMessage());
            }
        });

    }
}
