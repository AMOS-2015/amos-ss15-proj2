package org.croudtrip;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.croudtrip.db.DatabaseHelper;
import org.croudtrip.api.ServerModule;

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
        OpenHelperManager.setOpenHelperClass(DatabaseHelper.class);
        Timber.plant(new Timber.DebugTree());
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
}
