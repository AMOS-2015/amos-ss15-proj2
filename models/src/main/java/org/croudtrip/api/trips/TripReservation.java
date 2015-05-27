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

import org.croudtrip.api.account.User;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * A trip that is being offered by a driver.
 */
@Entity(name = TripReservation.ENTITY_NAME)
@Table(name = "trip_reservation")
@NamedQueries({
		@NamedQuery(
				name = TripReservation.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + TripReservation.ENTITY_NAME + " r"
		)
})
public class TripReservation {

	public static final String
			ENTITY_NAME =  "TripMatchReservation",
			COLUMN_ID = "trip_match_reservation_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.TripReservation.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	@Column(name = "priceInCents", nullable = false)
	private int totalPriceInCents;

	@Column(name = "pricePerKmInCents", nullable = false)
	private int pricePerKmInCents;

	@Column(name = "offerId", nullable = false)
	private long offerId;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User driver;

	@Column(name = "creationTimestamp", nullable = false)
	private long creationTimestamp; // unix timestamp in seconds

	public TripReservation() { }

	@JsonCreator
	public TripReservation(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("totalPriceInCents") int totalPriceInCents,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents,
			@JsonProperty("offerId") long offerId,
			@JsonProperty("driver") User driver,
			@JsonProperty("creationTimestamp") long creationTimestamp) {

		this.id = id;
		this.query = query;
		this.totalPriceInCents = totalPriceInCents;
		this.pricePerKmInCents = pricePerKmInCents;
		this.offerId = offerId;
		this.driver = driver;
		this.creationTimestamp = creationTimestamp;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	public int getTotalPriceInCents() {
		return totalPriceInCents;
	}

	public int getPricePerKmInCents() {
		return pricePerKmInCents;
	}

	public long getOfferId() {
		return offerId;
	}

	public User getDriver() {
		return driver;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripReservation that = (TripReservation) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(totalPriceInCents, that.totalPriceInCents) &&
				Objects.equal(pricePerKmInCents, that.pricePerKmInCents) &&
				Objects.equal(offerId, that.offerId) &&
				Objects.equal(query, that.query) &&
				Objects.equal(driver, that.driver) &&
				Objects.equal(creationTimestamp, that.creationTimestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, totalPriceInCents, pricePerKmInCents, offerId, driver, creationTimestamp);
	}
}
