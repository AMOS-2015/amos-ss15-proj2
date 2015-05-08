package org.croudtrip.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.trip.JoinTripRequestsAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import roboguice.inject.InjectView;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Shows the driver a list of passengers who want to join the trip.
 * @author Vanessa Lange
 */
public class JoinTripRequestsFragment extends SubscriptionFragment{

    //************************* Variables ****************************//

    @InjectView(R.id.tv_join_trip_requests_caption) private TextView caption;
    @InjectView(R.id.rv_join_trip_requests)         private RecyclerView recyclerView;
    @InjectView(R.id.pb_join_trip_requests)         private ProgressBar progressBar;
    @InjectView(R.id.tv_join_trip_requests_error)   private TextView error;

    @Inject private TripsResource tripsResource;

    private RecyclerView.LayoutManager layoutManager;
    private JoinTripRequestsAdapter adapter;


    //************************* Methods ****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_join_trip_requests, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use a linear layout manager to use the RecyclerView
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new JoinTripRequestsAdapter(null);
        recyclerView.setAdapter(adapter);

        // Ask the server for join-trip-requests
        Subscription subscription = tripsResource.getJoinRequests(0 /* TODO: Offer ID */)
                .compose(new DefaultTransformer<List<JoinTripRequest>>()).subscribe(

                        new Action1<List<JoinTripRequest>>() {

                            @Override
                            public void call(List<JoinTripRequest> joinTripRequests) {
                                // UI:
                                progressBar.setVisibility(View.GONE);

                                // Fill the list with results
                                int numRequests = joinTripRequests.size();
                                adapter.addElements(joinTripRequests);

                                // Show a summary caption
                                caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                                        numRequests, numRequests));
                            }

                        }, new Action1<Throwable>() {

                            @Override
                            public void call(Throwable throwable) {
                                // ERROR
                                // UI:
                                progressBar.setVisibility(View.GONE);
                                error.setVisibility(View.VISIBLE);

                                caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                                        0, 0));

                                Timber.e("Receiving JoinTripRequests failed:\n" + throwable.getMessage());
                            }
                        }
                );
        subscriptions.add(subscription);
    }
}
