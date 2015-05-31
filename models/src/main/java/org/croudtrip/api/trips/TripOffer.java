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

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;
import org.croudtrip.api.directions.RouteLocation;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
 * A trip that is being offered by a driver.
 */
@Entity(name = TripOffer.ENTITY_NAME)
@Table(name = "offered_trips")
@NamedQueries({
		@NamedQuery(
				name = TripOffer.QUERY_NAME_FIND_ALL,
				query = "SELECT t FROM " + TripOffer.ENTITY_NAME + " t"
		),
		@NamedQuery(
				name = TripOffer.QUERY_NAME_FIND_ALL_ACTIVE,
				query = "SELECT t FROM " + TripOffer.ENTITY_NAME + " t WHERE t.status = 'ACTIVE_NOT_FULL'"
		),
		@NamedQuery(
				name = TripOffer.QUERY_FIND_BY_DRIVER_ID,
				query = "SELECT t FROM " + TripOffer.ENTITY_NAME + " t WHERE t.driver.id = :" + TripOffer.QUERY_PARAM_DRIVER_ID
		)
})
public class TripOffer {

	public static final String
			ENTITY_NAME =  "TripOffer",
			COLUMN_ID = "trip_offer_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.trips.TripOffer.findAll",
			QUERY_NAME_FIND_ALL_ACTIVE = "org.croudtrip.api.trips.TripOffer.findAllActive",
			QUERY_FIND_BY_DRIVER_ID = "org.croudtrip.api.trips.TripOffer.findByDriverId",
			QUERY_PARAM_DRIVER_ID = "driver_id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private Route driverRoute;

	/*
	TODO: Maybe could be useful to have start and end of the drivers rout well defined, but currently there is no urgent need for it.
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "startLat")),
			@AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "startLng"))
	})
	private RouteLocation startLocation;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "destLat")),
			@AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "destLng"))
	})
	private RouteLocation destinationLocation;*/

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name= RouteLocation.COLUMN_LAT, column = @Column(name = "currLat")),
			@AttributeOverride(name= RouteLocation.COLUMN_LNG, column = @Column(name = "currLng"))
	})
	private RouteLocation currentLocation;

    @Column(name = "estimatedArrivalTimeInSeconds", nullable = false)
    private long estimatedArrivalTimeInSeconds;


	@Column(name = "maxDiversionInMeters", nullable = false)
	private long maxDiversionInMeters;

	@Column(name = "pricePerKmInCents", nullable = false)
	private int pricePerKmInCents;

	@ManyToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User driver;

	@ManyToOne
	@JoinColumn(name = Vehicle.COLUMN_ID, nullable = false)
	private Vehicle vehicle;

    @Column(name="lastPositionUpdate", nullable = false)
    private long lastPositonUpdateInSeconds;

	@Column(name = "status", nullable = false)
	@Enumerated(EnumType.STRING)
	private TripOfferStatus status;

	public TripOffer() { }

	@JsonCreator
	public TripOffer(
			@JsonProperty("id") long id,
			@JsonProperty("driverRoute") Route driverRoute,
            @JsonProperty("estimatedArrivalTimeInSeconds") long estimatedArrivalTimeInSeconds,
			@JsonProperty("currentLocation") RouteLocation currentLocation,
			@JsonProperty("maxDiversionsInMeters") long maxDiversionInMeters,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents,
			@JsonProperty("driver") User driver,
			@JsonProperty("vehicle") Vehicle vehicle,
			@JsonProperty("status") TripOfferStatus status,
            @JsonProperty("lastPositionUpdate") long lastPositonUpdateInSeconds) {

		this.id = id;
		this.driverRoute = driverRoute;
        this.estimatedArrivalTimeInSeconds = estimatedArrivalTimeInSeconds;
		this.currentLocation = currentLocation;
		this.maxDiversionInMeters = maxDiversionInMeters;
		this.pricePerKmInCents = pricePerKmInCents;
		this.driver = driver;
		this.vehicle = vehicle;
		this.status = status;
        this.lastPositonUpdateInSeconds = lastPositonUpdateInSeconds;
	}


	public long getId() {
		return id;
	}


	public Route getDriverRoute() {
		return driverRoute;
	}


	public long getMaxDiversionInMeters() {
		return maxDiversionInMeters;
	}


	public int getPricePerKmInCents() {
		return pricePerKmInCents;
	}


	public User getDriver() {
		return driver;
	}


	public Vehicle getVehicle() {
		return vehicle;
	}


	public TripOfferStatus getStatus() {
		return status;
	}

    public long getEstimatedArrivalTimeInSeconds() { return estimatedArrivalTimeInSeconds; }

    public long getLastPositonUpdateInSeconds() { return lastPositonUpdateInSeconds; }

    @JsonProperty("lastPositionUpdate")
    public void setLastPositonUpdateInSeconds(long lastPositonUpdateInSeconds) { this.lastPositonUpdateInSeconds = lastPositonUpdateInSeconds; }

	public RouteLocation getCurrentLocation() { return currentLocation; }

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOffer)) return false;
		TripOffer offer = (TripOffer) other;
		return Objects.equal(id, offer.id)
				&& Objects.equal(driverRoute, offer.driverRoute)
				&& Objects.equal(maxDiversionInMeters, offer.maxDiversionInMeters)
				&& Objects.equal(pricePerKmInCents, offer.pricePerKmInCents)
				&& Objects.equal(driver, offer.driver)
				&& Objects.equal(vehicle, offer.vehicle)
				&& Objects.equal(status, offer.status);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(id, driverRoute, maxDiversionInMeters, pricePerKmInCents, driver, vehicle, status);
	}

}
