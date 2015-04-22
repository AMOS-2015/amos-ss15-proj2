package org.croudtrip.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.croudtrip.R;
import org.croudtrip.fragments.MainFragment;

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
        this.addSection(newSection("Main", R.drawable.ic_settings, new MainFragment()));



        // create bottom section
        this.addBottomSection(newSection(getString(R.string.action_settings),R.drawable.ic_settings,new Intent(this,MainActivity.class)));



    }
}
