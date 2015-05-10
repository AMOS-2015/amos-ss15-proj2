package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.xml.stream.Location;

/**
 * A passenger query for available routes.
 */
@Embeddable
public class TripQuery {

	@Embedded
	private Route passengerRoute;

    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "sLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "sLng"))
    })
    private RouteLocation startLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "eLat")),
            @AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "eLng"))
    })
    private RouteLocation destinationLocation;

	@Column(name = "maxWaitingTimeInSeconds", nullable = false)
	private long maxWaitingTimeInSeconds;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID + "_passenger", nullable = false)
	private User passenger;


	public TripQuery() { }

	@JsonCreator
	public TripQuery(
			@JsonProperty("passengerRoute") Route passengerRoute,
            @JsonProperty("startLocation") RouteLocation startLocation,
            @JsonProperty("destinationLocation") RouteLocation destinationLocation,
			@JsonProperty("maxWaitingTimeSeconds") long maxWaitingTimeInSeconds,
			@JsonProperty("passenger") User passenger) {

		this.passengerRoute = passengerRoute;
        this.startLocation = startLocation;
        this.destinationLocation = destinationLocation;
		this.maxWaitingTimeInSeconds = maxWaitingTimeInSeconds;
		this.passenger = passenger;
	}


	public Route getPassengerRoute() {
		return passengerRoute;
	}


	public long getMaxWaitingTimeInSeconds() {
		return maxWaitingTimeInSeconds;
	}


	public User getPassenger() {
		return passenger;
	}

    public RouteLocation getStartLocation() { return startLocation; }

    public RouteLocation getDestinationLocation() { return destinationLocation; }


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripQuery tripQuery = (TripQuery) o;
		return Objects.equal(maxWaitingTimeInSeconds, tripQuery.maxWaitingTimeInSeconds) &&
                Objects.equal(startLocation, tripQuery.startLocation) &&
                Objects.equal(destinationLocation, tripQuery.destinationLocation) &&
				Objects.equal(passengerRoute, tripQuery.passengerRoute) &&
				Objects.equal(passenger, tripQuery.passenger);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(passengerRoute, maxWaitingTimeInSeconds, passenger, startLocation, destinationLocation);
	}

}
