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

package org.croudtrip.auth;


import com.google.common.base.Objects;

import org.croudtrip.api.account.User;

import java.util.Arrays;

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
 * Set of basic auth credentials.
 */
@Entity(name = BasicCredentials.ENTITY_NAME)
@Table(name = "credentials")
@NamedQueries({
		@NamedQuery(
				name = BasicCredentials.QUERY_NAME_FIND_BY_USER_ID,
				query = "SELECT c FROM " + BasicCredentials.ENTITY_NAME + " c WHERE c.user.id = :" + BasicCredentials.QUERY_PARAM_USER_ID
		)
})
public class BasicCredentials {

	public static final String
			ENTITY_NAME = "BasicCredentials",
			COLUMN_ID = "credentials_id",
			QUERY_NAME_FIND_BY_USER_ID = "org.croudtrip.auth.BasicCredentials.findByUserId",
			QUERY_PARAM_USER_ID = "user_id";

	@Id
	@Column(name = COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne
	@JoinColumn(name = User.COLUMN_ID, nullable = false)
	private User user;

	@Column(name = "encryptedPassword", nullable = false)
	private byte[] encryptedPassword;

	@Column(name = "salt", nullable = false)
	private byte[] salt;


	BasicCredentials() { }

	public BasicCredentials(long id, User user, byte[] encryptedPassword, byte[] salt) {
		this.id = id;
		this.user = user;
		this.encryptedPassword = encryptedPassword;
		this.salt = salt;
	}


	public long getId() {
		return id;
	}


	public User getUser() {
		return user;
	}


	public byte[] getEncryptedPassword() {
		return encryptedPassword;
	}


	public byte[] getSalt() {
		return salt;
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof BasicCredentials)) return false;
		BasicCredentials credentials = (BasicCredentials) other;
		return Objects.equal(user, credentials.user)
				&& Arrays.equals(encryptedPassword, credentials.encryptedPassword)
				&& Arrays.equals(salt, credentials.salt);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(user.hashCode(), Arrays.hashCode(encryptedPassword), Arrays.hashCode(salt));
	}

}