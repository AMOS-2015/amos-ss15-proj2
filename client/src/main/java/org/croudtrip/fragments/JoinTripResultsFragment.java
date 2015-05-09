package org.croudtrip.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.trip.JoinTripResultsAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
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

    @InjectView(R.id.pb_join_trip)                  private ProgressBar progressBar;
    @InjectView(R.id.tv_join_trip_results_caption)  private TextView caption;
    @InjectView(R.id.rv_join_trip_results)          private RecyclerView recyclerView;
    @InjectView(R.id.tv_join_trip_error)            private TextView error;

    @Inject TripsResource tripsResource;

    private RecyclerView.LayoutManager layoutManager;
    private JoinTripResultsAdapter adapter;
    


    //******************** Methods ************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
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


        // Get currentLocation and destination
        Bundle extras = getArguments();
        final SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        //If extras is null this Fragment was called by the NavigationDrawer. This means the server is already searching for trips
        if (extras == null || prefs.getBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false)) {
            return;
        }


        double currentLocationLat = extras.getDouble(KEY_CURRENT_LOCATION_LATITUDE);
        double currentLocationLon = extras.getDouble(KEY_CURRENT_LOCATION_LONGITUDE);
        double destinationLat = extras.getDouble(KEY_DESTINATION_LATITUDE);
        double destinationLon = extras.getDouble(KEY_DESTINATION_LONGITUDE);
        long maxWaitingTime = 1000; /* TODO: Get this from passengeres choice*/

        // Ask the server for matches
        TripQueryDescription tripQueryDescription = new TripQueryDescription(
                new RouteLocation(currentLocationLat, currentLocationLon),
                new RouteLocation(destinationLat, destinationLon), maxWaitingTime);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
        editor.apply();

        Subscription subscription = tripsResource.createReservations(tripQueryDescription)
                .compose(new DefaultTransformer<List<TripReservation>>())
                .subscribe(new Action1<List<TripReservation>>() {
                    // SUCCESS

                    @Override
                    public void call(List<TripReservation> reservations) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
                        editor.apply();

                        // Update the caption text
                        int numMatches = reservations.size();
                        caption.setText(getResources().getQuantityString(R.plurals.join_trip_results,
                                numMatches, numMatches));

                        // Fill the results list
                        adapter.addElements(reservations);

                        if (!(AccountManager.isUserLoggedIn(getActivity()))) {
                            recyclerView.setBackgroundColor(Color.GRAY);
                            drawRegisterDialog();
                        }
                    }

                }, new Action1<Throwable>() {
                    // ERROR

                    @Override
                    public void call(Throwable throwable) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
                        editor.apply();

                        // on main thread; something went wrong
                        Timber.e("Error when trying to join a trip: " + throwable.getMessage());
                        error.setVisibility(View.VISIBLE);
                        caption.setText(getResources().getQuantityString(R.plurals.join_trip_results,
                                0, 0));
                    }
                }, new Action0() {
                    // DONE

                    @Override
                    public void call() {
                        progressBar.setVisibility(View.GONE);
                    }
                });

        subscriptions.add(subscription);
    }

    public void drawRegisterDialog() {
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
