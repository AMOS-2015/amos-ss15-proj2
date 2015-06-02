package org.croudtrip.account;

import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.auth.BasicAuthenticationUtils;
import org.croudtrip.auth.BasicCredentials;
import org.croudtrip.db.BasicCredentialsDAO;
import org.croudtrip.db.UserDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;


@RunWith(JMockit.class)
public class UserManagerTest {

	@Mocked UserDAO userDAO;
	@Mocked BasicCredentialsDAO basicCredentialsDAO;
	@Mocked BasicAuthenticationUtils basicAuthenticationUtils;

	private UserManager userManager;
	private final UserDescription userDescription = new UserDescription(
			"email",
			"firstName",
			"lastName",
			"password");

	@Before
	public void setUp() throws Exception {
		userManager = new UserManager(userDAO, basicCredentialsDAO, basicAuthenticationUtils);
	}


	@Test
	public void testAddUser() {
		new Expectations() {{
			userDAO.findByEmail(userDescription.getEmail());
			result = Optional.absent();
		}};

		final User user = userManager.addUser(userDescription);
		assertEquals(user, userDescription);

		new Verifications() {{
			userDAO.save(user);
			basicCredentialsDAO.save((BasicCredentials) any);
		}};
	}


	@Test(expected = IllegalArgumentException.class)
	public void testAddUserAlreadyRegistered() {
		new Expectations() {{
			userDAO.findByEmail(userDescription.getEmail());
			result = Optional.fromNullable(new User(0, "", "", "", "", true, new Date(), "", "", 0));
		}};
		userManager.addUser(userDescription);
	}


	@Test
	public void testUpdateUser() {
		final User user = new User(0, userDescription.getEmail(), "", "", "", true, new Date(), "", "", 0);
		new Expectations() {{
			userDAO.findByEmail(userDescription.getEmail());
			result = Optional.fromNullable(user);

			basicCredentialsDAO.findByUserId(user.getId());
			result = Optional.of(new BasicCredentials(0, user, new byte[1], new byte[1]));
		}};

		User updatedUser = userManager.updateUser(user, userDescription);
		assertEquals(updatedUser, userDescription);
	}


	private void assertEquals(User user, UserDescription description) {
		Assert.assertEquals(description.getEmail(), user.getEmail());
		Assert.assertEquals(description.getFirstName(), user.getFirstName());
		Assert.assertEquals(description.getLastName(), user.getLastName());
		Assert.assertTrue(user.getLastModified() >= System.currentTimeMillis() / 1000 - 5);
	}

}