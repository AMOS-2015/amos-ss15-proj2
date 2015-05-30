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
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.db.GcmRegistrationDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.Pair;

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

    /**
     * Sends a gcm message to a specific user passing one message type.
     * @param receiver the user that should receive the message
     * @param messageType the type of the message (see {@link org.croudtrip.api.gcm.GcmConstants})
     * @throws IOException if the connection to Google was not successful and msg could not be sen
     */
    public void sendGcmMessageToUser(User receiver, String messageType) {
        sendGcmMessageToUser(receiver, messageType, new Pair<String, String>("",""));
    }


    /**
     * Sends a gcm message to a specific user passing one message type that contains a certain message
     * @param receiver the user that should receive the message
     * @param messageType the type of the message (see {@link org.croudtrip.api.gcm.GcmConstants})
     * @param message the message that is sent itself
     */
    public void sendGcmMessageToUser(User receiver, String messageType, String message) {
        sendGcmMessageToUser(receiver, messageType, new Pair<String, String>(messageType, message));
    }

    /**
     * Sends a gcm message to a specific user passing a certain message type and multiple messages with it
     * @param receiver the user that should receive the messageu
     * @param messageType the type of the message (see {@link org.croudtrip.api.gcm.GcmConstants})
     * @param messageData multiple instances {@link org.croudtrip.utils.Pair} that contain several messages that should be sent with the gcm
     * @throws IOException IOException if the connection to Google was not successful and msg could not be sent
     * @throws java.lang.IllegalArgumentException if the receiver is not valid or not registered.
     */
    public void sendGcmMessageToUser(User receiver, String messageType, Pair<String, String>... messageData) {
        try {
			if (receiver == null) {
				logManager.e("SendToUser failed, because user is null");
				return;
				// throw new IllegalStateException("Receiver not specified");
			}

			GcmRegistration gcmRegistration = findRegistrationByUser(receiver).orNull();
			if (gcmRegistration == null) {
				logManager.e("User " + receiver.getId() + " (" + receiver.getFirstName() + " " + receiver.getLastName() + ") is not registered.");
				return;
				// throw new IllegalStateException("Receiver is not registered");
			}

			final List<String> devices = new ArrayList<>();
			devices.add(gcmRegistration.getGcmId());

			// send
			Message.Builder builder = new Message.Builder();
			builder.addData(GcmConstants.GCM_TYPE, messageType);

			for (Pair<String, String> p : messageData)
				builder.addData(p.getKey(), p.getValue());

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
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
    }


	public void sendDeclinePassengerMsg(JoinTripRequest request) {
		sendGcmMessageToUser(request.getQuery().getPassenger(), GcmConstants.GCM_MSG_REQUEST_DECLINED,
				new Pair<>(GcmConstants.GCM_MSG_REQUEST_DECLINED, "Your request was declined"),
				new Pair<>(GcmConstants.GCM_MSG_USER_MAIL, "" + request.getQuery().getPassenger().getEmail()),
				new Pair<>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + request.getId()),
				new Pair<>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + request.getOffer().getId()));
	}


	public void sendAcceptPassengerMsg(JoinTripRequest request) {
		sendGcmMessageToUser(request.getQuery().getPassenger(), GcmConstants.GCM_MSG_REQUEST_ACCEPTED,
				new Pair<>(GcmConstants.GCM_MSG_USER_MAIL, "" + request.getQuery().getPassenger().getEmail()),
				new Pair<>(GcmConstants.GCM_MSG_REQUEST_ACCEPTED, "Your request was accepted"),
				new Pair<>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + request.getId()),
				new Pair<>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + request.getOffer().getId()));
	}

}
