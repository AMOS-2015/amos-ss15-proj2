package org.croudtrip.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.directions.RouteLocation;

/**
 * A description of a trip that is being offered by a driver.
 */
public class TripOfferDescription {

	private final RouteLocation start, end;
	private final long maxDiversionInMeters;

	@JsonCreator
	public TripOfferDescription(
			@JsonProperty("start") RouteLocation start,
			@JsonProperty("end") RouteLocation end,
			@JsonProperty("maxDiversionInMeters") long maxDiversionInMeters) {

		this.start = start;
		this.end = end;
		this.maxDiversionInMeters = maxDiversionInMeters;
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


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOfferDescription)) return false;
		TripOfferDescription offer = (TripOfferDescription) other;
		return Objects.equal(start, offer.start)
				&& Objects.equal(end, offer.end)
				&& Objects.equal(maxDiversionInMeters, offer.maxDiversionInMeters);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(start, end, maxDiversionInMeters);
	}

}
