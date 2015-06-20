package org.croudtrip.api.trips;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A collection of {@link JoinTripRequest}s which together form a "super trip" for one
 * passenger.
 */
@Entity(name = SuperTrip.ENTITY_NAME)
@Table(name = "super_trips")
@NamedQueries({
		@NamedQuery(
				name = SuperTrip.QUERY_NAME_FIND_ALL,
				query = "SELECT s FROM " + SuperTrip.ENTITY_NAME + " s"
		),
		@NamedQuery(
				name = SuperTrip.QUERY_FIND_BY_PASSENGER_ID,
				query = "SELECT s FROM " + SuperTrip.ENTITY_NAME + " s WHERE " +
						"s.query.passenger.id = :" + SuperTrip.QUERY_PARAM_USER_ID
		)
})
public class SuperTrip {

	public static final String
			ENTITY_NAME = "SuperTrip",
			COLUMN_ID = "super_trip_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.SuperTrip.findAll",
			QUERY_FIND_BY_PASSENGER_ID = "org.croudtrip.api.trips.SuperTrip.findByPassengerId",
			QUERY_PARAM_USER_ID = "user_id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	@OneToMany(mappedBy = "superTrip", fetch = FetchType.EAGER)
	@JsonBackReference
	private List<JoinTripRequest> joinRequests;


	public SuperTrip() { }

	@JsonCreator
	public SuperTrip(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query,
			@JsonProperty("joinRequests") List<JoinTripRequest> joinRequests) {

		this.id = id;
		this.query = query;
		this.joinRequests = joinRequests;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	/**
	 * When deserializing this object from JSON, this field will always be NULL!!
	 */
	public List<JoinTripRequest> getJoinRequests() {
		return joinRequests;
	}

	public void setJoinRequests(List<JoinTripRequest> joinRequests) {
		this.joinRequests = joinRequests;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SuperTrip that = (SuperTrip) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(query, that.query) &&
				Objects.equal(joinRequests, that.joinRequests);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query, joinRequests);
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

		public SuperTrip build() {
			return new SuperTrip(id, query, requests);
		}

	}

}
