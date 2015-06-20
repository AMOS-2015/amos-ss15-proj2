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
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A (price) reservation for one offered trip.
 */
@Embeddable
public class TripReservation {

    /**
     * Contains all the relevant information about the sub-trip that should be done by one driver
     * within a super trip. If this reservation is part of a simple trips the subQuery value should
     * contain all the relevant information of the original query
     */
    @Embedded
    SuperTripSubQuery subQuery;

	@Column(name = "priceInCents", nullable = false)
	private int totalPriceInCents;

	@Column(name = "pricePerKmInCents", nullable = false)
	private int pricePerKmInCents;

	@Column(name = "offerId", nullable = false)
	private long offerId;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User driver;

	public TripReservation() { }


	@JsonCreator
	public TripReservation(
            @JsonProperty("subQuery") SuperTripSubQuery subQuery,
			@JsonProperty("totalPriceInCents") int totalPriceInCents,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents,
			@JsonProperty("offerId") long offerId,
			@JsonProperty("driver") User driver) {

        this.subQuery = subQuery;
		this.totalPriceInCents = totalPriceInCents;
		this.pricePerKmInCents = pricePerKmInCents;
		this.offerId = offerId;
		this.driver = driver;
	}

    public SuperTripSubQuery getSubQuery() { return subQuery; }

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripReservation that = (TripReservation) o;
		return Objects.equal(totalPriceInCents, that.totalPriceInCents) &&
				Objects.equal(pricePerKmInCents, that.pricePerKmInCents) &&
				Objects.equal(offerId, that.offerId) &&
				Objects.equal(driver, that.driver);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(totalPriceInCents, pricePerKmInCents, offerId, driver);
	}
}
