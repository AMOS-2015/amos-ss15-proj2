package org.croudtrip.gcm;


import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.gcm.GcmRegistration;
import org.croudtrip.api.gcm.GcmRegistrationDescription;
import org.croudtrip.db.GcmRegistrationDAO;
import org.croudtrip.logs.LogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GcmManager {
	private final GcmRegistrationDAO registrationDAO;
	private final Sender sender;
	private final LogManager logManager;

	@Inject
	GcmManager(GcmRegistrationDAO registrationDAO, String googleApiKey, LogManager logManager) {
		this.registrationDAO = registrationDAO;
		this.sender = new Sender(googleApiKey);
		this.logManager = logManager;
	}


	public GcmRegistration register(User user, GcmRegistrationDescription registrationDescription) {
		Optional<GcmRegistration> oldRegistration = registrationDAO.findByUserId(user.getId());

		if (oldRegistration.isPresent()) {
			// update previous registration
			GcmRegistration newRegistration = new GcmRegistration(oldRegistration.get().getId(), registrationDescription.getGcmId(), user);
			registrationDAO.update(newRegistration);
			return newRegistration;

		} else {
			// create new registration
			GcmRegistration newRegistration = new GcmRegistration(0, registrationDescription.getGcmId(), user);
			registrationDAO.save(newRegistration);
			return newRegistration;
		}
	}


	public void unregister(GcmRegistration registration) {
		registrationDAO.delete(registration);
	}


	public Optional<GcmRegistration> findRegistrationByUser(User user) {
		return registrationDAO.findByUserId(user.getId());
	}


	public void sendGcmMessage(GcmRegistration gcmRegistration, String message) throws IOException {
		final List<String> devices = new ArrayList<>();
		devices.add(gcmRegistration.getGcmId());

		// send
		Message.Builder builder = new Message.Builder();
		builder.addData(GcmConstants.GCM_MSG_DUMMY, message);

		MulticastResult multicastResult = sender.send(builder.build(), devices, 5);

		// analyze the results
		List<Result> results = multicastResult.getResults();
		for (int i = 0; i < devices.size(); i++) {
			String gcmId = devices.get(i);
			Result result = results.get(i);
			String messageId = result.getMessageId();
			if (messageId != null) {
				logManager.d("send msg to " + gcmId + " with msg id " + messageId);
				String canonicalRegId = result.getCanonicalRegistrationId();

				// update gcm id
				if (canonicalRegId != null) {
					logManager.i("updating gcmId from " + gcmId + " to " + canonicalRegId);
					register(gcmRegistration.getUser(), new GcmRegistrationDescription(canonicalRegId));
				}

			} else {
				String error = result.getErrorCodeName();
				if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
					// application has been removed from device - unregister it
					unregister(gcmRegistration);

				} else {
					// unknown error
					logManager.e("unknown gcm error " + error);
				}
			}
		}
	}

    public void sendGcmMessageToUser(User receiver, String messageType, String message) throws IOException {
        if( receiver == null ) {
            logManager.e("SendToUser failed, because user is null");
            return;
        }

        GcmRegistration gcmRegistration = findRegistrationByUser(receiver).orNull();
        if( gcmRegistration == null ) {
            logManager.e("User " + receiver.getId() + " (" + receiver.getFirstName() + " " + receiver.getLastName() + ") is not registered.");
            return;
        }

        final List<String> devices = new ArrayList<>();
        devices.add(gcmRegistration.getGcmId());

        // send
        Message.Builder builder = new Message.Builder();
        builder.addData( GcmConstants.GCM_TYPE, messageType );
        builder.addData(messageType, message);

        MulticastResult multicastResult = sender.send(builder.build(), devices, 5);

        // analyze the results
        List<Result> results = multicastResult.getResults();
        for (int i = 0; i < devices.size(); i++) {
            String gcmId = devices.get(i);
            Result result = results.get(i);
            String messageId = result.getMessageId();
            if (messageId != null) {
                logManager.d("send msg to " + gcmId + " with msg id " + messageId);
                String canonicalRegId = result.getCanonicalRegistrationId();

                // update gcm id
                if (canonicalRegId != null) {
                    logManager.i("updating gcmId from " + gcmId + " to " + canonicalRegId);
                    register(gcmRegistration.getUser(), new GcmRegistrationDescription(canonicalRegId));
                }

            } else {
                String error = result.getErrorCodeName();
                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    // application has been removed from device - unregister it
                    unregister(gcmRegistration);

                } else {
                    // unknown error
                    logManager.e("unknown gcm error " + error);
                }
            }
        }
    }

}
