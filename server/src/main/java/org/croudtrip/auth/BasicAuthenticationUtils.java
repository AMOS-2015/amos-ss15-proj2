package org.croudtrip.auth;


import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Helper methods for dealing with basic authentication.
 */
public class BasicAuthenticationUtils {

	public byte[] generateSalt() {
		try {
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[8];
			random.nextBytes(salt);
			return salt;
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException(nsae);
		}
	}


	public boolean checkPassword(String inputPassword, BasicCredentials storedCredentials) {
		byte[] encryptedInputPassword = getEncryptedPassword(inputPassword, storedCredentials.getSalt());
		return Arrays.equals(storedCredentials.getEncryptedPassword(), encryptedInputPassword);
	}


	public byte[] getEncryptedPassword(String password, byte[] salt) {
		try {
			int derivedKeyLength = 160;
			int iterations = 20000;
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			return factory.generateSecret(spec).getEncoded();
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}