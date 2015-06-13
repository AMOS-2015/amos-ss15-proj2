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

package org.croudtrip.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.api.trips.JoinTripRequestUpdateType;
import org.croudtrip.trip.JoinTripRequestsAdapter;
import org.croudtrip.trip.OnDiversionUpdateListener;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.DefaultTransformer;
import org.croudtrip.utils.SwipeListener;

import java.util.List;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
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
    private SwipeListener touchListener;

    @Inject private TripsResource tripsResource;

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

        // Use a linear layout manager to use the RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new JoinTripRequestsAdapter(this);
        AcceptDeclineRequestListener acceptDeclineListener = new AcceptDeclineRequestListener();
        this.touchListener = new SwipeListener(recyclerView, acceptDeclineListener);
        adapter.setOnRequestAcceptDeclineListener(acceptDeclineListener);

        recyclerView.setAdapter(adapter);
        recyclerView.setOnTouchListener(touchListener);
        recyclerView.setOnScrollListener(touchListener.makeScrollListener());


        // Ask the server for join-trip-requests
        subscriptions.add(tripsResource
                .getJoinRequests(true)
                .compose(new DefaultTransformer<List<JoinTripRequest>>())
                .subscribe(new ReceiveRequestsSubscriber()));
    }


    public void informAboutDiversion(final JoinTripRequest joinRequest, final OnDiversionUpdateListener listener,
                                     final TextView textView){

        // Ask the server for the diversion
        subscriptions.add(tripsResource
                        .getDiversionInSecondsForJoinRequest(joinRequest.getId())
                        .compose(new DefaultTransformer<Long>())
                        .subscribe(new Action1<Long>() {
                            @Override
                            public void call(Long diversionInSeconds) {
                                int diversionInMinutes = (int) (diversionInSeconds / 60);
                                listener.onDiversionUpdate(joinRequest, textView, diversionInMinutes);
                            }

                        }, new CrashCallback(getActivity()))
        );
    }


    //*************************** Inner classes **********************//

    private class AcceptDeclineRequestListener implements SwipeListener.DismissCallbacks,
            JoinTripRequestsAdapter.OnRequestAcceptDeclineListener {

        /**
         * Listener to listen for any driver decisions to accept or decline
         * a pending JoinTripRequest. As soon as such a decision is received, the server
         * is contacted.
         *
         * @param accept if this method should handle "accept" (true) or "decline" (false)
         * @param position the position of the clicked JoinTripRequest in the adapter
         */
        private synchronized void handleAcceptDecline(final boolean accept, final int position) {

            String task;
            if (accept) {
                task = "Accepting";
            } else {
                task = "Declining";
            }

            final JoinTripRequest request = adapter.getRequest(position);
            Timber.i(task + " Request with ID " + request.getId());

            // UI
            progressBar.setVisibility(View.VISIBLE);

            // Don't allow other user clicks while the task is performed
            //adapter.setOnRequestAcceptDeclineListener(null);
            recyclerView.setOnTouchListener(null);

            //Get a list of current Join trip requests from the server
            //and make sure that the request is still active (hasn't expired)
            Subscription Jsubscription = tripsResource.getJoinRequests(true)
                    .compose(new DefaultTransformer<List<JoinTripRequest>>())
                    .subscribe(new Action1<List<JoinTripRequest>>() {
                        @Override
                        public void call(List<JoinTripRequest> requests) {
                            if (requests.size() > 0) {
                                for (int i=0;i<requests.size();i++)
                                {
                                    if (request.getId() == requests.get(i).getId()) {
                                        //Inform server only if the request is still active (not expired)
                                        JoinTripRequestUpdate requestUpdate;
                                        if (accept)
                                            requestUpdate = new JoinTripRequestUpdate(JoinTripRequestUpdateType.ACCEPT_PASSENGER);
                                        else
                                            requestUpdate = new JoinTripRequestUpdate(JoinTripRequestUpdateType.DECLINE_PASSENGER);
                                        Subscription subscription = tripsResource.updateJoinRequest(request.getId(), requestUpdate)
                                                .compose(new DefaultTransformer<JoinTripRequest>())
                                                .subscribe(new AcceptDeclineRequestSubscriber(accept, position));
                                        subscriptions.add(subscription);
                                        // UI
                                        progressBar.setVisibility(View.VISIBLE);
                                        Timber.i("request has not expired");
                                        //Break from the loop when the request is found in the list
                                        break;
                                    }
                                    //If the request wasn't found in the list, show a toast to the driver
                                    // and remove the card from the list
                                    if (i==requests.size()-1)
                                    {
                                        Timber.d("Request has expired");
                                        Toast.makeText(getActivity(), getResources().getString(R.string.offer_trip_request_expired), Toast.LENGTH_SHORT).show();
                                        //Enable clicking the list items again and remove the progress bar
                                        recyclerView.setOnTouchListener(touchListener);
                                        progressBar.setVisibility(View.GONE);
                                        adapter.removeRequest(position);
                                        int numRequests = adapter.getItemCount();
                                        //Show text caption if there are no more requests
                                        if(numRequests == 0) {
                                            caption.setVisibility(View.VISIBLE);
                                        }else{
                                            caption.setVisibility(View.GONE);
                                        }
                                    }
                                }
                            }
                            else
                            //If the expired request was the last one in the list, size() will be 0
                            //This snippet takes care of this case
                            {
                                Timber.d("No requests found (Request has expired)");
                                Toast.makeText(getActivity(), getResources().getString(R.string.offer_trip_request_expired), Toast.LENGTH_SHORT).show();
                                //Enable clicking the list items again and remove the progress bar
                                recyclerView.setOnTouchListener(touchListener);
                                progressBar.setVisibility(View.GONE);
                                adapter.removeRequest(position);
                                int numRequests = adapter.getItemCount();
                                //Show text caption if there are no more requests
                                if (numRequests == 0) {
                                    caption.setVisibility(View.VISIBLE);
                                } else {
                                    caption.setVisibility(View.GONE);
                                }
                            }
                        }
                    }, new CrashCallback(getActivity()) {
                        @Override
                        public void call(Throwable throwable) {
                            super.call(throwable);
                            Response response = ((RetrofitError) throwable).getResponse();
                            if (response != null && response.getStatus() == 401) {  // Not Authorized
                            } else {
                                Timber.e("error" + throwable.getMessage());
                            }
                            Timber.e("Couldn't get data" + throwable.getMessage());
                        }
                    });

            subscriptions.add(Jsubscription);


        }

        @Override
        public void onJoinRequestDecline(View view, int position) {
            handleAcceptDecline(false, position);
        }

        @Override
        public void onJoinRequestAccept(View view, int position) {
            handleAcceptDecline(true, position);
        }

        @Override
        public boolean canDismiss(int position) {
            return true;
        }

        @Override
        public void onSwipeLeft(RecyclerView recyclerView, int[] dismissedItems) {
            // Decline only the first item and ignore the rest
            if(dismissedItems != null && dismissedItems.length > 0) {
                handleAcceptDecline(false, dismissedItems[0]);
            }

        }

        @Override
        public void onSwipeRight(RecyclerView recyclerView, int[] dismissedItems) {
            // Accept only the first item and ignore the rest
            if(dismissedItems != null && dismissedItems.length > 0) {
                handleAcceptDecline(true, dismissedItems[0]);
            }
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
            if(numRequests == 0) {
                caption.setVisibility(View.VISIBLE);
            }else{
                caption.setVisibility(View.GONE);
            }
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
            //adapter.setOnRequestAcceptDeclineListener(new AcceptDeclineRequestListener());
            recyclerView.setOnTouchListener(touchListener);

            // UI
            progressBar.setVisibility(View.GONE);
        }
    }


    /**
     * Subscribes to any newly received JoinTripRequests which are then added to the adapter.
     */
    private class ReceiveRequestsSubscriber extends Subscriber<List<JoinTripRequest>> {

        @Override
        public void onNext(List<JoinTripRequest> requests) {
            // SUCCESS
            Timber.d("Received " + requests.size() + " JoinTripRequest(s)");

            int numRequests = adapter.getItemCount();
            numRequests += requests.size();

            if(numRequests == 0){
                // Show a summary caption
                caption.setVisibility(View.VISIBLE);
            }else{
                // Fill the list with results
                adapter.addRequests(requests);
                caption.setVisibility(View.GONE);
            }
        }

        @Override
        public void onError(Throwable e) {
            // ERROR
            error.setVisibility(View.VISIBLE);

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
