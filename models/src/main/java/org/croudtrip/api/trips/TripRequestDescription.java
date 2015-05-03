package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.directions.RouteLocation;

import javax.validation.constraints.NotNull;

/**
 * A description of a trip request created by potential passengers.
 */
public class TripRequestDescription {

	@NotNull private final RouteLocation start;
	@NotNull private final RouteLocation end;

	@JsonCreator
	public TripRequestDescription(
			@JsonProperty("start") RouteLocation start,
			@JsonProperty("end") RouteLocation end) {

		this.start = start;
		this.end = end;
	}


	public RouteLocation getStart() {
		return start;
	}


	public RouteLocation getEnd() {
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
