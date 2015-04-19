package org.croudtrip.db;


import com.google.common.base.Optional;

import org.croudtrip.auth.BasicCredentials;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

import io.dropwizard.hibernate.AbstractDAO;

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


	public void save(BasicCredentials credentials) {
		currentSession().save(credentials);
	}


	public void delete(BasicCredentials credentials) {
		currentSession().delete(credentials);
	}

}
