package org.croudtrip.fragments.join;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.utils.DefaultTransformer;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class JoinDrivingFragment extends SubscriptionFragment implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    @InjectView(R.id.btn_joint_trip_cancel)         private Button btnCancelTrip;
    @InjectView(R.id.tv_joint_description)          private TextView jointDescription;

    @Inject TripsResource tripsResource;


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

        jointDescription.setText("");

        /*
        Cancel trip
         */
        btnCancelTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        } else {
            tripsResource.getDriverAcceptedJoinRequests()
                    .compose(new DefaultTransformer<List<JoinTripRequest>>())
                    .subscribe( new Action1<List<JoinTripRequest>>() {
                        @Override
                        public void call(List<JoinTripRequest> jtr) {
                            if( jtr == null || jtr.isEmpty() ) {
                                Timber.d("Currently there are no trips running.");
                                // The user is here though he should not - send him back to join trip
                                sendUserBackToSearch();
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
                            sendUserBackToSearch();
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
            jointDescription.setText( getString(R.string.join_trip_results_pickup, request.getOffer().getDriver().getFirstName(), pEuros, pCents));
        }
    }

    private void sendUserBackToSearch() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false);
        editor.apply();

        Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(startingIntent);
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