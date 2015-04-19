package org.croudtrip.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

import org.croudtrip.DB_Dummy;
import org.croudtrip.MainApplication;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * This is an example class for an activity that uses the database.
 * Created by Vanessa Lange on 18.04.15.
 */
public class DB_DummyActivity extends ActionBarActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Dao<DB_Dummy, Long> dummyDao = ((MainApplication) getApplicationContext()).getHelper().getDB_DummyDao();

            // Create some entries to demonstrate how this is working
            DB_Dummy brother = new DB_Dummy("Brother", null);
            dummyDao.create(brother);

            DB_Dummy dummy = new DB_Dummy("Dummy", brother);
            dummyDao.create(dummy);

            // Query db for all DB_Dummies
            List<DB_Dummy> dummies = dummyDao.queryForAll();
            Log.i("DB_DummyActivity", "Found these dummies in DB: " + Arrays.toString(dummies.toArray()));
            Log.i("DB_DummyActivity", "Found DB_Dummy with id 1: " + dummyDao.queryForId(1L).toString());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
