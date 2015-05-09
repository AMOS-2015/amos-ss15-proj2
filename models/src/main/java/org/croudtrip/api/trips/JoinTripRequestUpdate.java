package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Confirms or declines a {@link JoinTripRequest},
 */
public class JoinTripRequestUpdate {

	private final boolean acceptPassenger;

	@JsonCreator
	public JoinTripRequestUpdate(@JsonProperty("acceptPassenger") boolean acceptPassenger) {
		this.acceptPassenger = acceptPassenger;
	}

	public boolean getAcceptPassenger() {
		return acceptPassenger;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JoinTripRequestUpdate that = (JoinTripRequestUpdate) o;
		return Objects.equal(acceptPassenger, that.acceptPassenger);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(acceptPassenger);
	}

}
