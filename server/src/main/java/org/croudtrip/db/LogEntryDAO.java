package org.croudtrip.db;


import org.croudtrip.logs.LogEntry;
import org.hibernate.SessionFactory;

import java.util.List;

import javax.inject.Inject;

public class LogEntryDAO extends AbstractDAO<LogEntry> {

	@Inject
	LogEntryDAO(SessionFactory sessionFactory) {
		super(sessionFactory);
	}

	public List<LogEntry> findN(int n) {
		return list(namedQuery(LogEntry.QUERY_NAME_FIND_ALL).setMaxResults(n));
	}

}
