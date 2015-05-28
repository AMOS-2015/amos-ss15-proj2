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

package org.croudtrip.fragments.offer;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.croudtrip.R;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.fragments.SubscriptionFragment;
import org.croudtrip.trip.MyTripDriverPassengersAdapter;
import org.croudtrip.utils.DefaultTransformer;

import java.util.List;

import javax.inject.Inject;

import roboguice.inject.InjectView;
import rx.Subscriber;
import timber.log.Timber;

/**
 * This class shows a screen for the driver after he has offered a trip and hence is currently
 * already on his way. He is shown a map, his earnings and the passengers that he has accepted.
 * @author Vanessa Lange
 */
public class MyTripDriverFragment extends SubscriptionFragment {

    //************************* Variables ****************************//

    @InjectView(R.id.rv_my_trip_driver_passengers)
    private RecyclerView recyclerView;

    @InjectView(R.id.ll_my_trip_progressBar)
    private LinearLayout progressBarLayout;

    private MyTripDriverPassengersAdapter adapter;

    @Inject
    private TripsResource tripsResource;


    //************************* Methods ****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_my_trip_driver, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new MyTripDriverPassengersAdapter(this);

        // Use a linear layout manager to use the RecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Ask the server for all accepted passengers
        subscriptions.add(tripsResource
                .getDriverAcceptedJoinRequests()
                .compose(new DefaultTransformer<List<JoinTripRequest>>())
                .subscribe(new ReceiveAcceptedPassengersReceiver()));

    }



    //*************************** Inner classes ********************************//

    /**
     * Subscriber to receive accepted passengers which can then be shown in the RecyclerView-list
     */
    private class ReceiveAcceptedPassengersReceiver extends Subscriber<List<JoinTripRequest>> {

        @Override
        public void onNext(List<JoinTripRequest> requests) {
            // SUCCESS
            Timber.d("Received " + requests.size() + " accepted passengers");
            adapter.addRequests(requests);
        }

        @Override
        public void onError(Throwable e) {
            // ERROR
            // TODO error.setVisibility(View.VISIBLE);

            Timber.e("Receiving accepted Passengers (JoinTripRequest) failed:\n" + e.getMessage());
            onDone();
        }

        @Override
        public void onCompleted() {
            onDone();
        }

        private void onDone(){
            // UI
            progressBarLayout.setVisibility(View.GONE);
        }
    }

}
