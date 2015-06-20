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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * A collection of {@link TripReservation} which together make up the reservation
 * for a whole trip.
 */
@Entity(name = SuperTripReservation.ENTITY_NAME)
@Table(name = "super_trip_reservation")
@NamedQueries({
		@NamedQuery(
				name = SuperTripReservation.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + SuperTripReservation.ENTITY_NAME + " r"
		)
})
public class SuperTripReservation {

	public static final String
			ENTITY_NAME =  "SuperTripReservation",
			COLUMN_ID = "super_trip_reservation_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.SuperTripReservation.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "reservations")
	private List<TripReservation> reservations;


	public SuperTripReservation() { }

	@JsonCreator
	public SuperTripReservation(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("tripReservation") List<TripReservation> reservations) {

		this.id = id;
		this.query = query;
		this.reservations = reservations;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	public List<TripReservation> getReservations() {
		return reservations;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SuperTripReservation that = (SuperTripReservation) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(query, that.query) &&
				Objects.equal(reservations, that.reservations);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, reservations);
	}


	public static class Builder {

		private long id;
		private TripQuery query;
		private final List<TripReservation> reservations = new ArrayList<>();

		public Builder setId(long id) {
			this.id = id;
			return this;
		}

		public Builder setQuery(TripQuery query) {
			this.query = query;
			return this;
		}

		public Builder addReservation(TripReservation reservation) {
			this.reservations.add(reservation);
			return this;
		}

		public SuperTripReservation build() {
			return new SuperTripReservation(id, query, reservations);
		}

	}
}
