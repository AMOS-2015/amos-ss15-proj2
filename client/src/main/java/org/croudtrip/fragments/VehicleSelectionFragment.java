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


import org.croudtrip.R;
import org.croudtrip.api.VehicleResource;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.DefaultTransformer;
import org.croudtrip.utils.VehiclesListSelectAdapter;

import java.util.List;

import javax.inject.Inject;

import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * This fragment allows the user to enter the vehicle information and uploads this information to the server
 * @author nazeehammari
 */
public class VehicleSelectionFragment extends SubscriptionFragment {

    //************************* Variables ***************************//

    @InjectView(R.id.vehicles_list_select)          private RecyclerView recyclerView;
    @Inject  VehicleResource vehicleResource;


    private RecyclerView.LayoutManager layoutManager;
    private VehiclesListSelectAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_vehicle_select, container, false);

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use a linear layout manager to use the RecyclerView
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new VehiclesListSelectAdapter(getActivity(), null);
        recyclerView.setAdapter(adapter);

        //Get a list of user vehicles and add it to the RecyclerView
        Subscription subscription = vehicleResource.getVehicles()
                .compose(new DefaultTransformer<List<Vehicle>>())
                .subscribe(new Action1<List<Vehicle>>() {
                    @Override
                    public void call(List<Vehicle> vehicles) {
                        if (vehicles.size() > 0) {
                            adapter.addElements(vehicles);
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

        subscriptions.add(subscription);


    }


    }

