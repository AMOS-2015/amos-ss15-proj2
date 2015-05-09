package org.croudtrip.db;


import org.croudtrip.api.trips.TripOffer;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class TripOfferDAO extends AbstractDAO<TripOffer> {

	@Inject
	TripOfferDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public List<TripOffer> findAll() {
		return list(namedQuery(TripOffer.QUERY_NAME_FIND_ALL));
	}


	public List<TripOffer> findByDriverId(long driverId) {
		return list(namedQuery(TripOffer.QUERY_FIND_BY_DRIVER_ID)
				.setParameter(TripOffer.QUERY_PARAM_DRIVER_ID, driverId));
	}

}
