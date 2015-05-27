/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.trips;


import com.google.common.base.Optional;

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
import org.croudtrip.api.trips.TripOfferUpdate;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.api.trips.TripQueryDescription;
import org.croudtrip.api.trips.TripQueryResult;
import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.JoinTripRequestDAO;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.db.TripOfferDAO;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.directions.DirectionsManager;
import org.croudtrip.directions.RouteNotFoundException;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.Assert;
import org.croudtrip.utils.Pair;

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


	public TripOffer addOffer(User owner, TripOfferDescription description) throws RouteNotFoundException {
        logManager.d("Searching for routes");

        // check if there is a route
		List<Route> route = directionsManager.getDirections(description.getStart(), description.getEnd());
		if (route.size() == 0) throw new RouteNotFoundException();

        logManager.d("Found " + route.size() + " routes to " + description.getEnd());

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
                TripOfferStatus.ACTIVE_NOT_FULL,
                System.currentTimeMillis()/1000);
		tripOfferDAO.save(offer);

        // compare offer with running queries
        for (RunningTripQuery runningQuery : runningTripQueryDAO.findByStatusRunning()) {
            if (!runningQuery.getStatus().equals(RunningTripQueryStatus.RUNNING)) continue;
            if (runningQuery.getCreationTimestamp() + runningQuery.getQuery().getMaxWaitingTimeInSeconds() < System.currentTimeMillis() / 1000) continue;

            TripQuery query = runningQuery.getQuery();
            boolean isPotentialMatch = isPotentialMatch(offer, query);

            // notify passenger about potential match
            if (isPotentialMatch) {
                logManager.d("Found a potential match: send gcm message to user " + query.getPassenger().getFirstName() + " " + query.getPassenger().getLastName());
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


    public TripOffer updateOffer(TripOffer offer, TripOfferUpdate offerUpdate) throws RouteNotFoundException {
        // check if there is a route
        List<Route> routes = directionsManager.getDirections(offerUpdate.getUpdatedStart(), offer.getDriverRoute().getWayPoints().get(1));
        if (routes.size() == 0) throw new RouteNotFoundException();

        // update and store offer
        TripOffer updatedOffer = new TripOffer(
                offer.getId(),
                routes.get(0),
                offer.getMaxDiversionInMeters(),
                offer.getPricePerKmInCents(),
                offer.getDriver(),
                offer.getVehicle(),
                offer.getStatus(),
                System.currentTimeMillis() / 1000);
        tripOfferDAO.update(updatedOffer);
        return updatedOffer;
    }


	public TripQueryResult queryOffers(User passenger, TripQueryDescription queryDescription) throws RouteNotFoundException {
        logManager.d("User " + passenger.getId() + " (" + passenger.getFirstName() + " " + passenger.getLastName() + ") sent query from " + queryDescription.getStart() + " " + queryDescription.getEnd() + ".");

        // compute passenger route
        List<Route> possiblePassengerRoutes = directionsManager.getDirections(queryDescription.getStart(), queryDescription.getEnd());
        if (possiblePassengerRoutes.isEmpty()) throw new RouteNotFoundException();

        // analyse offers
        TripQuery query = new TripQuery(possiblePassengerRoutes.get(0), queryDescription.getStart(), queryDescription.getEnd(), queryDescription.getMaxWaitingTimeInSeconds(), passenger);
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


    public Optional<JoinTripRequest> joinTrip(TripReservation tripReservation) {
        // remove reservation (either it has now been accepted or is can be discarded)
        tripReservationDAO.delete(tripReservation);

        // find and check trip
        Optional<TripOffer> offerOptional = tripOfferDAO.findById(tripReservation.getOfferId());
        if (!offerOptional.isPresent()) return Optional.absent();
        TripOffer offer = offerOptional.get();
        if (!isPotentialMatch(offer, tripReservation.getQuery())) return Optional.absent();

        // update join request
        JoinTripRequest joinTripRequest = new JoinTripRequest(
                0,
                tripReservation.getQuery(),
                tripReservation.getTotalPriceInCents(),
                tripReservation.getPricePerKmInCents(),
                offer,
                JoinTripStatus.PASSENGER_ACCEPTED);

        joinTripRequestDAO.save(joinTripRequest);

        // send push notification to driver
        gcmManager.sendGcmMessageToUser(offerOptional.get().getDriver(), GcmConstants.GCM_MSG_JOIN_REQUEST,
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST, "There is a new request to join your trip"),
                new Pair<String, String>(GcmConstants.GCM_MSG_USER_MAIL, "" + offerOptional.get().getDriver().getEmail()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_ID, "" + joinTripRequest.getId()),
                new Pair<String, String>(GcmConstants.GCM_MSG_JOIN_REQUEST_OFFER_ID, "" + offerOptional.get().getId()));

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


    public JoinTripRequest updateJoinRequest(JoinTripRequest joinRequest, boolean passengerAccepted) {
        Assert.assertTrue(joinRequest.getStatus().equals(JoinTripStatus.PASSENGER_ACCEPTED), "cannot modify join request");

        // update join request status
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

        // update offer if vehicle is full
        TripOffer offer = joinRequest.getOffer();
        int passengerCount = getActiveJoinRequestsForOffer(offer);
        if (passengerCount >= offer.getVehicle().getCapacity()) {
            TripOffer updatedOffer = new TripOffer(
                    offer.getId(),
                    offer.getDriverRoute(),
                    offer.getMaxDiversionInMeters(),
                    offer.getPricePerKmInCents(),
                    offer.getDriver(),
                    offer.getVehicle(),
                    TripOfferStatus.ACTIVE_FULL,
                    offer.getLastPositonUpdate() );
            tripOfferDAO.update(updatedOffer);
        }

        // notify the passenger about status
        logManager.d("User " + joinRequest.getQuery().getPassenger().getId() + " (" + joinRequest.getQuery().getPassenger().getFirstName() + " " + joinRequest.getQuery().getPassenger().getLastName() + ") got status update for joinTripRequest.");
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

        return joinTripRequestDAO.findById(joinRequest.getId()).get();
    }


    private List<TripOffer> findPotentialMatches(List<TripOffer> offers, TripQuery query) {
        // analyse offers
        List<TripOffer> potentialMatches = new ArrayList<>();
        for (TripOffer offer : offers) {
            if (isPotentialMatch(offer, query)) {
                potentialMatches.add(offer);
            }
        }

        return potentialMatches;
    }


    private boolean isPotentialMatch(TripOffer offer, TripQuery query) {
        // find declined trips for this user
        List<JoinTripRequest> declinedRequests = joinTripRequestDAO.findDeclinedRequests(query.getPassenger().getId());

        // check trip status
        if (!offer.getStatus().equals(TripOfferStatus.ACTIVE_NOT_FULL)) return false;

		// skip already declined offers
		for( JoinTripRequest request : declinedRequests ) {
			if( offer.getId() == request.getOffer().getId()) {
                return false;
			}
		}

        // check current passenger count
        int passengerCount = getActiveJoinRequestsForOffer(offer);
        if (passengerCount >= offer.getVehicle().getCapacity()) return false;

		// compute total driver route
		List<RouteLocation> passengerWayPoints = query.getPassengerRoute().getWayPoints();
		List<RouteLocation> driverWayPoints = offer.getDriverRoute().getWayPoints();

        logManager.d("passenger Waypoints: " + driverWayPoints.get(0) + " " + driverWayPoints.get(1) + " : Passenger: " + passengerWayPoints.get(0) + " " + passengerWayPoints.get(1));

		List<Route> possibleDriverRoutes = directionsManager.getDirections(
				driverWayPoints.get(0),
				driverWayPoints.get(1),
				passengerWayPoints);

		if (possibleDriverRoutes == null || possibleDriverRoutes.isEmpty()) return false;

		// check is passenger route is within max diversion
		Route driverRoute = offer.getDriverRoute();
        logManager.d("Driver Route Distance: " + driverRoute.getDistanceInMeters() );
        logManager.d("Passenger Route: " + possibleDriverRoutes.get(0).getDistanceInMeters());
		logManager.d("Diversion: " + (possibleDriverRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters()));
		if (possibleDriverRoutes.get(0).getDistanceInMeters() - driverRoute.getDistanceInMeters() > offer.getMaxDiversionInMeters()) {
            return false;
		}

		// check passenger max waiting time
		Route routeToPassenger = directionsManager.getDirections(driverWayPoints.get(0), passengerWayPoints.get(0)).get(0);
		if (routeToPassenger.getDurationInSeconds() > query.getMaxWaitingTimeInSeconds()) {
            return false;
		}

        return true;
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


    public int getActiveJoinRequestsForOffer(TripOffer offer) {
        int requestsCount = 0;
        for (JoinTripRequest request : joinTripRequestDAO.findByOfferId(offer.getId())) {
            if (!request.getStatus().equals(JoinTripStatus.DRIVER_DECLINED)) ++requestsCount;
        }
        return requestsCount;
    }

}

