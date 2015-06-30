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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.NavigationResult;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripRequestUpdateType;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.trip.MyTripDriverPassengersAdapter;
import org.croudtrip.trip.OnDiversionUpdateListener;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.DefaultTransformer;
import org.croudtrip.utils.SwipeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscriber;
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

    private GoogleMap googleMap;

    private NfcAdapter nfcAdapter;
    private NdefMessage ndefMessage;

    private long offerID = -1;

    // Passengers list
    private MyTripDriverPassengersAdapter adapter;
    private SwipeListener touchListener;

    @InjectView(R.id.rv_my_trip_driver_passengers)
    private RecyclerView recyclerView;

    private ProgressWheel mapProgressBar;
    private ProgressWheel passengersProgressBar;
    private ProgressWheel finishProgressBar;
    private ProgressWheel cancelProgressBar;
    private ProgressWheel generalProgressBar;

    @InjectView(R.id.iv_transparent_image)
    private ImageView transparentImageView;

    @Inject
    private TripsResource tripsResource;
    @Inject
    private LocationUpdater locationUpdater;

    private Button finishButton;
    private Button cancelButton;

    // Detect if a passenger cancels his trip or has reached his destination
    private BroadcastReceiver passengersChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Load the whole offer incl. passengers etc. again
            loadOffer();
        }
    };


    //************************* Methods ****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        NdefRecord ndefRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], null);
        ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return inflater.inflate(R.layout.fragment_my_trip_driver, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fill the passengers list
        View header = view.findViewById(R.id.ll_my_trip_driver_info);
        adapter = new MyTripDriverPassengersAdapter(this, header);
        AcceptDeclineRequestListener acceptDeclineListener = new AcceptDeclineRequestListener();
        this.touchListener = new SwipeListener(recyclerView, acceptDeclineListener);
        adapter.setOnRequestAcceptDeclineListener(acceptDeclineListener);

        mapProgressBar = (ProgressWheel) adapter.getHeader().findViewById(R.id.pb_my_trip_map_progressBar);
        passengersProgressBar = (ProgressWheel) adapter.getHeader().findViewById(R.id.pb_my_trip_passengers_progressBar);
        finishProgressBar = (ProgressWheel) adapter.getHeader().findViewById(R.id.pb_my_trip_finish);
        cancelProgressBar = (ProgressWheel) adapter.getHeader().findViewById(R.id.pb_my_trip_cancel);
        generalProgressBar = (ProgressWheel) view.findViewById(R.id.pb_my_trip_progressBar);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setOnTouchListener(touchListener);
        recyclerView.setOnScrollListener(touchListener.makeScrollListener());

        setupCancelButton(header);
        setupFinishButton(header);

        // Get the route to display it on the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.f_my_trip_driver_map);

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
            loadOffer();
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

        // Remove the header from the layout. Otherwise it exists twice
        ((ViewManager) view).removeView(header);
    }

    @Override
    public void onResume() {
        super.onResume();


        if (nfcAdapter != null) {
            nfcAdapter.setNdefPushMessage(ndefMessage, getActivity());
        }

        // Get notified if a passenger cancels his trip or has reached his destination
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.EVENT_PASSENGER_CANCELLED_TRIP);
        filter.addAction(Constants.EVENT_PASSENGER_REACHED_DESTINATION);
        filter.addAction(Constants.EVENT_PASSENGER_ENTERED_CAR);
        filter.addAction(Constants.EVENT_NEW_JOIN_REQUEST);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(passengersChangeReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .unregisterReceiver(passengersChangeReceiver);
    }

    private void setupCancelButton(View header) {

        cancelButton = ((Button) (header.findViewById(R.id.btn_my_trip_driver_cancel_trip)));
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                cancelButton.setEnabled(false);
                cancelProgressBar.setVisibility(View.VISIBLE);

                // Tell the server to cancel this trip
                subscriptions.add(
                        tripsResource.updateOffer(offerID, TripOfferUpdate.createCancelUpdate())
                                .compose(new DefaultTransformer<TripOffer>())
                                .subscribe(new Action1<TripOffer>() {
                                    @Override
                                    public void call(TripOffer offer) {
                                        // After the server has been contacted successfully, clean
                                        // up the SharedPref and show "Offer Trip" screen again
                                        removeRunningTripOfferState();
                                    }

                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Toast.makeText(getActivity(), R.string.join_trip_results_error, Toast.LENGTH_SHORT).show();
                                        cancelButton.setEnabled(true);
                                        cancelProgressBar.setVisibility(View.GONE);
                                    }
                                }));
            }
        });
    }

    private void setupFinishButton(View header) {

        finishButton = ((Button) (header.findViewById(R.id.btn_my_trip_driver_finish_trip)));
        finishButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finishButton.setEnabled(false);
                finishProgressBar.setVisibility(View.VISIBLE);

                // Tell the server to finish this trip
                subscriptions.add(
                        tripsResource.updateOffer(offerID, TripOfferUpdate.createFinishUpdate())
                                .compose(new DefaultTransformer<TripOffer>())
                                .subscribe(new Action1<TripOffer>() {
                                    @Override
                                    public void call(TripOffer offer) {
                                        // After the server has been contacted successfully, clean
                                        // up the SharedPref and show "Offer Trip" screen again
                                        removeRunningTripOfferState();
                                    }

                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Toast.makeText(getActivity(), R.string.join_trip_results_error, Toast.LENGTH_SHORT).show();
                                        finishButton.setEnabled(true);
                                        finishProgressBar.setVisibility(View.GONE);
                                    }
                                }));
            }
        });
    }


    /**
     * Downloads the current offer and processes its information with the {@link LoadOfferSubscriber}
     */
    private synchronized void loadOffer() {

        // UI
        mapProgressBar.setVisibility(View.VISIBLE);
        // Don't show spinner again, looks ugly with already filled adapter
        // passengersProgressBar.setVisibility(View.VISIBLE);

        subscriptions.add(tripsResource.getActiveOffers()
                .compose(new DefaultTransformer<List<TripOffer>>())
                .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {

                    @Override
                    public Observable<TripOffer> call(List<TripOffer> tripOffers) {

                        if (tripOffers.isEmpty()) {
                            // Inform user
                            String errorMsg = getString(R.string.navigation_error_no_offer);
                            Toast.makeText(getActivity(), errorMsg, Toast.LENGTH_LONG).show();
                            removeRunningTripOfferState();
                            throw new NoSuchElementException(errorMsg);
                        }

                        return Observable.just(tripOffers.get(0));
                    }
                }).subscribe(new LoadOfferSubscriber()));
    }


    private void loadPassengers() {
        subscriptions.add(tripsResource.getJoinRequests(false)
                        .compose(new DefaultTransformer<List<JoinTripRequest>>())
                        .subscribe(new ImportantPassengersSubscriber())
        );
    }


    public void informAboutDiversion(final JoinTripRequest joinRequest, final OnDiversionUpdateListener listener,
                                     final TextView textView) {

        // Ask the server for the diversion
        subscriptions.add(tripsResource
                .getDiversionInSecondsForJoinRequest(joinRequest.getId())
                .compose(new DefaultTransformer<Long>())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long diversionInSeconds) {
                        int diversionInMinutes = (int) (diversionInSeconds / 60);
                        listener.onDiversionUpdate(joinRequest, textView, diversionInMinutes);
                    }

                }, new CrashCallback(getActivity(), "failed to get diversion")));
    }


    private void removeRunningTripOfferState() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES,
                Context.MODE_PRIVATE);
        prefs.edit().remove(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER).apply();


        // Change "My Trip" (driver) to "Offer Trip" in navigation drawer
        MaterialNavigationDrawer drawer = ((MaterialNavigationDrawer) getActivity());

        // Find "last" "My Trip", so we don't accidentally rename the join-trip-my trip
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


    private void generateRouteOnMap(TripOffer offer, NavigationResult navigationResult) {

        // only one route will be shown (old route will be deleted
        googleMap.clear();


        // get the polyline that should be shown as a list of RouteLocation objects and convert
        // it to LatLng objects. We have to do it this way, because the models can not import
        // the android maps libraries since the models also exist on the server.
        List<RouteLocation> polyline = navigationResult.getRoute().getPolylineWaypointsForUser(offer.getDriver(), navigationResult.getUserWayPoints());
        List<LatLng> polylinePoints = new ArrayList<LatLng>();
        for (RouteLocation loc : polyline)
            polylinePoints.add(new LatLng(loc.getLat(), loc.getLng()));

        //Timber.d("polyline: " + polyline);
        // Show route information on the map
        googleMap.addPolyline(new PolylineOptions().addAll(polylinePoints));
        googleMap.setMyLocationEnabled(true);

        for (UserWayPoint userWp : navigationResult.getUserWayPoints()) {
            if (!userWp.getUser().equals(offer.getDriver())) {
                googleMap.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(userWp.getLocation().getLat(), userWp.getLocation().getLng()))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                                .anchor(0.5f, 0.5f)
                                .flat(true)
                );
            }
        }

        // Move camera to current position
        Location location = locationUpdater.getLastLocation();
        if (location == null)
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        googleMap.animateCamera(cameraUpdate);

        mapProgressBar.setVisibility(View.GONE);
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
                        offerID = tripOffer.getId();

                        // show route information on the map
                        generateRouteOnMap(tripOffer, NavigationResult.createNavigationResultForDriverRoute(tripOffer));

                        loadPassengers();

                        // Remember that a trip was offered to show "My Trip" instead of "Offer Trip"
                        // in the Navigation drawer
                        getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE)
                                .edit().putBoolean(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER, true).apply();

                    }

                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // ERROR
                        Timber.e(throwable.getMessage());

                        // Inform user
                        Toast.makeText(getActivity(), getString(R.string.offer_trip_failed), Toast.LENGTH_LONG).show();
                        removeRunningTripOfferState();
                    }
                });
    }


    //*************************** Inner classes ********************************//

    /**
     * This Subscriber loads the current route on the map and load the passengers with the help of
     * {@link ImportantPassengersSubscriber}
     */
    private class LoadOfferSubscriber extends Subscriber<TripOffer> {

        @Override
        public void onNext(final TripOffer offer) {

            if (offer == null) {
                throw new NoSuchElementException(getString(R.string.navigation_error_no_offer));
            }

            Timber.d("Received Offer with ID " + offer.getId());

            // Remember the offerID for later and load the passengers for this offer
            offerID = offer.getId();
            loadPassengers();

            // Load the Route
            tripsResource.computeNavigationResultForOffer(offerID)
                    .compose(new DefaultTransformer<NavigationResult>())
                    .subscribe(new Action1<NavigationResult>() {

                                   @Override
                                   public void call(NavigationResult navigationResult) {

                                       if (navigationResult == null) {
                                           throw new NoSuchElementException("No route available");
                                       }

                                       generateRouteOnMap(offer, navigationResult);
                                   }

                               }, new Action1<Throwable>() {
                                   @Override
                                   public void call(Throwable throwable) {
                                       onError(throwable);
                                   }
                               }
                    );

        }


        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable throwable) {
            mapProgressBar.setVisibility(View.GONE);
            passengersProgressBar.setVisibility(View.GONE);
            Timber.e(throwable.getMessage());
            Toast.makeText(getActivity(), getString(R.string.load_trip_failed), Toast.LENGTH_LONG).show();
        }
    }


    /**
     * This Subscriber checks if there are still "important" passengers to take care of. That is,
     * e.g. there are still passengers sitting in the car or there are still passengers accepted.
     * If there are none, the driver may click the finishButton. Furthermore, all
     * relevant passengers (all that were not declined or canceled), will be shown in the RecyclerView-list.
     */
    private class ImportantPassengersSubscriber extends Subscriber<List<JoinTripRequest>> {

        @Override
        public synchronized void onNext(List<JoinTripRequest> joinTripRequests) {

            // Only allow finish if there are no passengers in the car or accepted
            boolean allowFinish = true;
            boolean allowCancel = true;

            for (JoinTripRequest joinTripRequest : joinTripRequests) {

                // TODO: Filter already on server
                if (joinTripRequest.getOffer().getId() != offerID) {
                    continue;
                }

                JoinTripStatus status = joinTripRequest.getStatus();
                if (status == JoinTripStatus.PASSENGER_ACCEPTED) {
                    adapter.updatePendingPassenger(joinTripRequest);

                } else {
                    // Passenger should not appear in pending requests list
                    adapter.removePendingPassenger(joinTripRequest.getId());
                }

                if (status != JoinTripStatus.DRIVER_DECLINED
                        && status != JoinTripStatus.PASSENGER_ACCEPTED) {

                    if (status == JoinTripStatus.PASSENGER_CANCELLED
                            || status == JoinTripStatus.DRIVER_CANCELLED) {
                        // Remove any cancelled passengers from the adapter
                        adapter.removePassenger(joinTripRequest.getId());

                    } else if (status == JoinTripStatus.DRIVER_ACCEPTED
                            || status == JoinTripStatus.PASSENGER_IN_CAR
                            || status == JoinTripStatus.PASSENGER_AT_DESTINATION) {

                        // Simply update (or implicitly add) this passenger request
                        adapter.updatePassenger(joinTripRequest);

                        if (status == JoinTripStatus.DRIVER_ACCEPTED
                                || status == JoinTripStatus.PASSENGER_IN_CAR) {

                            Timber.d("Finishing trip not allowed: there is still an important passenger: " + status);
                            allowFinish = false;

                            if (status == JoinTripStatus.PASSENGER_IN_CAR) {
                                allowCancel = false;
                                Timber.d("Cancelling trip not allowed: there is still a passenger in the car");
                            }
                        }
                    }
                }
            }

            // Dis-/Allow the driver to finish the trip
            finishButton.setEnabled(allowFinish);
            cancelButton.setEnabled(allowCancel);

            adapter.maintainOrder();
            adapter.updateEarnings();   // notifyDataSetChanged is called automatically
        }

        @Override
        public void onCompleted() {
            passengersProgressBar.setVisibility(View.GONE);
        }

        @Override
        public void onError(Throwable e) {
            passengersProgressBar.setVisibility(View.GONE);
            Timber.e("Receiving Passengers (JoinTripRequest) failed:\n" + e.getMessage());
            Toast.makeText(getActivity(), getString(R.string.load_passengers_failed), Toast.LENGTH_LONG).show();
        }
    }


    private class AcceptDeclineRequestListener implements SwipeListener.DismissCallbacks,
            MyTripDriverPassengersAdapter.OnRequestAcceptDeclineListener {

        /**
         * Listener to listen for any driver decisions to accept or decline
         * a pending JoinTripRequest. As soon as such a decision is received, the server
         * is contacted.
         *
         * @param accept   if this method should handle "accept" (true) or "decline" (false)
         * @param position the position of the clicked JoinTripRequest in the adapter
         */
        private synchronized void handleAcceptDecline(final boolean accept, final int position) {

            Timber.d("Swiped position: " + position);

            String task;
            if (accept) {
                task = "Accepting";
            } else {
                task = "Declining";
            }

            final JoinTripRequest request = adapter.getPendingPassenger(position - 1);  // -1 because of header
            Timber.i(task + " Request with ID " + request.getId());

            // UI
            generalProgressBar.setVisibility(View.VISIBLE);

            // Don't allow other user clicks while the task is performed
            recyclerView.setOnTouchListener(null);

            //Get a list of current Join trip requests from the server
            //and make sure that the request is still active (hasn't expired)
            subscriptions.add(tripsResource.getJoinRequests(true)
                    .compose(new DefaultTransformer<List<JoinTripRequest>>())
                    .subscribe(new Action1<List<JoinTripRequest>>() {
                        @Override
                        public void call(List<JoinTripRequest> requests) {

                            if (requests.size() > 0) {

                                for (int i = 0; i < requests.size(); i++) {
                                    if (request.getId() == requests.get(i).getId()) {

                                        //Inform server only if the request is still active (not expired)
                                        JoinTripRequestUpdate requestUpdate;

                                        if (accept) {
                                            requestUpdate = new JoinTripRequestUpdate(JoinTripRequestUpdateType.ACCEPT_PASSENGER);
                                        } else {
                                            requestUpdate = new JoinTripRequestUpdate(JoinTripRequestUpdateType.DECLINE_PASSENGER);
                                        }

                                        subscriptions.add(tripsResource.updateJoinRequest(request.getId(), requestUpdate)
                                                .compose(new DefaultTransformer<JoinTripRequest>())
                                                .subscribe(new AcceptDeclineRequestSubscriber(accept)));

                                        Timber.i("Request has not expired");
                                        break;
                                    }

                                    //If the request wasn't found in the list, show a toast to the driver
                                    // and remove the card from the list
                                    if (i == requests.size() - 1) {
                                        Timber.d("Request has expired");
                                        Toast.makeText(getActivity(), getResources().getString(R.string.offer_trip_request_expired), Toast.LENGTH_SHORT).show();

                                        //Enable clicking the list items again and remove the progress bar
                                        recyclerView.setOnTouchListener(touchListener);
                                        generalProgressBar.setVisibility(View.GONE);

                                        adapter.removePendingPassenger(request.getId());
                                    }
                                }

                            } else {
                                //If the expired request was the last one in the list, size() will be 0
                                //This snippet takes care of this case
                                Timber.d("No requests found (Request has expired)");
                                Toast.makeText(getActivity(), getResources().getString(R.string.offer_trip_request_expired), Toast.LENGTH_SHORT).show();

                                //Enable clicking the list items again and remove the progress bar
                                recyclerView.setOnTouchListener(touchListener);
                                generalProgressBar.setVisibility(View.GONE);

                                adapter.removePendingPassenger(request.getId());
                            }

                            adapter.maintainOrder();
                            adapter.notifyDataSetChanged();

                        }
                    }, new Action1<Throwable>() {

                        @Override
                        public void call(Throwable throwable) {
                            Toast.makeText(getActivity(), R.string.join_trip_results_error, Toast.LENGTH_SHORT).show();
                            generalProgressBar.setVisibility(View.GONE);
                            recyclerView.setOnTouchListener(touchListener);

                        }
                    }));
        }

        @Override
        public void onJoinRequestDecline(View view, int position) {
            handleAcceptDecline(false, position);
        }

        @Override
        public void onJoinRequestAccept(View view, int position) {
            handleAcceptDecline(true, position);
        }

        @Override
        public boolean canDismiss(int position) {
            return adapter.isPositionPendingPassenger(position);
        }

        @Override
        public void onSwipeLeft(RecyclerView recyclerView, int[] dismissedItems) {
            // Decline only the first item and ignore the rest
            if (dismissedItems != null && dismissedItems.length > 0) {
                handleAcceptDecline(false, dismissedItems[0]);
            }

        }

        @Override
        public void onSwipeRight(RecyclerView recyclerView, int[] dismissedItems) {
            // Accept only the first item and ignore the rest
            if (dismissedItems != null && dismissedItems.length > 0) {
                handleAcceptDecline(true, dismissedItems[0]);
            }
        }
    }


    /**
     * A simple Subscriber that removes the previously accepted/declined request
     * from the adapter
     */
    private class AcceptDeclineRequestSubscriber extends Subscriber<JoinTripRequest> {

        private boolean accept;

        protected AcceptDeclineRequestSubscriber(boolean accept) {
            super();
            this.accept = accept;
        }

        @Override
        public void onNext(JoinTripRequest joinTripRequest) {
            // SUCCESS
            Timber.i("Successfully informed the server about a request with ID " + joinTripRequest.getId());

            // Everything worked out, so remove the request from the adapter
            adapter.removePendingPassenger(joinTripRequest.getId());
            adapter.notifyDataSetChanged();

            if (accept) {
                loadOffer();
            }
        }

        @Override
        public void onError(Throwable e) {
            // ERROR
            String task;

            if (accept) {
                task = "accepting";
            } else {
                task = "declining";
            }

            Timber.e("Error when " + task + " a JoinTripRequest: " + e.getMessage());
            onDone();
        }

        @Override
        public void onCompleted() {
            onDone();
        }

        private void onDone() {

            // Allow clicks on trips again
            //adapter.setOnRequestAcceptDeclineListener(new AcceptDeclineRequestListener());
            recyclerView.setOnTouchListener(touchListener);

            // UI
            generalProgressBar.setVisibility(View.GONE);
        }
    }

}
