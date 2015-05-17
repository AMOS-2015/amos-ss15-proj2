package org.croudtrip.fragments;


import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.croudtrip.R;
import org.croudtrip.api.DirectionsResource;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.DirectionsRequest;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.utils.DefaultTransformer;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationFragment extends SubscriptionFragment {

    public static final String ARG_ACTION = "ARG_ACTION";
    public static final String ACTION_LOAD = "ACTION_LOAD";
    public static final String ACTION_CREATE = "ACTION_CREATE";


    @Inject TripsResource tripsResource;
    @Inject DirectionsResource directionsResource;

    private MapFragment mapFragment;
    @InjectView(R.id.duration_text) private TextView durationText;
    @InjectView(R.id.distance_text) private TextView distanceText;
    @InjectView(R.id.progressLayout) private LinearLayout progressLayout;
    @InjectView(R.id.errorLayout) private RelativeLayout errorLayout;
    @InjectView(R.id.errorText) private TextView errorText;

    @Inject private LocationUpdater locationUpdater;

    private GoogleMap googleMap;

    private static View rootView;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if( rootView != null ){
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null )
                parent.removeView(rootView);
        }


        try {
            rootView  = inflater.inflate(R.layout.fragment_maps, container, false);
        } catch (InflateException e) {
            /* map is already there, just return view as it is */
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Timber.d("ON VIEW CREATED");

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.location_map);
        googleMap = mapFragment.getMap();
        durationText.setText("");
        distanceText.setText("");


        Bundle b = getArguments();
        if( b == null )
            b = new Bundle();
        String action = b.getString(ARG_ACTION, ACTION_LOAD);

        errorLayout.setVisibility(View.GONE);
        progressLayout.setVisibility(View.VISIBLE);

        if( action.equals( ACTION_CREATE ) ) {
            Timber.d("Create Offer");
            createOffer( b );
        }
        else
        {
            Timber.d("Load Offer");
            loadOffer(b);
        }
    }

    private void loadOffer(Bundle arguments) {

        final RouteLocation[] driverWp = new RouteLocation[2] ;

        tripsResource.getOffers(true)
                .compose(new DefaultTransformer<List<TripOffer>>())
                .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {
                    @Override
                    public Observable<TripOffer> call(List<TripOffer> tripOffers) {
                        if( tripOffers.isEmpty() )
                            throw new NoSuchElementException("There's currently no offer of you");

                        return Observable.just(tripOffers.get(0));
                    }
                })
                .subscribe( new Action1<TripOffer>() {
                    @Override
                    public void call(TripOffer offer) {
                        if( offer == null )
                            throw new NoSuchElementException("There's currently no offer of you");

                        Timber.d("Got Offer: " + offer.getDriver().getFirstName() + " " + offer.getDriver().getLastName());
                        driverWp[0] = offer.getDriverRoute().getWayPoints().get(0);
                        driverWp[1] = offer.getDriverRoute().getWayPoints().get(1);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable.getMessage());

                        errorText.setText(R.string.navigation_error_no_offer);
                        errorLayout.setVisibility(View.VISIBLE);
                    }
                } );

        tripsResource.getDriverAcceptedJoinRequests()
                .compose(new DefaultTransformer<List<JoinTripRequest>>())
                .flatMap(new Func1<List<JoinTripRequest>, Observable<List<RouteLocation>>>() {
                    @Override
                    public Observable<List<RouteLocation>> call(List<JoinTripRequest> joinTripRequests) {
                        // TODO: Handle multiple join trip request, but we will stick to one for now
                        List<RouteLocation> waypoints = new LinkedList<RouteLocation>();
                        if (joinTripRequests == null || joinTripRequests.isEmpty()) {
                            return Observable.just(waypoints);
                        }

                        JoinTripRequest firstRequest = joinTripRequests.get(0);
                        List<RouteLocation> reqWaypoints = firstRequest.getQuery().getPassengerRoute().getWayPoints();

                        waypoints.addAll(reqWaypoints);

                        return Observable.just(waypoints);
                    }
                })
                .flatMap(new Func1<List<RouteLocation>, Observable<List<Route>>>() {
                    @Override
                    public Observable<List<Route>> call(List<RouteLocation> routeLocations) {
                        routeLocations.add(0, driverWp[0]);
                        routeLocations.add(driverWp[1]);
                        Timber.d("Sending directions request with " + routeLocations.size() + " waypoints");
                        DirectionsRequest directionsRequest = new DirectionsRequest(routeLocations);
                        return directionsResource.getDirections(directionsRequest);
                    }
                })
                .flatMap( new Func1<List<Route>, Observable<Route>>() {
                    @Override
                    public Observable<Route> call(List<Route> routes) {
                        if( routes == null || routes.isEmpty())
                            return Observable.empty();

                        return Observable.just( routes.get(0) );
                    }
                })
                .compose(new DefaultTransformer<Route>())
                .subscribe(new Action1<Route>() {
                    @Override
                    public void call(Route route) {
                        Timber.d("Your offer was successfully loaded from the server");
                        progressLayout.setVisibility(View.GONE);
                        generateRouteOnMap(route);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // on main thread; something went wrong
                        progressLayout.setVisibility(View.GONE);
                        Timber.e(throwable.getMessage());
                    }
                });
    }

    private void generateRouteOnMap(Route route) {
        // show route information on the map
        googleMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(route.getPolyline())));
        googleMap.setMyLocationEnabled(true);

        setDurationText(route);
        distanceText.setText( String.format( getResources().getString(R.string.distance_text ), (route.getDistanceInMeters() / 1000.0) ) );

        // move camera to current position
        Location location = locationUpdater.getLastLocation();
        if( location == null )
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        googleMap.animateCamera(cameraUpdate);
    }

    private void setDurationText(Route route) {
        // compute duration for driving
        long seconds = route.getDurationInSeconds();
        long hours = seconds/3600;
        long minutes = (seconds%3600)/60;
        seconds = (seconds%3600)%60;
        durationText.setText( String.format( getResources().getString(R.string.duration_text ), hours, minutes, seconds ) );
    }

    private void createOffer(Bundle arguments) {
        int maxDiversion = arguments.getInt( "maxDiversion" );
        int pricePerKilometer = arguments.getInt("pricePerKilometer");

        double fromLat = arguments.getDouble("fromLat");
        double fromLng = arguments.getDouble("fromLng");

        double toLat = arguments.getDouble("toLat");
        double toLng = arguments.getDouble("toLng");

        long vehicleId = arguments.getLong("vehicle_id");

        TripOfferDescription tripOffer = new TripOfferDescription(
                new RouteLocation( fromLat, fromLng ),
                new RouteLocation( toLat, toLng ),
                maxDiversion * 1000L,
                pricePerKilometer,
                vehicleId);

        tripsResource.addOffer( tripOffer )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<TripOffer>() {
                    @Override
                    public void call(TripOffer routeNavigations) {
                        Timber.d("Your offer was successfully sent to the server");

                        // show route information on the map
                        progressLayout.setVisibility(View.GONE);
                        generateRouteOnMap(routeNavigations.getDriverRoute());

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // on main thread; something went wrong
                        progressLayout.setVisibility(View.GONE);
                        Timber.e(throwable.getMessage());
                    }
                });
    }
}
