package org.croudtrip.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.croudtrip.account.AccountManager;

import timber.log.Timber;

/**
 * Activity without UI. It just redirects to other activities, based on the login status of the current user
 */
public class DispatchActivity extends Activity {

    private static final int REQUEST_LOGIN = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (AccountManager.isUserLoggedIn(this)) {
            Timber.i("User is logged in");
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Timber.i("User is not logged in");
            startActivityForResult(new Intent(DispatchActivity.this, LoginActivity.class), REQUEST_LOGIN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        //wrong requestCode logIn canceled
        if (requestCode != REQUEST_LOGIN || resultCode == RESULT_CANCELED) {
            finish();
        } else {
            // all good (logged in or skipped)
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

}