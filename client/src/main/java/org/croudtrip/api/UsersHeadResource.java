package org.croudtrip.api;

import retrofit.client.Response;
import retrofit.http.HEAD;
import retrofit.http.Headers;
import rx.Observable;

/**
 * Creates HTTP HEAD requests for the users resource.
 */
public interface UsersHeadResource {

	@HEAD("/users")
	@Headers("Content-type: application/json")
	Observable<Response> getLastModified();

}
