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

package org.croudtrip.api.gcm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import org.croudtrip.api.account.User;

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

/**
 * One GCM registration.
 */
@Entity(name = GcmRegistration.ENTITY_NAME)
@Table(name = "gcm_registrations")
@NamedQueries({
		@NamedQuery(
				name = GcmRegistration.QUERY_NAME_FIND_ALL,
				query = "SELECT r FROM " + GcmRegistration.ENTITY_NAME + " r"
		),
		@NamedQuery(
				name = GcmRegistration.QUERY_NAME_FIND_BY_USER_ID,
				query = "SELECT r FROM " + GcmRegistration.ENTITY_NAME + " r WHERE r.user.id = :" + GcmRegistration.QUERY_PARAM_USER_ID
		)
})
public class GcmRegistration {

	public static final String
			ENTITY_NAME = "GcmRegistration",
			COLUMN_ID = "gcm_registration_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.api.gcm.GcmRegistration.findAll",
			QUERY_NAME_FIND_BY_USER_ID = "org.croudtrip.api.gcm.GcmRegistration.findByUserId",
			QUERY_PARAM_USER_ID = "user_id";


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Column(name = "gcm_id", nullable = false)
	private String gcmId;

	@OneToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User user;


	GcmRegistration() { }

	@JsonCreator
	public GcmRegistration(
			@JsonProperty("id") long id,
			@JsonProperty("gcmId") String gcmId,
			@JsonProperty("user") User user) {

		this.id = id;
		this.gcmId = gcmId;
		this.user = user;
	}

	public long getId() {
		return id;
	}

	public String getGcmId() {
		return gcmId;
	}

	public User getUser() {
		return user;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GcmRegistration that = (GcmRegistration) o;
		return Objects.equal(id, that.id) &&
				Objects.equal(gcmId, that.gcmId) &&
				Objects.equal(user, that.user);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, gcmId, user);
	}

}
