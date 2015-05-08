package org.croudtrip.gcm;

import android.content.BroadcastReceiver;
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
import rx.functions.Action1;
import timber.log.Timber;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, Intent intent) {
		// check for proper GCM message
		String messageType = GoogleCloudMessaging.getInstance(context).getMessageType(intent);
		if (!GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) return;

        String gcmMessageType = intent.getExtras().getString(GcmConstants.GCM_TYPE);
        Timber.d(gcmMessageType);
        switch(gcmMessageType)
        {
            case GcmConstants.GCM_MSG_DUMMY:
                break;
            case GcmConstants.GCM_MSG_JOIN_REQUEST:
                // TODO: This is just for testing. Will be cleanly combined with the UI.
                Timber.d("JOIN_REQUEST");
                long joinTripRequestId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_ID) );
                long offerId = Long.parseLong( intent.getExtras().getString(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID) );
                TripsResource tripsResource = new RestAdapter.Builder()
                                                            .setEndpoint(context.getString(R.string.server_address))
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
                tripsResource.updateJoinRequest( offerId, joinTripRequestId, requestUpdate ).subscribe( new Action1<JoinTripRequest>() {
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
		Toast.makeText(context, "Server says " + dummyMessage, Toast.LENGTH_SHORT).show();
	}
}
