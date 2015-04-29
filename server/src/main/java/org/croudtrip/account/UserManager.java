package org.croudtrip.account;


import com.google.common.base.Optional;

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
		// email must be unique
		Assert.assertFalse(
				findUserByEmail(userDescription.getEmail()).isPresent(),
				"user with email " + userDescription.getEmail() + " already registered");

		// store new user
		User user = new User(0, userDescription.getEmail(), userDescription.getFirstName(), userDescription.getLastName(), null, null, null, null, null);
		userDAO.save(user);

		// store credentials
		byte[] salt = authenticationUtils.generateSalt();
		byte[] encryptedPassword = authenticationUtils.getEncryptedPassword(userDescription.getPassword(), salt);
		BasicCredentials credentials = new BasicCredentials(user, encryptedPassword, salt);
		credentialsDAO.save(credentials);

		return user;
	}


	public User updateUser(User user) {
		userDAO.update(user);
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


}
