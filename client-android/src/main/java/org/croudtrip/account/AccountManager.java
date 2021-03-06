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
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;

import org.croudtrip.activities.DispatchActivity;
import org.croudtrip.api.account.User;
import java.util.Date;

import retrofit.RequestInterceptor;
import timber.log.Timber;

/**
 * This class handles the user's account.
 * Created by Vanessa Lange on 30.04.15.
 */
public class AccountManager {

    //******************************* Variables *******************************//

    private final static String SHARED_PREF_FILE_USER = "org.croudtrip.user";

    private final static String SHARED_PREF_KEY_EMAIL = "email";
    private final static String SHARED_PREF_KEY_PWD = "password";
    private final static String SHARED_PREF_KEY_FIRSTNAME = "firstname";
    private final static String SHARED_PREF_KEY_LASTNAME = "lastname";
    private final static String SHARED_PREF_KEY_ADDRESS = "address";
    private final static String SHARED_PREF_KEY_PHONE = "phone";
    private final static String SHARED_PREF_KEY_BIRTHDAY = "birthday";
    private final static String SHARED_PREF_KEY_ID = "id";
    private final static String SHARED_PREF_KEY_MALE = "male";
    private final static String SHARED_PREF_KEY_AVATAR_URL = "avatar";
    private final static String SHARED_PREF_KEY_LAST_MODIFIED = "last_modified";


    //******************************* Methods ********************************//


    /**
     * Logs in the given user to be remembered after app restart. Call
     * {@link #getLoggedInUser(Context)} to receive the currently logged-in user.
     * @param context application context
     * @param user the user to log in
     * @param password the password the user uses to log in
     */
    public static void login(Context context, User user, String password){

        if(context == null || user == null || password == null){
            Timber.e("Invalid login parameters: context, user or password is null");
            return;
        }

        // Logout any previously logged-in user
        logout(context, false);

        saveUser(context, user, password);
    }


    /**
     * This methods saves the given user locally on the device. Note that
     * {@link #logout(Context, boolean)} removes all locally
     * stored data about the user's profile.
     * @param context application context
     * @param user the user to save
     * @param password The new password of the user. If null, the old password will be preserved.
     */
    public static void saveUser(Context context, User user, String password){

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Remember the user by storing his data in a SharedPreference file
        editor.putLong(SHARED_PREF_KEY_ID, user.getId());
        editor.putString(SHARED_PREF_KEY_EMAIL, user.getEmail());
        editor.putString(SHARED_PREF_KEY_ADDRESS, user.getAddress());
        editor.putString(SHARED_PREF_KEY_FIRSTNAME, user.getFirstName());
        editor.putString(SHARED_PREF_KEY_LASTNAME, user.getLastName());
        editor.putString(SHARED_PREF_KEY_PHONE, user.getPhoneNumber());
        editor.putString(SHARED_PREF_KEY_AVATAR_URL, user.getAvatarUrl());
        editor.putLong(SHARED_PREF_KEY_LAST_MODIFIED, user.getLastModified());

        if(user.getBirthday() != null) {
            editor.putLong(SHARED_PREF_KEY_BIRTHDAY, user.getBirthday().getTime());
        }else{
            editor.remove(SHARED_PREF_KEY_BIRTHDAY);
        }

        if(user.getIsMale() != null) {
            editor.putBoolean(SHARED_PREF_KEY_MALE, user.getIsMale());
        }else{
            editor.remove(SHARED_PREF_KEY_MALE);
        }

        // The password is not encrypted for the prototype app, since SharedPrefs are already private
        if(password != null) {
            editor.putString(SHARED_PREF_KEY_PWD, password);
        }

        editor.apply();
    }


     /**
     * Deletes any local authorization data of the currently logged-in user from the device.
     * Afterwards, the user can be regarded as "logged out". After being logged out, the user can be
     * sent to the login-screen.
     * @param context application context
     * @param redirect true if the user shall be redirected to the login screen automatically,
      *                otherwise false
     */
    public static void logout(Context context, boolean redirect){



        if(context == null){
            Timber.e("Invalid logout parameters: context is null");
            return;
        }

        // Remove any saved login data
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        //Revert default vehicle
        VehicleManager.saveDefaultVehicle(context, -3);

        if(redirect) {
            // Redirect to login-screen and delete any activities "before"
            Intent intent = new Intent(context, DispatchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


    /**
     * Returns the currently logged-in user. If no user is logged in, null is returned.
     * Every attribute not filled in by the user is null or -1 for numbers.
     * @param context application context
     * @return the currently logged-in user or null
     */
    public static User getLoggedInUser(Context context){

        // Create a User object from the info stored in the SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER, Context.MODE_PRIVATE);

        if(!prefs.contains(SHARED_PREF_KEY_PWD)){
            return null;
        }

        Boolean isMale = null;
        if(prefs.contains(SHARED_PREF_KEY_MALE)){
            isMale = prefs.getBoolean(SHARED_PREF_KEY_MALE, true);
        }

        Date birthday = null;
        if (prefs.contains(SHARED_PREF_KEY_BIRTHDAY)) {
            birthday = new Date(prefs.getLong(SHARED_PREF_KEY_BIRTHDAY, 0));
        }


        User user = new User(
                prefs.getLong(SHARED_PREF_KEY_ID, -1),
                prefs.getString(SHARED_PREF_KEY_EMAIL, null),
                prefs.getString(SHARED_PREF_KEY_FIRSTNAME, null),
                prefs.getString(SHARED_PREF_KEY_LASTNAME, null),
                prefs.getString(SHARED_PREF_KEY_PHONE, null),
                isMale,
                birthday,
                prefs.getString(SHARED_PREF_KEY_ADDRESS, null),
                prefs.getString(SHARED_PREF_KEY_AVATAR_URL, null),
                prefs.getLong(SHARED_PREF_KEY_LAST_MODIFIED, 0));
        return user;
    }


    /**
     * @param context application context
     * @return true if a user is currently logged in, otherwise false.
     */
    public static boolean isUserLoggedIn(Context context){
        return getLoggedInUser(context) != null;
    }


    /**
     * Adds the authorization header to a request to the server such that the server can authorize
     * the user.
     * @param context application context
     * @param request the request the header should be added to.
     * @return true if adding the header was successful, false otherwise (e.g. User is not logged in)
     */
    public static boolean addAuthorizationHeader(Context context, RequestInterceptor.RequestFacade request) {

        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREF_FILE_USER,
                Context.MODE_PRIVATE);
        String email = prefs.getString(SHARED_PREF_KEY_EMAIL, null);
        String password = prefs.getString(SHARED_PREF_KEY_PWD, null);

        return addAuthorizationHeader(email, password, request);
    }


    /**
     * Adds the authorization header to a request to the server such that the server can authorize
     * the user. Use {@link #addAuthorizationHeader(Context, RequestInterceptor.RequestFacade)
     * addAuthorizationHeader(Context, RequestInterceptor.RequestFacade)} if email or password are
     * unknown.
     * @param email email address of the user
     * @param password password of the user
     * @param request the request the header should be added to
     * @return true if adding the header was successful, false otherwise (e.g. User is not logged in)
     */
    public static boolean addAuthorizationHeader(String email, String password,
                                                 RequestInterceptor.RequestFacade request) {

        if ( email == null || password == null ) {
            return false;
        }

        // Put email and password in header
        String credentials = email + ":" + password;
        String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
        return true;
    }

}
