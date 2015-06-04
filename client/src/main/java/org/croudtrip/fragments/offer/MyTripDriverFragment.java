/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.fragments.offer;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.api.DirectionsResource;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.DirectionsRequest;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.fragments.OfferTripFragment;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.trip.MyTripDriverPassengersAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This class shows a screen for the driver after he has offered a trip and hence is currently
 * already on his way. He is shown a map, his earnings and the passengers that he has accepted.
 *
 * @author Frederik Simon, Vanessa Lange
 */
public class MyTripDriverFragment extends SubscriptionFragment {

    //************************* Variables ****************************//

    // Route/Navigation
    public static final String ARG_ACTION = "ARG_ACTION";
    public static final String ACTION_LOAD = "ACTION_LOAD";
    public static final String ACTION_CREATE = "ACTION_CREATE";

    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;

    // Passengers list
    private MyTripDriverPassengersAdapter adapter;

    @InjectView(R.id.rv_my_trip_driver_passengers)
    private RecyclerView recyclerView;

    @InjectView(R.id.pb_my_trip_progressBar)
    private ProgressBar progressBar;

    @InjectView(R.id.iv_transparent_image)
    private ImageView transparentImageView;

    @Inject
    private TripsResource tripsResource;
    @Inject
    private DirectionsResource directionsResource;
    @Inject
    private LocationUpdater locationUpdater;

    private Button finishButton;


    //************************* Methods ****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_my_trip_driver, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fill the passengers list
        View header = view.findViewById(R.id.ll_my_trip_driver_info);
        adapter = new MyTripDriverPassengersAdapter(this, header);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Cancel Trip Button
        ((Button) (header.findViewById(R.id.btn_my_trip_driver_cancel_trip)))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        subscriptions.add(tripsResource.getOffers(false)
                                        .compose(new DefaultTransformer<List<TripOffer>>())
                                        .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {
                                            @Override
                                            public Observable<TripOffer> call(List<TripOffer> tripOffers) {
                                                // Cancel all the offers for the time being to be changed to
                                                // get the specific offer at a later stage
                                                if (!tripOffers.isEmpty())
                                                    for (int i = 0; i < tripOffers.size(); i++)
                                                        cancelTripOffer(tripOffers.get(i).getId());
                                                return Observable.just(tripOffers.get(0));
                                            }
                                        })
                                        .subscribe(new Action1<TripOffer>() {
                                            @Override
                                            public void call(TripOffer offer) {
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                Timber.e(throwable.getMessage());
                                            }
                                        })
                        );
                    }
                });

        // Finish Trip Button
        finishButton = ((Button) (header.findViewById(R.id.btn_my_trip_driver_finish_trip)));
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Get the current offer (to get the ID) to tell the server to finish this offer
                tripsResource.getOffers(true)
                        .compose(new DefaultTransformer<List<TripOffer>>())
                        .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {
                            @Override
                            public Observable<TripOffer> call(List<TripOffer> tripOffers) {

                                if (tripOffers.isEmpty()) {
                                    throw new NoSuchElementException("There's currently no offer");
                                }

                                if (tripOffers.size() > 1) {
                                    Timber.e("Found multiple open offers!");
                                }

                                // There should only be one offer, so simply finish the first
                                return Observable.just(tripOffers.get(0));
                            }
                        }).subscribe(new FinishTripSubscription());
            }
        });


        // Get the route to display it on the map
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.f_my_trip_driver_map);

        // TODO: Make it asynchronously
        googleMap = mapFragment.getMap();
        Bundle bundle = getArguments();
        if (bundle == null) {
            bundle = new Bundle();
        }

        String action = bundle.getString(ARG_ACTION, ACTION_LOAD);
        if (action.equals(ACTION_CREATE)) {
            Timber.d("Create Offer");
            createOffer(bundle);
        } else {
            Timber.d("Load Offer");
            loadOffer(bundle);
        }

        transparentImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        recyclerView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    case MotionEvent.ACTION_UP:
                        recyclerView.requestDisallowInterceptTouchEvent(false);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        recyclerView.requestDisallowInterceptTouchEvent(true);
                        return false;

                    default:
                        return true;
                }
            }
        });

        // Enable the finish button only if there are currently no passengers
        // in the car anymore and none are accepted
        subscriptions.add(tripsResource
                        .getJoinRequests(false)
                        .compose(new DefaultTransformer<List<JoinTripRequest>>())
                        .subscribe(new ImportantPassengersSubscriber())
        );

        // Remove the header from the layout. Otherwise it exists twice
        ((ViewManager) view).removeView(header);
    }


    private void loadOffer(Bundle arguments) {

        final RouteLocation[] driverWp = new RouteLocation[2];

        subscriptions.add(tripsResource.getOffers(true)
                        .compose(new DefaultTransformer<List<TripOffer>>())
                        .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {
                            @Override
                            public Observable<TripOffer> call(List<TripOffer> tripOffers) {
                                if (tripOffers.isEmpty())
                                    throw new NoSuchElementException("There's currently no offer of you");

                                return Observable.just(tripOffers.get(0));
                            }
                        })
                        .subscribe(new Action1<TripOffer>() {
                            @Override
                            public void call(TripOffer offer) {
                                if (offer == null)
                                    throw new NoSuchElementException("There's currently no offer of you");

                                Timber.d("Got Offer: " + offer.getDriver().getFirstName() + " " + offer.getDriver().getLastName());
                                driverWp[0] = offer.getDriverRoute().getWayPoints().get(0);
                                driverWp[1] = offer.getDriverRoute().getWayPoints().get(1);

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
                                        .flatMap(new Func1<List<Route>, Observable<Route>>() {
                                            @Override
                                            public Observable<Route> call(List<Route> routes) {
                                                if (routes == null || routes.isEmpty())
                                                    return Observable.empty();

                                                return Observable.just(routes.get(0));
                                            }
                                        })
                                        .compose(new DefaultTransformer<Route>())
                                        .subscribe(new Action1<Route>() {
                                            @Override
                                            public void call(Route route) {
                                                Timber.d("Your offer was successfully loaded from the server");
                                                progressBar.setVisibility(View.GONE);
                                                generateRouteOnMap(route);
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                // on main thread; something went wrong
                                                progressBar.setVisibility(View.GONE);
                                                Timber.e(throwable.getMessage());
                                            }
                                        });

                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e(throwable.getMessage());

                                //errorText.setText(R.string.navigation_error_no_offer);
                                //errorLayout.setVisibility(View.VISIBLE);
                            }
                        })
        );
    }


    private void removeRunningTripOfferState() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES,
                Context.MODE_PRIVATE);
        prefs.edit().remove(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER).apply();


        // Change "My Trip" (driver) to "Offer Trip" in navigation drawer
        MaterialNavigationDrawer drawer = ((MaterialNavigationDrawer) getActivity());

        // find "last" "My Trip", so we don't accidently rename the join-trip-my trip
        List<MaterialSection> sections = drawer.getSectionList();
        MaterialSection section = null;
        for (MaterialSection s : sections) {
            if (s.getTitle().equals(getString(R.string.menu_my_trip))) {
                section = s;
            }
        }
        section.setTitle(getString(R.string.menu_offer_trip));

        // The next fragment shows the "Offer trip" screen
        drawer.setFragment(new OfferTripFragment(), getString(R.string.menu_offer_trip));
    }

    private void generateRouteOnMap(Route route) {

        // Show route information on the map
        googleMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(route.getPolyline())));
        googleMap.setMyLocationEnabled(true);

        // Move camera to current position
        Location location = locationUpdater.getLastLocation();
        if (location == null)
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        googleMap.animateCamera(cameraUpdate);
    }


    private void createOffer(Bundle arguments) {

        int maxDiversion = arguments.getInt("maxDiversion");
        int pricePerKilometer = arguments.getInt("pricePerKilometer");

        double fromLat = arguments.getDouble("fromLat");
        double fromLng = arguments.getDouble("fromLng");

        double toLat = arguments.getDouble("toLat");
        double toLng = arguments.getDouble("toLng");

        long vehicleId = arguments.getLong("vehicle_id");

        TripOfferDescription tripOffer = new TripOfferDescription(
                new RouteLocation(fromLat, fromLng),
                new RouteLocation(toLat, toLng),
                maxDiversion * 1000L,
                pricePerKilometer,
                vehicleId);

        tripsResource.addOffer(tripOffer)
                .compose(new DefaultTransformer<TripOffer>())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<TripOffer>() {

                    @Override
                    public void call(TripOffer tripOffer) {
                        // SUCCESS
                        Timber.d("Your offer was successfully sent to the server");

                        // show route information on the map
                        generateRouteOnMap(tripOffer.getDriverRoute());

                        // Remember that a trip was offered to show "My Trip" instead of "Offer Trip"
                        // in the Navigation drawer
                        getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE)
                                .edit().putBoolean(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER, true).apply();

                        // UI
                        progressBar.setVisibility(View.GONE);
                    }

                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // ERROR
                        Timber.e(throwable.getMessage());

                        // UI
                        progressBar.setVisibility(View.GONE);

                        // Inform user
                        Toast.makeText(getActivity(), getString(R.string.offer_trip_failed), Toast.LENGTH_LONG).show();
                        removeRunningTripOfferState();
                    }
                });
    }


    private void cancelTripOffer(final long id) {
        Subscription subscription = tripsResource.updateOffer(id, TripOfferUpdate.createCancelUpdate())
                .compose(new DefaultTransformer<TripOffer>())
                .subscribe(new Action1<TripOffer>() {
                    @Override
                    public void call(TripOffer offer) {
                        // After the server has been contacted successfully, clean up the SharedPref
                        // and show "Offer Trip" screen again
                        removeRunningTripOfferState();
                        Toast.makeText(getActivity(), "Trip with id: " + id + " was canceled!", Toast.LENGTH_SHORT).show();
                    }

                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.i("Error when cancelling trip with ID " + id + " : " + throwable.getMessage());
                        Toast.makeText(getActivity(), "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        subscriptions.add(subscription);
    }


    /**
     * Updates the displayed passenger list by adding a given JoinTripRequest.
     * JoinTripRequests cannot be added twice.
     * This also updates the displayed earnings for the driver.
     * @param request the JoinTripRequest to be added/removed
     */
    public void addPassengerToList(JoinTripRequest request){

        if(request == null || adapter.contains(request)){
            return;
        }

        adapter.addRequest(request);
        adapter.updateEarnings();
    }


    /**
     * Removes a passenger (identified through the JoinTripRequest) from the displayed list.
     * This also updates the displayed earnings for the driver.
     * @param request the JoinTripRequest from the passenger to ber removed
     */
    public void removePassengerFromList(JoinTripRequest request){

        if(request == null){
            return;
        }

        adapter.removeRequest(request);
        adapter.updateEarnings();
    }


    /**
     * If a passenger has reached his destination, this method should be called to mark him
     * as such in the passenger list.
     * @param request the passenger (identified though the JoinTripRequest) that has reached
     *                his destination
     */
    public void markPassengerAsDestinationReached(JoinTripRequest request){

        if(request == null || request.getStatus() != JoinTripStatus.PASSENGER_AT_DESTINATION) {
            Timber.e("Passenger to mark as 'destination reached' is null or hasn't reached " +
                    "his destination yet ");
        }

        adapter.updateRequest(request);
    }


    //*************************** Inner classes ********************************//


    private class FinishTripSubscription extends Subscriber<TripOffer> {

        @Override
        public void onNext(final TripOffer tripOffer) {

            tripsResource.updateOffer(tripOffer.getId(), TripOfferUpdate.createFinishUpdate())
                    .compose(new DefaultTransformer<TripOffer>())
                    .subscribe(new Action1<TripOffer>() {
                        @Override
                        public void call(TripOffer offer) {
                            // After the server has been contacted successfully, clean up the SharedPref
                            // and show "Offer Trip" screen again
                            removeRunningTripOfferState();
                        }

                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.i("Error when finishing trip with ID " + tripOffer.getId() + ": " + throwable.getMessage());
                            Toast.makeText(getActivity(), "Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });


        }


        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onCompleted() {}
    }


    /**
     * This Subscriber checks if there are still "important" passengers to take care of. That is,
     * e.g. there are still passengers sitting in the car or there are still passengers accepted.
     * If there are none, the driver may click the finishButton. Furthermore, all
     * relevant passengers (all that were not declined or canceled), will be shown in the RecyclerView-list.
     */
    private class ImportantPassengersSubscriber extends Subscriber<List<JoinTripRequest>>{

        @Override
        public void onNext(List<JoinTripRequest> joinTripRequests) {
            Timber.d("Received " + joinTripRequests.size() + " passenger requests");

            // Remember all relevant (all that somehow joined or will join the trip) passengers
            List<JoinTripRequest> allRelevantPassengers = new ArrayList<JoinTripRequest>();

            // Only allow finish if there are no passengers in the car or accepted
            boolean allowFinish = true;


            for (JoinTripRequest joinTripRequest : joinTripRequests) {
                JoinTripStatus status = joinTripRequest.getStatus();

                if (status == JoinTripStatus.DRIVER_ACCEPTED || status == JoinTripStatus.PASSENGER_IN_CAR) {
                    Timber.d("There is still an important passenger: " + status);
                    allowFinish = false;
                }

                if(status != JoinTripStatus.DRIVER_DECLINED && status != JoinTripStatus.PASSENGER_CANCELLED){
                    allRelevantPassengers.add(joinTripRequest);
                }
            }

            if(allowFinish) {
                // Allow the driver to finish the trip
                finishButton.setEnabled(true);
            }

            Timber.d(joinTripRequests.size() + " passengers are relevant to this trip offer");
            adapter.addRequests(allRelevantPassengers);
            adapter.updateEarnings();
        }

        @Override
        public void onCompleted() {}

        @Override
        public void onError(Throwable e) {
            Timber.e("Receiving Passengers (JoinTripRequest) failed:\n" + e.getMessage());

        }
    }
}
