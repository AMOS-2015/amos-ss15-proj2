package org.croudtrip.account;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.auth.BasicAuthenticationUtils;
import org.croudtrip.auth.BasicCredentials;
import org.croudtrip.db.BasicCredentialsDAO;
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
	private final BasicCredentialsDAO credentialsDAO;
	private final BasicAuthenticationUtils authenticationUtils;

	@Inject
	UserManager(UserDAO userDAO, BasicCredentialsDAO credentialsDAO, BasicAuthenticationUtils authenticationUtils) {
		this.userDAO = userDAO;
		this.credentialsDAO = credentialsDAO;
		this.authenticationUtils = authenticationUtils;
	}


	public User addUser(UserDescription userDescription) {
		//  email, first name, last name and password cannot be null
		Assert.assertNotNull(userDescription.getEmail(), userDescription.getFirstName(), userDescription.getLastName(), userDescription.getPassword());
		// email must be unique
		Assert.assertFalse(
				findUserByEmail(userDescription.getEmail()).isPresent(),
				"user with email " + userDescription.getEmail() + " already registered");

		// store new user
		long lastModified = System.currentTimeMillis() / 1000;
		User user = new User(0, userDescription.getEmail(), userDescription.getFirstName(), userDescription.getLastName(), null, null, null, null, null, lastModified);
		userDAO.save(user);

		// store credentials
		byte[] salt = authenticationUtils.generateSalt();
		byte[] encryptedPassword = authenticationUtils.getEncryptedPassword(userDescription.getPassword(), salt);
		BasicCredentials credentials = new BasicCredentials(0, user, encryptedPassword, salt);
		credentialsDAO.save(credentials);

		return user;
	}


	public User updateUser(User user, UserDescription userDescription) {
		// update user
		User updatedUser = new User(user.getId(),
				getNonNull(userDescription.getEmail(), user.getEmail()),
				getNonNull(userDescription.getFirstName(), user.getFirstName()),
				getNonNull(userDescription.getLastName(), user.getLastName()),
				getNonNull(userDescription.getPhoneNumber(), user.getPhoneNumber()),
				getNonNull(userDescription.getIsMale(), user.getIsMale()),
				getNonNull(userDescription.getBirthday(), user.getBirthDay()),
				getNonNull(userDescription.getAddress(), user.getAddress()),
				getNonNull(userDescription.getAvatarUrl(), user.getAvatarUrl()),
				System.currentTimeMillis() / 1000);
		userDAO.update(updatedUser);

		// update password
		if (userDescription.getPassword() != null) {
			byte[] salt = authenticationUtils.generateSalt();
			byte[] encryptedPassword = authenticationUtils.getEncryptedPassword(userDescription.getPassword(), salt);
			BasicCredentials credentials = credentialsDAO.findByUserId(user.getId()).get();
			BasicCredentials updatedCredentials = new BasicCredentials(credentials.getId(), updatedUser, encryptedPassword, salt);
			credentialsDAO.update(updatedCredentials);
		}

		return user;
	}


	public Optional<User> findUserById(long userId) {
		return userDAO.findById(userId);
	}


	public List<User> findAllUsers() {
		return userDAO.findAll();
	}


	public Optional<User> findUserByEmail(String email) {
		return userDAO.findByEmail(email);
	}


	public void deleteUser(User user) {
		credentialsDAO.delete(credentialsDAO.findByUserId(user.getId()).get());
		userDAO.delete(user);
	}


	public Optional<BasicCredentials> findCredentialsByUserId(long userId) {
		return credentialsDAO.findByUserId(userId);
	}


	private <T> T getNonNull(T value, T defaultValue) {
		if (value == null) return defaultValue;
		return value;
	}

}
