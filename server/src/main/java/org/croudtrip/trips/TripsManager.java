package org.croudtrip.trips;


import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.croudtrip.account.VehicleManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.JoinTripStatus;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.RunningTripQueryStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripOfferDescription;
import org.croudtrip.api.trips.TripOfferStatus;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.Assert;
import org.croudtrip.utils.Pair;

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
    private final RunningTripQueryDAO runningTripQueryDAO;
    private final TripReservationDAO tripReservationDAO;
    private final JoinTripRequestDAO joinTripRequestDAO;
	private final DirectionsManager directionsManager;
    private final VehicleManager vehicleManager;
    private final GcmManager gcmManager;
    private final LogManager logManager;


	@Inject
	TripsManager(
            TripOfferDAO tripOfferDAO,
            RunningTripQueryDAO runningTripQueryDAO,
            TripReservationDAO tripReservationDAO,
            JoinTripRequestDAO joinTripRequestDAO,
            DirectionsManager directionsManager,
            VehicleManager vehicleManager,
            GcmManager gcmManager,
            LogManager logManager) {

		this.tripOfferDAO = tripOfferDAO;
        this.runningTripQueryDAO = runningTripQueryDAO;
        this.tripReservationDAO = tripReservationDAO;
        this.joinTripRequestDAO = joinTripRequestDAO;
		this.directionsManager = directionsManager;
        this.vehicleManager = vehicleManager;
        this.gcmManager = gcmManager;
        this.logManager = logManager;
	}


	public TripOffer addOffer(User owner, TripOfferDescription description) throws Exception {
        // check if there is a route
		List<Route> route = directionsManager.getDirections(description.getStart(), description.getEnd());
		if (route.size() == 0) throw new Exception("not route found");

        // find vehicle
        Optional<Vehicle>vehicle = vehicleManager.findVehicleById(description.getVehicleId());
        Assert.assertTrue(vehicle.isPresent() && vehicle.get().getOwner().getId() == owner.getId(), "no vehilce for id " + description.getVehicleId());

        // create and store offer
		TripOffer offer = new TripOffer(
                0,
                route.get(0),
                description.getMaxDiversionInMeters(),
                description.getPricePerKmInCents(),
                owner,
                vehicle.get(),
                TripOfferStatus.ACTIVE_NOT_FULL);
		tripOfferDAO.save(offer);

        // compare offer with running queries
        for (RunningTripQuery runningQuery : runningTripQueryDAO.findByStatusRunning()) {
            if (!runningQuery.getStatus().equals(RunningTripQueryStatus.RUNNING)) continue;
            if (runningQuery.getCreationTimestamp() + runningQuery.getQuery().getMaxWaitingTimeInSeconds() < System.currentTimeMillis() / 1000) continue;

            TripQuery query = runningQuery.getQuery();
            List<TripOffer> potentialMatches = findPotentialMatches(Lists.newArrayList(offer), query);

            // notify passenger about potential match
            if (!potentialMatches.isEmpty()) {
                gcmManager.sendGcmMessageToUser(
                        query.getPassenger(),
                        GcmConstants.GCM_MSG_FOUND_MATCHES,
                        new Pair<>(GcmConstants.GCM_MSG_FOUND_MATCHES_QUERY_ID, "" + runningQuery.getId()));
                        RunningTripQuery updatedRunningQuery = new RunningTripQuery(
                                runningQuery.getId(),
                                runningQuery.getQuery(),
                                runningQuery.getCreationTimestamp(),
                                RunningTripQueryStatus.FOUND);
                runningTripQueryDAO.update(updatedRunningQuery);
            }
        }
		return offer;
	}


	public List<TripOffer> findOffersByDriver(User driver) {
        return tripOfferDAO.findByDriverId(driver.getId());
	}


	public Optional<TripOffer> findOffer(long offerId) {
		return tripOfferDAO.findById(offerId);
	}


	public void deleteOffer(TripOffer offer) {
		tripOfferDAO.delete(offer);
	}


	public TripQueryResult queryOffers(User passenger, TripQueryDescription queryDescription) throws Exception {
        logManager.d("User " + passenger.getId() + " (" + passenger.getFirstName() + " " + passenger.getLastName() + ") sent query.");

        // compute passenger route
        List<Route> possiblePassengerRoutes = directionsManager.getDirections(queryDescription.getStart(), queryDescription.getEnd());
        if (possiblePassengerRoutes.isEmpty()) return new TripQueryResult(new ArrayList<TripReservation>(), null);

        // find declined trips for this user
        TripQuery query = new TripQuery(possiblePassengerRoutes.get(0), queryDescription.getStart(), queryDescription.getEnd(), queryDescription.getMaxWaitingTimeInSeconds(), passenger);
        List<JoinTripRequest> declinedRequests = joinTripRequestDAO.findDeclinedRequests( passenger.getId() );
        logManager.d("Found " + declinedRequests.size() + " declined entries in the database.");

        // analyse offers
        List<TripOffer> potentialMatches = findPotentialMatches(tripOfferDAO.findAllActive(), query);

        // find and store reservations
        List<TripReservation> reservations = findCheapestMatch(query, potentialMatches);
        for (TripReservation reservation : reservations) tripReservationDAO.save(reservation);

        // if no reservations start "background search"
        RunningTripQuery runningQuery = null;
        if (reservations.isEmpty()) {
            runningQuery = new RunningTripQuery(
                    0,
                    query,
                    System.currentTimeMillis() / 1000,
                    RunningTripQueryStatus.RUNNING);
            runningTripQueryDAO.save(runningQuery);
        }

        return new TripQueryResult(reservations, runningQuery);
	}


    public List<RunningTripQuery> getRunningQueries(User passenger, boolean showOnlyRunning) {
        if (showOnlyRunning) return runningTripQueryDAO.findByPassengerIdAndSatusRunning(passenger.getId());
        else return runningTripQueryDAO.findByPassengerId(passenger.getId());
    }


    public Optional<RunningTripQuery> getRunningQuery(long queryId) {
        return runningTripQueryDAO.findById(queryId);
    }


    public void deleteRunningQuery(RunningTripQuery runningTripQuery) {
        runningTripQueryDAO.delete(runningTripQuery);
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
        gcmManager.sendGcmMessageToUser(offer.get().getDriver(), GcmConstants.GCM_MSG_JOIN_REQUEST,
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST, "There is a new request to join your trip"),
                new Pair<String, String>(GcmConstants.GCM_MSG_USER_MAIL, "" + offer.get().getDriver().getEmail()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + joinTripRequest.getId()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + offer.get().getId()));

        return Optional.of(joinTripRequest);
    }


    public List<JoinTripRequest> findAllJoinRequests(User passengerOrDriver, boolean showOnlyPassengerAccepted) {
        if (showOnlyPassengerAccepted) return joinTripRequestDAO.findByUserIdAndStatusPassengerAccepted(passengerOrDriver.getId());
        else return joinTripRequestDAO.findByUserId(passengerOrDriver.getId());
    }

    public List<JoinTripRequest> findDriverAcceptedJoinRequests(User passengerOrDriver) {
        return joinTripRequestDAO.findByUserIdAndStatusDriverAccepted(passengerOrDriver.getId());
    }


    public Optional<JoinTripRequest> findJoinRequest(long joinRequestId) {
        return joinTripRequestDAO.findById(joinRequestId);
    }


    public JoinTripRequest updateJoinRequest(JoinTripRequest joinRequest, boolean passengerAccepted) throws IOException {
        Assert.assertTrue(joinRequest.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED), "cannot modify join request");

        JoinTripStatus newStatus;
        if (passengerAccepted) newStatus = JoinTripStatus.DRIVER_ACCEPTED;
        else newStatus = JoinTripStatus.DRIVER_DECLINED;

        System.out.println("Update join request manager");

        JoinTripRequest updatedRequest = new JoinTripRequest(
                joinRequest.getId(),
                joinRequest.getQuery(),
                joinRequest.getTotalPriceInCents(),
                joinRequest.getPricePerKmInCents(),
                joinRequest.getOffer(),
                newStatus);
        joinTripRequestDAO.update(updatedRequest);

        System.out.println("Update join request db stored");

        logManager.d("User " + joinRequest.getQuery().getPassenger().getId() + " (" + joinRequest.getQuery().getPassenger().getFirstName() + " " + joinRequest.getQuery().getPassenger().getLastName() + ") got status update for joinTripRequest.");

        // notify the passenger about his trip status
        if( passengerAccepted ) {
            gcmManager.sendGcmMessageToUser(joinRequest.getQuery().getPassenger(), GcmConstants.GCM_MSG_REQUEST_ACCEPTED,
                    new Pair<String, String>(GcmConstants.GCM_MSG_USER_MAIL, "" + joinRequest.getQuery().getPassenger().getEmail()),
                    new Pair<String, String>(GcmConstants.GCM_MSG_REQUEST_ACCEPTED, "Your request was accepted"),
                    new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + joinRequest.getId()),
                    new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + joinRequest.getOffer().getId()));
        }
        else {
            gcmManager.sendGcmMessageToUser(joinRequest.getQuery().getPassenger(), GcmConstants.GCM_MSG_REQUEST_DECLINED,
                    new Pair<String, String>(GcmConstants.GCM_MSG_REQUEST_DECLINED, "Your request was declined"),
                    new Pair<String, String>(GcmConstants.GCM_MSG_USER_MAIL, "" + joinRequest.getQuery().getPassenger().getEmail()),
                    new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + joinRequest.getId()),
                    new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + joinRequest.getOffer().getId()));
        }

        System.out.println("Update join request gcm sent");

        return updatedRequest;
    }


    private List<TripOffer> findPotentialMatches(List<TripOffer> offers, TripQuery query) throws Exception {
        // find declined trips for this user
        List<JoinTripRequest> declinedRequests = joinTripRequestDAO.findDeclinedRequests(query.getPassenger().getId());

        // analyse offers
        List<TripOffer> potentialMatches = new ArrayList<>();
        offersLabel: for (TripOffer offer : offers) {
            // skip already declined offers
            for( JoinTripRequest request : declinedRequests ) {
                if( offer.getId() == request.getOffer().getId()) {
                    continue offersLabel;
                }
            }

            // compute total driver route
            List<RouteLocation> passengerWayPoints = query.getPassengerRoute().getWayPoints();
            List<RouteLocation> driverWayPoints = offer.getDriverRoute().getWayPoints();
            List<Route> possibleDriverRoutes = directionsManager.getDirections(
                    driverWayPoints.get(0),
                    driverWayPoints.get(1),
                    passengerWayPoints);

            if (possibleDriverRoutes == null || possibleDriverRoutes.isEmpty()) continue;

            // check is passenger route is within max diversion
            Route driverRoute = possibleDriverRoutes.get(0);
            logManager.d("Diversion: " + (possibleDriverRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters()));
            if (possibleDriverRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters() < offer.getMaxDiversionInMeters()) {
                potentialMatches.add(offer);
            }
        }

        return potentialMatches;
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
        int lowestPricePerKmInCents  = potentialMatches.get(0).getPricePerKmInCents(), secondLowestPricePerKmInCents = -1;
        for (TripOffer potentialMatch : potentialMatches) {
            if (potentialMatch.getPricePerKmInCents() == lowestPricePerKmInCents) {
                // all cheapest trips are matches
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

