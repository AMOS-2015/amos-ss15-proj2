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
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.SuperTrip;
import org.croudtrip.api.trips.TripQuery;
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

    @Inject
    TripsResource tripsResource;

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


        switch (gcmMessageType) {

            case GcmConstants.GCM_MSG_REQUEST_EXPIRED:
                handleJoinRequestExpired();
                break;
            case GcmConstants.GCM_MSG_DUMMY:
                break;
            case GcmConstants.GCM_MSG_JOIN_REQUEST:
                handleJoinRequest(intent);
                break;
            case GcmConstants.GCM_MSG_REQUEST_ACCEPTED:
                handleRequestAccepted(intent);
                break;
            case GcmConstants.GCM_MSG_REQUEST_DECLINED:
                handleRequestDeclined(intent);
                break;
            case GcmConstants.GCM_MSG_FOUND_MATCHES:
                handleFoundMatches(intent);
                break;
            case GcmConstants.GCM_MESSAGE_TRIP_CANCELLED_BY_DRIVER:
                handleTripCanceledByDriver();
                break;
            case GcmConstants.GCM_MESSAGE_TRIP_CANCELLED_BY_PASSENGER:
                handleTripCanceledByPassenger();
                break;
            case GcmConstants.GCM_MSG_PASSENGER_AT_DESTINATION:
                handlePassengerAtDestination();
                break;
            case GcmConstants.GCM_MSG_PASSENGER_ENTERED_CAR:
                handlePassengerEnteredCar();
                break;
            case GcmConstants.GCM_MSG_ARRIVAL_TIME_UPDATE:
                handleArrivalTimeUpdate(intent);
            default:
                break;
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void handleArrivalTimeUpdate(Intent intent) {
        Timber.d("ARRIVAL_TIME_UPDATE");

        // extract join request and offer from message
        final long joinTripRequestId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID));

        // download the join trip request
        tripsResource.getJoinRequest(joinTripRequestId)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<JoinTripRequest>() {
                            @Override
                            public void call(JoinTripRequest joinTripRequest) {

                                final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, true);
                                editor.putBoolean(Constants.SHARED_PREF_KEY_WAITING, false);
                                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
                                editor.putLong(Constants.SHARED_PREF_KEY_TRIP_ID, joinTripRequest.getId());
                                editor.apply();

                                Bundle extras = new Bundle();
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                    extras.putString(JoinDispatchFragment.KEY_JOIN_TRIP_REQUEST_RESULT, mapper.writeValueAsString(joinTripRequest));
                                } catch (JsonProcessingException e) {
                                    Timber.e("Could not map join trip result");
                                    e.printStackTrace();
                                }

                                if (LifecycleHandler.isApplicationInForeground()) {
                                    Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                    startingIntent.putExtras(extras);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
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

    private void handlePassengerAtDestination() {
        if (LifecycleHandler.isApplicationInForeground()) {
            // send broadcast while the app is running to reload the application
            Intent startingIntent = new Intent(Constants.EVENT_PASSENGER_REACHED_DESTINATION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(startingIntent);
        }
    }

    private void handlePassengerEnteredCar() {
        if (LifecycleHandler.isApplicationInForeground()) {
            // send broadcast while the app is running to reload the application
            Intent startingIntent = new Intent(Constants.EVENT_PASSENGER_ENTERED_CAR);
            LocalBroadcastManager.getInstance(this).sendBroadcast(startingIntent);
        }
    }

    private void handleFoundMatches(Intent intent) {
        Timber.d("FOUND_MATCHES");

        // extract join request and offer from message
        long queryId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_FOUND_MATCHES_QUERY_ID));

        // download the join trip request
        tripsResource.getQuery(queryId)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<RunningTripQuery>() {
                            @Override
                            public void call(RunningTripQuery query) {

                                final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
                                editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, false);
                                editor.putLong(Constants.SHARED_PREF_KEY_QUERY_ID, -1);
                                editor.apply();

                                Bundle extras = new Bundle();
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LATITUDE, query.getQuery().getStartLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LONGITUDE, query.getQuery().getStartLocation().getLng());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LATITUDE, query.getQuery().getDestinationLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LONGITUDE, query.getQuery().getDestinationLocation().getLng());
                                extras.putInt(JoinDispatchFragment.KEY_MAX_WAITING_TIME, (int) query.getQuery().getMaxWaitingTimeInSeconds());

                                if (LifecycleHandler.isApplicationInForeground()) {
                                    Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                    startingIntent.putExtras(extras);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                } else {
                                    // create notification for the user
                                    Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    startingIntent.putExtras(extras);
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    createNotification(getString(R.string.found_matches_title), getString(R.string.found_matches_msg),
                                            GcmConstants.GCM_NOTIFICATION_FOUND_MATCHES_ID, contentIntent);
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

                                //Check the two starting positions. If they are the same, this was the declined message from the first driver
                                RouteLocation r1 = joinTripRequest.getSuperTrip().getQuery().getStartLocation();
                                RouteLocation r2 = joinTripRequest.getSubQuery().getStartLocation();
                                boolean firstDriver = r1.equals(r2);

                                //save the canceled waiting status only if the first driver canceled
                                if (firstDriver) {
                                    final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, true);
                                    editor.putBoolean(Constants.SHARED_PREF_KEY_WAITING, false);
                                    editor.apply();

                                    tripsResource.cancelSuperTrip(joinTripRequest.getSuperTrip().getId())
                                            .observeOn(Schedulers.io())
                                            .subscribeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new Action1<SuperTrip>() {
                                                           @Override
                                                           public void call(SuperTrip superTrip) {
                                                               Timber.d("Cancelled your super trip");
                                                           }
                                                       },
                                                    new Action1<Throwable>() {
                                                        @Override
                                                        public void call(Throwable throwable) {
                                                            Timber.e("Could not cancel your trip: " + throwable.getMessage());
                                                        }
                                                    });


                                }


                                Bundle extras = new Bundle();
                                TripQuery query = joinTripRequest.getSuperTrip().getQuery();
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LATITUDE, query.getStartLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_CURRENT_LOCATION_LONGITUDE, query.getStartLocation().getLng());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LATITUDE, query.getDestinationLocation().getLat());
                                extras.putDouble(JoinDispatchFragment.KEY_DESTINATION_LONGITUDE, query.getDestinationLocation().getLng());
                                extras.putInt(JoinDispatchFragment.KEY_MAX_WAITING_TIME, (int) query.getMaxWaitingTimeInSeconds());

                                if (LifecycleHandler.isApplicationInForeground()) {
                                    //go back to search UI only if the first driver canceled
                                    if (firstDriver) {
                                        //Toast.makeText(getApplicationContext(), getString(R.string.join_request_declined_msg), Toast.LENGTH_SHORT).show();
                                        Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                        startingIntent.putExtras(extras);
                                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                    } else {
                                        Intent startingIntent = new Intent(Constants.EVENT_SECONDARY_DRIVER_DECLINED);
                                        startingIntent.putExtras(extras);
                                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                    }
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

    private void handleRequestAccepted(Intent intent) {
        Timber.d("REQUEST_ACCEPTED");

        // extract join request and offer from message
        final long joinTripRequestId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID));
        long offerId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID));

        // download the join trip request
        tripsResource.getJoinRequest(joinTripRequestId)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<JoinTripRequest>() {
                            @Override
                            public void call(JoinTripRequest joinTripRequest) {

                                //Check the two starting positions. If they are the same, this was the declined message from the first driver
                                RouteLocation r1 = joinTripRequest.getSuperTrip().getQuery().getStartLocation();
                                RouteLocation r2 = joinTripRequest.getSubQuery().getStartLocation();
                                boolean firstDriver = r1.equals(r2);

                                //save the accepted status only if the first driver accepted
                                if (firstDriver) {
                                    final SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, true);
                                    editor.putBoolean(Constants.SHARED_PREF_KEY_WAITING, false);
                                    editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
                                    editor.putLong(Constants.SHARED_PREF_KEY_TRIP_ID, joinTripRequest.getId());
                                    editor.apply();
                                }


                                Bundle extras = new Bundle();
                                ObjectMapper mapper = new ObjectMapper();
                                try {
                                    extras.putString(JoinDispatchFragment.KEY_JOIN_TRIP_REQUEST_RESULT, mapper.writeValueAsString(joinTripRequest));
                                } catch (JsonProcessingException e) {
                                    Timber.e("Could not map join trip result");
                                    e.printStackTrace();
                                }


                                if (LifecycleHandler.isApplicationInForeground()) {
                                    if (firstDriver) {
                                        Intent startingIntent = new Intent(Constants.EVENT_CHANGE_JOIN_UI);
                                        startingIntent.putExtras(extras);
                                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                    } else {
                                        Intent startingIntent = new Intent(Constants.EVENT_SECONDARY_DRIVER_ACCEPTED);
                                        startingIntent.putExtras(extras);
                                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);
                                    }
                                } else {
                                    // create notification for the user only if the first driver accepts
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

        if (LifecycleHandler.isApplicationInForeground()) {
            // Have screen update itself
            Intent startingIntent = new Intent(Constants.EVENT_NEW_JOIN_REQUEST);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(startingIntent);

        } else {
            // Show notification
            // extract join request and offer from message
            long joinTripRequestId = Long.parseLong(intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID));

            // download the join trip request
            tripsResource.getJoinRequest(joinTripRequestId).observeOn(Schedulers.io())
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
                                                    joinTripRequest.getSuperTrip().getQuery().getPassenger().getFirstName()),
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
    }

    private void handleTripCanceledByDriver() {
        Timber.d("Trip Canceled by Driver");

        //Create a notification for the passengers who already joined the trip
        Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
        startingIntent.setAction(MainActivity.ACTION_SHOW_JOIN_TRIP_REQUESTS);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        createNotification(getString(R.string.new_msg), getString(R.string.trip_canceled_msg),
                GcmConstants.GCM_NOTIFICATION_TRIP_CANCELLED_ID, contentIntent);

    }

    private void handleTripCanceledByPassenger() {
        Timber.d("Trip Canceled by passenger");

        if (LifecycleHandler.isApplicationInForeground()) {
            // send broadcast while the app is running to reload the application
            Intent startingIntent = new Intent(Constants.EVENT_PASSENGER_CANCELLED_TRIP);
            LocalBroadcastManager.getInstance(this).sendBroadcast(startingIntent);
        } else {
            // create notification if the application is not in foreground
            Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
            startingIntent.setAction(MainActivity.ACTION_SHOW_JOIN_TRIP_REQUESTS);

            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            createNotification(getString(R.string.new_msg), getString(R.string.trip_canceled_by_passenger),
                    GcmConstants.GCM_NOTIFICATION_TRIP_CANCELLED_ID, contentIntent);
        }

    }


    private void createNotification(String title, String message, int notificationId) {
        createNotification(title, message, notificationId, null);
    }

    private void createNotification(String title, String message, int notificationId, PendingIntent contentIntent) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_directions_car_white)
                        .setContentTitle(title)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message)
                        .setContentIntent(contentIntent)
                        .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(notificationId, notification);
    }

    public void handleJoinRequestExpired() {
        Timber.d("Request Expired");
        if (LifecycleHandler.isApplicationInForeground()) {
            Timber.d("Request Expired and Broadcast was sent to LocalBroadcastManager");
            Intent startingIntent = new Intent(Constants.EVENT_JOIN_REQUEST_EXPIRED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(startingIntent);
        } else
            Timber.d("Request Expired but Broadcast was not sent to LocalBroadcastManager, application not in Foreground");
    }

}
