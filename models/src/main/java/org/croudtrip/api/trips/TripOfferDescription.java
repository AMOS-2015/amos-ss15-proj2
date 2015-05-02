package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.directions.RouteLocation;

/**
 * A description of a trip that is being offered by a driver.
 */
public class TripOfferDescription {

	private final RouteLocation start, end;
	private final long maxDiversionInMeters;
	private final int pricePerKmInCents;

	@JsonCreator
	public TripOfferDescription(
			@JsonProperty("start") RouteLocation start,
			@JsonProperty("end") RouteLocation end,
			@JsonProperty("maxDiversionInMeters") long maxDiversionInMeters,
			@JsonProperty("pricePerKmInCents") int pricePerKmInCents) {

		this.start = start;
		this.end = end;
		this.maxDiversionInMeters = maxDiversionInMeters;
		this.pricePerKmInCents = pricePerKmInCents;
	}


	public RouteLocation getStart() {
		return start;
	}


	public RouteLocation getEnd() {
		return end;
	}


	public long getMaxDiversionInMeters() {
		return maxDiversionInMeters;
	}


	public int getPricePerKmInCents() {
		return pricePerKmInCents;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOfferDescription)) return false;
		TripOfferDescription offer = (TripOfferDescription) other;
		return Objects.equal(start, offer.start)
				&& Objects.equal(end, offer.end)
				&& Objects.equal(maxDiversionInMeters, offer.maxDiversionInMeters)
				&& Objects.equal(pricePerKmInCents, offer.pricePerKmInCents);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(start, end, maxDiversionInMeters, pricePerKmInCents);
	}

}
