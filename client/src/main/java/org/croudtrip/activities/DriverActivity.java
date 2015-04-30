package org.croudtrip.activities;

import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.croudtrip.R;
import org.croudtrip.TripsResource;
import org.croudtrip.directions.RouteLocation;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.trips.TripOffer;
import org.croudtrip.trips.TripOfferDescription;

import javax.inject.Inject;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectFragment;
import roboguice.inject.InjectView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This activity is shown to the user as soon as he offers a trip to our passengers
 * Created by Frederik Simon on 30.04.2015.
 */
@ContentView(R.layout.fragment_maps)
public class DriverActivity extends RoboActivity {
    @Inject TripsResource tripsResource;

    @InjectFragment(R.id.location_map) private MapFragment mapFragment;
    @InjectView(R.id.duration_text) private TextView durationText;
    @InjectView(R.id.distance_text) private TextView distanceText;

    @Inject private LocationUpdater locationUpdater;
    private Subscription locationUpdateSubscription;

    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        durationText.setText("");
        distanceText.setText("");

        //mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.location_map);
        googleMap = mapFragment.getMap();

        Bundle b = getIntent().getExtras();
        int maxDiversion = b.getInt( "maxDiversion" );
        int pricePerKilometer = b.getInt("pricePerKilometer");

        double fromLat = b.getDouble("fromLat");
        double fromLng = b.getDouble("fromLng");

        double toLat = b.getDouble("toLat");
        double toLng = b.getDouble("toLng");

        TripOfferDescription tripOffer = new TripOfferDescription(
                new RouteLocation( fromLat, fromLng ),
                new RouteLocation( toLat, toLng ),
                maxDiversion * 1000L,
                pricePerKilometer);

        tripsResource.addOffer( tripOffer )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<TripOffer>() {
                    @Override
                    public void call(TripOffer routeNavigations) {
                        Timber.d("Your offer was successfully sent to the server");

                        // show route information on the map
                        googleMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(routeNavigations.getRoute().getPolyline())));
                        googleMap.setMyLocationEnabled(true);

                        // compute duration for driving
                        long seconds = routeNavigations.getRoute().getDurationInSeconds();
                        long hours = seconds/3600;
                        long minutes = (seconds%3600)/60;
                        seconds = (seconds%3600)%60;
                        durationText.setText( String.format( getResources().getString(R.string.duration_text ), hours, minutes, seconds ) );

                        // show distance information
                        distanceText.setText( String.format( getResources().getString(R.string.distance_text ), (routeNavigations.getRoute().getDistanceInMeters() / 1000.0) ) );

                        // move camera to current position
                        Location location = locationUpdater.getLastLocation();
                        if( location == null )
                            return;
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
                        googleMap.animateCamera(cameraUpdate);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // on main thread; something went wrong
                        Timber.e(throwable.getMessage());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // subscribe to location updates
        LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(5)
                .setInterval(100);
        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(this);
        locationUpdateSubscription = locationProvider.getUpdatedLocation(request)
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        locationUpdater.setLastLocation( location );
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();

        locationUpdateSubscription.unsubscribe();
    }
}
