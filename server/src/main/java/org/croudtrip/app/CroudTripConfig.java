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
	public CroudTripConfig(@JsonProperty("database") DataSourceFactory database) {
		this.database = database;
	}


	public DataSourceFactory getDatabase() {
		return database;
	}

    @JsonProperty("googleKey")
    public String getGoogleAPIKey() {
        return apiKey;
    }

    @JsonProperty("googleKey")
    public void setGoogleAPIKey( String googleAPIKey ) {
        this.apiKey = googleAPIKey;
    }

}
