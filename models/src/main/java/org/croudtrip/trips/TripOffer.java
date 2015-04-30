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
 * A trip that is being offered by a driver.
 */
@Entity(name = TripOffer.ENTITY_NAME)
@Table(name = "offered_trips")
@NamedQueries({
		@NamedQuery(
				name = TripOffer.QUERY_NAME_FIND_ALL,
				query = "SELECT t FROM " + TripOffer.ENTITY_NAME + " t"
		)
})
public class TripOffer {

	public static final String
			ENTITY_NAME =  "TripOffer",
			COLUMN_ID = "trip_offer_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.trips.TripOffer.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private Route route;

	@Column(name = "maxDiversionInMeters", nullable = false)
	private long maxDiversionInMeters;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User driver;


	public TripOffer() { }

	@JsonCreator
	public TripOffer(
			@JsonProperty("id") long id,
			@JsonProperty("route") Route route,
			@JsonProperty("maxDiversionsInMeters") long maxDiversionInMeters,
			@JsonProperty("driver") User driver) {

		this.id = id;
		this.route = route;
		this.maxDiversionInMeters = maxDiversionInMeters;
		this.driver = driver;
	}


	public long getId() {
		return id;
	}


	public Route getRoute() {
		return route;
	}


	public long getMaxDiversionInMeters() {
		return maxDiversionInMeters;
	}


	public User getDriver() {
		return driver;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOffer)) return false;
		TripOffer offer = (TripOffer) other;
		return Objects.equal(id, offer.id)
				&& Objects.equal(route, offer.route)
				&& Objects.equal(maxDiversionInMeters, offer.maxDiversionInMeters)
				&& Objects.equal(driver, offer.driver);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(id, route, maxDiversionInMeters, driver);
	}

}
