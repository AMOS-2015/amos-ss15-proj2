package org.croudtrip.activities;

import android.os.Bundle;

import org.croudtrip.R;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.fragments.OfferTripFragment;
import org.croudtrip.fragments.ProfileFragment;
import org.croudtrip.fragments.SettingsFragment;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * We will probably use fragments, so this activity works as a container for all these fragments and will probably do
 * some initialization and stuff
 */
public class MainActivity extends MaterialNavigationDrawer {


    @Override
    public void init(Bundle savedInstanceState) {

        this.disableLearningPattern();
        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        this.setDrawerHeaderImage(R.drawable.background_drawer);


        // create sections
        this.addSection(newSection(getString(R.string.menu_join_trip), R.drawable.ic_settings, new JoinTripFragment()));
        this.addSection(newSection(getString(R.string.menu_offer_trip), R.drawable.ic_settings, new OfferTripFragment()));
        this.addSection(newSection(getString(R.string.menu_profile), R.drawable.ic_settings, new ProfileFragment()));




        // create bottom section
        this.addBottomSection(newSection(getString(R.string.menu_settings),R.drawable.ic_settings, new SettingsFragment()));



    }
}
