package org.croudtrip.account;


import org.croudtrip.db.AvatarDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class AvatarManagerTest {

	@Mocked AvatarDAO avatarDAO;

	private AvatarManager manager;
	private final Avatar avatar = new Avatar(new byte[1], "image");

	@Before
	public void setupManager() {
		manager = new AvatarManager(avatarDAO);
	}


	@Test
	public void testAddAvatar() {
		Avatar resultAvatar = manager.addAvatar(avatar);
		Assert.assertEquals(avatar, resultAvatar);

		new Verifications() {{
			avatarDAO.save(avatar);
		}};
	}


	@Test
	public void testDeleteAvatar() {
		manager.deleteAvatar(avatar);
		new Verifications() {{
			avatarDAO.delete(avatar);
		}};
	}


	@Test
	public void testFindAvatarById() {
		final long id = 0;
		manager.findAvatarById(id);
		new Verifications() {{
			avatarDAO.findById(id);
		}};
	}

}