package org.croudtrip.gcm;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.gcm.GcmRegistration;
import org.croudtrip.api.gcm.GcmRegistrationDescription;
import org.croudtrip.db.GcmRegistrationDAO;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GcmManager {

	private final GcmRegistrationDAO registrationDAO;

	@Inject
	GcmManager(GcmRegistrationDAO registrationDAO) {
		this.registrationDAO = registrationDAO;
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


	public Optional<GcmRegistration> findRegistrationByUser(User user) {
		return registrationDAO.findByUserId(user.getId());
	}

}
