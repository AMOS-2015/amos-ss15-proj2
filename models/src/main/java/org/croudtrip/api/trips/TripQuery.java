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
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A passenger query for available routes.
 */
@Embeddable
public class TripQuery {

	@Embedded
	private Route passengerRoute;

    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "sLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "sLng"))
    })
    private RouteLocation startLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "eLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "eLng"))
    })
    private RouteLocation destinationLocation;

	@Column(name = "maxWaitingTimeInSeconds", nullable = false)
	private long maxWaitingTimeInSeconds;

	@Column(name = "creationTimestamp", nullable = false)
	private long creationTimestamp; // unix timestamp in seconds

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID + "_passenger", nullable = false)
	private User passenger;


	public TripQuery() { }

	@JsonCreator
	public TripQuery(
			@JsonProperty("passengerRoute") Route passengerRoute,
            @JsonProperty("startLocation") RouteLocation startLocation,
            @JsonProperty("destinationLocation") RouteLocation destinationLocation,
			@JsonProperty("maxWaitingTimeSeconds") long maxWaitingTimeInSeconds,
			@JsonProperty("creationTimestamp") long creationTimestamp,
			@JsonProperty("passenger") User passenger) {

		this.passengerRoute = passengerRoute;
        this.startLocation = startLocation;
        this.destinationLocation = destinationLocation;
		this.maxWaitingTimeInSeconds = maxWaitingTimeInSeconds;
		this.creationTimestamp = creationTimestamp;
		this.passenger = passenger;
	}

	public Route getPassengerRoute() {
		return passengerRoute;
	}

	public long getMaxWaitingTimeInSeconds() {
		return maxWaitingTimeInSeconds;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public User getPassenger() {
		return passenger;
	}

    public RouteLocation getStartLocation() { return startLocation; }

    public RouteLocation getDestinationLocation() { return destinationLocation; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripQuery tripQuery = (TripQuery) o;
		return Objects.equal(maxWaitingTimeInSeconds, tripQuery.maxWaitingTimeInSeconds) &&
                Objects.equal(creationTimestamp, tripQuery.creationTimestamp) &&
				Objects.equal(startLocation, tripQuery.startLocation) &&
                Objects.equal(destinationLocation, tripQuery.destinationLocation) &&
				Objects.equal(passengerRoute, tripQuery.passengerRoute) &&
				Objects.equal(passenger, tripQuery.passenger);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(passengerRoute, maxWaitingTimeInSeconds, creationTimestamp, passenger, startLocation, destinationLocation);
	}


	public static class Builder {

		private Route passengerRoute;
		private RouteLocation startLocation;
		private RouteLocation destinationLocation;
		private long maxWaitingTimeInSeconds;
		private long creationTimestamp; // unix timestamp in seconds
		private User passenger;

		public Builder setPassengerRoute(Route passengerRoute) {
			this.passengerRoute = passengerRoute;
			return this;
		}

		public Builder setStartLocation(RouteLocation startLocation) {
			this.startLocation = startLocation;
			return this;
		}

		public Builder setDestinationLocation(RouteLocation destinationLocation) {
			this.destinationLocation = destinationLocation;
			return this;
		}

		public Builder setMaxWaitingTimeInSeconds(long maxWaitingTimeInSeconds) {
			this.maxWaitingTimeInSeconds = maxWaitingTimeInSeconds;
			return this;
		}

		public Builder setCreationTimestamp(long creationTimestamp) {
			this.creationTimestamp = creationTimestamp;
			return this;
		}

		public Builder setPassenger(User passenger) {
			this.passenger = passenger;
			return this;
		}

		public  TripQuery build() {
			return new TripQuery(passengerRoute, startLocation, destinationLocation, maxWaitingTimeInSeconds, creationTimestamp, passenger);
		}
	}

}
