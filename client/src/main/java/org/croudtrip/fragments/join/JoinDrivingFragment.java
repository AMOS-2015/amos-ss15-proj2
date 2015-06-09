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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripRequestUpdateType;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.utils.DefaultTransformer;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.inject.InjectView;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class JoinDrivingFragment extends SubscriptionFragment implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    @InjectView(R.id.btn_joint_trip_reached)        private Button btnReachedDestination; //also handles the "My driver is here" stuff
    @InjectView(R.id.btn_joint_trip_cancel)         private Button btnCancelTrip;
    @InjectView(R.id.btn_joint_trip_report)         private Button btnReportDriver;

    @InjectView(R.id.join_trip_sending)         private LinearLayout llSending;
    @InjectView(R.id.join_trip_waiting)         private LinearLayout llWaiting;
    @InjectView(R.id.join_trip_driving)         private LinearLayout llDriving;

    @InjectView(R.id.my_driver)                 private View cvDriver;

    @InjectView(R.id.pickup_time)               private TextView tvPickupTime;
    @InjectView(R.id.card_name)                 private TextView tvCardName;
    @InjectView(R.id.card_car)                  private TextView tvCardCar;
    @InjectView(R.id.card_price)                private TextView tvCardPrice;
    @InjectView(R.id.card_icon)                 private ImageView ivCardIcon;




    @Inject TripsResource tripsResource;

    private JoinTripRequest cachedRequest;
    private ArrayList<JoinTripRequestUpdateType> simpleRequestUpdateCache;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Simple cache to store failed requests
        simpleRequestUpdateCache = new ArrayList<>();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(joinRequestExpiredReceiver,
                new IntentFilter(Constants.EVENT_JOIN_REQUEST_EXPIRED));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_my_trip));


        View view = inflater.inflate(R.layout.fragment_join_driving, container, false);
        return view;
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);

        llSending.setVisibility(View.GONE);
        llWaiting.setVisibility(View.GONE);
        llDriving.setVisibility(View.GONE);
        cvDriver.setVisibility(View.GONE);

        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_WAITING, false)) {
            //passenger is currently waiting for the drivers approval

            setButtonInactive(btnReportDriver);
            setButtonInactive(btnReachedDestination);
            setButtonActive(btnCancelTrip);

            llSending.setVisibility(View.VISIBLE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_driverArrival));


        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
            //passenger is currently on a trip already in the car

            setButtonInactive(btnCancelTrip);
            setButtonActive(btnReachedDestination);
            setButtonActive(btnReportDriver);

            cvDriver.setVisibility(View.VISIBLE);
            llDriving.setVisibility(View.VISIBLE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_reached));
            btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Handle here all the stuff that happens when the trip is successfully completed (user hits "I have reached my destination")
                    updateTrip(JoinTripRequestUpdateType.LEAVE_CAR);
                    sendUserBackToSearch();
                }
            });
        } else {
            //passenger is currently waiting for his driver but already accepted

            setButtonActive(btnCancelTrip);
            setButtonActive(btnReachedDestination);
            setButtonActive(btnReportDriver);

            cvDriver.setVisibility(View.VISIBLE);
            llWaiting.setVisibility(View.VISIBLE);

            btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_driverArrival));
            btnReachedDestination.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Handle here all the stuff that happens when the user enters the car (user hits "My driver is here")

                    if (prefs.getBoolean(Constants.SHARED_PREF_KEY_DRIVING, false)) {
                        updateTrip(JoinTripRequestUpdateType.LEAVE_CAR);
                        sendUserBackToSearch();
                    } else {
                        // If the user enters the car and leaves the car without this method called twice we need this distinction
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(Constants.SHARED_PREF_KEY_DRIVING, true);
                        editor.apply();
                        btnReachedDestination.setText(getResources().getString(R.string.join_trip_results_reached));
                        setButtonInactive(btnCancelTrip);

                        llWaiting.setVisibility(View.GONE);
                        llDriving.setVisibility(View.VISIBLE);

                        updateTrip(JoinTripRequestUpdateType.ENTER_CAR);
                        showJoinedTrip(cachedRequest);
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
                updateTrip(JoinTripRequestUpdateType.CANCEL);
                sendUserBackToSearch();
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
                Timber.e("Could not parse JoinTripRequest");
                e.printStackTrace();
            }
            showJoinedTrip(request);
            cachedRequest = request;
        } else {
            tripsResource.getJoinRequests(false)
                    .compose(new DefaultTransformer<List<JoinTripRequest>>())
                    .subscribe( new Action1<List<JoinTripRequest>>() {
                        @Override
                        public void call(List<JoinTripRequest> jtr) {
                            if(jtr == null || jtr.isEmpty()) {
                                Timber.d("Currently there are no trips running.");
                                return;
                            }

                            //Update the view with the received data
                            if (isAdded()) {
                                showJoinedTrip(jtr.get(0));
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.e(throwable.getMessage());
                        }
                    });
        }
    }

    /*
    Parse and show information about the current trip, like price, driver and cost
     */
    private void showJoinedTrip(JoinTripRequest request) {
        if( request != null ) {
            int earningsInCents = request.getTotalPriceInCents();
            String pEuros = (earningsInCents / 100) + "";
            String pCents;

            // Format cents correctly
            int cents = (earningsInCents % 100);

            if (cents == 0) {
                pCents = "00";
            } else if (cents < 10) {
                pCents = "0" + cents;
            } else {
                pCents = cents + "";
            }

            String avatarURL = request.getOffer().getDriver().getAvatarUrl();
            if (avatarURL != null) {
                try {
                    new URL(avatarURL);
                    Picasso.with(getActivity()).load(avatarURL).into(ivCardIcon);
                } catch (MalformedURLException e) {
                    ivCardIcon.setImageResource(R.drawable.profile);
                }
            }

            String dateAsString = "";
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getDefault());
            calendar.setTimeInMillis(1000*request.getEstimatedArrivalTimestamp());
            dateAsString = calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE);


            tvPickupTime.setText(dateAsString);
            tvCardName.setText(request.getOffer().getDriver().getFirstName() + " " + request.getOffer().getDriver().getLastName());
            tvCardCar.setText(request.getOffer().getVehicle().getType());
            tvCardPrice.setText(getString(R.string.join_trip_results_price, pEuros, pCents));
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
    private void updateTrip(final JoinTripRequestUpdateType updateType) {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        JoinTripRequestUpdate requestUpdate= new JoinTripRequestUpdate(updateType);
        Subscription subscription = tripsResource.updateJoinRequest(prefs.getLong(Constants.SHARED_PREF_KEY_TRIP_ID, -1), requestUpdate)
                .compose(new DefaultTransformer<JoinTripRequest>())
                .subscribe(new Subscriber<JoinTripRequest>() {

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.error_contacting_server), Toast.LENGTH_SHORT).show();
                        Timber.e(e.getMessage());

                        /*
                        Add this JoinTripRequestUpdateType to a simple cache to try it again some other time
                         */
                        simpleRequestUpdateCache.add(updateType);
                    }

                    @Override
                    public void onNext(JoinTripRequest joinTripRequest) {

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .unregisterReceiver(joinRequestExpiredReceiver);

        /*
        Try to resend every failed server call. This will be tried only once.
         */
        for (Iterator<JoinTripRequestUpdateType> iterator = simpleRequestUpdateCache.iterator(); iterator.hasNext();) {
            updateTrip(iterator.next());
            iterator.remove();
        }
    }



    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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
}
