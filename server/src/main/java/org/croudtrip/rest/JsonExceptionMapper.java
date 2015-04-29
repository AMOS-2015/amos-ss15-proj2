package org.croudtrip.rest;

import com.fasterxml.jackson.databind.JsonMappingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public final class JsonExceptionMapper implements ExceptionMapper<JsonMappingException> {

	@Override
	public Response toResponse(JsonMappingException jme) {
		return RestUtils.createJsonFormattedException(jme.getMessage(), 400).getResponse();
	}

}
