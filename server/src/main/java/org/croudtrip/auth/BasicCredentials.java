package org.croudtrip.auth;


import com.google.common.base.Objects;

import java.util.Arrays;

import javax.validation.constraints.NotNull;

/**
 * Set of basic auth credentials (username + password).
 */
class BasicCredentials {

	@NotNull private final String userId;
	@NotNull private final byte[] encryptedPassword;
	@NotNull private final byte[] salt;

	public BasicCredentials(String userId, byte[] encryptedPassword, byte[] salt) {
		this.userId = userId;
		this.encryptedPassword = encryptedPassword;
		this.salt = salt;
	}


	public String getUserId() {
		return userId;
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
		return Objects.equal(userId, credentials.userId)
				&& Arrays.equals(encryptedPassword, credentials.encryptedPassword)
				&& Arrays.equals(salt, credentials.salt);
	}


	@Override
	public int hashCode() {
		return Objects.hashCode(userId, Arrays.hashCode(encryptedPassword), Arrays.hashCode(salt));
	}

}