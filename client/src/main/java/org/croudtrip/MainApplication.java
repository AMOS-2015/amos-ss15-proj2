package org.croudtrip;

import android.app.Application;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.croudtrip.db.DatabaseHelper;

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
