package org.croudtrip.account;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class handles the user's default vehicle.
 * Created by Nazeeh Ammari on 20.05.15.
 */
public class VehicleManager {

    //******************************* Variables *******************************//

    private final static String SHARED_PREF_FILE_VEHICLE = "org.croudtrip.vehicle";


    private final static String SHARED_PREF_KEY_DEFAULT_VEHICLE_ID = "vehicle_id";


    //******************************* Methods ********************************//




    /**
     * This methods saves the default vehicle Id locally on the device.
     * @param context application context
     * @param Id the vehicle to save
     */
    public static void saveDefaultVehicle(Context context, long Id){

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_VEHICLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(SHARED_PREF_KEY_DEFAULT_VEHICLE_ID, Id);
        editor.apply();
    }





    /**
     * Returns the currently logged-in user. If no user is logged in, null is returned.
     * Every attribute not filled in by the user is null or -1 for numbers.
     * @param context application context
     * @return the currently logged-in user or null
     */
    public static long getDefaultVehicleId(Context context){

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_VEHICLE, Context.MODE_PRIVATE);
        long defaultVehicleId = prefs.getLong(SHARED_PREF_KEY_DEFAULT_VEHICLE_ID, -3);

        return defaultVehicleId;
    }









}
