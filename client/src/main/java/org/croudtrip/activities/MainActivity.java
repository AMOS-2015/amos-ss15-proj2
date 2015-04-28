package org.croudtrip.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.fragments.NavigationFragment;
import org.croudtrip.fragments.OfferTripFragment;
import org.croudtrip.fragments.ProfileFragment;
import org.croudtrip.fragments.SettingsFragment;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;

/**
 * We will probably use fragments, so this activity works as a container for all these fragments and will probably do
 * some initialization and stuff
 */
public class MainActivity extends MaterialNavigationDrawer<Fragment> {

    @Override
    public void init(Bundle savedInstanceState) {

        this.disableLearningPattern();
        this.allowArrowAnimation();
        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        this.setDrawerHeaderImage(R.drawable.background_drawer);

        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        String firstName = prefs.getString(Constants.SHARED_PREF_KEY_FIRSTNAME, "");
        String lastName = prefs.getString(Constants.SHARED_PREF_KEY_LASTNAME, "");
        String email = prefs.getString(Constants.SHARED_PREF_KEY_EMAIL, "");
        MaterialAccount account = new MaterialAccount(this.getResources(),firstName+ " " + lastName,email,R.drawable.profile, R.drawable.background_drawer);
        this.addAccount(account);


        // create sections
        this.addSection(newSection(getString(R.string.menu_join_trip), R.drawable.ic_settings, new JoinTripFragment()));
        this.addSection(newSection(getString(R.string.menu_offer_trip), R.drawable.ic_settings, new OfferTripFragment()));
        if(LoginActivity.isUserLoggedIn(this)) {
            // only logged-in users can view their profile
            this.addSection(newSection(getString(R.string.menu_profile), R.drawable.ic_settings, new ProfileFragment()));
        }
        this.addSection(newSection(getString(R.string.navigation), R.drawable.ic_settings, new NavigationFragment()));

        ((MaterialSection) getSectionList().get(0)).setNotifications(3);

        // create bottom section
        this.addBottomSection(newSection(getString(R.string.menu_settings), R.drawable.ic_settings, new SettingsFragment()));
    }

}
