package org.croudtrip.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripRequestUpdate;

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
                // TODO: This is just for testing. Will be done cleanly and combined with the UI and android notifications.
                Timber.d("JOIN_REQUEST");
                long joinTripRequestId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID) );
                long offerId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID) );
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

                // always accepting for testing
                JoinTripRequestUpdate requestUpdate = new JoinTripRequestUpdate( true );

                Timber.d("JOIN REQUEST SERVER CALL");
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
}
