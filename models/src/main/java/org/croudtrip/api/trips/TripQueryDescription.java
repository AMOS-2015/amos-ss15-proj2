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

import org.croudtrip.api.directions.RouteLocation;

import javax.validation.constraints.NotNull;

/**
 * A (price) reservation for one matched trip.
 */
public class TripQueryDescription {

	@NotNull private final RouteLocation start;
	@NotNull private final RouteLocation end;
    private final long maxWaitingTimeInSeconds;

	@JsonCreator
	public TripQueryDescription(
			@JsonProperty("start") RouteLocation start,
			@JsonProperty("end") RouteLocation end,
            @JsonProperty("maxWaitingTimeInSeconds") long maxWaitingTimeInSeconds) {

		this.start = start;
		this.end = end;
        this.maxWaitingTimeInSeconds = maxWaitingTimeInSeconds;
	}

	public RouteLocation getStart() {
		return start;
	}

	public RouteLocation getEnd() {
		return end;
	}

    public long getMaxWaitingTimeInSeconds() { return maxWaitingTimeInSeconds; }

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof TripQueryDescription)) return false;
		TripQueryDescription request = (TripQueryDescription) other;
		return Objects.equal(start, request.start)
				&& Objects.equal(end, request.end)
                && Objects.equal(maxWaitingTimeInSeconds, request.maxWaitingTimeInSeconds);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(start, end);
	}
}
