package org.croudtrip.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.TripsResource;
import org.croudtrip.directions.RouteLocation;
import org.croudtrip.trip.JoinTripResultsAdapter;
import org.croudtrip.trips.TripMatch;
import org.croudtrip.trips.TripRequestDescription;

import java.util.List;

import javax.inject.Inject;

import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * This Activity shows the results for a join request. It displays drivers, their costs and several
 * other information,should the user want to join.
 * Created by Vanessa Lange on 01.05.15.
 */
@ContentView(R.layout.activity_join_trip_results)
public class JoinTripResultsActivity extends RoboActivity {

    //******************** Variables ************************//

    public final static String KEY_CURRENT_LOCATION_LATITUDE = "current_location_latitude";
    public final static String KEY_CURRENT_LOCATION_LONGITUDE = "current_location_longitude";
    public final static String KEY_DESTINATION_LATITUDE = "destination_latitude";
    public final static String KEY_DESTINATION_LONGITUDE = "destination_longitude";

    @InjectView(R.id.pb_join_trip)                  private ProgressBar progressBar;
    @InjectView(R.id.tv_join_trip_results_caption)  private TextView caption;
    @InjectView(R.id.lv_join_trip_results)          private ListView resultsList;
    @InjectView(R.id.tv_join_trip_error)            private TextView error;


    @Inject
    TripsResource tripsResource;


    //******************** Methods ************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get currentLocation and destination
        Bundle extras = getIntent().getExtras();
        double currentLocationLat = extras.getDouble(KEY_CURRENT_LOCATION_LATITUDE);
        double currentLocationLon = extras.getDouble(KEY_CURRENT_LOCATION_LONGITUDE);
        double destinationLat = extras.getDouble(KEY_DESTINATION_LATITUDE);
        double destinationLon = extras.getDouble(KEY_DESTINATION_LONGITUDE);

        // Ask the server for matches
        TripRequestDescription tripRequestDescription = new TripRequestDescription(
                new RouteLocation(currentLocationLat, currentLocationLon),
                new RouteLocation(destinationLat, destinationLon));

        tripsResource.findMatches(tripRequestDescription).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<TripMatch>>() {

                    @Override
                    public void call(List<TripMatch> tripMatches) {

                        // Update the caption text
                        int numMatches = tripMatches.size();
                        caption.setText(getResources().getQuantityString(R.plurals.join_trip_results,
                                numMatches, numMatches));

                        // Fill the results list
                        JoinTripResultsAdapter adapter = new JoinTripResultsAdapter(
                                JoinTripResultsActivity.this,
                                R.layout.listview_row_join_trip_results, tripMatches);

                        resultsList.setAdapter(adapter);
                        progressBar.setVisibility(View.GONE);

                    }

                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // on main thread; something went wrong
                        Timber.e(throwable.getMessage());
                        progressBar.setVisibility(View.GONE);

                        caption.setText(getResources().getQuantityString(R.plurals.join_trip_results,
                                0, 0));
                        error.setVisibility(View.VISIBLE);
                    }
                });

    }

}
