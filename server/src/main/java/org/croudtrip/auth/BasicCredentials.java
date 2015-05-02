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

	public BasicCredentials(User user, byte[] encryptedPassword, byte[] salt) {
		this.user = user;
		this.encryptedPassword = encryptedPassword;
		this.salt = salt;
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