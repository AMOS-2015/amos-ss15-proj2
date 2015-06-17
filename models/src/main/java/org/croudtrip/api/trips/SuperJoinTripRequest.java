package org.croudtrip.api.trips;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A collection of {@link JoinTripRequest}s.
 */
@Entity(name = SuperJoinTripRequest.ENTITY_NAME)
@Table(name = "super_join_trip_requests")
@NamedQueries({
		@NamedQuery(
				name = SuperJoinTripRequest.QUERY_NAME_FIND_ALL,
				query = "SELECT s FROM " + SuperJoinTripRequest.ENTITY_NAME + " s"
		),
		@NamedQuery(
				name = SuperJoinTripRequest.QUERY_FIND_BY_PASSENGER_ID,
				query = "SELECT s FROM " + SuperJoinTripRequest.ENTITY_NAME + " s WHERE " +
						"s.query.passenger.id = :" + SuperJoinTripRequest.QUERY_PARAM_USER_ID
		)
})
public class SuperJoinTripRequest {

	public static final String
			ENTITY_NAME = "SuperJoinTripRequest",
			COLUMN_ID = "super_join_trip_request_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.SuperJoinTripRequest.findAll",
			QUERY_FIND_BY_PASSENGER_ID = "org.croudtrip.api.trips.SuperJoinTripRequest.findByPassengerId",
			QUERY_PARAM_USER_ID = "user_id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	@OneToMany(mappedBy = "superJoinTripRequest")
	private List<JoinTripRequest> requests;


	public SuperJoinTripRequest() {
	}

	@JsonCreator
	public SuperJoinTripRequest(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("requests") List<JoinTripRequest> requests) {

		this.id = id;
		this.query = query;
		this.requests = requests;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	public List<JoinTripRequest> getRequests() {
		return requests;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SuperJoinTripRequest that = (SuperJoinTripRequest) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(query, that.query) &&
				Objects.equal(requests, that.requests);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, requests);
	}

	public static class Builder {

		private long id;
		private TripQuery query;
		private final List<JoinTripRequest> requests = new ArrayList<>();

		public Builder setId(long id) {
			this.id = id;
			return this;
		}

		public Builder setQuery(TripQuery query) {
			this.query = query;
			return this;
		}

		public Builder addRequest(JoinTripRequest request) {
			this.requests.add(request);
			return this;
		}

		public SuperJoinTripRequest build() {
			return new SuperJoinTripRequest(id, query, requests);
		}

	}

}
