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

package org.croudtrip;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import io.fabric.sdk.android.Fabric;
import org.croudtrip.db.DatabaseHelper;
import org.croudtrip.api.ServerModule;
import org.croudtrip.utils.LifecycleHandler;

import roboguice.RoboGuice;
import timber.log.Timber;

/**
 * Use this place to initialize frameworks etc.
 */
public class MainApplication extends Application {

    private DatabaseHelper dbHelper = null;


    @Override
    public void onCreate() {
        super.onCreate();

        //Fabric.with(this, new Crashlytics());

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashlyticsTree());
        }

        registerActivityLifecycleCallbacks( new LifecycleHandler() );

        OpenHelperManager.setOpenHelperClass(DatabaseHelper.class);


        RoboGuice.getOrCreateBaseApplicationInjector(
                this,
                RoboGuice.DEFAULT_STAGE,
                RoboGuice.newDefaultRoboModule(this),
                new ServerModule());
    }


    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    @Override
    public void onTerminate() {
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }
        super.onTerminate();
    }

    public DatabaseHelper getHelper() {
        if (dbHelper == null) {
            dbHelper = (DatabaseHelper) OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }

    private static class CrashlyticsTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {

            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

            if (t != null) {
                Crashlytics.getInstance().core.logException(t);
            } else {
                Crashlytics.getInstance().core.logException(new Throwable(message));
            }
        }
    }
}
