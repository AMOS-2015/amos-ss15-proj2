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
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.SupportMapFragment;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripRequestUpdateType;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.trip.MyTripPassengerDriversAdapter;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.CrashPopup;
import org.croudtrip.utils.DefaultTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.inject.InjectView;
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
    @InjectView(R.id.pb_my_trip_map_progressBar)
    private ProgressWheel progressBarMap;
    @InjectView(R.id.pb_my_trip_drivers_progressBar)
    private ProgressWheel progressBarDrivers;

    @InjectView(R.id.iv_transparent_image)
    private ImageView transparentImageView;

    @Inject
    TripsResource tripsResource;

    private JoinTripRequest cachedRequest;
    private ArrayList<JoinTripRequestUpdateType> simpleRequestUpdateCache;

    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;

    // Passengers list
    private MyTripPassengerDriversAdapter adapter;

    @InjectView(R.id.rv_join_trip_driving_drivers)
    private RecyclerView recyclerView;



    //***************************** Methods *****************************//


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Simple cache to store failed requests
        simpleRequestUpdateCache = new ArrayList<>();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(joinRequestExpiredReceiver,
                new IntentFilter(Constants.EVENT_JOIN_REQUEST_EXPIRED));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(nfcScannedReceiver,
                new IntentFilter(Constants.EVENT_NFC_TAG_SCANNED));

        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter != null) {
            nfcPendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        }

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

        // TODO: do things with the map here or down further


        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_WAITING, false)) {
            //passenger is currently waiting for the drivers approval

            setButtonInactive(btnReportDriver);
            setButtonInactive(btnReachedDestination);
            setButtonActive(btnCancelTrip);

            llSending.setVisibility(View.VISIBLE);
            flMap.setVisibility(View.GONE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_driverArrival));


        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
            //passenger is currently on a trip already in the car

            setButtonInactive(btnCancelTrip);
            setButtonActive(btnReachedDestination);
            setButtonActive(btnReportDriver);

            llDriving.setVisibility(View.VISIBLE);
            flMap.setVisibility(View.VISIBLE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_reached));
            btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Handle here all the stuff that happens when the trip is successfully completed (user hits "I have reached my destination")
                    updateTrip(JoinTripRequestUpdateType.LEAVE_CAR, progressBarDest);
                    sendUserBackToSearch();
                }
            });
        } else {
            //passenger is currently waiting for his driver but already accepted

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
                        sendUserBackToSearch();
                    } else {
                        // If the user enters the car and leaves the car without this method called twice we need this distinction
                        enterCar();
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
                updateTrip(JoinTripRequestUpdateType.CANCEL, progressBarCancel);
            }
        });


        /*
        Get the current request either from the arguments (trip got downloaded somewhere else, so we dont have to
        do it again here)´or directly from the server
         */
        if (getArguments() != null) {
            JoinTripRequest request = null;
            ObjectMapper mapper = new ObjectMapper();
            try {
                request = mapper.readValue(getArguments().getString(JoinDispatchFragment.KEY_JOIN_TRIP_REQUEST_RESULT), JoinTripRequest.class);
            } catch (IOException e) {
                CrashPopup.show(getActivity(), e);
                Timber.e("Could not parse JoinTripRequest");
                e.printStackTrace();
            }
            showJoinedTrip(request);
            cachedRequest = request;
        } else {
            tripsResource.getJoinRequests(false)
                    .compose(new DefaultTransformer<List<JoinTripRequest>>())
                    .subscribe(new Action1<List<JoinTripRequest>>() {
                        @Override
                        public void call(List<JoinTripRequest> jtr) {
                            if (jtr == null || jtr.isEmpty()) {
                                Timber.d("Currently there are no trips running.");
                                return;
                            }

                            //Update the view with the received data
                            if (isAdded()) {
                                showJoinedTrip(jtr.get(0));
                            }
                        }
                    }, new CrashCallback(getActivity()) {
                        @Override
                        public void call(Throwable throwable) {
                            super.call(throwable);
                            Timber.e(throwable.getMessage());
                        }
                    });
        }
    }

    /*
    Parse and show information about the current trip, like price, driver and cost
     */
    private void showJoinedTrip(JoinTripRequest request) {

        if (request == null) {
            return;
        }

        progressBarDrivers.setVisibility(View.GONE);

        // Show drivers
        adapter.updateRequest(request);

        // Show correct plural of drivers
        int numDrivers = adapter.getNumDrivers();
        Resources res = getResources();
        tvMyDrivers.setText(res.getQuantityString(R.plurals.join_trip_results_my_drivers, numDrivers, numDrivers));

        // Show arrival time
        String dateAsString = "";
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(1000 * (request.getEstimatedArrivalTimestamp()-18540));

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

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        JoinTripRequestUpdate requestUpdate = new JoinTripRequestUpdate(updateType);
        Subscription subscription = tripsResource.updateJoinRequest(prefs.getLong(Constants.SHARED_PREF_KEY_TRIP_ID, -1), requestUpdate)
                .compose(new DefaultTransformer<JoinTripRequest>())
                .subscribe(new Action1<JoinTripRequest>() {
                    @Override
                    public void call(JoinTripRequest joinTripRequest) {
                        Timber.d("update trip successfully called");

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        if (updateType.equals(JoinTripRequestUpdateType.CANCEL)) {
                            sendUserBackToSearch();
                        }
                    }
                }, new CrashCallback(getActivity()) {

                    @Override
                    public void call(Throwable throwable) {
                        super.call(throwable);

                        Timber.e(throwable.getMessage());

                        /*
                        Add this JoinTripRequestUpdateType to a simple cache to try it again some other time
                        */
                        if (simpleRequestUpdateCache != null) {
                            simpleRequestUpdateCache.add(updateType);
                        }

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                });

        subscriptions.add(subscription);
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


    /*
    The passenger enters the car. Show the correct UI, save the status and send a notification to the server
     */
    private void enterCar() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_DRIVING, true);
        editor.apply();
        updateTrip(JoinTripRequestUpdateType.ENTER_CAR, progressBarDest);

        ivNfcIcon.setVisibility(View.GONE);
        tvNfcExplanation.setVisibility(View.GONE);

        btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_reached));
        setButtonInactive(btnCancelTrip);

        llWaiting.setVisibility(View.GONE);
        llDriving.setVisibility(View.VISIBLE);
        flMap.setVisibility(View.VISIBLE);
        btnReachedDestination.setVisibility(View.VISIBLE);

        showJoinedTrip(cachedRequest);
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

        /*
        Try to resend every failed server call. This will be tried only once.
         */
        for (Iterator<JoinTripRequestUpdateType> iterator = simpleRequestUpdateCache.iterator(); iterator.hasNext(); ) {
            updateTrip(iterator.next(), null);
            iterator.remove();
        }
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
                enterCar();

                //disable listening to the "scanned NFC" event
                LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(nfcScannedReceiver);
            }
        }
    };
}
