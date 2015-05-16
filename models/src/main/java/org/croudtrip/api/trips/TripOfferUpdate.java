package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.directions.RouteLocation;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Updates to a {@link TripOffer}.
 */
public class TripOfferUpdate {

	@Valid @NotNull
	private final RouteLocation updatedStart;

	@JsonCreator
	public TripOfferUpdate(@JsonProperty("updatedStart") RouteLocation updatedStart) {
		this.updatedStart = updatedStart;
	}

	public RouteLocation getUpdatedStart() {
		return updatedStart;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripOfferUpdate that = (TripOfferUpdate) o;
		return Objects.equal(updatedStart, that.updatedStart);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(updatedStart);
	}

}
