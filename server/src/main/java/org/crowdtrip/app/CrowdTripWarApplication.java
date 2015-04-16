package org.crowdtrip.app;


import javax.servlet.annotation.WebListener;

import be.fluid_it.tools.dropwizard.box.WebApplication;

@WebListener
public final class CrowdTripWarApplication extends WebApplication<CrowdTripConfig> {

	public CrowdTripWarApplication() {
		super(new CrowdTripApplication(), "configuration.yml");
	}

}
