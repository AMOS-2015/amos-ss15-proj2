package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * A trip that is being offered by a driver.
 */
@Entity(name = TripMatchReservation.ENTITY_NAME)
@Table(name = "trip_match_reservations")
@NamedQueries({
		@NamedQuery(
				name = org.croudtrip.api.trips.TripMatchReservation.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + org.croudtrip.api.trips.TripMatchReservation.ENTITY_NAME + " r"
		)
})
public class TripMatchReservation {

	public static final String
			ENTITY_NAME =  "TripMatchReservation",
			COLUMN_ID = "trip_match_reservation_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.TripMatchReservation.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	@Column(name = "priceInCents", nullable = false)
	private int priceInCents;

	@Column(name = "offerId", nullable = false)
	private long offerId;


	public TripMatchReservation() { }

	@JsonCreator
	public TripMatchReservation(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("priceInCents") int priceInCents,
			@JsonProperty("offerId") long offerId) {

		this.id = id;
		this.query = query;
		this.priceInCents = priceInCents;
		this.offerId = offerId;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	public int getPriceInCents() {
		return priceInCents;
	}

	public long getOfferId() {
		return offerId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripMatchReservation that = (TripMatchReservation) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(priceInCents, that.priceInCents) &&
				Objects.equal(offerId, that.offerId) &&
				Objects.equal(query, that.query);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, priceInCents, offerId);
	}
}
