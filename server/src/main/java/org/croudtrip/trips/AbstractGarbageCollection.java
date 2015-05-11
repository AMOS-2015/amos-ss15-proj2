package org.croudtrip.trips;

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
abstract class AbstractGarbageCollection implements Managed, Runnable {

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final SessionFactory sessionFactory;
	protected final LogManager logManager;
	private final long runIntervalValue;
	private final TimeUnit runIntervalUnit;

	private ScheduledFuture<?> gcTask = null;

	AbstractGarbageCollection(
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
