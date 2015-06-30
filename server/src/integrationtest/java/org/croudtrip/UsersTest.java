package org.croudtrip;


import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class UsersTest {

	private ApiFactory apiFactory = new ApiFactory();

	@Test
	public void testAddUser() {
		String randomValue = UUID.randomUUID().toString();
		UserDescription description = new UserDescription(
				randomValue,
				randomValue,
				randomValue,
				randomValue);

		// test registration
		User user = apiFactory.getUsersResource().registerUserSynchronously(description);
		Assert.assertEquals(description.getEmail(), user.getEmail());
		Assert.assertEquals(description.getFirstName(), user.getFirstName());
		Assert.assertEquals(description.getLastName(), user.getLastName());

		// get authenticated get
		apiFactory.setUser(description);
		User userSecondDownload = apiFactory.getUsersResource().getUserSynchronously();
		Assert.assertEquals(user, userSecondDownload);
	}

}
