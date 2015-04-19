package org.croudtrip.auth;


import com.google.common.base.Objects;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Set of basic auth credentials (username + password).
 */
@Entity
@Table(name = "credentials")
class BasicCredentials {

	@Id
	@Column(name = "credentails_id")
	@GeneratedValue
	private long id;

	@OneToOne
	@JoinColumn(name = User.COLUMN_ID)
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