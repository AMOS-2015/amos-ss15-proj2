package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;

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
	private Route route;

	@Column(name = "maxWaitingTimeInSeconds", nullable = false)
	private long maxWaitingTimeInSeconds;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID + "_passenger", nullable = false)
	private User passenger;


	public TripQuery() { }

	@JsonCreator
	public TripQuery(
			@JsonProperty("route") Route route,
			@JsonProperty("maxWaitingTimeSeconds") long maxWaitingTimeInSeconds,
			@JsonProperty("passenger") User passenger) {

		this.route = route;
		this.maxWaitingTimeInSeconds = maxWaitingTimeInSeconds;
		this.passenger = passenger;
	}


	public Route getRoute() {
		return route;
	}


	public long getMaxWaitingTimeInSeconds() {
		return maxWaitingTimeInSeconds;
	}


	public User getPassenger() {
		return passenger;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripQuery tripQuery = (TripQuery) o;
		return Objects.equal(maxWaitingTimeInSeconds, tripQuery.maxWaitingTimeInSeconds) &&
				Objects.equal(route, tripQuery.route) &&
				Objects.equal(passenger, tripQuery.passenger);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(route, maxWaitingTimeInSeconds, passenger);
	}

}
