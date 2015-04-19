package org.croudtrip.db;


import com.google.inject.AbstractModule;

import org.hibernate.SessionFactory;

public class DbModule extends AbstractModule {

	private final SessionFactory sessionFactory;

	public DbModule(SessionFactory sessionFactory) {
		if (sessionFactory == null) throw new RuntimeException("is null");
		this.sessionFactory = sessionFactory;
	}

	@Override
	protected void configure() {
		bind(SessionFactory.class).toInstance(sessionFactory);
	}

}
