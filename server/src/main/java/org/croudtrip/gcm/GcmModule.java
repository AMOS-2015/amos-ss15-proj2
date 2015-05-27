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
