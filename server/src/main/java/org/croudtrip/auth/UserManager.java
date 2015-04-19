package org.croudtrip.auth;


import com.google.common.base.Optional;

import org.croudtrip.db.UserDAO;
import org.croudtrip.utils.Assert;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates, stores and finds registered users.
 */
@Singleton
public class UserManager {

	private final UserDAO userDAO;

	// basic authentication
	// private final Map<String, BasicCredentials> credentialsMap = new HashMap<>();
	private final BasicAuthenticationUtils authenticationUtils;

	@Inject
	UserManager(UserDAO userDAO, BasicAuthenticationUtils authenticationUtils) {
		this.userDAO = userDAO;
		this.authenticationUtils = authenticationUtils;
	}


	public User addUser(UserDescription userDescription) {
		// email must be unique
		Assert.assertFalse(
				findUserByEmail(userDescription.getEmail()).isPresent(),
				"user with email " + userDescription.getEmail() + " already registered");

		// store new user
		User user = new User(0, userDescription.getEmail(), userDescription.getFirstName(), userDescription.getLastName());
		userDAO.save(user);

		/*
		// store credentials
		byte[] salt = authenticationUtils.generateSalt();
		byte[] encryptedPassword = authenticationUtils.getEncryptedPassword(userDescription.getPassword(), salt);
		BasicCredentials credentials = new BasicCredentials(user.getId(), encryptedPassword, salt);
		credentialsMap.put(userId, credentials);
		*/

		return user;
	}


	public Optional<User> getUser(long userId) {
		return userDAO.findById(userId);
	}


	public List<User> getAllUsers() {
		return userDAO.findAll();
	}


	public void deleteUser(User user) {
		userDAO.delete(user);
	}


	public Optional<User> findUserByEmail(String email) {
		return userDAO.findByEmail(email);
	}

}
