package org.croudtrip.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.croudtrip.DB_Dummy;

import java.sql.SQLException;

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

    // The DAO object we use to access the DB_Dummy table
    private Dao<DB_Dummy, Long> dummyDAO = null;

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
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, DB_Dummy.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create tables", e);
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
            Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            TableUtils.dropTable(connectionSource, DB_Dummy.class, true);

            // After the old tables have been dropped, simply create the new ones
            onCreate(db, connectionSource);

        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop tables", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Returns the Dao (Database Access Object) for the DB_Dummy class. It will create it or just
     * give the cached value.
     */
    public Dao<DB_Dummy, Long> getDB_DummyDao() throws SQLException{
        if (dummyDAO == null) {
            dummyDAO = getDao(DB_Dummy.class);
        }
        return dummyDAO;
    }


    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        dummyDAO = null;
    }
}
