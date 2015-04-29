package org.croudtrip.db;


import com.google.common.base.Optional;

import org.croudtrip.trips.TripOffer;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

import io.dropwizard.hibernate.AbstractDAO;

public class TripOfferDAO extends AbstractDAO<TripOffer> {

	@Inject
	TripOfferDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public Optional<TripOffer> findById(long id) {
		return Optional.fromNullable(get(id));
	}


	public List<TripOffer> findAll() {
		return list(namedQuery(TripOffer.QUERY_NAME_FIND_ALL));
	}


	public void save(TripOffer offer) {
		currentSession().save(offer);
	}


	public void update(TripOffer offer) {
		currentSession().merge(offer);
	}


	public void delete(TripOffer offer) {
		currentSession().delete(offer);
	}

}
