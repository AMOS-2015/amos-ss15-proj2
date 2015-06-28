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

package org.croudtrip.fragments.join;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import org.croudtrip.api.trips.SuperTrip;
import org.croudtrip.api.trips.UserWayPoint;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.trip.MyTripPassengerDriversAdapter;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.CrashPopup;
import org.croudtrip.utils.DefaultTransformer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * This fragment shows the passenger his current trip and several information like the related
 * drivers, the prices, a map etc.
 *
 * @author Alexander Popp, Vanessa Lange
 */
public class JoinDrivingFragment extends SubscriptionFragment {

    @InjectView(R.id.btn_joint_trip_reached)
    private Button btnReachedDestination; //also handles the "My driver is here" stuff
    @InjectView(R.id.btn_joint_trip_cancel)
    private Button btnCancelTrip;
    @InjectView(R.id.btn_joint_trip_report)
    private Button btnReportDriver;

    @InjectView(R.id.join_trip_sending)
    private LinearLayout llSending;
    @InjectView(R.id.join_trip_waiting)
    private LinearLayout llWaiting;
    @InjectView(R.id.join_trip_driving)
    private LinearLayout llDriving;
    @InjectView(R.id.fl_join_trip_driving_map)
    private FrameLayout flMap;

    @InjectView(R.id.pickup_time)
    private TextView tvPickupTime;
    @InjectView(R.id.tv_my_trip_driver_passengers_title)
    private TextView tvMyDrivers;

    @InjectView(R.id.nfc_explanation)
    private TextView tvNfcExplanation;
    @InjectView(R.id.nfc_icon)
    private ImageView ivNfcIcon;

    @InjectView(R.id.pb_join_trip_driving_reached_destination)
    private ProgressWheel progressBarDest;
    @InjectView(R.id.pb_join_trip_driving_report)
    private ProgressWheel progressBarReport;
    @InjectView(R.id.pb_join_trip_driving_cancel)
    private ProgressWheel progressBarCancel;
    @InjectView(R.id.pb_join_trip_map_progressBar)
    private ProgressWheel progressBarMap;
    @InjectView(R.id.pb_my_trip_drivers_progressBar)
    private ProgressWheel progressBarDrivers;

    @InjectView(R.id.iv_transparent_image)
    private ImageView transparentImageView;

    @Inject
    TripsResource tripsResource;

    @Inject
    private LocationUpdater locationUpdater;


    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;

    private GoogleMap googleMap;


    // Passengers list
    private MyTripPassengerDriversAdapter adapter;

    @InjectView(R.id.rv_join_trip_driving_drivers)
    private RecyclerView recyclerView;

    private ArrayList<Integer> colors;
    private int colorPosition = 0;



    //***************************** Methods *****************************//


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Register local broadcasts
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(joinRequestExpiredReceiver,
                new IntentFilter(Constants.EVENT_JOIN_REQUEST_EXPIRED));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(nfcScannedReceiver,
                new IntentFilter(Constants.EVENT_NFC_TAG_SCANNED));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.EVENT_SECONDARY_DRIVER_ACCEPTED);
        filter.addAction(Constants.EVENT_SECONDARY_DRIVER_DECLINED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(secondaryDriverAcceptedDeclinedReceiver, filter);

        //Register nfc adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter != null) {
            nfcPendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }

        //Initialize colors for different routes on the map
        colors = new ArrayList<>();
        colors.add(Color.BLUE);
        colors.add(Color.GREEN);
        colors.add(Color.RED);
        colors.add(Color.YELLOW);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_my_trip));
        ((MaterialNavigationDrawer) getActivity()).setTitle(R.string.menu_my_trip);

        View view = inflater.inflate(R.layout.fragment_join_driving, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llSending.setVisibility(View.GONE);
        llWaiting.setVisibility(View.GONE);
        llDriving.setVisibility(View.GONE);

        // Fill the drivers list
        View header = view.findViewById(R.id.ll_join_trip_driving_info);
        adapter = new MyTripPassengerDriversAdapter(header);

        //mapProgressBar = (ProgressWheel) adapter.getHeader().findViewById(R.id.pb_my_trip_map_progressBar);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Get the route to display it on the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.f_join_trip_driving_map);
        googleMap = mapFragment.getMap();

        // Remove the header from the layout. Otherwise it exists twice
        ((ViewManager) view).removeView(header);

        // Hack to prevent the recyclerview from scrolling when touching the map
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


        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_WAITING, false)) {
            // WAITING for ACCEPT: passenger is currently waiting for the drivers approval

            setButtonInactive(btnReportDriver);
            setButtonInactive(btnReachedDestination);
            setButtonActive(btnCancelTrip);

            llSending.setVisibility(View.VISIBLE);
            flMap.setVisibility(View.GONE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_driverArrival));


        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
            // IN THE CAR: passenger is currently on a trip already in the car

            setButtonInactive(btnCancelTrip);
            setButtonActive(btnReachedDestination);
            setButtonActive(btnReportDriver);

            llDriving.setVisibility(View.VISIBLE);
            flMap.setVisibility(View.VISIBLE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_left_Car));
            btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Handle here all the stuff that happens when the trip is successfully completed (user hits "I have reached my destination")
                    if (prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
                        updateTrip(JoinTripRequestUpdateType.LEAVE_CAR, progressBarDest);
                    } else {
                        // If the user enters the car and leaves the car without this method called twice we need this distinction
                        updateTrip(JoinTripRequestUpdateType.ENTER_CAR, progressBarDest);
                    }
                }
            });
        } else {
            // WAITING for DRIVER: passenger is currently waiting for his driver but already accepted

            setButtonActive(btnCancelTrip);
            setButtonActive(btnReachedDestination);
            setButtonActive(btnReportDriver);

            llWaiting.setVisibility(View.VISIBLE);
            flMap.setVisibility(View.VISIBLE);

            switchToNfcIfAvailable();
            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_driverArrival));
            btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Handle here all the stuff that happens when the user enters the car (user hits "My driver is here")

                    if (prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
                        updateTrip(JoinTripRequestUpdateType.LEAVE_CAR, progressBarDest);
                    } else {
                        // If the user enters the car and leaves the car without this method called twice we need this distinction
                        updateTrip(JoinTripRequestUpdateType.ENTER_CAR, progressBarDest);
                    }

                }
            });
        }

        /*
        Cancel trip
         */
        btnCancelTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Handle here all the stuff that happens when the user cancels the trip
                Toast.makeText(getActivity(), getResources().getString(R.string.my_trip_driver_cancel_trip), Toast.LENGTH_SHORT).show();
                //updateTrip(JoinTripRequestUpdateType.CANCEL, progressBarCancel);
                // just a quick cancel of all active super trips
                // TODO: Adjust the stuff that is written to the shared preferences, since it is not that simple anymore for super trips
                progressBarCancel.setVisibility(View.VISIBLE);
                tripsResource.cancelActiveSuperTrips().compose( new DefaultTransformer<Object>() )
                        .subscribe( new Action1<Object>() {
                                        @Override
                                        public void call(Object o) {
                                            progressBarCancel.setVisibility(View.GONE);
                                            sendUserBackToSearch();
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            Toast.makeText(getActivity(), R.string.join_trip_results_error, Toast.LENGTH_SHORT).show();
                                            progressBarCancel.setVisibility(View.GONE);
                                        }
                                    });
            }
        });

        loadRequest();
    }


    /**
     * Downloads the current(super-)trip from the server
     */
    private void loadRequest(){
        subscriptions.add(tripsResource.getAllActiveTrips()
                .compose(new DefaultTransformer<List<SuperTrip>>())
                .subscribe(new LoadRequestSubscriber()));
    }

    /*
    Parse and show information about the current trip, like price, driver and cost
     */
    private void showJoinedTrip(List<JoinTripRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            Timber.e("List<JoinTripRequest> is empty or doesn't exist");
            return;
        }

        if(flMap.getVisibility() == View.VISIBLE) {
            drawRoutesOnMap(requests);
        }

        // Show drivers
        for(JoinTripRequest r : requests) {
            adapter.updateRequest(r);
        }

        progressBarDrivers.setVisibility(View.GONE);

        // Show correct plural of drivers
        int numDrivers = adapter.getNumDrivers();
        Resources res = getResources();
        tvMyDrivers.setText(res.getQuantityString(R.plurals.join_trip_results_my_drivers, numDrivers, numDrivers));

        // TODO: for first/next driver
        // Show arrival time
        String dateAsString = "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(1000 * (requests.get(0).getEstimatedArrivalTimestamp()));

        //Display remaining time in the format hh:mm
        if ((calendar.get(Calendar.HOUR_OF_DAY) < 10) && (calendar.get((Calendar.MINUTE)) < 10))
            dateAsString = "0" + calendar.get(Calendar.HOUR_OF_DAY) + ":0" + calendar.get(Calendar.MINUTE);
        else if (calendar.get(Calendar.HOUR_OF_DAY) < 10)
            dateAsString = "0" + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE);
        else if (calendar.get((Calendar.MINUTE)) < 10)
            dateAsString = calendar.get(Calendar.HOUR_OF_DAY) + ":0" + calendar.get(Calendar.MINUTE);
        else
            dateAsString = calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE);

        tvPickupTime.setText(dateAsString);
    }

    private void drawRoutesOnMap(List<JoinTripRequest> requests) {
        colorPosition = 0;
        googleMap.clear();

        for (final JoinTripRequest joinTripRequest : requests) {
            subscriptions.add(tripsResource
                    .computeNavigationResultForOffer(joinTripRequest.getOffer().getId())
                    .subscribe(new Action1<NavigationResult>() {
                        @Override
                        public void call(final NavigationResult navigationResult) {

                            //Update the view with the received data
                            if (isAdded()) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            List<UserWayPoint> wayPoints = navigationResult.getUserWayPoints();
                                            List<RouteLocation> polyline = navigationResult.getRoute()
                                                    .getPolylineWaypointsForUser(
                                                            joinTripRequest.getSuperTrip().getQuery().getPassenger(),
                                                            wayPoints);

                                            List<LatLng> polylinePoints = new ArrayList<LatLng>();
                                            LatLng first = null;
                                            LatLng last = null;
                                            for (RouteLocation loc : polyline) {

                                                last = new LatLng(loc.getLat(), loc.getLng());
                                                if (first == null) {
                                                    first = last;
                                                }

                                                polylinePoints.add(last);
                                            }

                                            // Correct line color (alternating)
                                            googleMap.addPolyline(new PolylineOptions()
                                                    .addAll(polylinePoints).color(colors.get(colorPosition % colors.size())));
                                            colorPosition++;

                                            // Show dots at different driver pick-up/drop-offs
                                            googleMap.addMarker(
                                                    new MarkerOptions()
                                                            .position(first)
                                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                                                            .anchor(0.5f, 0.5f)
                                                            .flat(true)
                                            );
                                            googleMap.addMarker(
                                                    new MarkerOptions()
                                                            .position(last)
                                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                                                            .anchor(0.5f, 0.5f)
                                                            .flat(true)
                                            );

                                        } catch (IllegalArgumentException ex) {
                                            CrashPopup.show(getActivity(), ex);
                                        }
                                    }
                                });
                            }

                        }
                    }, new CrashCallback(getActivity(), "failed to get navigation")));
        }

        googleMap.setMyLocationEnabled(true);

        Location location = locationUpdater.getLastLocation();
        if (location == null)
            return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        googleMap.animateCamera(cameraUpdate);

        progressBarMap.setVisibility(View.INVISIBLE);
    }

    /*
    Changes the UI to indicate the passenger that he should use NFC to enter the car.
    In detail: Show icon and explanation for NFC and hide the corresponding button
     */
    private void switchToNfcIfAvailable() {
        NfcManager manager = (NfcManager) getActivity().getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            // nfc exists and is enabled.
            ivNfcIcon.setVisibility(View.VISIBLE);
            tvNfcExplanation.setVisibility(View.VISIBLE);
            btnReachedDestination.setVisibility(View.GONE);
        }
    }

    /*
    Redirect user to the very first screen of the "join" flow. Here he can start a new join-request
     */
    private void sendUserBackToSearch() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false);
        editor.putBoolean(Constants.SHARED_PREF_KEY_DRIVING, false);
        editor.putBoolean(Constants.SHARED_PREF_KEY_WAITING, false);
        editor.apply();

        Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(startingIntent);
    }

    /*
    Send the new status of the trip to the server. The status may be canceled, entered the car and left the car
     */
    private void updateTrip(final JoinTripRequestUpdateType updateType, final ProgressWheel progressBar) {

        //show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        //send update trip request to server
        JoinTripRequestUpdate requestUpdate = new JoinTripRequestUpdate(updateType);
        Subscription subscription = tripsResource.updateJoinRequest(prefs.getLong(Constants.SHARED_PREF_KEY_TRIP_ID, -1), requestUpdate)
                .compose(new DefaultTransformer<JoinTripRequest>())
                .subscribe(new Action1<JoinTripRequest>() {
                    @Override
                    public void call(JoinTripRequest joinTripRequest) {
                        Timber.d("update trip successfully called");

                        //hide loading indicator
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (updateType.equals(JoinTripRequestUpdateType.CANCEL)) {
                            //if a user cancelled everything related to the status can be erased locally
                            // + redirection to the search screen
                            sendUserBackToSearch();
                            return;
                        } else if (updateType.equals(JoinTripRequestUpdateType.LEAVE_CAR)) {

                            //Check if this was the last part of a supertrip
                            tripsResource.getAllActiveTrips(new Callback<List<SuperTrip>>() {
                                @Override
                                public void success(List<SuperTrip> superTrips, Response response) {

                                    if (superTrips.size() == 0) {
                                        //no trips active anymore means that the passenger has reached the destination...
                                        sendUserBackToSearch();
                                    } else {
                                        //..otherwise he must be able to enter the next car

                                        //get JoinTripRequest for the remaining SuperTrip
                                        tripsResource.getJoinTripRequestsForSuperTrip(superTrips.get(0).getId(), new Callback<List<JoinTripRequest>>() {
                                            @Override
                                            public void success(List<JoinTripRequest> joinTripRequests, Response response) {
                                                //get the next JoinTripRequest
                                                JoinTripRequest nextRequest = null;

                                                //the next request which was accepted by passenger+driver but the passenger did not enter the
                                                //car yet is the next part of the SuperTrip
                                                for (JoinTripRequest request : joinTripRequests) {
                                                    if (request.getStatus().equals(JoinTripStatus.DRIVER_ACCEPTED)) {
                                                        nextRequest = request;
                                                        break;
                                                    }
                                                }

                                                //No correct JoinTripRequest is found
                                                if (nextRequest == null) {
                                                    showErrorMessage(progressBar);
                                                    return;
                                                }

                                                //remove status "driving" locally
                                                final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                                SharedPreferences.Editor editor = prefs.edit();
                                                editor.putBoolean(Constants.SHARED_PREF_KEY_DRIVING, false);
                                                editor.putLong(Constants.SHARED_PREF_KEY_TRIP_ID,  nextRequest.getId()); //will break
                                                editor.apply();

                                                //show correct ui elements
                                                switchToNfcIfAvailable();

                                                //change description and functionality of button back to "driver is here"
                                                btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_driverArrival));
                                                btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        updateTrip(JoinTripRequestUpdateType.ENTER_CAR, progressBarDest);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void failure(RetrofitError error) {
                                                showErrorMessage(progressBar);
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    showErrorMessage(progressBar);
                                }
                            });
                        } else if (updateType.equals(JoinTripRequestUpdateType.ENTER_CAR)) {
                            //the user is now in the car -> switch to driving status

                            //save status "driving" locally
                            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(Constants.SHARED_PREF_KEY_DRIVING, true);
                            editor.apply();

                            //hide nfc related ui elements
                            ivNfcIcon.setVisibility(View.GONE);
                            tvNfcExplanation.setVisibility(View.GONE);

                            //change description of button from "enter car" to "leave car"
                            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_left_Car));
                            btnReachedDestination.setVisibility(View.VISIBLE);
                            btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    updateTrip(JoinTripRequestUpdateType.LEAVE_CAR, progressBarDest);
                                }
                            });

                            //passenger can not cancel while he is in the car -> disable button
                            setButtonInactive(btnCancelTrip);

                            //display or hide the correct view groups
                            llWaiting.setVisibility(View.GONE);
                            llDriving.setVisibility(View.VISIBLE);
                            flMap.setVisibility(View.VISIBLE);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        CrashPopup.show(getActivity(), throwable);
                        showErrorMessage(progressBar);
                    }
                });
        subscriptions.add(subscription);
    }

    private void showErrorMessage(ProgressWheel progressBar) {
        Toast.makeText(getActivity(), R.string.join_trip_results_error, Toast.LENGTH_SHORT).show();

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /*
    Activate the given button and make it clickable
     */
    private void setButtonActive(Button button) {
        button.setClickable(true);
        button.setBackgroundColor(getResources().getColor(R.color.primary));
    }

    /*
    Deactivate the given button and make it unclickable
     */
    private void setButtonInactive(Button button) {
        button.setClickable(false);
        button.setBackgroundColor(getResources().getColor(R.color.inactive));
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(Constants.SHARED_PREF_KEY_WAITING, false) && !prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
            //listen for NFC events
            if (nfcAdapter != null) {
                nfcAdapter.enableForegroundDispatch(getActivity(), nfcPendingIntent, null, null);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .unregisterReceiver(joinRequestExpiredReceiver);

        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .unregisterReceiver(secondaryDriverAcceptedDeclinedReceiver);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        //Since the NFC scan pauses the fragment we must unregister this receiver in onDestroy,
        //otherwise we could not catch this Intent
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(nfcScannedReceiver);

    }


    //The onReceive method is fired when the join trip request expires on the server
    //The passenger is redirected to the join trip UI accordingly
    private BroadcastReceiver joinRequestExpiredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Request expired broadcast receiver: onReceive");
            sendUserBackToSearch();
        }
    };

    //The onReceive method is fired when an nfc tag is scanned
    private BroadcastReceiver nfcScannedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Check again if the passenger has the correct status for an nfc scan
            SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(Constants.SHARED_PREF_KEY_WAITING, false) && !prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
                updateTrip(JoinTripRequestUpdateType.ENTER_CAR, progressBarDest);

                //disable listening to the "scanned NFC" event
                LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(nfcScannedReceiver);
            }
        }
    };

    private BroadcastReceiver secondaryDriverAcceptedDeclinedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("A secondary driver has accepted or declined");
            loadRequest();
        }
    };


    //*************************** Inner classes ********************************//

    /**
     * This Subscriber loads the current JoinTripRequest (SuperTripRequest)
     */
    private class LoadRequestSubscriber extends Subscriber<List<SuperTrip>> {

        @Override
        public void onNext(final List<SuperTrip> trips) {

            if (trips == null || trips.isEmpty()) {
                Timber.d("Currently there are no trips running.");
                return;
            }

            subscriptions.add(tripsResource
                    .getJoinTripRequestsForSuperTrip(trips.get(0).getId())
                    .subscribe(new Action1<List<JoinTripRequest>>() {
                        @Override
                        public void call(final List<JoinTripRequest> joinTripRequests) {

                            Timber.d("Got List of JoinTripRequests");

                            //Update the view with the received data
                            //if (isAdded()) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showJoinedTrip(joinTripRequests);
                                }
                            });
                            //}

                        }
                    }, new CrashCallback(getActivity(), "failed to get join requests")));
        }


        @Override
        public void onCompleted() {}

        @Override
        public void onError(Throwable throwable) {
            progressBarDrivers.setVisibility(View.GONE);
            Timber.e(throwable.getMessage());
            Toast.makeText(getActivity(), getString(R.string.join_trip_results_error), Toast.LENGTH_LONG).show();
        }
    }
}
