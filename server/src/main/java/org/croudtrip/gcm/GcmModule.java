package org.croudtrip.gcm;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.croudtrip.db.GcmRegistrationDAO;
import org.croudtrip.logs.LogManager;

import javax.inject.Singleton;

public class GcmModule extends AbstractModule {
	
	private final String googleApiKey;
	
	public GcmModule(String googleApiKey) {
		this.googleApiKey = googleApiKey;
	}

	@Override
	protected void configure() {
		// nothing to do for now
	}
	
	
	@Provides
	@Singleton
	public GcmManager provideGcMananger(GcmRegistrationDAO registrationDAO, LogManager logManager) {
		return new GcmManager(registrationDAO, googleApiKey, logManager);
	}
	
}
