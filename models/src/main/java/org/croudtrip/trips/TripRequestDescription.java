package org.croudtrip.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.directions.Location;

import javax.validation.constraints.NotNull;

/**
 * A description of a trip request created by potential passengers.
 */
public class TripRequestDescription {

	@NotNull private final Location start;
	@NotNull private final Location end;

	@JsonCreator
	public TripRequestDescription(
			@JsonProperty("start") Location start,
			@JsonProperty("end") Location end) {

		this.start = start;
		this.end = end;
	}


	public Location getStart() {
		return start;
	}


	public Location getEnd() {
		return end;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripRequestDescription)) return false;
		TripRequestDescription request = (TripRequestDescription) other;
		return Objects.equal(start, request.start)
				&& Objects.equal(end, request.end);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(start, end);
	}
}
