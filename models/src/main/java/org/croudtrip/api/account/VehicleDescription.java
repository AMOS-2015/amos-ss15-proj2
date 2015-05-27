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

package org.croudtrip.api.account;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

public class VehicleDescription {

	@NotNull
	public final String licensePlate;
	private final String color, type;
	private final int capacity;


	@JsonCreator
	public VehicleDescription(
			@JsonProperty("licensePlate") String licensePlate,
			@JsonProperty("color") String color,
			@JsonProperty("type") String type,
			@JsonProperty("capacity") int capacity) {

		this.licensePlate = licensePlate;
		this.color = color;
		this.type = type;
		this.capacity = capacity;
	}


	public String getLicensePlate() {
		return licensePlate;
	}


	public String getColor() {
		return color;
	}


	public String getType() {
		return type;
	}


	public int getCapacity() {
		return capacity;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VehicleDescription vehicle = (VehicleDescription) o;
		return Objects.equal(capacity, vehicle.capacity) &&
				Objects.equal(licensePlate, vehicle.licensePlate) &&
				Objects.equal(color, vehicle.color) &&
				Objects.equal(type, vehicle.type);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(licensePlate, color, type, capacity);
	}

}
