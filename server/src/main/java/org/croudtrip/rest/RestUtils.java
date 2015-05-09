package org.croudtrip.rest;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.dropwizard.jersey.errors.ErrorMessage;

class RestUtils {

	private RestUtils() { }


	public static WebApplicationException createNotFoundException() {
		return createNotFoundException("not found");
	}


	public static WebApplicationException createNotFoundException(String msg) {
		return createJsonFormattedException("not found", 404);
	}


	public static WebApplicationException createUnauthorizedException() {
		return createJsonFormattedException("not authorized", 401);
	}


	public static WebApplicationException createJsonFormattedException(String message, int code) {
		return new WebApplicationException(
				Response
						.status(code)
						.entity(new ErrorMessage(code, message))
						.type(MediaType.APPLICATION_JSON)
						.build());

	}

}