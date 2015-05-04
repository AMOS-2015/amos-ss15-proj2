package org.croudtrip.db;


import org.croudtrip.api.gcm.GcmRegistration;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class GcmRegistrationDAO extends AbstractDAO<GcmRegistration> {

	@Inject
	GcmRegistrationDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	public List<GcmRegistration> findAll() {
		return list(namedQuery(GcmRegistration.QUERY_NAME_FIND_ALL));
	}

}
