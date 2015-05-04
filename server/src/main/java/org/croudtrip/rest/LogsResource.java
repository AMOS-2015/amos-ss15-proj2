package org.croudtrip.rest;

import org.croudtrip.logs.LogEntry;
import org.croudtrip.logs.LogManager;

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

    private final LogManager logManager;

    @Inject
    LogsResource(LogManager logManager) {
        this.logManager = logManager;
    }

    @GET
    @UnitOfWork
    public List<LogEntry> getLogs(@QueryParam("count") int count) {
        if (count < 1) throw RestUtils.createJsonFormattedException("query param \"count\" must be > 0", 400);
        return logManager.findN(count);
    }

}
