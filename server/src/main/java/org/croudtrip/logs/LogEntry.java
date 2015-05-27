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

package org.croudtrip.logs;

import com.google.common.base.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * One log entry which can be stored in DB.
 */
@Entity(name = LogEntry.ENTITY_NAME)
@Table(name = "log_entries")
@NamedQueries({
		@NamedQuery(
				name = LogEntry.QUERY_NAME_FIND_ALL,
				query = "SELECT l FROM " + LogEntry.ENTITY_NAME + " l ORDER BY " + LogEntry.COLUMN_ID + " DESC"
		),
})
public class LogEntry {

	public static final String
			ENTITY_NAME = "LogEntry",
			COLUMN_ID = "log_entry_id",
			QUERY_NAME_FIND_ALL = "org.croudtrip.logs.LogEntry.findAll";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = COLUMN_ID)
	private long id;

	@Column(name = "level", nullable = false)
	@Enumerated(EnumType.STRING)
	private LogLevel level;

	@Column(name = "tag", nullable = false)
	private String tag;

	@Column(name = "message", nullable = false, length = 65535)
	private String message;

	@Column(name = "timestamp", nullable = false)
	private long timestamp; // unix timestamp in seconds

	public LogEntry() { }

	public LogEntry(LogLevel level,  String tag, String message, long timestamp) {
		this.level = level;
		this.tag = tag;
		this.message = message;
		this.timestamp = timestamp;
	}

	public long getId() {
		return id;
	}

	public LogLevel getLevel() {
		return level;
	}

	public String getTag() {
		return tag;
	}

	public String getMessage() {
		return message;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LogEntry logEntry = (LogEntry) o;
		return Objects.equal(id, logEntry.id) &&
				Objects.equal(level, logEntry.level) &&
				Objects.equal(tag, logEntry.tag) &&
				Objects.equal(message, logEntry.message) &&
				Objects.equal(timestamp, logEntry.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, level, tag, message, timestamp);
	}

}
