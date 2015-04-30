package org.croudtrip.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.croudtrip.account.AccountManager;

import timber.log.Timber;

/**
 * Activity without UI. It just redirects to other activities, based on the login status of the current user
 */
public class DispatchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AccountManager.isUserLoggedIn(this)) {
            Timber.i("User is logged in");
            startActivity(new Intent(this, MainActivity.class));
        } else {
            Timber.i("User is not logged in");
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}