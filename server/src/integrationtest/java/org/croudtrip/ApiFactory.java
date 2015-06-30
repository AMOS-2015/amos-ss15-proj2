package org.croudtrip;


import com.google.common.base.Optional;

import org.croudtrip.api.AvatarsUploadResource;
import org.croudtrip.api.GcmRegistrationResource;
import org.croudtrip.api.TripsResource;
import org.croudtrip.api.UsersHeadResource;
import org.croudtrip.api.UsersResource;
import org.croudtrip.api.VehicleResource;
import org.croudtrip.api.account.UserDescription;
import org.glassfish.jersey.internal.util.Base64;

import java.io.IOException;
import java.net.HttpURLConnection;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Request;
import retrofit.client.UrlConnectionClient;
import retrofit.converter.JacksonConverter;

public class ApiFactory {

	private Optional<UserDescription> authenticatedUser = Optional.absent();

	public void setUser(UserDescription user) {
		authenticatedUser = Optional.fromNullable(user);
	}

	public UsersResource getUsersResource() {
		return getRestAdapter().create(UsersResource.class);
	}

	public AvatarsUploadResource getAvatarsUploadResource() {
		return getRestAdapter().create(AvatarsUploadResource.class);
	}

	public VehicleResource getVehicleResource() {
		return getRestAdapter().create(VehicleResource.class);
	}

	public UsersHeadResource getUsersHeadResource() {
		return getRestAdapter().create(UsersHeadResource.class);
	}

	public TripsResource getTripsResource() {
		return getRestAdapter().create(TripsResource.class);
	}

	public GcmRegistrationResource getGcmRegistrationResource() {
		return getRestAdapter().create(GcmRegistrationResource.class);
	}

	private RestAdapter getRestAdapter() {
		return new RestAdapter.Builder()
				.setEndpoint("http://localhost:8080")
				.setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						if (!authenticatedUser.isPresent()) return;
						String credentials = authenticatedUser.get().getEmail() + ":" + authenticatedUser.get().getPassword();
						String base64EncodedCredentials = Base64.encodeAsString(credentials.getBytes());
						request.addHeader("Authorization", "Basic " + base64EncodedCredentials);
					}
				})
				.setClient(new LongTimeoutUrlConnectionClient())
				.setConverter(new JacksonConverter())
				.build();
	}


	/**
	 * Increases default HTTP timeout length.
	 */
	public static class LongTimeoutUrlConnectionClient extends UrlConnectionClient {

		@Override
		protected HttpURLConnection openConnection(Request request) throws IOException {
			HttpURLConnection connection = super.openConnection(request);
			connection.setConnectTimeout(10 * 1000); // 10 seconds
			connection.setReadTimeout(90 * 1000); // 90 seconds
			return connection;
		}

	}


}
