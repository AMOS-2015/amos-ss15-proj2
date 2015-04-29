package org.croudtrip.db;


import com.google.common.base.Optional;

import org.hibernate.SessionFactory;

abstract class AbstractDAO<T> extends io.dropwizard.hibernate.AbstractDAO<T> {

	AbstractDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	public final Optional<T> findById(long id) {
		return Optional.fromNullable(get(id));
	}


	public final void save(T value) {
		currentSession().save(value);
	}


	public final void update(T value) {
		currentSession().merge(value);
	}


	public final void delete(T value) {
		currentSession().delete(value);
	}


}
