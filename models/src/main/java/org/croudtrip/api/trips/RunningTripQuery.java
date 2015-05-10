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
 * A trip query that is running on the server ("background search").
 */
@Entity(name = RunningTripQuery.ENTITY_NAME)
@Table(name = "running_trip_queries")
@NamedQueries({
		@NamedQuery(
				name = RunningTripQuery.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + RunningTripQuery.ENTITY_NAME + " r"
		)
})
public class RunningTripQuery {

	public static final String
			ENTITY_NAME =  "RunningTripQuery",
			COLUMN_ID = "running_trip_query_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.JoinTripRequest.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private TripQuery query;

	public RunningTripQuery() { }

	@JsonCreator
	public RunningTripQuery(
			@JsonProperty("id") long id,
			@JsonProperty("query") TripQuery query) {

		this.id = id;
		this.query = query;
	}

	public long getId() {
		return id;
	}

	public TripQuery getQuery() {
		return query;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RunningTripQuery that = (RunningTripQuery) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(query, that.query);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, query);
	}

}
