package org.croudtrip.api.account;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity(name = Vehicle.ENTITY_NAME)
@Table(name = "vehicles")
@NamedQueries({
		@NamedQuery(
				name = Vehicle.QUERY_NAME_FIND_BY_USER_ID,
				query = "SELECT v FROM " + Vehicle.ENTITY_NAME + " v WHERE v.owner.id = :" + Vehicle.QUERY_PARAM_USER_ID
		)
})
public class Vehicle {

	public static final String
			ENTITY_NAME = "Vehicle",
			COLUMN_ID = "vehicle_id",
			QUERY_NAME_FIND_BY_USER_ID = "org.croudtrip.api.account.Vehicle.findByUserId",
			QUERY_PARAM_USER_ID = "user_id";


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	@NotNull
	private long id;

	@Column(name = "license_plate", nullable = false)
	@NotNull
	public String licensePlate;

	@Column(name = "color", nullable = true)
	private String color;

	@Column(name = "type", nullable = true)
	private String type;

	@Column(name = "capacity", nullable = true)
	private int capacity;

	@OneToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	@NotNull
	private User owner;


	public Vehicle() { }


	@JsonCreator
	public Vehicle(
			@JsonProperty("id") long id,
			@JsonProperty("licensePlate") String licensePlate,
			@JsonProperty("color") String color,
			@JsonProperty("type") String type,
			@JsonProperty("capacity") int capacity,
			@JsonProperty("owner") User owner) {

		this.id = id;
		this.licensePlate = licensePlate;
		this.color = color;
		this.type = type;
		this.capacity = capacity;
		this.owner = owner;
	}


	public long getId() {
		return id;
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


	public User getOwner() {
		return owner;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vehicle vehicle = (Vehicle) o;
		return Objects.equal(id, vehicle.id) &&
				Objects.equal(capacity, vehicle.capacity) &&
				Objects.equal(licensePlate, vehicle.licensePlate) &&
				Objects.equal(color, vehicle.color) &&
				Objects.equal(type, vehicle.type) &&
				Objects.equal(owner, vehicle.owner);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(id, licensePlate, color, type, capacity, owner);
	}

}
