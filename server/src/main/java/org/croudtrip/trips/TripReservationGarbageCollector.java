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

import org.croudtrip.api.trips.TripReservation;
import org.croudtrip.db.TripReservationDAO;
import org.croudtrip.logs.LogManager;
import org.croudtrip.utils.AbstractScheduledTaskExecutor;
import org.hibernate.SessionFactory;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Removes unused {@link org.croudtrip.api.trips.TripReservation} after they have expired.
 */
public class TripReservationGarbageCollector extends AbstractScheduledTaskExecutor {

	private final TripReservationDAO tripReservationDAO;

	@Inject
	TripReservationGarbageCollector(
			TripReservationDAO tripReservationDAO,
			SessionFactory sessionFactory,
			LogManager logManager) {

		super(sessionFactory, logManager, 60*60, TimeUnit.SECONDS);
		this.tripReservationDAO = tripReservationDAO;
	}

	@Override
	protected void doRun() {
		long currentTimestamp = System.currentTimeMillis() / 1000;
		for (TripReservation reservation : tripReservationDAO.findAll()) {
			if (currentTimestamp > reservation.getCreationTimestamp() + reservation.getQuery().getMaxWaitingTimeInSeconds()) {
				logManager.d("Removing reservation " + reservation.getId());
				tripReservationDAO.delete(reservation);
			}
		}
	}

}
