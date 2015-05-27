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
