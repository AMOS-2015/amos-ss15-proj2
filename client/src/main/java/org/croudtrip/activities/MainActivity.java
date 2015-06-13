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


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.gms.location.LocationRequest;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.account.User;
import org.croudtrip.fragments.JoinTripRequestsFragment;
import org.croudtrip.fragments.JoinTripResultsFragment;
import org.croudtrip.fragments.ProfileFragment;
import org.croudtrip.fragments.SettingsFragment;
import org.croudtrip.fragments.join.JoinDispatchFragment;
import org.croudtrip.fragments.offer.DispatchOfferTripFragment;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.location.LocationUpdater;
import org.croudtrip.location.LocationUploadTimerReceiver;
import org.croudtrip.utils.CrashCallback;
import org.croudtrip.utils.DefaultTransformer;

import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * We will probably use fragments, so this activity works as a container for all these fragments and will probably do
 * some initialization and stuff
 */
public class MainActivity extends AbstractRoboDrawerActivity {

    //******************************** Variables ***********************************//

    // If a notification arrives this sometimes has to change the displayed screen.
    // These constants are used to determine which screen should be shown.
    public final static String ACTION_SHOW_JOIN_TRIP_REQUESTS = "SHOW_JOIN_TRIP_REQUESTS";
    public final static String ACTION_SHOW_REQUEST_ACCEPTED = "SHOW_REQUEST_ACCEPTED";
    public final static String ACTION_SHOW_REQUEST_DECLINED = "SHOW_REQUEST_DECLINED";
    public final static String ACTION_SHOW_FOUND_MATCHES = "SHOW_FOUND_MATCHES";

    @Inject private GcmManager gcmManager;
    @Inject private LocationUpdater locationUpdater;
    @Inject private TripsResource tripsResource;

    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription locationUpdateSubscription;


    //********************************* Methods ************************************//
    @Override
    protected void onNewIntent(Intent intent){
        if (intent.getAction() != null && intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            //user scanned an NFC tag -> notify the passenger driving UI to save new status and maybe update UI
            Intent startingIntent = new Intent(Constants.EVENT_NFC_TAG_SCANNED);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
        } else {
            Timber.d("Another Intent or detected some other NFC stuff...");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        LocalBroadcastManager.getInstance(this).registerReceiver(driverAcceptedReceiver,
                new IntentFilter(Constants.EVENT_DRIVER_ACCEPTED));
    }


    @Override
    public void init(Bundle savedInstanceState) {

        // Configure Navigation Drawer
        this.disableLearningPattern();
        this.allowArrowAnimation();
        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        this.setDrawerHeaderImage(R.drawable.background_drawer);

        // Get all the saved user data to display some info in the navigation drawer
        showUserInfoInNavigationDrawer();

        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES,
                Context.MODE_PRIVATE);


        // Start timer to update the user's offers all the time
        if( AccountManager.isUserLoggedIn( this ) ){
            Intent alarmIntent = new Intent(this, LocationUploadTimerReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

            AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            alarmManager.setRepeating( AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                    120 * 1000, pendingIntent );
        }


        // Subscribe to location updates
        LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000);

        ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(this);
        Subscription locationUpdateSubscription = locationProvider.getUpdatedLocation(request)
                .subscribe(new Action1<Location>() {
                    @Override
                    public void call(Location location) {
                        locationUpdater.setLastLocation( location );
                    }
                });

        subscriptions.add(locationUpdateSubscription);


        // GPS availability
        if (!GPSavailable()) {
            checkForGPS();
        }


        // Registration for GCM, if we are not registered yet
        if( !gcmManager.isRegistered() ) {
            Subscription subscription = gcmManager.register()
                    .compose(new DefaultTransformer<Void>())
                    .retry(3)
                    .subscribe( new Action1<Void>() {
                        @Override
                        public void call(Void aVoid) {
                            Timber.d("Registered at GCM.");
                        }
                    }, new CrashCallback(this) {
                        @Override
                        public void call(Throwable throwable) {
                            super.call(throwable);
                            Timber.e("Could not register at GCM services: " + throwable.getMessage() );
                        }
                    });
            subscriptions.add(subscription);
        }

        fillNavigationDrawer();
    }

    @Override
    public void onBackPressed() {
        if (getCurrentSection().getTargetFragment() instanceof JoinDispatchFragment) {
            JoinDispatchFragment currentFragment = (JoinDispatchFragment) getCurrentSection().getTargetFragment();
            if (!currentFragment.allowBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }


    @Override
    public void onPause() {
        super.onPause();

        subscriptions.unsubscribe();
        subscriptions.clear();
    }


    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(driverAcceptedReceiver);
        super.onDestroy();
    }


    private BroadcastReceiver driverAcceptedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle args = intent.getExtras();

            Fragment frag = new JoinTripResultsFragment();
            frag.setArguments(args);

            getSectionList().get(0).setTarget( frag );
            getSectionList().get(0).setTitle(getString(R.string.menu_my_trip));
            setFragment( frag, getString(R.string.menu_my_trip));
        }
    };



    private boolean GPSavailable() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private void checkForGPS() {
        final SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();


        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View dialogLayout = adbInflater.inflate(R.layout.dialog_enable_gps, null);
        final CheckBox dontShowAgain = (CheckBox) dialogLayout.findViewById(R.id.skip);
        adb.setView(dialogLayout);
        adb.setTitle(getResources().getString(R.string.enable_gps_title));
        adb.setMessage(getResources().getString(R.string.enable_gps_description));
        adb.setPositiveButton(getResources().getString(R.string.enable_gps_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                if (dontShowAgain.isChecked()) {
                    editor.putBoolean(Constants.SHARED_PREF_KEY_SKIP_ENABLE_GPS, true);
                    editor.apply();
                }
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                return;
            }
        });

        adb.setNegativeButton(getResources().getString(R.string.enable_gps_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });

        adb.show();
    }


    private void showUserInfoInNavigationDrawer(){

        // Get logged-in user and his data
        User user = AccountManager.getLoggedInUser(getApplicationContext());
        String firstName = (user == null || user.getFirstName() == null) ? "" : user.getFirstName();
        String lastName = (user == null || user.getLastName() == null) ? "" : user.getLastName();
        String email = (user == null || user.getEmail() == null) ? "" : user.getEmail();
        final String avatarUrl = (user == null || user.getAvatarUrl() == null) ? null : user.getAvatarUrl();

        final MaterialAccount account = new MaterialAccount(this.getResources(),
                firstName+ " " + lastName, email, R.drawable.profile, R.drawable.background_drawer);
        this.addAccount(account);

        // Download his avatar
        if (avatarUrl != null) {
            Observable.defer(new Func0<Observable<Bitmap>>() {
                @Override
                public Observable<Bitmap> call() {
                    try {
                        URL url = new URL(avatarUrl);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        return Observable.just(BitmapFactory.decodeStream(input));
                    } catch (Exception e) {
                        return Observable.error(e);
                    }
                }
            }).compose(new DefaultTransformer<Bitmap>())
                    .subscribe(new Action1<Bitmap>() {
                        @Override
                        public void call(Bitmap avatar) {
                            Timber.d("avatar is null " + (avatar == null));
                            Timber.d("" + avatar.getWidth());
                            account.setPhoto(avatar);
                            notifyAccountDataChanged();
                        }
                    }, new CrashCallback(this) {
                        @Override
                        public void call(Throwable throwable) {
                            Timber.e(throwable, "failed to download avatar");
                        }
                    });
        }
    }


    private void fillNavigationDrawer(){

        // Check the action that was given to the activity to determine the sections that
        // should be shown
        Intent intent = getIntent();
        String action = intent.getAction();
        action = (action == null) ? "" : action;

        // (0) JOIN TRIP
        JoinDispatchFragment joinDispatchFragment = new JoinDispatchFragment();
        joinDispatchFragment.setArguments(getIntent().getExtras());
        this.addSection(newSection(getString(R.string.menu_join_trip),
                R.drawable.hitchhiker,
                joinDispatchFragment));


        // (1) OFFER TRIP
        String offerName = getString(R.string.menu_offer_trip);
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, MODE_PRIVATE);
        if(prefs.getBoolean(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER, false)){
            offerName = getString(R.string.menu_my_trip);
        }

        DispatchOfferTripFragment dispatchOfferTripFragment = new DispatchOfferTripFragment();
        dispatchOfferTripFragment.setArguments(getIntent().getExtras());
        this.addSection(newSection(offerName,
                R.drawable.ic_directions_car_white,
                dispatchOfferTripFragment));


        // (2) PENDING JOIN REQUESTS
        // TODO: code also down below?
        if( action.equalsIgnoreCase(ACTION_SHOW_JOIN_TRIP_REQUESTS)
                || prefs.getBoolean(Constants.SHARED_PREF_KEY_RUNNING_TRIP_OFFER, false)) {
            this.addSection(newSection("My Trip Requests",
                    R.drawable.distance,
                    new JoinTripRequestsFragment()));
        }


        // (3) PROFILE
        if(AccountManager.isUserLoggedIn(this)) {
            // Only logged-in users can view their profile
            this.addSection(newSection(getString(R.string.menu_profile),
                    R.drawable.profile_icon,
                    new ProfileFragment()));
        }


        // (4) SETTINGS
        this.addBottomSection(newSection(getString(R.string.menu_settings),
                R.drawable.ic_settings,
                new SettingsFragment()));


        // Set the section that should be loaded at the start of the application
        if( action.equalsIgnoreCase(ACTION_SHOW_REQUEST_DECLINED) || action.equals(ACTION_SHOW_FOUND_MATCHES) ) {
            this.setDefaultSectionLoaded(0);
            MaterialSection section = this.getSectionByTitle(getString(R.string.menu_my_trip));

            Bundle extras = getIntent().getExtras();
            Bundle bundle = new Bundle();
            bundle.putAll(extras);

            Fragment requestFrag = (Fragment) section.getTargetFragment();
            requestFrag.setArguments(bundle);

        }else if( action.equalsIgnoreCase(ACTION_SHOW_JOIN_TRIP_REQUESTS) ) {
            this.setDefaultSectionLoaded(2);    // (2) PENDING JOIN REQUESTS
        }
    }
}
