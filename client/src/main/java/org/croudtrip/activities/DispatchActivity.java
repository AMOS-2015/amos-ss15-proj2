package org.croudtrip.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import timber.log.Timber;

/**
 * Activity without UI. It just redirects to other activities, based on the login status of the current user
 */
public class DispatchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.SHARED_PREF_KEY_EMAIL, "my@email.com");
        editor.putString(Constants.SHARED_PREF_KEY_PWD, "password");
        editor.putString(Constants.SHARED_PREF_KEY_FIRSTNAME, "Alex");
        editor.putString(Constants.SHARED_PREF_KEY_LASTNAME, "Test");
        editor.apply();
        */

        if (LoginActivity.isUserLoggedIn(this)) {
            Timber.i("User is logged in");
            startActivity(new Intent(this, MainActivity.class));
        } else {
            Timber.i("User is not logged in");
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

}