package org.croudtrip.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;
import org.croudtrip.utils.LifecycleHandler;

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

        // create rest request handler
        TripsResource tripsResource = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.server_address))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        AccountManager.addAuthorizationHeader(context, request);
                    }
                })
                .build()
                .create(TripsResource.class);

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
                Timber.d("JOIN_REQUEST");

                // extract join request and offer from message
                long joinTripRequestId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID) );
                long offerId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID) );

                // download the join trip request
                tripsResource.getJoinRequest(offerId, joinTripRequestId ).observeOn( Schedulers.io() )
                                        .subscribeOn( AndroidSchedulers.mainThread() )
                                        .subscribe(
                                                new Action1<JoinTripRequest>() {
                                                   @Override
                                                   public void call(JoinTripRequest joinTripRequest) {
                                                       // create notification for the user
                                                       // TODO: Send the join trip request or at least the join trip request id
                                                       if(LifecycleHandler.isApplicationInForeground()) {
                                                           // TODO: start request activity immediately
                                                       } else {
                                                           createNotification(getString(R.string.join_request_title), getString(R.string.joint_request_msg, joinTripRequest.getQuery().getPassenger().getFirstName()), GcmConstants.GCM_NOTIFICATION_JOIN_REQUEST_ID);
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

                break;
            default:
                break;
        }

        String dummyMessage  = intent.getExtras().getString(gcmMessageType);
        Timber.d("Server says " + dummyMessage);

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void createNotification( String title, String message, int notificationId ) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        //PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, .class), 0);

        Notification notification  =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_directions_car_white)
                        .setContentTitle( title )
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message)
                        .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify( notificationId, notification );
    }
}
