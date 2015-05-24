package org.croudtrip.fragments.join;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
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
 */
public class JoinDispatchFragment extends SubscriptionFragment {

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
        replaceChildFragment(null);

        return view;
    }


    /*
    Inflate the correct child fragment.

    Default is "JoinSearchFragment"; if we are currently looking for a trip on the server or there some trips were
    found then the "JoinResultsFragment" should be visible; if the user is on a trip he sees the "JoinDrivingFragment".
    These three Views are persistent -> they are dependent on what is in the Shared Prefs.
     */
    private void replaceChildFragment(Bundle args) {
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setNotificationsText("");
        ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_join_trip));

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false)) {
            if (args != null) {
                resultsFragment = new JoinResultsFragment();
                resultsFragment.setArguments(args);
            }
            transaction.replace(R.id.child_fragment, resultsFragment).commitAllowingStateLoss();
        } else if (prefs.getBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false)) {
            if (args != null) {
                drivingFragment = new JoinDrivingFragment();
                drivingFragment.setArguments(args);
            }
            transaction.replace(R.id.child_fragment, drivingFragment).commitAllowingStateLoss();
        } else {
            if (args != null) {
                searchFragment = new JoinSearchFragment();
                searchFragment.setArguments(args);
            }
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
}
