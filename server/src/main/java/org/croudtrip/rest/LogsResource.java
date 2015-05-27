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

package org.croudtrip.rest;

import org.croudtrip.logs.LogEntry;
import org.croudtrip.logs.LogManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.dropwizard.hibernate.UnitOfWork;

/**
 * Resource for managing users.
 */
@Path("/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogsResource {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

    private final LogManager logManager;

    @Inject
    LogsResource(LogManager logManager) {
        this.logManager = logManager;
    }

    @GET
    @UnitOfWork
    public List<String> getLogs(@QueryParam("count") int count) {
        if (count < 1) throw RestUtils.createJsonFormattedException("query param \"count\" must be > 0", 400);

        List<LogEntry> entries = logManager.findN(count);
        List<String> logs = new LinkedList<>();
        for (LogEntry entry : entries) {
            logs.add(entry.getLevel() + "  [" + dateFormat.format(new Date(entry.getTimestamp() * 1000)) + "] " + entry.getTag() + ": " + entry.getMessage());
        }
        return logs;
    }

}
