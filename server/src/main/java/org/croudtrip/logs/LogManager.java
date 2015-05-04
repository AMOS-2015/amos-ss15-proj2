package org.croudtrip.logs;


import org.croudtrip.db.LogEntryDAO;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LogManager {

	private final LogEntryDAO logEntryDAO;

	@Inject
	LogManager(LogEntryDAO logEntryDAO) {
		this.logEntryDAO = logEntryDAO;
	}


	public void d(String message) {
		log(LogLevel.DEBUG, message);
	}


	public void d(Throwable throwable, String message) {
		log(LogLevel.DEBUG, throwable, message);
	}


	public void i(String message) {
		log(LogLevel.INFO, message);
	}


	public void i(Throwable throwable, String message) {
		log(LogLevel.INFO, throwable, message);
	}


	public void w(String message) {
		log(LogLevel.WARNING, message);
	}


	public void w(Throwable throwable, String message) {
		log(LogLevel.WARNING, throwable, message);
	}


	public void e(String message) {
		log(LogLevel.ERROR, message);
	}


	public void e(Throwable throwable, String message) {
		log(LogLevel.ERROR, throwable, message);
	}


	public List<LogEntry> findN(int n) {
		return logEntryDAO.findN(n);
	}


	private void log(LogLevel level, String message) {
		log(level, getCallerClassName(), message, System.currentTimeMillis() / 1000);
	}


	private void log(LogLevel level, Throwable throwable, String message) {
		// log message and then throwable
		long timestamp = System.currentTimeMillis() / 1000;
		log(level, getCallerClassName(), message, timestamp);
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		log(level, getCallerClassName(), writer.toString(), timestamp);
	}


	private void log(LogLevel level, String tag, String message, long timestamp) {
		logEntryDAO.save(new LogEntry(level, tag, message, timestamp));
	}


	private String getCallerClassName() {
		return Thread.currentThread().getStackTrace()[4].getClassName();
	}

}
