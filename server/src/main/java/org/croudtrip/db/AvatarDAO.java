package org.croudtrip.db;


import org.croudtrip.account.Avatar;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

public class AvatarDAO extends AbstractDAO<Avatar> {

	@Inject
	AvatarDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

}
