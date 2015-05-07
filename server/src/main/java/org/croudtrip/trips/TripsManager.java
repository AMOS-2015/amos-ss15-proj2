package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.db.TripMatchReservationDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TripsManager {

	private final TripOfferDAO tripOfferDAO;
    private final TripMatchReservationDAO tripMatchReservationDAO;
	private final DirectionsManager directionsManager;


	@Inject
	TripsManager(TripOfferDAO tripOfferDAO, TripMatchReservationDAO tripMatchReservationDAO, DirectionsManager directionsManager) {
		this.tripOfferDAO = tripOfferDAO;
        this.tripMatchReservationDAO = tripMatchReservationDAO;
		this.directionsManager = directionsManager;
	}


	public TripOffer addOffer(User owner, TripOfferDescription description) throws Exception {
		List<Route> route = directionsManager.getDirections(description.getStart(), description.getEnd());
		if (route.size() == 0) throw new Exception("not route found");
		TripOffer offer = new TripOffer(0, route.get(0), description.getMaxDiversionInMeters(), description.getPricePerKmInCents(), owner);
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


	public List<TripReservation> createReservations(User passenger, TripQueryDescription queryDescription) throws Exception {

        // compute passenger route
        List<Route> possiblePassengerRoutes = directionsManager.getDirections(queryDescription.getStart(), queryDescription.getEnd());
        if (possiblePassengerRoutes.isEmpty()) return new ArrayList<>();
        TripQuery query = new TripQuery(possiblePassengerRoutes.get(0), queryDescription.getMaxWaitingTime(), passenger);

        // analyse offers
        List<TripOffer> potentialMatches = new ArrayList<>();
        for (TripOffer offer : findAllOffers()) {
            Optional<TripOffer> potentialMatch = analyzeOffer(offer, query);
            if (potentialMatch.isPresent()) potentialMatches.add(potentialMatch.get());
        }

        // find and store reservations
        List<TripReservation> reservations = findCheapestMatch(query, potentialMatches);
        for (TripReservation reservation : reservations) tripMatchReservationDAO.save(reservation);

        return reservations;
	}


    public List<TripReservation> findAllReservations() {
        return tripMatchReservationDAO.findAll();
    }


    private Optional<TripOffer> analyzeOffer(TripOffer offer, TripQuery query) throws Exception {
        // compute total driver route
        List<RouteLocation> wayPoints = new ArrayList<>();
        wayPoints.add(query.getRoute().getStart());
        wayPoints.add(query.getRoute().getEnd());
        List<Route> possibleDriverRoutes = directionsManager.getDirections(
                offer.getRoute().getStart(),
                offer.getRoute().getEnd(),
                wayPoints);

        if (possibleDriverRoutes == null || possibleDriverRoutes.isEmpty()) return Optional.absent();

        // check is passenger route is within max diversion
        Route driverRoute = possibleDriverRoutes.get(0);
        if (driverRoute.getDistanceInMeters() - query.getRoute().getDistanceInMeters() < offer.getMaxDiversionInMeters()) {
            return Optional.of(offer);
        } else {
            return Optional.absent();
        }
    }


    private List<TripReservation> findCheapestMatch(TripQuery query, List<TripOffer> potentialMatches) {
        if (potentialMatches.isEmpty()) return new ArrayList<>();

        Collections.sort(potentialMatches, new Comparator<TripOffer>() {
            @Override
            public int compare(TripOffer offer1, TripOffer offer2) {
                return Integer.valueOf(offer1.getPricePerKmInCents()).compareTo(offer2.getPricePerKmInCents());
            }
        });

        List<TripOffer> matches = new ArrayList<>();

        // find prices
        int lowestPricePerKmInCents  = -1, secondLowestPricePerKmInCents = -1;
        for (TripOffer potentialMatch : potentialMatches) {
            if (potentialMatch.getPricePerKmInCents() != lowestPricePerKmInCents) {
                // all cheapest trips are matches
                lowestPricePerKmInCents = potentialMatch.getPricePerKmInCents();
                matches.add(potentialMatch);

            } else if (potentialMatch.getPricePerKmInCents() != secondLowestPricePerKmInCents) {
                // second cheapest determines price
                secondLowestPricePerKmInCents = potentialMatch.getPricePerKmInCents();
                break;
            }
        }

        // calculate final price
        int pricePerKmInCents = lowestPricePerKmInCents;
        if (secondLowestPricePerKmInCents != -1) pricePerKmInCents = secondLowestPricePerKmInCents;
        int totalPriceInCents = (int) (pricePerKmInCents * query.getRoute().getDistanceInMeters() / 1000);

        // create price reservations
        List<TripReservation> reservations = new ArrayList<>();
        for (TripOffer match : matches) {
            reservations.add(new TripReservation(
                    0,
                    query,
                    totalPriceInCents,
                    match.getPricePerKmInCents(),
                    match.getId(),
                    match.getDriver()));

        }

        return reservations;
    }

}

