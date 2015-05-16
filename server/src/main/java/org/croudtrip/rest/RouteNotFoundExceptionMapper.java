package org.croudtrip.rest;


import org.croudtrip.directions.RouteNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public final class RouteNotFoundExceptionMapper implements ExceptionMapper<RouteNotFoundException> {

	@Override
	public Response toResponse(RouteNotFoundException exception) {
		return RestUtils.createJsonFormattedException("no route found", 404).getResponse();
	}

}