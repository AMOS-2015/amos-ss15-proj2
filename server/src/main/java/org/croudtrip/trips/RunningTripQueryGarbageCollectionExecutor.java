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

package org.croudtrip.trips;

import org.croudtrip.api.trips.RunningTripQuery;
import org.croudtrip.db.RunningTripQueryDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Removes unused {@link RunningTripQuery} after they have expired.
 */
public class RunningTripQueryGarbageCollectionExecutor extends AbstractScheduledTaskExecutor {

	/**
	 * Keeps {@link RunningTripQuery} around a little longer to give clients a chance to download results.
	 */
	private static final int ADDITIONAL_LIFETIME_IN_SECONDS = 60 * 60 * 12; // 12 hours

	private final RunningTripQueryDAO runningTripQueryDAO;

	@Inject
	RunningTripQueryGarbageCollectionExecutor(
			RunningTripQueryDAO runningTripQueryDAO,
			SessionFactory sessionFactory,
			LogManager logManager) {

		super(sessionFactory, logManager, 15, TimeUnit.SECONDS);
		this.runningTripQueryDAO = runningTripQueryDAO;
	}

	@Override
	protected void doRun() {
		long currentTimestamp = System.currentTimeMillis() / 1000;
		for (RunningTripQuery query : runningTripQueryDAO.findAll()) {
			if (currentTimestamp > query.getQuery().getCreationTimestamp() + query.getQuery().getMaxWaitingTimeInSeconds() + ADDITIONAL_LIFETIME_IN_SECONDS) {
				logManager.d("Removing RunningQuery " + query.getId());
				runningTripQueryDAO.delete(query);
			}

		}
	}

}
