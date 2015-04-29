package org.croudtrip.app;


import com.google.inject.Guice;
import com.google.inject.Injector;

import org.croudtrip.auth.BasicAuthenticator;
import org.croudtrip.auth.BasicCredentials;
import org.croudtrip.auth.User;
import org.croudtrip.db.DbModule;
import org.croudtrip.directions.DirectionsModule;
import org.croudtrip.rest.AvatarsResource;
import org.croudtrip.rest.DirectionsResource;
import org.croudtrip.rest.JsonExceptionMapper;
import org.croudtrip.rest.NotFoundExceptionMapper;
import org.croudtrip.rest.UsersResource;
import org.croudtrip.user.Avatar;
import org.croudtrip.user.ThrowableExceptionMapper;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthFactory;
import io.dropwizard.auth.basic.BasicAuthFactory;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
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
		bootstrap.addBundle(new MultiPartBundle());
	}


	@Override
	public void run(CroudTripConfig configuration, Environment environment) throws Exception {
		Injector injector = Guice.createInjector(
				new DbModule(hibernateBundle.getSessionFactory()),
                new DirectionsModule( configuration ));


        environment.jersey().register(injector.getInstance(UsersResource.class));
		environment.jersey().register(injector.getInstance(AvatarsResource.class));
        environment.jersey().register(injector.getInstance(DirectionsResource.class));
		environment.jersey().register(injector.getInstance(NotFoundExceptionMapper.class));
		environment.jersey().register(injector.getInstance(JsonExceptionMapper.class));
		environment.jersey().register(injector.getInstance(ThrowableExceptionMapper.class));
		environment.jersey().register(AuthFactory.binder(new BasicAuthFactory<>(
				injector.getInstance(BasicAuthenticator.class),
				"all secret",
				User.class)));
	}


	private final HibernateBundle<CroudTripConfig> hibernateBundle = new HibernateBundle<CroudTripConfig>(
			User.class,
			BasicCredentials.class,
			Avatar.class) {
		@Override
		public DataSourceFactory getDataSourceFactory(CroudTripConfig configuration) {
			return configuration.getDatabase();
		}
	};

}
