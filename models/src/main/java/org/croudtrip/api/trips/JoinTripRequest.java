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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * A pending / accepting request from a passenger to join a trip.
 */
@Entity(name = JoinTripRequest.ENTITY_NAME)
@Table(name = "join_trip_requests")
@NamedQueries({
		@NamedQuery(
				name = JoinTripRequest.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + JoinTripRequest.ENTITY_NAME + " r"
		)
})
public class JoinTripRequest {

	public static final String
			ENTITY_NAME =  "JoinTripRequest",
			COLUMN_ID = "join_trip_request_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.JoinTripRequest.findAll";

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

	@ManyToOne
	@JoinColumn(name = TripOffer.COLUMN_ID, nullable = false)
	private TripOffer offer;

	public JoinTripRequest() { }

	@JsonCreator
	public JoinTripRequest(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("totalPriceInCents") int totalPriceInCents,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents,
			@JsonProperty("offer") TripOffer offer) {

		this.id = id;
		this.query = query;
		this.totalPriceInCents = totalPriceInCents;
		this.pricePerKmInCents = pricePerKmInCents;
		this.offer = offer;
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

	public TripOffer getOffer() {
		return offer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JoinTripRequest that = (JoinTripRequest) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(totalPriceInCents, that.totalPriceInCents) &&
				Objects.equal(pricePerKmInCents, that.pricePerKmInCents) &&
				Objects.equal(query, that.query) &&
				Objects.equal(offer, that.offer);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, totalPriceInCents, pricePerKmInCents, offer);
	}
}
