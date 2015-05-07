package org.croudtrip.api.trips;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Description of a {@link JoinTripRequest}
 */
public class JoinTripRequestDescription {

	private final long reservationId;

	@JsonCreator
	public JoinTripRequestDescription(@JsonProperty("reservationId") long reservationId) {
		this.reservationId = reservationId;
	}

	public long getReservationId() {
		return reservationId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JoinTripRequestDescription that = (JoinTripRequestDescription) o;
		return Objects.equal(reservationId, that.reservationId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(reservationId);
	}

}
