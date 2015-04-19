package org.croudtrip.auth;


import org.croudtrip.utils.Assert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates, stores and finds registered users.
 */
@Singleton
public class UserManager {

	private final Map<String, User> userMap = new HashMap<>();

	// basic authentication
	private final Map<String, BasicCredentials> credentialsMap = new HashMap<>();
	private final BasicAuthenticationUtils authenticationUtils;

	@Inject
	UserManager(BasicAuthenticationUtils authenticationUtils) {
		this.authenticationUtils = authenticationUtils;
	}


	public User addUser(UserDescription userDescription) {
		// email must be unique
		Assert.assertTrue(
				findUserByEmail(userDescription.getEmail()) == null,
				"user with email " + userDescription.getEmail() + " already registered");

		// store new user
		String userId = UUID.randomUUID().toString();
		User user = new User(userId, userDescription.getEmail(), userDescription.getFirstName(), userDescription.getLastName());

		// store user
		userMap.put(userId, user);

		// store credentials
		byte[] salt = authenticationUtils.generateSalt();
		byte[] encryptedPassword = authenticationUtils.getEncryptedPassword(userDescription.getPassword(), salt);
		BasicCredentials credentials = new BasicCredentials(user.getId(), encryptedPassword, salt);
		credentialsMap.put(userId, credentials);

		return user;
	}


	public User getUser(String userId) {
		return userMap.get(userId);
	}


	public List<User> getAllUsers() {
		return new LinkedList<>(userMap.values());
	}


	public void removeUser(User user) {
		userMap.remove(user.getId());
		credentialsMap.remove(user.getId());
	}


	public User findUserByEmail(String email) {
		// TODO better performance via db query
		for (User user : userMap.values()) {
			if (user.getEmail().equals(email)) return user;
		}
		return null;
	}

}
