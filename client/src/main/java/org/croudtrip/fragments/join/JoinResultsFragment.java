package org.croudtrip.fragments.join;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.trip.JoinTripResultsAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by alex on 22.04.15.
 */
public class JoinResultsFragment extends SubscriptionFragment implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    @InjectView(R.id.layout_join_trip_results)      private View resultView;
    @InjectView(R.id.layout_join_trip_waiting)      private View waitingView;
    @InjectView(R.id.btn_joint_trip_stop)           private Button btnStop;
    @InjectView(R.id.tv_join_trip_results_caption)  private TextView caption;
    @InjectView(R.id.rv_join_trip_results)          private RecyclerView recyclerView;

    @Inject TripsResource tripsResource;

    private RecyclerView.LayoutManager layoutManager;
    private JoinTripResultsAdapter adapter;

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

        View view = inflater.inflate(R.layout.fragment_join_results, container, false);

        return view;
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new JoinTripResultsAdapter(getActivity(), null);
        recyclerView.setAdapter(adapter);

        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("...");
        waitingView.setVisibility(View.VISIBLE);

        /*
        Start background search
         */
        if (getArguments() != null) {
            startBackgroundSearch(getArguments());
        }

        /*
        Stop background search
         */
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);


                Toast.makeText(getActivity().getApplicationContext(), R.string.join_trip_results_canceled, Toast.LENGTH_LONG);

                //tell the server to stop the background search
                if (prefs.getLong(Constants.SHARED_PREF_KEY_QUERY_ID, -1) != -1) {
                    tripsResource.deleteQuery(prefs.getLong(Constants.SHARED_PREF_KEY_QUERY_ID, -1), new Callback<Response>() {
                        @Override
                        public void success(Response response, Response response2) {
                            //yeay
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            //try again?
                        }
                    });
                    editor.putLong(Constants.SHARED_PREF_KEY_QUERY_ID, -1);
                }

                editor.apply();

                Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(startingIntent);
            }
        });

        // On click of a reservation we request to join this trip.
        adapter.setOnItemClickListener(new JoinTripResultsAdapter.OnItemClickListener() {

            @Override
            public void onItemClicked(View view, int position) {
                TripReservation reservation = adapter.getItem(position);
                Timber.d("Clicked on reservation " + reservation.getId());

                Subscription subscription = tripsResource.joinTrip(reservation.getId())
                        .compose(new DefaultTransformer<JoinTripRequest>())
                        .subscribe(new Action1<JoinTripRequest>() {
                            // SUCCESS

                            @Override
                            public void call(JoinTripRequest joinTripRequest) {
                                Toast.makeText(getActivity(), R.string.join_request_sent,
                                        Toast.LENGTH_SHORT).show();

                                // TODO: Start a new view? Not clear in which state the passenger should wait.
                            }

                        }, new Action1<Throwable>() {
                            // ERROR

                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Error when trying to join a trip: " + throwable.getMessage());
                                Toast.makeText(getActivity(), R.string.join_request_sending_error, Toast.LENGTH_SHORT).show();

                                // TODO: Refresh this fragment. Current reservation could already
                                // have been removed on the server (we don't know when the error happened).

                            }
                        });
                subscriptions.add(subscription);
            }
        });
    }

    private void startBackgroundSearch(Bundle bundle) {
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        double currentLocationLat = bundle.getDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LATITUDE);
        double currentLocationLon = bundle.getDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LONGITUDE);
        double destinationLat = bundle.getDouble(JoinDispatchFragment.KEY_DESTINATION_LATITUDE);
        double destinationLon = bundle.getDouble(JoinDispatchFragment.KEY_DESTINATION_LONGITUDE);
        int maxWaitingTime = bundle.getInt(JoinDispatchFragment.KEY_MAX_WAITING_TIME);

        // Ask the server for matches
        TripQueryDescription tripQueryDescription = new TripQueryDescription(
                new RouteLocation(currentLocationLat, currentLocationLon),
                new RouteLocation(destinationLat, destinationLon), maxWaitingTime);

        Subscription subscription = tripsResource.queryOffers(tripQueryDescription)
                .compose(new DefaultTransformer<TripQueryResult>())
                .subscribe(new Action1<TripQueryResult>() {
                    // SUCCESS


                    @Override
                    public void call(TripQueryResult result) {

                        List<TripReservation> reservations = result.getReservations();

                        // Update the caption text
                        int numMatches = reservations.size();
                        if (numMatches != 0) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
                            editor.apply();

                            //Set the notificationText in the navigationDrawer
                            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotifications(numMatches);

                            //Switch out the view
                            waitingView.setVisibility(View.GONE);
                            resultView.setVisibility(View.VISIBLE);

                            caption.setText(getResources().getQuantityString(R.plurals.join_trip_results,
                                    numMatches, numMatches));

                            // Fill the results list
                            adapter.addElements(reservations);

                            if (!(AccountManager.isUserLoggedIn(getActivity()))) {
                                recyclerView.setBackgroundColor(Color.GRAY);
                                drawRegisterDialog();
                            }
                        } else if (result.getRunningQuery() != null) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putLong(Constants.SHARED_PREF_KEY_QUERY_ID, result.getRunningQuery().getId());
                            editor.apply();
                        }
                    }

                }, new Action1<Throwable>() {
                    // ERROR

                    @Override
                    public void call(Throwable throwable) {
                        Timber.e("Error when trying to join a trip: " + throwable.getMessage());

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
                        editor.apply();

                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error), Toast.LENGTH_LONG).show();

                        Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(startingIntent);
                    }
                }, new Action0() {
                    // DONE

                    @Override
                    public void call() {
                        //progressBar.setVisibility(View.GONE);
                    }
                });

        subscriptions.add(subscription);
    }

    private void drawRegisterDialog() {
        final Dialog registerDialog = new Dialog(getActivity());
        registerDialog.setTitle("Register");
        registerDialog.setContentView(R.layout.ask_to_register_dialog);
        Button set = (Button) registerDialog.findViewById(R.id.register);
        Button cancel = (Button) registerDialog.findViewById(R.id.cancel);
        registerDialog.show();

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Register view will be shown", Toast.LENGTH_SHORT).show();
                registerDialog.hide();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerDialog.hide();
            }
        });

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
