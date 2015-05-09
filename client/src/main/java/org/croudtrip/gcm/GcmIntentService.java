package org.croudtrip.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.croudtrip.Constants;
import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.activities.MainActivity;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.fragments.JoinTripFragment;
import org.croudtrip.utils.LifecycleHandler;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * An intent server that handles the gcm messages comming from the server. It will create notifications
 * so that the user can react on new offered trips or open requests.
 * Created by Frederik Simon on 08.05.2015.
 */
public class GcmIntentService extends IntentService {

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
            default:
                break;
        }

        String dummyMessage  = intent.getExtras().getString(gcmMessageType);
        Timber.d("Server says " + dummyMessage);

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void handleDriverAccepted(Intent intent) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_FILE_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SHARED_PREF_KEY_SEARCHING, false);
        editor.putBoolean(Constants.SHARED_PREF_KEY_ACCEPTED, true);
        editor.apply();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.EVENT_DRIVER_ACCEPTED));
    }

    private void handleJoinRequest(Intent intent) {
        Timber.d("JOIN_REQUEST");

        final ObjectMapper mapper = new ObjectMapper();

        // create rest request handler
        TripsResource tripsResource = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.server_address))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        AccountManager.addAuthorizationHeader(getApplicationContext(), request);
                    }
                })
                .build()
                .create(TripsResource.class);

        // extract join request and offer from message
        long joinTripRequestId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID) );
        long offerId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID) );

        // download the join trip request
        tripsResource.getJoinRequest(offerId, joinTripRequestId ).observeOn( Schedulers.io() )
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<JoinTripRequest>() {
                            @Override
                            public void call(JoinTripRequest joinTripRequest) {
                                // create notification for the user
                                // TODO: Send the join trip request or at least the join trip request id
                                if (LifecycleHandler.isApplicationInForeground()) {
                                    // TODO: start request activity immediately
                                } else {
                                    Intent startingIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    startingIntent.setAction("HAVE_TO_INSERT_SOME_ACTION_STRING");
                                    try {
                                        startingIntent.putExtra("ANOTHER_STRING_THAT_CURRENTLY_NOT_EXISTS", mapper.writeValueAsString(joinTripRequest));
                                    } catch (JsonProcessingException e) {
                                        Timber.e(e.getMessage());
                                    }
                                    PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, startingIntent, 0);
                                    createNotification(getString(R.string.join_request_title), getString(R.string.joint_request_msg,
                                                    joinTripRequest.getQuery().getPassenger().getFirstName()),
                                            GcmConstants.GCM_NOTIFICATION_JOIN_REQUEST_ID, contentIntent);
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Timber.e("Something went wrong when downloading join request: " + throwable.getMessage());
                            }
                        });


        // TODO: This is just for testing. Will be done cleanly and combined with the UI and android notifications.
        // always accepting for testing
        JoinTripRequestUpdate requestUpdate = new JoinTripRequestUpdate( true );
        tripsResource.updateJoinRequest( offerId, joinTripRequestId, requestUpdate ).observeOn(Schedulers.io()).subscribeOn(Schedulers.newThread()).subscribe(new Action1<JoinTripRequest>() {
            @Override
            public void call(JoinTripRequest joinTripRequest) {

            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                // on main thread; something went wrong
                Timber.e("Error when trying to join a trip: " + throwable.getMessage());
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
}
