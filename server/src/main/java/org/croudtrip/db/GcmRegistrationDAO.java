package org.croudtrip.db;


import com.google.common.base.Optional;

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

	public Optional<GcmRegistration> findByUserId(long userId) {
		return Optional.fromNullable(
				uniqueResult(
						namedQuery(GcmRegistration.QUERY_NAME_FIND_BY_USER_ID)
								.setParameter(GcmRegistration.QUERY_PARAM_USER_ID, userId)));
	}

}
