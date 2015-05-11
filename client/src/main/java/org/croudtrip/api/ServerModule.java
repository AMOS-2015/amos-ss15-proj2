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
	public DirectionsResource provideDirectionsResource(Context context) {
		return provideRestAdapter(context).create(DirectionsResource.class);
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
