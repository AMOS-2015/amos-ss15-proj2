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

package org.croudtrip.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.croudtrip.api.account.UserDescription;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class CroudTripConfig extends Configuration {

	@NotNull @Valid private final DataSourceFactory database;
    @NotNull @Valid private final String apiKey;
	@NotNull @Valid private final List<UserDescription> users;

	@JsonCreator
	public CroudTripConfig(
			@JsonProperty("database") DataSourceFactory database,
			@JsonProperty("googleKey") String apiKey,
			@JsonProperty("users") List<UserDescription> users) {

		this.database = database;
        this.apiKey = apiKey;
		this.users = users;
	}


	public DataSourceFactory getDatabase() {
		return database;
	}


	public String getGoogleAPIKey() {
        return apiKey;
    }


	public List<UserDescription> getUsers() {
		return users;
	}

}
