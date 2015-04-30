package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.account.User;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.Route;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TripsManager {

	private final TripOfferDAO tripOfferDAO;
	private final DirectionsManager directionsManager;


	@Inject
	TripsManager(TripOfferDAO tripOfferDAO, DirectionsManager directionsManager) {
		this.tripOfferDAO = tripOfferDAO;
		this.directionsManager = directionsManager;
	}


	public TripOffer addOffer(User owner, TripOfferDescription description) throws Exception {
		List<Route> route = directionsManager.getDirections(description.getStart(), description.getEnd());
		if (route.size() == 0) throw new Exception("not route found");
		TripOffer offer = new TripOffer(0, route.get(0), description.getMaxDiversionInMeters(), owner);
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


	public List<TripMatch> findMatches(User passenger, TripRequestDescription requestDescription) {
		List<TripMatch> matches = new ArrayList<>();

		List<TripOffer> offers = findAllOffers();
		if (offers.size() == 0) return matches;

		// TODO don't return dummy match
		TripOffer offer = offers.get(0);
		Route route = offer.getRoute();
		TripMatch match = new TripMatch(0, route, route.getDistanceInMeters() + 100, route.getDurationInSeconds() + 100, offer.getDriver(), passenger);
		matches.add(match);
		return matches;
	}

}

