package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.account.User;
import org.croudtrip.api.directions.Route;

/**
 * A potential match between a driver and a passenger.
 */
public class TripMatch {

	private final long offerId;
	private final Route route;
	private final long diversionInMeters, diversionInSeconds;
	private final int estimatedPriceInCents, pricePerKilometer;
	private final User driver, passenger;

	@JsonCreator
	public TripMatch(
			@JsonProperty("offerId") long offerId,
			@JsonProperty("route") Route route,
			@JsonProperty("diversionInMeters") long diversionInMeters,
			@JsonProperty("diversionInSeconds") long diversionInSeconds,
			@JsonProperty("estimatedPriceInCents") int estimatedPriceInCents,
            @JsonProperty("pricePerKilometer") int pricePerKilometer,
			@JsonProperty("driver") User driver,
			@JsonProperty("passenger") User passenger) {

		this.offerId = offerId;
		this.route = route;
		this.diversionInMeters = diversionInMeters;
		this.diversionInSeconds = diversionInSeconds;
		this.estimatedPriceInCents = estimatedPriceInCents;
        this.pricePerKilometer = pricePerKilometer;
		this.driver = driver;
		this.passenger = passenger;
	}


	public long getOfferId() {
		return offerId;
	}


	public Route getRoute() {
		return route;
	}


	public long getDiversionInMeters() {
		return diversionInMeters;
	}


	public long getDiversionInSeconds() {
		return diversionInSeconds;
	}


	public int getEstimatedPriceInCents() {
		return estimatedPriceInCents;
	}


    public int getPricePerKilometerInCents() { return pricePerKilometer; }


	public User getDriver() {
		return driver;
	}


	public User getPassenger() {
		return passenger;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripMatch)) return false;
		TripMatch offer = (TripMatch) other;
		return Objects.equal(offerId, offer.offerId)
				&& Objects.equal(route, offer.route)
				&& Objects.equal(diversionInMeters, offer.diversionInMeters)
				&& Objects.equal(diversionInSeconds, offer.diversionInSeconds)
				&& Objects.equal(estimatedPriceInCents, offer.estimatedPriceInCents)
                && Objects.equal(pricePerKilometer, offer.pricePerKilometer)
				&& Objects.equal(driver, offer.driver)
				&& Objects.equal(passenger, offer.passenger);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(offerId, route, diversionInMeters, diversionInSeconds, estimatedPriceInCents, pricePerKilometer, driver, passenger);
	}

}
