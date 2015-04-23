package org.croudtrip.user;

import com.google.common.base.Optional;

import org.croudtrip.db.AvatarDAO;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AvatarManager {

	private final AvatarDAO avatarDAO;

	@Inject
	AvatarManager(AvatarDAO avatarDAO) {
		this.avatarDAO = avatarDAO;
	}


	public Avatar addAvatar(Avatar avatar) {
		avatarDAO.save(avatar);
		return avatar;
	}


	public void deleteAvatar(Avatar avatar) {
		avatarDAO.delete(avatar);
	}


	public Optional<Avatar> findAvatarById(long avatarId) {
		return avatarDAO.findById(avatarId);
	}

}
