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

/**
 * Updates to a {@link TripOffer}.
 */
public class TripOfferUpdate {

	public static TripOfferUpdate createNewStartUpdate(RouteLocation updatedStart) {
		return new TripOfferUpdate(updatedStart, false, false);
	}

	public static TripOfferUpdate createFinishUpdate() {
		return new TripOfferUpdate(null, true, false);
	}

	public static TripOfferUpdate createCancelUpdate() {
		return new TripOfferUpdate(null, false, true);
	}


	private final RouteLocation updatedStart;
	private final boolean finishOffer;
	private final boolean cancelOffer;


	@JsonCreator
	public TripOfferUpdate(
			@JsonProperty("updatedStart") RouteLocation updatedStart,
			@JsonProperty("finishOffer") boolean finishOffer,
			@JsonProperty("cancelOffer") boolean cancelOffer) {
		this.updatedStart = updatedStart;
		this.finishOffer = finishOffer;
		this.cancelOffer = cancelOffer;
	}

	public RouteLocation getUpdatedStart() {
		return updatedStart;
	}

	public boolean getFinishOffer() {
		return finishOffer;
	}

	public boolean getCancelOffer() {
		return cancelOffer;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TripOfferUpdate that = (TripOfferUpdate) o;
		return Objects.equal(finishOffer, that.finishOffer) &&
				Objects.equal(cancelOffer, that.cancelOffer) &&
				Objects.equal(updatedStart, that.updatedStart);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(updatedStart, finishOffer, cancelOffer);
	}
}
