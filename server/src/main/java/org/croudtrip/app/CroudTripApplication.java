package org.croudtrip.app;


import com.google.inject.Guice;
import com.google.inject.Injector;

import org.croudtrip.rest.HelloWorldResource;
import org.croudtrip.rest.UserResource;

import io.dropwizard.Application;
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
		bootstrap.addBundle(new HibernateBundle<CroudTripConfig>(String.class) {
			@Override
			public DataSourceFactory getDataSourceFactory(CroudTripConfig configuration) {
				return configuration.getDatabase();
			}
		});
	}


	@Override
	public void run(CroudTripConfig configuration, Environment environment) throws Exception {
		Injector injector = Guice.createInjector();

		environment.jersey().register(injector.getInstance(HelloWorldResource.class));
        environment.jersey().register(injector.getInstance(UserResource.class));
	}

}
