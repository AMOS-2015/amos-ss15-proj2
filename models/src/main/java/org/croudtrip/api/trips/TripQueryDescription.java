package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.directions.RouteLocation;

import javax.validation.constraints.NotNull;

/**
 * A (price) reservation for one matched trip.
 */
public class TripQueryDescription {

	@NotNull private final RouteLocation start;
	@NotNull private final RouteLocation end;
    private final long maxWaitingTime;

	@JsonCreator
	public TripQueryDescription(
			@JsonProperty("start") RouteLocation start,
			@JsonProperty("end") RouteLocation end,
            @JsonProperty("maxWaitingTime") long maxWaitingTime) {

		this.start = start;
		this.end = end;
        this.maxWaitingTime = maxWaitingTime;
	}

	public RouteLocation getStart() {
		return start;
	}

	public RouteLocation getEnd() {
		return end;
	}

    public long getMaxWaitingTime() { return maxWaitingTime; }

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripQueryDescription)) return false;
		TripQueryDescription request = (TripQueryDescription) other;
		return Objects.equal(start, request.start)
				&& Objects.equal(end, request.end)
                && Objects.equal(maxWaitingTime, request.maxWaitingTime);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(start, end);
	}
}
