/*
 * The CroudTrip! application aims at revolutionizing the car-ride-sharing market with its easy,
 * user-friendly and highly automated way of organizing shared Trips. Copyright (C) 2015  Nazeeh Ammari,
 *  Philipp Eichhorn, Ricarda Hohn, Vanessa Lange, Alexander Popp, Frederik Simon, Michael Weber
 * This program is free software: you can redistribute it and/or modify  it under the terms of the GNU
 *  Affero General Public License as published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *    If not, see http://www.gnu.org/licenses/.
 */

package org.croudtrip.app;


import com.google.inject.Guice;
import com.google.inject.Injector;

import org.croudtrip.account.Avatar;
import org.croudtrip.account.UserManager;
import org.croudtrip.api.account.User;
import org.croudtrip.api.account.UserDescription;
import org.croudtrip.api.account.Vehicle;
import org.croudtrip.api.gcm.GcmRegistration;
import org.croudtrip.api.trips.JoinTripRequest;
import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.api.trips.SuperTrip;
import org.croudtrip.api.trips.SuperTripReservation;
import org.croudtrip.api.trips.TripOffer;
import org.croudtrip.auth.BasicAuthenticator;
import org.croudtrip.auth.BasicCredentials;
import org.croudtrip.db.DbModule;
import org.croudtrip.directions.DirectionsModule;
import org.croudtrip.gcm.GcmModule;
import org.croudtrip.logs.LogEntry;
import org.croudtrip.places.PlacesModule;
import org.croudtrip.rest.AvatarsResource;
import org.croudtrip.rest.GcmRegistrationResource;
import org.croudtrip.rest.JsonExceptionMapper;
import org.croudtrip.rest.LogsResource;
import org.croudtrip.rest.NotFoundExceptionMapper;
import org.croudtrip.rest.RouteNotFoundExceptionMapper;
import org.croudtrip.rest.ThrowableExceptionMapper;
import org.croudtrip.rest.TripsResource;
import org.croudtrip.rest.UsersHeadResource;
import org.croudtrip.rest.UsersResource;
import org.croudtrip.rest.VehicleResource;
import org.croudtrip.trips.DisableTripOffersExecutor;
import org.croudtrip.trips.ExpireJoinTripRequestsExecutor;
import org.croudtrip.trips.ExpireTripOffersExecutor;
import org.croudtrip.trips.RunningTripQueryGarbageCollectionExecutor;
import org.croudtrip.trips.TripReservationGarbageCollectionExecutor;
import org.croudtrip.trips.TripsModule;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

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
                new DirectionsModule(configuration),
				new GcmModule(configuration.getGoogleAPIKey()),
				new TripsModule(),
				new PlacesModule( configuration ));

		// start managed instances (manually since deployment via WAR file does not seem to work)
		injector.getInstance(TripReservationGarbageCollectionExecutor.class).start();
		injector.getInstance(RunningTripQueryGarbageCollectionExecutor.class).start();
		injector.getInstance(DisableTripOffersExecutor.class).start();
        injector.getInstance(ExpireTripOffersExecutor.class).start();
		injector.getInstance(ExpireJoinTripRequestsExecutor.class).start();

		// setup REST api
        environment.jersey().register(injector.getInstance(UsersResource.class));
		environment.jersey().register(injector.getInstance(UsersHeadResource.class));
		environment.jersey().register(injector.getInstance(AvatarsResource.class));
		environment.jersey().register(injector.getInstance(TripsResource.class));
		environment.jersey().register(injector.getInstance(VehicleResource.class));
		environment.jersey().register(injector.getInstance(GcmRegistrationResource.class));
		environment.jersey().register(injector.getInstance(LogsResource.class));
		environment.jersey().register(injector.getInstance(NotFoundExceptionMapper.class));
		environment.jersey().register(injector.getInstance(JsonExceptionMapper.class));
		environment.jersey().register(injector.getInstance(RouteNotFoundExceptionMapper.class));
		environment.jersey().register(injector.getInstance(ThrowableExceptionMapper.class));
		environment.jersey().register(AuthFactory.binder(new BasicAuthFactory<>(
				injector.getInstance(BasicAuthenticator.class),
				"all secret",
				User.class)));

		// register a set of "default" users
		SessionFactory sessionFactory = injector.getInstance(SessionFactory.class);
		UserManager userManager = injector.getInstance(UserManager.class);

		Session session = null;
		try {
			session = sessionFactory.openSession();
			ManagedSessionContext.bind(session);
			for (UserDescription user : configuration.getUsers()) {
				if (!userManager.findUserByEmail(user.getEmail()).isPresent()) {
					userManager.addUser(user);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (session != null) {
				session.flush();
				session.close();
			}
		}
	}


	private final HibernateBundle<CroudTripConfig> hibernateBundle = new HibernateBundle<CroudTripConfig>(
			User.class,
			BasicCredentials.class,
			Avatar.class,
			TripOffer.class,
			SuperTripReservation.class,
			SuperTrip.class,
			JoinTripRequest.class,
			RunningTripQuery.class,
			Vehicle.class,
			LogEntry.class,
			GcmRegistration.class) {
		@Override
		public DataSourceFactory getDataSourceFactory(CroudTripConfig configuration) {
			return configuration.getDatabase();
		}
	};

}
