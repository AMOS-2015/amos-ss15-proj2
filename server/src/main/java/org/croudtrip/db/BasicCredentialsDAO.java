package org.croudtrip.db;


import com.google.common.base.Optional;

import org.croudtrip.auth.BasicCredentials;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

public class BasicCredentialsDAO extends AbstractDAO<BasicCredentials> {

	@Inject
	BasicCredentialsDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public Optional<BasicCredentials> findByUserId(long userId) {
		return Optional.fromNullable(
				uniqueResult(
						namedQuery(BasicCredentials.QUERY_NAME_FIND_BY_USER_ID)
								.setParameter(BasicCredentials.QUERY_PARAM_USER_ID, userId)));
	}

}
