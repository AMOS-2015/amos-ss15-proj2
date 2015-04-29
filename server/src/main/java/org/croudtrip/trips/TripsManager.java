package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.db.TripOfferDAO;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TripsManager {

	private final TripOfferDAO tripOfferDAO;


	@Inject
	TripsManager(TripOfferDAO tripOfferDAO) {
		this.tripOfferDAO = tripOfferDAO;
	}


	public TripOffer addOffer(TripOfferDescription description) {
		TripOffer offer = new TripOffer(0, description.getStart(), description.getEnd(), description.getMaxDiversionInKm());
		tripOfferDAO.save(offer);
		return offer;
	}


	public List<TripOffer> findAllOffers() {
		return tripOfferDAO.findAll();
	}


	public Optional<TripOffer> findOffer(long offerId) {
		return tripOfferDAO.findById(offerId);
	}


	public void deleteOffer(TripOffer offer) {
		tripOfferDAO.delete(offer);
	}

}

