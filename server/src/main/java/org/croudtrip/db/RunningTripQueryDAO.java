package org.croudtrip.db;


import org.croudtrip.api.trips.RunningTripQuery;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class RunningTripQueryDAO extends AbstractDAO<RunningTripQuery> {

	@Inject
	RunningTripQueryDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public List<RunningTripQuery> findAll() {
		return list(namedQuery(RunningTripQuery.QUERY_NAME_FIND_ALL));
	}


	public List<RunningTripQuery> findByPassengerId(long passengerId) {
		return list(namedQuery(RunningTripQuery.QUERY_FIND_BY_PASSENGER_ID)
				.setParameter(RunningTripQuery.QUERY_PARAM_PASSENGER_ID, passengerId));
	}

}
