package org.croudtrip.rest;


import org.croudtrip.HelloWorld;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class HelloWorldResource {

	@GET
	public HelloWorld getHelloWorld() {
		return new HelloWorld("Hello", "World");
	}

}
