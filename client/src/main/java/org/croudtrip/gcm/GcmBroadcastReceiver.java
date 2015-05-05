package org.croudtrip.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReceiver extends BroadcastReceiver {

	private static final String EXTRA_DUMMY = "DUMMY";

	@Override
	public void onReceive(Context context, Intent intent) {
		// check for proper GCM message
		String messageType = GoogleCloudMessaging.getInstance(context).getMessageType(intent);
		if (!GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) return;

		String dummyMessage  = intent.getExtras().getString(EXTRA_DUMMY);
		Toast.makeText(context, "Server says " + dummyMessage, Toast.LENGTH_SHORT).show();
	}
}
