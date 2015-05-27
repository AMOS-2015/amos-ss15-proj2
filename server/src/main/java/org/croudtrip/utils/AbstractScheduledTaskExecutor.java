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

package org.croudtrip.utils;

import org.croudtrip.logs.LogManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.dropwizard.lifecycle.Managed;

/**
 * Base class for various garbage collection task.
 */
public abstract class AbstractScheduledTaskExecutor implements Managed, Runnable {

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final SessionFactory sessionFactory;
	protected final LogManager logManager;
	private final long runIntervalValue;
	private final TimeUnit runIntervalUnit;

	private ScheduledFuture<?> gcTask = null;

	protected AbstractScheduledTaskExecutor(
            SessionFactory sessionFactory,
            LogManager logManager,
            long runIntervalValue,
            TimeUnit runIntervalUnit) {

		this.sessionFactory = sessionFactory;
		this.logManager = logManager;
		this.runIntervalValue = runIntervalValue;
		this.runIntervalUnit = runIntervalUnit;
	}

	@Override
	public void start() throws Exception {
		gcTask = scheduler.scheduleAtFixedRate(this, 0, runIntervalValue, runIntervalUnit);
	}

	@Override
	public void stop() throws Exception {
		gcTask.cancel(true);
	}

	@Override
	public void run() {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			ManagedSessionContext.bind(session);
			logManager.d("Running " + getClass().getName());
			doRun();
		} catch (Exception e) {
			logManager.e(e, "failed to run " + getClass().getName());
		} finally {
			if (session != null) {
				session.flush();
				session.close();
			}
		}

	}


	protected abstract void doRun() throws Exception;


}
