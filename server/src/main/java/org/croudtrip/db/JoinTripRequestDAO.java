package org.croudtrip.db;


import org.croudtrip.api.trips.JoinTripRequest;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class JoinTripRequestDAO extends AbstractDAO<JoinTripRequest> {

	@Inject
	JoinTripRequestDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public List<JoinTripRequest> findAll() {
		return list(namedQuery(JoinTripRequest.QUERY_NAME_FIND_ALL));
	}

}
