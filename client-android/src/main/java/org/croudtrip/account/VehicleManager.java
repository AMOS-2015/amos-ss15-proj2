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
