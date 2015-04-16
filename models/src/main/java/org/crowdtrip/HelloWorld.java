package org.crowdtrip;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HelloWorld {

	private final String greeting, target;

	@JsonCreator
	public HelloWorld(
			@JsonProperty("greeting") String greeting,
			@JsonProperty("target") String target) {

		this.greeting = greeting;
		this.target = target;
	}


	public String getGreeting() {
		return greeting;
	}


	public String getTarget() {
		return target;
	}

}
