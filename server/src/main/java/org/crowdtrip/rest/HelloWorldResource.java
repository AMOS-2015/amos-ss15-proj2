package org.crowdtrip.rest;


import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/hello")
public final class HelloWorldResource {

	@GET
	public String getHelloWorld() {
		return "hello world";
	}

}
