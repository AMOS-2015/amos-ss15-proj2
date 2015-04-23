package org.croudtrip.db;


import com.google.common.base.Optional;

import org.croudtrip.user.Avatar;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

import io.dropwizard.hibernate.AbstractDAO;

public class AvatarDAO extends AbstractDAO<Avatar> {

	@Inject
	AvatarDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public Optional<Avatar> findById(long id) {
		return Optional.fromNullable(get(id));
	}


	public void save(Avatar avatar) {
		currentSession().save(avatar);
	}


	public void update(Avatar avatar) {
		currentSession().merge(avatar);
	}


	public void delete(Avatar avatar) {
		currentSession().delete(avatar);
	}

}
