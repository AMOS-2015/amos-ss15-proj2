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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.fragments.SubscriptionFragment;

import java.lang.reflect.Field;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 *  This Dispatch Fragment has no "own view". It redirects/shows the correct fragments for a driver
 *  who offers/offer a trip. This is either the OfferTrip-Fragment or MyTripDriver-Fragment.
 */
public class DispatchOfferTripFragment extends SubscriptionFragment {

    //*************************** Variables ****************************//

    private Fragment offerTripFragment;
    private Fragment myTripDriverFragment;


    //**************************** Methods ******************************//

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        offerTripFragment = new OfferTripFragment();
        myTripDriverFragment = new MyTripDriverFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_offer_dispatch, container, false);
        replaceChildFragment(getArguments());

        return view;
    }


    /**
     * Sets the default title in the navigation drawer ("Offer trip") and shows the correct fragment
     * based on the status saved in the shared preferences
     * @param args arguments for the new fragment
     */
    private void replaceChildFragment(Bundle args) {

        // TODO clean saved fragment-navigation from SharedPref if logout

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        SharedPreferences prefs = getActivity().getSharedPreferences(
                Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);


        if (prefs.getBoolean(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER, false)) {
            // MY TRIP (driver) -> Show the MyTripDriverFragment

            if (args != null) {
                myTripDriverFragment = new MyTripDriverFragment();
                myTripDriverFragment.setArguments(args);
            }

            // "My trip" title
            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_my_trip));

            transaction.replace(R.id.fl_offer_dispatch_child_fragment, myTripDriverFragment)
                    .commitAllowingStateLoss();

        } else {
            // OFFER TRIP -> Show the OfferTripFragment

            if (args != null) {
                offerTripFragment = new OfferTripFragment();
                offerTripFragment.setArguments(args);
            }

            // "Offer trip" title
            ((MaterialNavigationDrawer) getActivity()).getCurrentSection().setTitle(getString(R.string.menu_offer_trip));

            transaction.replace(R.id.fl_offer_dispatch_child_fragment, offerTripFragment)
                    .commitAllowingStateLoss();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Propagate event to child fragment
        if (offerTripFragment != null) {
            offerTripFragment.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void onDetach() throws RuntimeException {
        super.onDetach();

        // Nested Fragments Bug Fix:
        // If the parent fragment is removed from the activity, the child fragment would be in an
        // "illegalState"
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
