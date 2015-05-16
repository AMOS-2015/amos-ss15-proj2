package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.directions.Route;

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
			QUERY_FIND_BY_DRIVER_ID = "org.croudtrip.api.trips.TripOffer.findByDriverId",
			QUERY_PARAM_DRIVER_ID = "driver_id";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Embedded
	private Route driverRoute;

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


	public TripOffer() { }

	@JsonCreator
	public TripOffer(
			@JsonProperty("id") long id,
			@JsonProperty("driverRoute") Route driverRoute,
			@JsonProperty("maxDiversionsInMeters") long maxDiversionInMeters,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents,
			@JsonProperty("driver") User driver,
			@JsonProperty("vehicle") Vehicle vehicle) {

		this.id = id;
		this.driverRoute = driverRoute;
		this.maxDiversionInMeters = maxDiversionInMeters;
		this.pricePerKmInCents = pricePerKmInCents;
		this.driver = driver;
		this.vehicle = vehicle;
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


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOffer)) return false;
		TripOffer offer = (TripOffer) other;
		return Objects.equal(id, offer.id)
				&& Objects.equal(driverRoute, offer.driverRoute)
				&& Objects.equal(maxDiversionInMeters, offer.maxDiversionInMeters)
				&& Objects.equal(pricePerKmInCents, offer.pricePerKmInCents)
				&& Objects.equal(driver, offer.driver)
				&& Objects.equal(vehicle, offer.vehicle);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(id, driverRoute, maxDiversionInMeters, pricePerKmInCents, driver, vehicle);
	}

}
