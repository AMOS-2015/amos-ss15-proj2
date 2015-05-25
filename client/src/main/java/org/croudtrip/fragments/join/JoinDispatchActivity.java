package org.croudtrip.fragments.join;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.croudtrip.Constants;
import org.croudtrip.R;

import java.lang.reflect.Field;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * Created by alex on 22.04.15.
 */
public class JoinDispatchActivity extends FragmentActivity {

    public final static String KEY_CURRENT_LOCATION_LATITUDE = "current_location_latitude";
    public final static String KEY_CURRENT_LOCATION_LONGITUDE = "current_location_longitude";
    public final static String KEY_DESTINATION_LATITUDE = "destination_latitude";
    public final static String KEY_DESTINATION_LONGITUDE = "destination_longitude";
    public final static String KEY_MAX_WAITING_TIME = "max_waiting_time";
    public final static String KEY_JOIN_TRIP_REQUEST_RESULT = "join_trip_request_result";


    private Fragment searchFragment, resultsFragment, drivingFragment;

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
        setContentView(R.layout.fragment_join_dispatch);

        searchFragment = new JoinSearchFragment();
        drivingFragment = new JoinDrivingFragment();
        resultsFragment = new JoinResultsFragment();

        LocalBroadcastManager.getInstance(this).registerReceiver(changeUiReceiver, new IntentFilter(Constants.EVENT_CHANGE_JOIN_UI));

        replaceChildFragment(null);
    }




    private void replaceChildFragment(Bundle args) {
        ((MaterialNavigationDrawer) getApplicationContext()).getCurrentSection().setNotificationsText("");
        ((MaterialNavigationDrawer) getApplicationContext()).getCurrentSection().setTitle(getString(R.string.menu_join_trip));

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);

        //SEARCHING -> Show the results fragment (waiting screen + results)
        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false)) {
            if (args != null) {
                resultsFragment = new JoinResultsFragment();
                resultsFragment.setArguments(args);
            }
            transaction.replace(R.id.child_fragment, resultsFragment).commitAllowingStateLoss();

        //ACCEPTED -> Show the driving fragment
        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false)) {
            if (args != null) {
                drivingFragment = new JoinDrivingFragment();
                drivingFragment.setArguments(args);
            }
            transaction.replace(R.id.child_fragment, drivingFragment).commitAllowingStateLoss();

        //OTHERWISE -> Show the default search fragment
        } else {
            if (args != null) {
                searchFragment = new JoinSearchFragment();
                searchFragment.setArguments(args);
            }
            transaction.replace(R.id.child_fragment, searchFragment).commitAllowingStateLoss();
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

}
