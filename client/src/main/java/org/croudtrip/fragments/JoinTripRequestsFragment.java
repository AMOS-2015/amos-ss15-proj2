package org.croudtrip.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.trip.JoinTripRequestsAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.List;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Shows the driver a list of passengers who want to join the trip. The driver can then
 * accept or decline such a JoinTripRequest.
 *
 * @author Vanessa Lange
 */
public class JoinTripRequestsFragment extends SubscriptionFragment {

    //************************* Variables ****************************//

    @InjectView(R.id.tv_join_trip_requests_caption)
    private TextView caption;
    @InjectView(R.id.rv_join_trip_requests)
    private RecyclerView recyclerView;
    @InjectView(R.id.pb_join_trip_requests)
    private ProgressBar progressBar;
    @InjectView(R.id.tv_join_trip_requests_error)
    private TextView error;

    private JoinTripRequestsAdapter adapter;
    private TripsResource tripsResource;


    //************************* Methods ****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_join_trip_requests, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tripsResource = createTripsResource();

        // Use a linear layout manager to use the RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new JoinTripRequestsAdapter(getActivity(), null);
        adapter.setOnRequestAcceptDeclineListener(new AcceptDeclineRequestListener());
        recyclerView.setAdapter(adapter);


        // Ask the server for join-trip-requests
        Subscription subscription = tripsResource.getOffers()
                .compose(new DefaultTransformer<List<TripOffer>>())
                .flatMap(new Func1<List<TripOffer>, Observable<TripOffer>>() {
                    @Override
                    public Observable<TripOffer> call(List<TripOffer> tripOffers) {
                        Timber.i("Received " + tripOffers.size() + " list(s) of TripOffers");
                        return Observable.from(tripOffers);
                    }
                })
                .flatMap(new Func1<TripOffer, Observable<List<JoinTripRequest>>>() {
                    @Override
                    public Observable<List<JoinTripRequest>> call(TripOffer tripOffer) {
                        Timber.i("Received TripOffer with ID: " + tripOffer.getId());

                        // Get the JoinTripRequests for this TripOffer
                        return tripsResource.getJoinRequests(true);
                    }
                })
                .toList()
                .compose(new DefaultTransformer<List<List<JoinTripRequest>>>())
                .subscribe(new ReceiveRequestsSubscriber());

        subscriptions.add(subscription);
    }


    private TripsResource createTripsResource() {

        return new RestAdapter.Builder()
                .setEndpoint(getString(R.string.server_address))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        AccountManager.addAuthorizationHeader(getActivity(), request);
                    }
                })
                .build()
                .create(TripsResource.class);
    }


    //*************************** Inner classes **********************//

    private class AcceptDeclineRequestListener implements JoinTripRequestsAdapter.OnRequestAcceptDeclineListener {

        /**
         * Listener to listen for any driver decisions to accept or decline
         * a pending JoinTripRequest. As soon as such a decision is received, the server
         * is contacted.
         *
         * @param accept if this method should handle "accept" (true) or "decline" (false)
         * @param position the position of the clicked JoinTripRequest in the adapter
         */
        private void handleAcceptDecline(boolean accept, int position) {

            String task;
            if (accept) {
                task = "Accepting";
            } else {
                task = "Declining";
            }

            JoinTripRequest request = adapter.getRequest(position);
            Timber.i(task + " Request with ID " + request.getId());

            // UI
            progressBar.setVisibility(View.VISIBLE);

            // Don't allow other user clicks while the task is performed
            adapter.setOnRequestAcceptDeclineListener(null);

            // Inform server
            JoinTripRequestUpdate requestUpdate = new JoinTripRequestUpdate(accept);
            Subscription subscription = tripsResource.updateJoinRequest(request.getId(), requestUpdate)
                    .compose(new DefaultTransformer<JoinTripRequest>())
                    .subscribe(new AcceptDeclineRequestSubscriber(accept, position));

            subscriptions.add(subscription);
        }

        @Override
        public void onJoinRequestDecline(View view, int position) {
            handleAcceptDecline(false, position);
        }

        @Override
        public void onJoinRequestAccept(View view, int position) {
            handleAcceptDecline(true, position);
        }
    }


    /**
     * A simple Subscriber that removes the previously accepted/declined request
     * from the adapter
     */
    private class AcceptDeclineRequestSubscriber extends Subscriber<JoinTripRequest> {

        private boolean accept;
        private int position;

        protected AcceptDeclineRequestSubscriber(boolean accept, int position) {
            super();
            this.accept = accept;
            this.position = position;
        }

        @Override
        public void onNext(JoinTripRequest joinTripRequest) {
            // SUCCESS
            Timber.i("Successfully informed the server about a request with ID " + joinTripRequest.getId());

            // Everything worked out, so remove the request from the adapter
            adapter.removeRequest(position);

            int numRequests = adapter.getItemCount();
            caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                    numRequests, numRequests));
        }

        @Override
        public void onError(Throwable e) {
            // ERROR
            String task;

            if (accept) {
                task = "accepting";
            } else {
                task = "declining";
            }

            Timber.e("Error when " + task + " a JoinTripRequest: " + e.getMessage());
            onDone();
        }

        @Override
        public void onCompleted() {
            onDone();
        }

        private void onDone(){

            // Allow clicks on trips again
            adapter.setOnRequestAcceptDeclineListener(new AcceptDeclineRequestListener());

            // UI
            progressBar.setVisibility(View.GONE);
        }
    }


    /**
     * Subscribes to any newly received JoinTripRequests which are then added to the adapter.
     */
    private class ReceiveRequestsSubscriber extends Subscriber<List<List<JoinTripRequest>>> {

        @Override
        public void onNext(List<List<JoinTripRequest>> joinTripRequests) {
            // SUCCESS
            int numRequests = adapter.getItemCount();

            for (List<JoinTripRequest> requests : joinTripRequests) {
                Timber.d("Received " + requests.size() + " JoinTripRequest(s)");
                // Fill the list with results
                numRequests += requests.size();
                adapter.addRequests(requests);
            }

            // Show a summary caption
            caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                    numRequests, numRequests));
        }

        @Override
        public void onError(Throwable e) {
            // ERROR
            error.setVisibility(View.VISIBLE);
            caption.setText(getResources().getQuantityString(R.plurals.join_trip_requests,
                    0, 0));

            Timber.e("Receiving JoinTripRequests failed:\n" + e.getMessage());
            onDone();
        }

        @Override
        public void onCompleted() {
            onDone();
        }

        private void onDone(){
            // UI
            progressBar.setVisibility(View.GONE);
        }
    }
}
