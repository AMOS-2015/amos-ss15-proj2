package org.croudtrip.server;

import android.content.Context;

import org.croudtrip.DirectionsResource;
import org.croudtrip.R;
import org.croudtrip.UsersResource;
import org.croudtrip.activities.LoginActivity;

import javax.inject.Inject;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class ServerModule {

	/*
	@Override
	public void configure(Binder binder) {
		// nothing to do for now
	}
	*/


	// @Provides
	@Inject
	public static RestAdapter provideRestAdapter(final Context context) {
		// TODO @Vanessa: please replace the static references to LoginActivity once it has been refactored
		return new RestAdapter.Builder()
				.setEndpoint(context.getString(R.string.server_address))
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						LoginActivity.addAuthorizationHeader(context, request);
					}
				})
				.build();
	}


	// @Provides
	@Inject
	public static UsersResource provideUsersResource(Context context) {
		return provideRestAdapter(context).create(UsersResource.class);
	}


	// @Provides
	@Inject
	public static DirectionsResource provideDirectionsResource(Context context) {
		return provideRestAdapter(context).create(DirectionsResource.class);
	}

}
