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

package org.croudtrip.api;

import android.content.Context;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.croudtrip.R;
import org.croudtrip.account.AccountManager;

import javax.inject.Inject;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

public class ServerModule implements Module {

	@Override
	public void configure(Binder binder) {
		// nothing to do for now
	}


	@Provides
	@Inject
	public RestAdapter provideRestAdapter(final Context context) {
		return new RestAdapter.Builder()
				.setEndpoint(context.getString(R.string.server_address))
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						AccountManager.addAuthorizationHeader(context, request);
					}
				})
				.setConverter(new JacksonConverter())
				.build();
	}


	@Provides
	@Inject
	public UsersResource provideUsersResource(Context context) {
		return provideRestAdapter(context).create(UsersResource.class);
	}

    @Provides
    @Inject
    public VehicleResource provideVehiclesResource(Context context) {
        return provideRestAdapter(context).create(VehicleResource.class);
    }

	@Provides
	@Inject
	public UsersHeadResource provideUsersHeadResource(Context context) {
		return provideRestAdapter(context).create(UsersHeadResource.class);
	}

    @Provides
    @Inject
    public TripsResource provideTripsResource(Context context) {
        return provideRestAdapter(context).create(TripsResource.class);
    }

	@Provides
	@Inject
	public GcmRegistrationResource provideGcmRegistrationResource(Context context) {
		return provideRestAdapter(context).create(GcmRegistrationResource.class);
	}

}
