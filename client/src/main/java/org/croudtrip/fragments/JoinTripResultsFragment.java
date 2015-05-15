package org.croudtrip.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
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
 * This Fragment shows the results for a join request. It displays drivers, their costs and several
 * other information,should the user want to join.
 * Created by Vanessa Lange on 01.05.15.
 */
public class JoinTripResultsFragment extends SubscriptionFragment {

    //******************** Variables ************************//

    public final static String KEY_CURRENT_LOCATION_LATITUDE = "current_location_latitude";
    public final static String KEY_CURRENT_LOCATION_LONGITUDE = "current_location_longitude";
    public final static String KEY_DESTINATION_LATITUDE = "destination_latitude";
    public final static String KEY_DESTINATION_LONGITUDE = "destination_longitude";
    public final static String KEY_MAX_WAITING_TIME = "max_waiting_time";


    //@InjectView(R.id.pb_join_trip)                  private ProgressBar progressBar;
    @InjectView(R.id.tv_join_trip_results_caption)  private TextView caption;
    @InjectView(R.id.rv_join_trip_results)          private RecyclerView recyclerView;
    @InjectView(R.id.tv_join_trip_error)            private TextView error;
    @InjectView(R.id.layout_join_trip_results)      private View resultView;
    @InjectView(R.id.layout_join_trip_waiting)      private View waitingView;
    @InjectView(R.id.layout_join_trip_accepted)     private View acceptedView;
    @InjectView(R.id.btn_joint_trip_stop)           private Button btnStop;
    @InjectView(R.id.btn_joint_trip_cancel)         private Button btnCancelTrip;





    @Inject TripsResource tripsResource;

    private RecyclerView.LayoutManager layoutManager;
    private JoinTripResultsAdapter adapter;
    


    //******************** Methods ************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_join_trip_results, container, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use a linear layout manager to use the RecyclerView
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new JoinTripResultsAdapter(getActivity(), null);
        recyclerView.setAdapter(adapter);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);


                ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTarget(new JoinTripFragment());
                ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.join_trip));
                ((MaterialNavigationDrawer) getActivity()).setFragment(new JoinTripFragment(), getString(R.string.join_trip));

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
            }
        });

        btnCancelTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false);
                editor.apply();

                ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTarget(new JoinTripFragment());
                ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.join_trip));
                ((MaterialNavigationDrawer) getActivity()).setFragment(new JoinTripFragment(), getString(R.string.join_trip));
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


        //If getArguments() is null this Fragment was called by the NavigationDrawer. This means the server is already searching for trips
        if (getArguments() != null) {
            startBackgroundSearch(getArguments());
        }

        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false)) {
            waitingView.setVisibility(View.VISIBLE);
            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("...");
        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false)) {
            acceptedView.setVisibility(View.VISIBLE);
            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        } else {
            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        }
    }

    private void startBackgroundSearch(Bundle bundle) {
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        double currentLocationLat = bundle.getDouble(KEY_CURRENT_LOCATION_LATITUDE);
        double currentLocationLon = bundle.getDouble(KEY_CURRENT_LOCATION_LONGITUDE);
        double destinationLat = bundle.getDouble(KEY_DESTINATION_LATITUDE);
        double destinationLon = bundle.getDouble(KEY_DESTINATION_LONGITUDE);
        long maxWaitingTime = 1000; /* TODO: Get this from passengeres choice*/

        // Ask the server for matches
        TripQueryDescription tripQueryDescription = new TripQueryDescription(
                new RouteLocation(currentLocationLat, currentLocationLon),
                new RouteLocation(destinationLat, destinationLon), maxWaitingTime);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
        editor.apply();


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

                            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotifications(numMatches);

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

                            //TODO: if potential drivers are found but the user navigates to another fragment and back the rides get dismissed
                            //TODO: should we save them? no user story -> ask PO
                            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTarget(new JoinTripFragment());
                            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.join_trip));
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
                        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");


                        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTarget(new JoinTripFragment());
                        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_join_trip));
                        ((MaterialNavigationDrawer) getActivity()).setFragment(new JoinTripFragment(), getString(R.string.join_trip));

                        Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error), Toast.LENGTH_LONG).show();
                        // on main thread; something went wrong
                        //error.setVisibility(View.VISIBLE);
                        //caption.setText(getResources().getQuantityString(R.plurals.join_trip_results, 0, 0));
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

}
