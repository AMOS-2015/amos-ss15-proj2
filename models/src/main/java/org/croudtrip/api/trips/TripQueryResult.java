package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.List;

/**
 * Contains either a list of reservations and, optionally,
 * a query that is still running on the server to inform passengers
 * about future trips.
 */
public class TripQueryResult {

	private final List<TripReservation> reservations;
	private final RunningTripQuery runningQuery;

	@JsonCreator
	public TripQueryResult(
			@JsonProperty("reservations") List<TripReservation> reservations,
			@JsonProperty("runningQuery") RunningTripQuery runningQuery) {

		this.reservations = reservations;
		this.runningQuery = runningQuery;
	}

	public List<TripReservation> getReservations() {
		return reservations;
	}

	public RunningTripQuery getRunningQuery() {
		return runningQuery;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripQueryResult that = (TripQueryResult) o;
		return Objects.equal(reservations, that.reservations) &&
				Objects.equal(runningQuery, that.runningQuery);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(reservations, runningQuery);
	}

}
