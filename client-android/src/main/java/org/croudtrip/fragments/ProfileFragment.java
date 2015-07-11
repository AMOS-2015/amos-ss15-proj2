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
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.VehicleResource;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.utils.DataHolder;
import org.croudtrip.utils.DefaultTransformer;
import org.croudtrip.utils.VehiclesListAdapter;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.InjectView;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * This fragment shows the user's profile with the data he has entered (e.g. address, phone number
 * etc.). From here he can also edit his profile (will be transferred to fragment EditProfileFragment)
 *
 * @author Vanessa Lange
 */
public class ProfileFragment extends SubscriptionFragment {

    //************************* Variables ***************************//

    @InjectView(R.id.vehicles_list)
    private RecyclerView recyclerView;

    @InjectView(R.id.pb_profile)
    private ProgressWheel progressBar;

    @Inject
    private VehicleResource vehicleResource;

    private RecyclerView.LayoutManager layoutManager;
    private VehiclesListAdapter adapter;

    private View profileHeaderView;

    //************************* Methods *****************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the navigation drawer
        Toolbar toolbar = ((MaterialNavigationDrawer) this.getActivity()).getToolbar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        // Profile wrapper with cars, includes profile header later
        final View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Button addNewVehicle = (Button) view.findViewById(R.id.add_new_vehicle);

        // Restore user from SharedPref file
        User user = AccountManager.getLoggedInUser(this.getActivity().getApplicationContext());

        // Top view of profile (avatar, user info etc.), combined with other profileView in onViewCreated
        profileHeaderView = inflater.inflate(R.layout.fragment_profile_header, container, false);
        fillInUserInfo(user, profileHeaderView);


        // Add car button
        addNewVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataHolder.getInstance().setVehicle_id(-1);
                ((MaterialNavigationDrawer) ProfileFragment.this.getActivity())
                        .setFragmentChild(new VehicleInfoFragment(), "Add car");
            }
        });


        // Edit profile button
        FloatingActionButton editProfile = (FloatingActionButton) profileHeaderView.findViewById(R.id.btn_edit_profile);
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MaterialNavigationDrawer) ProfileFragment.this.getActivity())
                        .setFragmentChild(new EditProfileFragment(), getString(R.string.profile_edit));
            }
        });

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use a linear layout manager to use the RecyclerView
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        adapter = new VehiclesListAdapter(getActivity(), null, profileHeaderView);
        recyclerView.setAdapter(adapter);

        //Get a list of user vehicles and add it to the RecyclerView
        subscriptions.add(vehicleResource.getVehicles()
                .compose(new DefaultTransformer<List<Vehicle>>())
                .subscribe(new Action1<List<Vehicle>>() {
                    @Override
                    public void call(List<Vehicle> vehicles) {

                        if (vehicles.size() > 0) {
                            profileHeaderView.findViewById(R.id.tv_profile_vehicles_title).setVisibility(View.VISIBLE);
                            adapter.addElements(vehicles);
                        }
                        //Check if this is the only vehicle the user has
                        if (vehicles.size() == 1) {
                            DataHolder.getInstance().setIsLast(true);
                        }

                        progressBar.setVisibility(View.GONE);

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Response response = ((RetrofitError) throwable).getResponse();
                        if (response != null && response.getStatus() == 401) {  // Not Authorized
                        } else {
                            Timber.e("error" + throwable.getMessage());
                        }
                        Timber.e("Couldn't get data" + throwable.getMessage());
                        progressBar.setVisibility(View.GONE);
                        profileHeaderView.findViewById(R.id.tv_profile_vehicles_title).setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                }));
    }

    private void fillInUserInfo(User user, View headerView) {

        //  Fill in the profile views
        String name = null;
        if (user.getFirstName() != null && user.getLastName() != null) {
            name = user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null) {
            name = user.getFirstName();
        } else if (user.getLastName() != null) {
            name = user.getLastName();
        }

        String birthYear = null;
        if (user.getBirthday() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(user.getBirthday());
            birthYear = calendar.get(Calendar.YEAR) + "";
        }

        String gender = null;
        if (user.getIsMale() != null) {
            if (user.getIsMale()) {
                gender = getString(R.string.profile_male);
            } else if (!user.getIsMale()) {
                gender = getString(R.string.profile_female);
            }
        }

        setTextViewContent(null, (TextView) headerView.findViewById(R.id.tv_profile_name), name);
        setTextViewContent(null, (TextView) headerView.findViewById(R.id.tv_profile_email), user.getEmail());
        setTextViewContent(null, (TextView) headerView.findViewById(R.id.tv_profile_phone), user.getPhoneNumber());
        setTextViewContent(headerView.findViewById(R.id.tv_profile_address_title),
                (TextView) headerView.findViewById(R.id.tv_profile_address), user.getAddress());
        setTextViewContent(headerView.findViewById(R.id.tv_profile_gender_title),
                (TextView) headerView.findViewById(R.id.tv_profile_gender), gender);
        setTextViewContent(headerView.findViewById(R.id.tv_profile_birthyear_title),
                (TextView) headerView.findViewById(R.id.tv_profile_birthyear), birthYear);


        // Download avatar
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null) {
            Timber.i(avatarUrl);
            Picasso.with(getActivity())
                    .load(avatarUrl)
                    .error(R.drawable.background_drawer)
                    .into((ImageView) headerView.findViewById(R.id.tv_profile_image));
        } else {
            Timber.i("Avatar url is null");
        }
    }


    private void setTextViewContent(View view, TextView tv, String content) {
        if (content != null && !content.equals("")) {
            tv.setText(content);
            tv.setVisibility(View.VISIBLE);

            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        } else {
            tv.setVisibility(View.GONE);

            if (view != null) {
                view.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }


}
