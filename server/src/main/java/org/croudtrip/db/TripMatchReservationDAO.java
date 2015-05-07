package org.croudtrip.db;


import org.croudtrip.api.trips.TripReservation;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class TripMatchReservationDAO extends AbstractDAO<TripReservation> {

	@Inject
	TripMatchReservationDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public List<TripReservation> findAll() {
		return list(namedQuery(TripReservation.QUERY_NAME_FIND_ALL));
	}

}
