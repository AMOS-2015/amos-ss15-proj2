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

package org.croudtrip.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.activities.MainActivity;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.fragments.JoinTripResultsFragment;
import org.croudtrip.fragments.join.JoinDispatchFragment;
import org.croudtrip.utils.LifecycleHandler;

import javax.inject.Inject;

import roboguice.service.RoboIntentService;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * An intent server that handles the gcm messages comming from the server. It will create notifications
 * so that the user can react on new offered trips or open requests.
 * Created by Frederik Simon on 08.05.2015.
 */
public class GcmIntentService extends RoboIntentService {

    @Inject TripsResource tripsResource;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final Context context = getApplicationContext();

        // check for proper GCM message
        String messageType = GoogleCloudMessaging.getInstance(context).getMessageType(intent);
        if (!GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) return;

        // get the type of message and handle it properly
        String gcmMessageType = intent.getExtras().getString(GcmConstants.GCM_TYPE);
        Timber.d(gcmMessageType);


        switch(gcmMessageType)
        {
            case GcmConstants.GCM_MSG_DUMMY:
                break;
            case GcmConstants.GCM_MSG_JOIN_REQUEST:
                handleJoinRequest(intent);
                break;
            case GcmConstants.GCM_MSG_REQUEST_ACCEPTED:
                handleRequestAccepted( intent );
                break;
            case GcmConstants.GCM_MSG_REQUEST_DECLINED:
                handleRequestDeclined(intent);
                break;
            case GcmConstants.GCM_MSG_FOUND_MATCHES:
                handleFoundMatches(intent);
                break;
            default:
                break;
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void handleFoundMatches( Intent intent ) {
        Timber.d("FOUND_MATCHES");

        // extract join request and offer from message
        long queryId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_FOUND_MATCHES_QUERY_ID) );

        // download the join trip request
        tripsResource.getQuery(queryId)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<RunningTripQuery>() {
                            @Override
                            public void call(RunningTripQuery query) {

                                /*OLD
                                Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);

                                // fill the arguments for the started fragment (main activity will dispatch to correct fragment) with information about the requested search
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_ACTION_TO_RUN, JoinTripResultsFragment.ACTION_START_BACKGROUND_SEARCH);
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LATITUDE, query.getQuery().getStartLocation().getLat());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LONGITUDE, query.getQuery().getStartLocation().getLng());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_DESTINATION_LATITUDE, query.getQuery().getDestinationLocation().getLat());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_DESTINATION_LONGITUDE, query.getQuery().getDestinationLocation().getLng());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_MAX_WAITING_TIME, query.getQuery().getMaxWaitingTimeInSeconds());

                                // set the action for the main activity that helps to decide which fragment has to be started
                                // If you want to do something based on shared prefs you probably want to change this part.
                                startingIntent.setAction(MainActivity.ACTION_SHOW_FOUND_MATCHES);

                                // create notification for the user
                                PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, 0);
                                createNotification(getString(R.string.found_matches_title), getString(R.string.found_matches_msg),
                                        GcmConstants.GCM_NOTIFICATION_FOUND_MATCHES_ID, contentIntent);

                                handleDriversFound(query);*/

                                //NEW
                                final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
                                editor.apply();

                                Bundle extras = new Bundle();
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LATITUDE, query.getQuery().getStartLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LONGITUDE, query.getQuery().getStartLocation().getLng());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LATITUDE, query.getQuery().getDestinationLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LONGITUDE, query.getQuery().getDestinationLocation().getLng());
                                extras.putInt(JoinDispatchFragment.KEY_MAX_WAITING_TIME, (int) query.getQuery().getMaxWaitingTimeInSeconds());

                                if(LifecycleHandler.isApplicationInForeground()) {
                                    Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                    startingIntent.putExtras(extras);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                } else {
                                    // create notification for the user
                                    Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    startingIntent.putExtras(extras);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    createNotification(getString(R.string.found_matches_title), getString(R.string.found_matches_msg),
                                            GcmConstants.GCM_NOTIFICATION_FOUND_MATCHES_ID , contentIntent);
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Something went wrong when downloading join request: " + throwable.getMessage());
                            }
                        });
    }

    private void handleRequestDeclined(Intent intent) {
        Timber.d("REQUEST_DECLINED");

        // extract join request and offer from message
        long joinTripRequestId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID) );
        long offerId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID) );

        // download the join trip request
        tripsResource.getJoinRequest(joinTripRequestId )
                .observeOn( Schedulers.io() )
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<JoinTripRequest>() {
                            @Override
                            public void call(JoinTripRequest joinTripRequest) {

                                /*
                                OLD

                                Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);

                                // fill the arguments for the started fragment (main activity will dispatch to correct fragment) with information about the requested search
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_ACTION_TO_RUN, JoinTripResultsFragment.ACTION_START_BACKGROUND_SEARCH);
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LATITUDE, joinTripRequest.getQuery().getStartLocation().getLat());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LONGITUDE, joinTripRequest.getQuery().getStartLocation().getLng());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_DESTINATION_LATITUDE, joinTripRequest.getQuery().getDestinationLocation().getLat());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_DESTINATION_LONGITUDE, joinTripRequest.getQuery().getDestinationLocation().getLng());
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_MAX_WAITING_TIME, joinTripRequest.getQuery().getMaxWaitingTimeInSeconds());

                                // set the action for the main activity that helps to decide which fragment has to be started
                                // TODO-Alexander: If you want to do something based on shared prefs you probably want to change this part.
                                startingIntent.setAction(MainActivity.ACTION_SHOW_REQUEST_DECLINED);

                                // create notification for the user
                                PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                createNotification(getString(R.string.join_request_declined_title), getString(R.string.join_request_declined_msg),
                                        GcmConstants.GCM_NOTIFICATION_REQUEST_DECLINED_ID, contentIntent);*/


                                //NEW
                                final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
                                editor.apply();

                                Bundle extras = new Bundle();
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LATITUDE, joinTripRequest.getQuery().getStartLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LONGITUDE, joinTripRequest.getQuery().getStartLocation().getLng());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LATITUDE, joinTripRequest.getQuery().getDestinationLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LONGITUDE, joinTripRequest.getQuery().getDestinationLocation().getLng());
                                extras.putInt(JoinDispatchFragment.KEY_MAX_WAITING_TIME, (int) joinTripRequest.getQuery().getMaxWaitingTimeInSeconds());

                                if(LifecycleHandler.isApplicationInForeground()) {
                                    Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                    startingIntent.putExtras(extras);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                } else {
                                    // create notification for the user
                                    Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    startingIntent.putExtras(extras);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    createNotification(getString(R.string.join_request_declined_title), getString(R.string.join_request_declined_msg),
                                            GcmConstants.GCM_NOTIFICATION_REQUEST_DECLINED_ID, contentIntent);
                                }


                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Something went wrong when downloading join request: " + throwable.getMessage());
                            }
                        });
    }

    private void handleRequestAccepted( Intent intent ) {
        Timber.d("REQUEST_ACCEPTED");

        // extract join request and offer from message
        long joinTripRequestId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID));
        long offerId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID));

        // download the join trip request
        tripsResource.getJoinRequest(joinTripRequestId)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<JoinTripRequest>() {
                            @Override
                            public void call(JoinTripRequest joinTripRequest) {

                                /* OLD
                                // create notification for the user
                                Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                startingIntent.setAction(MainActivity.ACTION_SHOW_REQUEST_ACCEPTED);
                                ObjectMapper mapper = new ObjectMapper();
                                startingIntent.putExtra(JoinTripResultsFragment.KEY_ACTION_TO_RUN, JoinTripResultsFragment.ACTION_SHOW_RESULT);

                                // We put the downloaded joinTripRequest as an argument, so that we do not have to download it again
                                try {
                                    startingIntent.putExtra( JoinTripResultsFragment.KEY_JOIN_TRIP_REQUEST_RESULT, mapper.writeValueAsString(joinTripRequest) );
                                } catch (JsonProcessingException e) {
                                    Timber.e("Could not map join trip result");
                                    e.printStackTrace();
                                }

                                PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                createNotification(getString(R.string.join_request_accepted_title), getString(R.string.join_request_accepted_msg,
                                                joinTripRequest.getOffer().getDriver().getFirstName()),
                                        GcmConstants.GCM_NOTIFICATION_REQUEST_ACCEPTED_ID, contentIntent);

                                handleDriverAccepted(joinTripRequest);*/


                                //NEW
                                final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, true);
                                editor.apply();

                                Bundle extras = new Bundle();
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                    extras.putString(JoinDispatchFragment.KEY_JOIN_TRIP_REQUEST_RESULT, mapper.writeValueAsString(joinTripRequest));
                                } catch (JsonProcessingException e) {
                                    Timber.e("Could not map join trip result");
                                    e.printStackTrace();
                                }

                                if(LifecycleHandler.isApplicationInForeground()) {
                                    Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                    startingIntent.putExtras(extras);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                } else {
                                    // create notification for the user
                                    Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    startingIntent.putExtras(extras);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    createNotification(getString(R.string.join_request_accepted_title), getString(R.string.join_request_accepted_msg,
                                                    joinTripRequest.getOffer().getDriver().getFirstName()),
                                            GcmConstants.GCM_NOTIFICATION_REQUEST_ACCEPTED_ID, contentIntent);
                                }

                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Something went wrong when downloading join request: " + throwable.getMessage());
                            }
                        });
    }

    private void handleJoinRequest(Intent intent) {
        Timber.d("JOIN_REQUEST");

        // extract join request and offer from message
        long joinTripRequestId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID) );

        // download the join trip request
        tripsResource.getJoinRequest(joinTripRequestId).observeOn( Schedulers.io() )
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<JoinTripRequest>() {
                            @Override
                            public void call(JoinTripRequest joinTripRequest) {
                                // create notification for the user

                                Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                startingIntent.setAction(MainActivity.ACTION_SHOW_JOIN_TRIP_REQUESTS);

                                PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                createNotification(getString(R.string.join_request_title), getString(R.string.joint_request_msg,
                                                joinTripRequest.getQuery().getPassenger().getFirstName()),
                                        GcmConstants.GCM_NOTIFICATION_JOIN_REQUEST_ID, contentIntent);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Something went wrong when downloading join request: " + throwable.getMessage());
                            }
                        });
    }

    private void createNotification( String title, String message, int notificationId ) {
        createNotification(title, message, notificationId, null);
    }

    private void createNotification( String title, String message, int notificationId, PendingIntent contentIntent ) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification  =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_directions_car_white)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message)
                        .setContentIntent(contentIntent)
                        .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify( notificationId, notification );
    }

    /*
    Should be called if the driver accepted this passenger.
     */
    private void handleDriverAccepted( JoinTripRequest request ) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
        editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, true);
        editor.apply();

        if( LifecycleHandler.isApplicationInForeground() ) {
            Intent startingIntent = new Intent(Constants.EVENT_DRIVER_ACCEPTED);
            ObjectMapper mapper = new ObjectMapper();
            startingIntent.putExtra(JoinTripResultsFragment.KEY_ACTION_TO_RUN, JoinTripResultsFragment.ACTION_SHOW_RESULT);
            try {
                startingIntent.putExtra( JoinTripResultsFragment.KEY_JOIN_TRIP_REQUEST_RESULT, mapper.writeValueAsString(request) );
            } catch (JsonProcessingException e) {
                Timber.e("Could not map join trip result");
                e.printStackTrace();
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(startingIntent);
        }
    }

    /*
    Should be called if the background search for "join trips" found something.
     */
    private void handleDriversFound( RunningTripQuery query ) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
        editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false);
        editor.putLong(Constants.SHARED_PREF_KEY_QUERY_ID, -1);
        editor.apply();

        if( LifecycleHandler.isApplicationInForeground() ) {

            Intent startingIntent = new Intent(Constants.EVENT_DRIVER_ACCEPTED);

            // fill the arguments for the started fragment (main activity will dispatch to correct fragment) with information about the requested search
            // adding these arguments we can start the query immediately
            startingIntent.putExtra(JoinTripResultsFragment.KEY_ACTION_TO_RUN, JoinTripResultsFragment.ACTION_START_BACKGROUND_SEARCH);
            startingIntent.putExtra(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LATITUDE, query.getQuery().getStartLocation().getLat());
            startingIntent.putExtra(JoinTripResultsFragment.KEY_CURRENT_LOCATION_LONGITUDE, query.getQuery().getStartLocation().getLng());
            startingIntent.putExtra(JoinTripResultsFragment.KEY_DESTINATION_LATITUDE, query.getQuery().getDestinationLocation().getLat());
            startingIntent.putExtra(JoinTripResultsFragment.KEY_DESTINATION_LONGITUDE, query.getQuery().getDestinationLocation().getLng());
            startingIntent.putExtra(JoinTripResultsFragment.KEY_MAX_WAITING_TIME, query.getQuery().getMaxWaitingTimeInSeconds());

            LocalBroadcastManager.getInstance(this).sendBroadcast(startingIntent);
        }
    }
}
