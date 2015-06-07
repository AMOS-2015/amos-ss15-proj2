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
		),
		@NamedQuery(
				name = JoinTripRequest.QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID,
				query = "SELECT r FROM " + JoinTripRequest.ENTITY_NAME + " r WHERE " +
						"r.offer.driver.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID + " OR " +
						"r.query.passenger.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID
		),
		@NamedQuery(
				name = JoinTripRequest.QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID_AND_PASSENGER_ACCEPTED_STATUS,
				query = "SELECT r FROM " + JoinTripRequest.ENTITY_NAME + " r WHERE (" +
						"r.offer.driver.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID + " OR " +
						"r.query.passenger.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID + " ) AND " +
						"r.status = 'PASSENGER_ACCEPTED'"
		),
        @NamedQuery(
                name = JoinTripRequest.QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID_AND_DRIVER_ACCEPTED_STATUS,
                query = "SELECT r FROM " + JoinTripRequest.ENTITY_NAME + " r WHERE (" +
                        "r.offer.driver.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID + " OR " +
                        "r.query.passenger.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID + " ) AND " +
                        "r.status = 'DRIVER_ACCEPTED'"
        ),
        @NamedQuery(
                name = JoinTripRequest.QUERY_FIND_BY_PASSENGER_ID_AND_DECLINED_STATUS,
                query = "SELECT r FROM " + JoinTripRequest.ENTITY_NAME + " r WHERE r.status = 'DRIVER_DECLINED' AND r.query.passenger.id = :" + JoinTripRequest.QUERY_PARAM_USER_ID
        ),
		@NamedQuery(
				name = JoinTripRequest.QUERY_FIND_BY_OFFER_ID,
				query = "SELECT r FROM " + JoinTripRequest.ENTITY_NAME + " r WHERE r.offer.id = :" + JoinTripRequest.QUERY_PARAM_OFFER_ID
		)
})
public class JoinTripRequest {

	public static final String
			ENTITY_NAME =  "JoinTripRequest",
			COLUMN_ID = "join_trip_request_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.JoinTripRequest.findAll",
			QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID = "org.croudtrip.api.trips.JoinTripRequest.findByPassengerOrDriverId",
			QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID_AND_PASSENGER_ACCEPTED_STATUS = "org.croudtrip.api.trips.JoinTripRequest.findByPassengerOrDriverIdAndPassengerAcceptStatus",
            QUERY_FIND_BY_PASSENGER_ID_AND_DECLINED_STATUS = "org.croudtrip.api.trips.JoinTripRequest.findDeclinedRequests",
            QUERY_FIND_BY_PASSENGER_OR_DRIVER_ID_AND_DRIVER_ACCEPTED_STATUS = "org.croudtrip.api.trips.JoinTripRequest.findAcceptedRequests",
			QUERY_FIND_BY_OFFER_ID = "org.croudtrip.api.trips.JoinTripRequest.findByOfferId",
            QUERY_PARAM_USER_ID = "user_id",
			QUERY_PARAM_OFFER_ID = "offer_id";

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

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private JoinTripStatus status;

    @Column(name="estimatedArrivalTimestamp")
    private long estimatedArrivalTimestamp;

	public JoinTripRequest() { }

	@JsonCreator
	public JoinTripRequest(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("totalPriceInCents") int totalPriceInCents,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents,
            @JsonProperty("estimatedArrivalTimestamp") long estimatedArrivalTimestamp,
			@JsonProperty("offer") TripOffer offer,
			@JsonProperty("status") JoinTripStatus status) {

		this.id = id;
		this.query = query;
		this.totalPriceInCents = totalPriceInCents;
		this.pricePerKmInCents = pricePerKmInCents;
		this.offer = offer;
		this.status = status;
        this.estimatedArrivalTimestamp = estimatedArrivalTimestamp;

	}


	public JoinTripRequest(
			JoinTripRequest oldRequest,
			JoinTripStatus newStatus) {
		this(
				oldRequest.getId(),
				oldRequest.getQuery(),
				oldRequest.getTotalPriceInCents(),
				oldRequest.getPricePerKmInCents(),
                oldRequest.getEstimatedArrivalTimestamp(),
				oldRequest.getOffer(),
				newStatus);
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

    public long getEstimatedArrivalTimestamp() { return estimatedArrivalTimestamp; }

	public TripOffer getOffer() {
		return offer;
	}

	public JoinTripStatus getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JoinTripRequest that = (JoinTripRequest) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(totalPriceInCents, that.totalPriceInCents) &&
				Objects.equal(pricePerKmInCents, that.pricePerKmInCents) &&
                Objects.equal(estimatedArrivalTimestamp, that.estimatedArrivalTimestamp) &&
				Objects.equal(query, that.query) &&
				Objects.equal(offer, that.offer) &&
				Objects.equal(status, that.status);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, totalPriceInCents, pricePerKmInCents, offer, status);
	}


	public static class Builder {

		private long id;
		private TripQuery query;
		private int totalPriceInCents;
		private int pricePerKmInCents;
        private long estimatedArrivalTimestamp;
		private TripOffer offer;
		private JoinTripStatus status;

		public Builder setId(long id) {
			this.id = id;
			return this;
		}

		public Builder setQuery(TripQuery query) {
			this.query = query;
			return this;
		}

		public Builder setTotalPriceInCents(int totalPriceInCents) {
			this.totalPriceInCents = totalPriceInCents;
			return this;
		}

		public Builder setPricePerKmInCents(int pricePerKmInCents) {
			this.pricePerKmInCents = pricePerKmInCents;
			return this;
		}

		public Builder setOffer(TripOffer offer) {
			this.offer = offer;
			return this;
		}

		public Builder setStatus(JoinTripStatus status) {
			this.status = status;
			return this;
		}

        public Builder setEstimatedArrivalTimestamp( long estimatedArrivalTimestamp ){
            this.estimatedArrivalTimestamp = estimatedArrivalTimestamp;
            return this;
        }

		public JoinTripRequest build() {
			return new JoinTripRequest(id, query, totalPriceInCents, pricePerKmInCents, estimatedArrivalTimestamp, offer, status);
		}

	}
}
