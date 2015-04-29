package org.croudtrip.account;


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
