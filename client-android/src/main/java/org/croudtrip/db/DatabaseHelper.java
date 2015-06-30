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

package org.croudtrip.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

import timber.log.Timber;

/**
 * Database helper class used to manage the creation and upgrading of the database. This class also
 * provides the DAOs used by the other classes.
 *
 * Created by Vanessa Lange on 18.04.15.
 * See <a href="https://github.com/j256/ormlite-examples/blob/master/android/HelloAndroid/src/com/example/helloandroid/DatabaseHelper.java">ormlite-examples</a>
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    // Name of the database file for this application
    private static final String DATABASE_NAME = "croudtrip.db";

    // Any time changes are made to the database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 1;

    private Dao<Place, String> placeDAO = null;


    //*********************** Constructor ***********************//

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /**
     * This is called when the database is first created. Usually you should call createTable
     * statements here to create the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {

        try {
            Timber.i("onCreate");
            TableUtils.createTable(connectionSource, Place.class);
        } catch (SQLException e) {
            Timber.e("Can't create tables", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when the application is upgraded and it has a higher version number.
     * This allows you to adjust the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {

        try {
            Timber.i("onUpgrade");
            TableUtils.dropTable(connectionSource, Place.class, true);

            // After the old tables have been dropped, simply create the new ones
            onCreate(db, connectionSource);

        } catch (SQLException e) {
            Timber.e("Can't drop tables", e);
            throw new RuntimeException(e);
        }
    }

    public Dao<Place, String> getPlaceDao() throws SQLException{
        if (placeDAO == null) {
            placeDAO = getDao(Place.class);
        }
        return placeDAO;
    }

}
