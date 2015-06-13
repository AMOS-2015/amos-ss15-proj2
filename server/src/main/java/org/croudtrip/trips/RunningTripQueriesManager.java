package org.croudtrip.trips;

import com.google.common.base.Optional;

import org.croudtrip.api.gcm.GcmConstants;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.RunningTripQueryStatus;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.api.trips.TripQuery;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.gcm.GcmManager;
import org.croudtrip.utils.Pair;

import javax.inject.Inject;

/**
 * Handles {@link org.croudtrip.api.trips.RunningTripQuery}s.
 */
public class RunningTripQueriesManager {

	private final RunningTripQueryDAO runningTripQueryDAO;
	private final TripsMatcher tripsMatcher;
	private final GcmManager gcmManager;

	@Inject
	RunningTripQueriesManager(
			RunningTripQueryDAO runningTripQueryDAO,
			TripsMatcher tripsMatcher,
			GcmManager gcmManager) {

		this.runningTripQueryDAO = runningTripQueryDAO;
		this.tripsMatcher = tripsMatcher;
		this.gcmManager = gcmManager;
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
			Optional<TripsMatcher.PotentialMatch> potentialMatch = tripsMatcher.isPotentialMatch(offer, query);

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
