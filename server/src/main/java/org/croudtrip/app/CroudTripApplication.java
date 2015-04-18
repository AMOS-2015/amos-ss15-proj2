package org.croudtrip.app;


import org.croudtrip.rest.HelloWorldResource;
import org.croudtrip.rest.RegisterUserResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public final class CroudTripApplication extends Application<CroudTripConfig> {

	public static void main(String[] args) throws Exception {
		new CroudTripApplication().run(args);
	}


	@Override
	public void run(CroudTripConfig configuration, Environment environment) throws Exception {
		environment.jersey().register(HelloWorldResource.class);
        environment.jersey().register(RegisterUserResource.class);
	}

}
