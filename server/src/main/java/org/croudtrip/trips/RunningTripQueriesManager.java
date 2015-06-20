package org.croudtrip.trips;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.RunningTripQueryStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.utils.Pair;

import java.util.List;

import javax.inject.Inject;

/**
 * Handles {@link org.croudtrip.api.trips.RunningTripQuery}s.
 */
public class RunningTripQueriesManager {

	private final RunningTripQueryDAO runningTripQueryDAO;
	private final SimpleTripsMatcher simpleTripsMatcher;
	private final GcmManager gcmManager;

	@Inject
	RunningTripQueriesManager(
			RunningTripQueryDAO runningTripQueryDAO,
			SimpleTripsMatcher simpleTripsMatcher,
			GcmManager gcmManager) {

		this.runningTripQueryDAO = runningTripQueryDAO;
		this.simpleTripsMatcher = simpleTripsMatcher;
		this.gcmManager = gcmManager;
	}


	/**
	 * Creates and stores a new {@link RunningTripQuery}.
	 */
	public RunningTripQuery startRunningQuery(TripQuery query) {
		RunningTripQuery runningQuery = new RunningTripQuery(0, query, RunningTripQueryStatus.RUNNING);
		runningTripQueryDAO.save(runningQuery);
		return runningQuery;
	}


	/**
	 * Find all the queries by a certain passenger.
	 * @param passenger The passenger you want to get the queries for
	 * @param showOnlyRunning true, if you want to get only the running queries, false otherwise
	 * @return A list of {@link org.croudtrip.api.trips.RunningTripQuery} by this passenger.
	 */
	public List<RunningTripQuery> getRunningQueries(User passenger, boolean showOnlyRunning) {
		if (showOnlyRunning) return runningTripQueryDAO.findByPassengerIdAndSatusRunning(passenger.getId());
		else return runningTripQueryDAO.findByPassengerId(passenger.getId());
	}


	/**
	 * Find a running query by its id.
	 * @param queryId the id of the query you want to get.
	 * @return An {@link com.google.common.base.Optional} that contains the {@link org.croudtrip.api.trips.RunningTripQuery}
	 * if it exists in the database.
	 */
	public Optional<RunningTripQuery> getRunningQuery(long queryId) {
		return runningTripQueryDAO.findById(queryId);
	}


	/**
	 * Delete a running query from the database
	 * @param runningTripQuery the query that should be deleted.
	 */
	public void deleteRunningQuery(RunningTripQuery runningTripQuery) {
		runningTripQueryDAO.delete(runningTripQuery);
	}


	/**
	 * Checks if the passed in offer is a match for one of the running background searches
	 * and alerts the passenger if that is the case.
	 */
	public void checkAndUpdateRunningQueries(TripOffer offer) {
		for (RunningTripQuery runningQuery : runningTripQueryDAO.findByStatusRunning()) {
			// check max waiting time
			if (runningQuery.getQuery().getCreationTimestamp() + runningQuery.getQuery().getMaxWaitingTimeInSeconds() < System.currentTimeMillis() / 1000) continue;

			// check if the newly offered trip matches to a running trip query
			TripQuery query = runningQuery.getQuery();
			Optional<SimpleTripsMatcher.PotentialMatch> potentialMatch = simpleTripsMatcher.isPotentialMatch(offer, query);

			// notify passenger about potential match
			if (potentialMatch.isPresent()) {
				gcmManager.sendGcmMessageToUser(
						query.getPassenger(),
						GcmConstants.GCM_MSG_FOUND_MATCHES,
						new Pair<>(GcmConstants.GCM_MSG_FOUND_MATCHES_QUERY_ID, "" + runningQuery.getId()));
				RunningTripQuery updatedRunningQuery = new RunningTripQuery(
						runningQuery.getId(),
						runningQuery.getQuery(),
						RunningTripQueryStatus.FOUND);
				runningTripQueryDAO.update(updatedRunningQuery);
			}
		}
	}

}
