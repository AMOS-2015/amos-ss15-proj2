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
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.trip.JoinTripRequestsAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.List;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Shows the driver a list of passengers who want to join the trip. The driver can then
 * accept or decline such a JoinTripRequest.
 * @author Vanessa Lange
 */
public class JoinTripRequestsFragment extends SubscriptionFragment{

    //************************* Variables ****************************//

    @InjectView(R.id.tv_join_trip_requests_caption) private TextView caption;
    @InjectView(R.id.rv_join_trip_requests)         private RecyclerView recyclerView;
    @InjectView(R.id.pb_join_trip_requests)         private ProgressBar progressBar;
    @InjectView(R.id.tv_join_trip_requests_error)   private TextView error;

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

        final TripsResource tripsResource = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.server_address))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        AccountManager.addAuthorizationHeader(getActivity(), request);
                    }
                })
                .build()
                .create(TripsResource.class);

        // Ask the server for join-trip-requests
        Subscription subscription = tripsResource.getOffers()
                .compose(new DefaultTransformer<List<TripOffer>>())
                .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {
                    @Override
                    public Observable<TripOffer> call(List<TripOffer> tripOffers) {
                        Timber.i("Received " + tripOffers.size() + "list(s) of TripOffers");
                        return Observable.from(tripOffers);
                    }
                })
                .flatMap(new Func1<TripOffer, Observable<List<JoinTripRequest>>>() {
                    @Override
                    public Observable<List<JoinTripRequest>> call(TripOffer tripOffer) {
                        Timber.i("Received TripOffer with ID: " + tripOffer.getId());

                        // Get the JoinTripRequests for this TripOffer
                        return tripsResource.getJoinRequests(tripOffer.getId(), true);
                    }
                })
                .toList()
                .compose(new DefaultTransformer<List<List<JoinTripRequest>>>())
                .subscribe(new Action1<List<List<JoinTripRequest>>>() {
                    // SUCCESS

                    @Override
                    public void call(List<List<JoinTripRequest>> joinTripRequests) {

                        int numRequests = adapter.getItemCount();

                        for(List<JoinTripRequest> requests : joinTripRequests){
                            // Fill the list with results
                            numRequests += requests.size();
                            adapter.addElements(requests);
                        }

                        // Show a summary caption
                        caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                                numRequests, numRequests));
                    }

                }, new Action1<Throwable>() {
                    // ERROR

                    @Override
                    public void call(Throwable throwable) {

                        error.setVisibility(View.VISIBLE);
                        caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                                0, 0));

                        Timber.e("Receiving JoinTripRequests failed:\n" + throwable.getMessage() +
                                "\n" + throwable.fillInStackTrace());
                    }
                }, new Action0(){
                    // DONE

                    @Override
                    public void call() {
                        // UI:
                        progressBar.setVisibility(View.GONE);
                    }
                });

        subscriptions.add(subscription);
    }
}
