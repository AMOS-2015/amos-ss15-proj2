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

import android.content.Context;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

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
import java.util.List;

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



    @Inject TripsResource tripsResource;

    private JoinTripRequest cachedRequest;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                updateTrip(JoinTripRequestUpdateType.CANCEL);
                sendUserBackToSearch();
            }
        });

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
                            if( jtr == null || jtr.isEmpty() ) {
                                Timber.d("Currently there are no trips running.");
                                // The user is here though he should not - send him back to join trip
                                //sendUserBackToSearch();
                                return;
                            }

                            if (isAdded()) {
                                showJoinedTrip( jtr.get(0) );
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            // The user is here though he should not - send him back to join trip
                            //sendUserBackToSearch();
                        }
                    });
        }
    }

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

            tvPickupTime.setText(request.getOffer().getEstimatedArrivalTimeInSeconds() + "");
            tvCardName.setText(request.getOffer().getDriver().getFirstName() + " " + request.getOffer().getDriver().getLastName());
            tvCardCar.setText(request.getOffer().getVehicle().getType());
            tvCardPrice.setText(getString(R.string.join_trip_results_price, pEuros, pCents));
            //jointDescription.setText( getString(R.string.join_trip_results_pickup, request.getOffer().getDriver().getFirstName(), pEuros, pCents));
        }
    }

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

    private void updateTrip(JoinTripRequestUpdateType updateType) {
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

                    }

                    @Override
                    public void onNext(JoinTripRequest joinTripRequest) {

                    }
                });

        subscriptions.add(subscription);
    }

    private void setButtonActive(Button button) {
        button.setClickable(true);
        button.setBackgroundColor(getResources().getColor(R.color.primary));
    }

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
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
