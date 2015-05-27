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

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * A trip query that is running on the server ("background search").
 */
@Entity(name = RunningTripQuery.ENTITY_NAME)
@Table(name = "running_trip_queries")
@NamedQueries({
		@NamedQuery(
				name = RunningTripQuery.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + RunningTripQuery.ENTITY_NAME + " r"
		),
		@NamedQuery(
				name = RunningTripQuery.QUERY_FIND_BY_PASSENGER_ID,
				query = "SELECT r FROM " + RunningTripQuery.ENTITY_NAME + " r WHERE r.query.passenger.id = :" + RunningTripQuery.QUERY_PARAM_PASSENGER_ID
		),
		@NamedQuery(
				name = RunningTripQuery.QUERY_FIND_BY_STATUS_RUNNING,
				query = "SELECT r FROM " + RunningTripQuery.ENTITY_NAME + " r WHERE r.status = 'RUNNING'"
		),
		@NamedQuery(
				name = RunningTripQuery.QUERY_FIND_BY_PASSENGER_ID_AND_STATUS_RUNNING,
				query = "SELECT r FROM " + RunningTripQuery.ENTITY_NAME + " r WHERE " +
						"r.query.passenger.id = :" + RunningTripQuery.QUERY_PARAM_PASSENGER_ID + " AND " +
						"r.status = 'RUNNING'"
		)
})
public class RunningTripQuery {

	public static final String
			ENTITY_NAME =  "RunningTripQuery",
			COLUMN_ID = "running_trip_query_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.RunningTripQuery.findAll",
			QUERY_FIND_BY_PASSENGER_ID = "org.croudtrip.api.trips.RunningTripQuery.findByPassengerId",
			QUERY_FIND_BY_STATUS_RUNNING = "org.croudtrip.api.trips.RunningTripQuery.findByStatusRunning",
			QUERY_FIND_BY_PASSENGER_ID_AND_STATUS_RUNNING = "org.croudtrip.api.trips.RunningTripQuery.findByPassengerIdAndStatusRunning",
			QUERY_PARAM_PASSENGER_ID = "passenger_id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	@Column(name = "creationTimestamp", nullable = false)
	private long creationTimestamp; // unix timestamp in seconds

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private RunningTripQueryStatus status;

	public RunningTripQuery() { }

	@JsonCreator
	public RunningTripQuery(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("creationTimestamp") long creationTimestamp,
			@JsonProperty("status") RunningTripQueryStatus status) {

		this.id = id;
		this.query = query;
		this.creationTimestamp = creationTimestamp;
		this.status = status;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public RunningTripQueryStatus getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RunningTripQuery that = (RunningTripQuery) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(query, that.query) &&
				Objects.equal(creationTimestamp, that.creationTimestamp) &&
				Objects.equal(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, creationTimestamp, status);
	}

}
