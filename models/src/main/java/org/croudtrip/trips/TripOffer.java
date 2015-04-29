package org.croudtrip.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.auth.User;
import org.croudtrip.directions.Location;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
	@AttributeOverrides({
			@AttributeOverride(name=Location.COLUMN_LAT, column = @Column(name = "startLat")),
			@AttributeOverride(name=Location.COLUMN_LNG, column = @Column(name = "startLng"))
	})
	private Location start;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name=Location.COLUMN_LAT, column = @Column(name = "endLat")),
			@AttributeOverride(name=Location.COLUMN_LNG, column = @Column(name = "endLng"))
	})
	private Location end;

	@Column(name = "maxDiversionInKm", nullable = false)
	private float maxDiversionInKm;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User owner;


	public TripOffer() { }

	@JsonCreator
	public TripOffer(
			@JsonProperty("id") long id,
			@JsonProperty("start") Location start,
			@JsonProperty("end") Location end,
			@JsonProperty("maxDiversionInKm") float maxDiversionInKm,
			@JsonProperty("owner") User owner) {

		this.id = id;
		this.start = start;
		this.end = end;
		this.maxDiversionInKm = maxDiversionInKm;
		this.owner = owner;
	}


	public long getId() {
		return id;
	}


	public Location getStart() {
		return start;
	}


	public Location getEnd() {
		return end;
	}


	public float getMaxDiversionInKm() {
		return maxDiversionInKm;
	}


	public User getOwner() {
		return owner;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOffer)) return false;
		TripOffer offer = (TripOffer) other;
		return Objects.equal(id, offer.id)
				&& Objects.equal(start, offer.start)
				&& Objects.equal(end, offer.end)
				&& Objects.equal(maxDiversionInKm, offer.maxDiversionInKm)
				&& Objects.equal(owner, offer.owner);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(id, start, end, maxDiversionInKm, owner);
	}

}
