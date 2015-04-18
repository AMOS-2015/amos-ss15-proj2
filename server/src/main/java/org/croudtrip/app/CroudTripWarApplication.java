package org.croudtrip.app;


import javax.servlet.annotation.WebListener;

import be.fluid_it.tools.dropwizard.box.WebApplication;

@WebListener
public final class CroudTripWarApplication extends WebApplication<CroudTripConfig> {

	public CroudTripWarApplication() {
		super(new CroudTripApplication(), "configuration.yml");
	}

}
