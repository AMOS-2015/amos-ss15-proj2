package org.croudtrip.trips;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.utils.Assert;
import org.croudtrip.gcm.GcmManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TripsManager {

	private final TripOfferDAO tripOfferDAO;
    private final TripReservationDAO tripReservationDAO;
    private final JoinTripRequestDAO joinTripRequestDAO;
	private final DirectionsManager directionsManager;
    private final GcmManager gcmManager;


	@Inject
	TripsManager(
            TripOfferDAO tripOfferDAO,
            TripReservationDAO tripReservationDAO,
            JoinTripRequestDAO joinTripRequestDAO,
            DirectionsManager directionsManager,
            GcmManager gcmManager) {

		this.tripOfferDAO = tripOfferDAO;
        this.tripReservationDAO = tripReservationDAO;
        this.joinTripRequestDAO = joinTripRequestDAO;
		this.directionsManager = directionsManager;
        this.gcmManager = gcmManager;
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
        TripQuery query = new TripQuery(possiblePassengerRoutes.get(0), queryDescription.getMaxWaitingTimeInSeconds(), passenger);

        // analyse offers
        List<TripOffer> potentialMatches = new ArrayList<>();
        for (TripOffer offer : findAllOffers()) {
            Optional<TripOffer> potentialMatch = analyzeOffer(offer, query);
            if (potentialMatch.isPresent()) potentialMatches.add(potentialMatch.get());
        }

        // find and store reservations
        List<TripReservation> reservations = findCheapestMatch(query, potentialMatches);
        for (TripReservation reservation : reservations) tripReservationDAO.save(reservation);

        return reservations;
	}


    public List<TripReservation> findAllReservations() {
        return tripReservationDAO.findAll();
    }


    public Optional<TripReservation> findReservation(long reservationId) {
        return tripReservationDAO.findById(reservationId);
    }


    public Optional<JoinTripRequest> joinTrip(TripReservation tripReservation) throws IOException {
        // remove reservation (either it has now been accepted or is can be discarded)
        tripReservationDAO.delete(tripReservation);

        // find and check trip
        Optional<TripOffer> offer = tripOfferDAO.findById(tripReservation.getOfferId());
        if (!offer.isPresent()) return Optional.absent();
        // TODO ensure that offer is actually still valid

        // notify driver
        JoinTripRequest joinTripRequest = new JoinTripRequest(
                0,
                tripReservation.getQuery(),
                tripReservation.getTotalPriceInCents(),
                tripReservation.getPricePerKmInCents(),
                offer.get(),
                JoinTripStatus.PASSENGER_ACCEPTED);

        joinTripRequestDAO.save(joinTripRequest);

        // send push notification to driver
        gcmManager.sendGcmMessageToUser( offer.get().getDriver(), GcmConstants.GCM_MSG_JOIN_REQUEST, ""+joinTripRequest.getId() );

        return Optional.of(joinTripRequest);
    }


    public List<JoinTripRequest> findAllJoinRequests(TripOffer offer) {
        return joinTripRequestDAO.findByOfferId(offer.getId());
    }


    public Optional<JoinTripRequest> findJoinRequest(long joinRequestId) {
        return joinTripRequestDAO.findById(joinRequestId);
    }


    public JoinTripRequest updateJoinRequest(JoinTripRequest joinRequest, boolean passengerAccepted) {
        Assert.assertTrue(joinRequest.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED), "cannot modify join request");

        JoinTripStatus newStatus;
        if (passengerAccepted) newStatus = JoinTripStatus.DRIVER_ACCEPTED;
        else newStatus = JoinTripStatus.DRIVER_DECLINED;

        JoinTripRequest updatedRequest = new JoinTripRequest(
                joinRequest.getId(),
                joinRequest.getQuery(),
                joinRequest.getTotalPriceInCents(),
                joinRequest.getPricePerKmInCents(),
                joinRequest.getOffer(),
                newStatus);
        joinTripRequestDAO.update(updatedRequest);
        return updatedRequest;
        // TODO notify passenger
    }


    private Optional<TripOffer> analyzeOffer(TripOffer offer, TripQuery query) throws Exception {
        // compute total driver route
        List<RouteLocation> wayPoints = new ArrayList<>();
        wayPoints.add(query.getPassengerRoute().getStart());
        wayPoints.add(query.getPassengerRoute().getEnd());
        List<Route> possibleDriverRoutes = directionsManager.getDirections(
                offer.getDriverRoute().getStart(),
                offer.getDriverRoute().getEnd(),
                wayPoints);

        if (possibleDriverRoutes == null || possibleDriverRoutes.isEmpty()) return Optional.absent();

        // check is passenger route is within max diversion
        Route driverRoute = possibleDriverRoutes.get(0);
        if (driverRoute.getDistanceInMeters() - query.getPassengerRoute().getDistanceInMeters() < offer.getMaxDiversionInMeters()) {
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
        int totalPriceInCents = (int) (pricePerKmInCents * query.getPassengerRoute().getDistanceInMeters() / 1000);

        // create price reservations
        List<TripReservation> reservations = new ArrayList<>();
        for (TripOffer match : matches) {
            reservations.add(new TripReservation(
                    0,
                    query,
                    totalPriceInCents,
                    match.getPricePerKmInCents(),
                    match.getId(),
                    match.getDriver(),
                    System.currentTimeMillis() / 1000));

        }

        return reservations;
    }

}

