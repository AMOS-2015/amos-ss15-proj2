package org.crowdtrip.app;


import org.crowdtrip.rest.HelloWorldResource;
import org.crowdtrip.rest.RegisterUserResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public final class CrowdTripApplication extends Application<CrowdTripConfig> {

	public static void main(String[] args) throws Exception {
		new CrowdTripApplication().run(args);
	}


	@Override
	public void run(CrowdTripConfig configuration, Environment environment) throws Exception {
		environment.jersey().register(HelloWorldResource.class);
        environment.jersey().register(RegisterUserResource.class);
	}

}
