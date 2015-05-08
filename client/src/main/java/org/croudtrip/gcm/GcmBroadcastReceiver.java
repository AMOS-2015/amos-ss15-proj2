package org.croudtrip.gcm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
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

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver{

	@Override
	public void onReceive(final Context context, Intent intent) {

        ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());

        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
	}
}
