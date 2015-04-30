package org.croudtrip.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.account.User;
import org.croudtrip.directions.Route;

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
 * A trip that has been matched with a driver and passenger.
 */
@Entity(name = TripMatch.ENTITY_NAME)
@Table(name = "matched_trips")
@NamedQueries({
		@NamedQuery(
				name = TripMatch.QUERY_NAME_FIND_ALL,
				query = "SELECT t FROM " + TripMatch.ENTITY_NAME + " t"
		)
})
public class TripMatch {

	public static final String
			ENTITY_NAME =  "TipMatch",
			COLUMN_ID = "trip_match_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.trips.TripMatch.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private Route route;

	@Column(name = "diversionInMeters", nullable = false)
	private long diversionInMeters;

	@Column(name = "diversionInSeconds", nullable = false)
	private long diversionInSeconds;

	@Column(name = "estimatedPriceInCents", nullable = false)
	private int estimatedPriceInCents;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID + "_driver", nullable = false)
	private User driver;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID + "_passenger", nullable = false)
	private User passenger;

	public TripMatch() { }

	@JsonCreator
	public TripMatch(
			@JsonProperty("id") long id,
			@JsonProperty("route") Route route,
			@JsonProperty("diversionInMeters") long diversionInMeters,
			@JsonProperty("diversionInSeconds") long diversionInSeconds,
			@JsonProperty("estimatedPriceInCents") int estimatedPriceInCents,
			@JsonProperty("driver") User driver,
			@JsonProperty("passenger") User passenger) {

		this.id = id;
		this.route = route;
		this.diversionInMeters = diversionInMeters;
		this.diversionInSeconds = diversionInSeconds;
		this.estimatedPriceInCents = estimatedPriceInCents;
		this.driver = driver;
		this.passenger = passenger;
	}


	public long getId() {
		return id;
	}


	public Route getRoute() {
		return route;
	}


	public long getDiversionInMeters() {
		return diversionInMeters;
	}


	public long getDiversionInSeconds() {
		return diversionInSeconds;
	}


	public int getEstimatedPriceInCents() {
		return estimatedPriceInCents;
	}


	public User getDriver() {
		return driver;
	}


	public User getPassenger() {
		return passenger;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripMatch)) return false;
		TripMatch offer = (TripMatch) other;
		return Objects.equal(id, offer.id)
				&& Objects.equal(route, offer.route)
				&& Objects.equal(diversionInMeters, offer.diversionInMeters)
				&& Objects.equal(diversionInSeconds, offer.diversionInSeconds)
				&& Objects.equal(estimatedPriceInCents, offer.estimatedPriceInCents)
				&& Objects.equal(driver, offer.driver)
				&& Objects.equal(passenger, offer.passenger);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(id, route, diversionInMeters, diversionInSeconds, estimatedPriceInCents, driver, passenger);
	}

}
