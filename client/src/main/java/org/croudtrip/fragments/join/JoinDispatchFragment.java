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

package org.croudtrip.fragments.join;

import android.annotation.TargetApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Build;
import android.content.BroadcastReceiver;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.fragments.SubscriptionFragment;

import java.lang.reflect.Field;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * Created by alex on 22.04.15.
 *
 The Dispatch Fragment has no "own view", it should only redirect/show the correct fragments.
 The main reason for this is that we have only one fragment in the menu/navigationDrawer and do
 not have to switch it out dynamically everytime something on the UI changes.
 */
public class JoinDispatchFragment extends SubscriptionFragment {

    public final static String KEY_CURRENT_LOCATION_LATITUDE = "current_location_latitude";
    public final static String KEY_CURRENT_LOCATION_LONGITUDE = "current_location_longitude";
    public final static String KEY_DESTINATION_LATITUDE = "destination_latitude";
    public final static String KEY_DESTINATION_LONGITUDE = "destination_longitude";
    public final static String KEY_MAX_WAITING_TIME = "max_waiting_time";
    public final static String KEY_JOIN_TRIP_REQUEST_RESULT = "join_trip_request_result";


    private Fragment searchFragment, resultsFragment, drivingFragment;
    private boolean allowBackPressed = true;

    private BroadcastReceiver changeUiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle args = intent.getExtras();
            replaceChildFragment(args);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchFragment = new JoinSearchFragment();
        drivingFragment = new JoinDrivingFragment();
        resultsFragment = new JoinResultsFragment();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(changeUiReceiver, new IntentFilter(Constants.EVENT_CHANGE_JOIN_UI));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_join_dispatch, container, false);
        replaceChildFragment(getArguments());

        return view;
    }


    /*
    Set default title and clear notification text.
    Show the correct fragment based on the status saved in the shared preferences
     */
    private void replaceChildFragment(Bundle args) {
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_join_trip));
        ((MaterialNavigationDrawer) getActivity()).setTitle(R.string.menu_join_trip);


        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        //SEARCHING -> Show the results fragment (waiting screen + results)
        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false)) {

            if (args != null) {
                resultsFragment = new JoinResultsFragment();
                resultsFragment.setArguments(args);
            }
            allowBackPressed = false;
            transaction.replace(R.id.child_fragment, resultsFragment).commitAllowingStateLoss();

        //ACCEPTED -> Show the driving fragment
        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false) || prefs.getBoolean(Constants.SHARED_PREF_KEY_WAITING, false)) {

            if (args != null) {
                drivingFragment = new JoinDrivingFragment();
                drivingFragment.setArguments(args);
            }
            allowBackPressed = false;
            transaction.replace(R.id.child_fragment, drivingFragment).commitAllowingStateLoss();

        //OTHERWISE -> Show the default search fragment
        } else {

            if (args != null) {
                searchFragment = new JoinSearchFragment();
                searchFragment.setArguments(args);
            }
            allowBackPressed = true;
            transaction.replace(R.id.child_fragment, searchFragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        //inflater.inflate(R.menu.menu_main, menu);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(changeUiReceiver);

        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    Handle the onBackPressed.
    Close the app if the user is at the searchFragment, otherwise navigate back to the searchFragment
     */
    public boolean allowBackPressed() {
        /*
        This doesn´t make any sense any more

        if (!allowBackPressed) {
            SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
            editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false);
            editor.putBoolean(Constants.SHARED_PREF_KEY_WAITING, false);
            editor.putLong(Constants.SHARED_PREF_KEY_QUERY_ID, -1);
            editor.apply();

            replaceChildFragment(null);

            return false;
        }*/

        return true;
    }
}
