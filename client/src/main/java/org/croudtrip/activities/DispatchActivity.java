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