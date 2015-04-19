package org.croudtrip.app;


import com.google.inject.Guice;
import com.google.inject.Injector;

import org.croudtrip.auth.BasicAuthenticator;
import org.croudtrip.auth.BasicCredentials;
import org.croudtrip.auth.User;
import org.croudtrip.db.DbModule;
import org.croudtrip.rest.UserResource;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthFactory;
import io.dropwizard.auth.basic.BasicAuthFactory;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Starting and central initialization point of server.
 */
public final class CroudTripApplication extends Application<CroudTripConfig> {

	public static void main(String[] args) throws Exception {
		new CroudTripApplication().run(args);
	}


	@Override
	public void initialize(Bootstrap<CroudTripConfig> bootstrap) {
		bootstrap.addBundle(hibernateBundle);
	}


	@Override
	public void run(CroudTripConfig configuration, Environment environment) throws Exception {
		Injector injector = Guice.createInjector(
				new DbModule(hibernateBundle.getSessionFactory()));

        environment.jersey().register(injector.getInstance(UserResource.class));
		environment.jersey().register(AuthFactory.binder(new BasicAuthFactory<>(
				injector.getInstance(BasicAuthenticator.class),
				"all secret",
				User.class)));
	}


	private final HibernateBundle<CroudTripConfig> hibernateBundle = new HibernateBundle<CroudTripConfig>(User.class, BasicCredentials.class) {
		@Override
		public DataSourceFactory getDataSourceFactory(CroudTripConfig configuration) {
			return configuration.getDatabase();
		}
	};

}
