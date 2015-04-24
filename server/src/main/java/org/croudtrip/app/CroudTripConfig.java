package org.croudtrip.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class CroudTripConfig extends Configuration {

	@NotNull @Valid DataSourceFactory database;
    @NotNull @Valid String apiKey;


	@JsonCreator
	public CroudTripConfig(@JsonProperty("database") DataSourceFactory database, @JsonProperty("googleKey") String apiKey) {
		this.database = database;
        this.apiKey = apiKey;
	}


	public DataSourceFactory getDatabase() {
		return database;
	}
    public String getGoogleAPIKey() {
        return apiKey;
    }

}
