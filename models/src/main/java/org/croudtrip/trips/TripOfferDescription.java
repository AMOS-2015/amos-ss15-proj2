package org.croudtrip.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.directions.Location;

/**
 * A description of a trip that is being offered by a driver.
 */
public class TripOfferDescription {

	private final Location start, end;
	private final float maxDiversionInKm;

	@JsonCreator
	public TripOfferDescription(
			@JsonProperty("start") Location start,
			@JsonProperty("end") Location end,
			@JsonProperty("maxDiversionInKm") float maxDiversionInKm) {

		this.start = start;
		this.end = end;
		this.maxDiversionInKm = maxDiversionInKm;
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


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripOfferDescription)) return false;
		TripOfferDescription offer = (TripOfferDescription) other;
		return Objects.equal(start, offer.start)
				&& Objects.equal(end, offer.end)
				&& Objects.equal(maxDiversionInKm, offer.maxDiversionInKm);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(start, end, maxDiversionInKm);
	}

}
