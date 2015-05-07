package org.croudtrip.db;


import org.croudtrip.api.trips.TripMatchReservation;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class TripMatchReservationDAO extends AbstractDAO<TripMatchReservation> {

	@Inject
	TripMatchReservationDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public List<TripMatchReservation> findAll() {
		return list(namedQuery(TripMatchReservation.QUERY_NAME_FIND_ALL));
	}

}
