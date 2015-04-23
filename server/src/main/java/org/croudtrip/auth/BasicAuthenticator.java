package org.croudtrip.auth;


import com.google.common.base.Optional;

import org.croudtrip.user.UserManager;

import javax.inject.Inject;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

public class BasicAuthenticator implements Authenticator<io.dropwizard.auth.basic.BasicCredentials, User> {

	private final UserManager userManager;
	private final BasicAuthenticationUtils authenticationUtils;

	@Inject
	BasicAuthenticator(UserManager userManager, BasicAuthenticationUtils authenticationUtils) {
		this.userManager = userManager;
		this.authenticationUtils = authenticationUtils;
	}


	@Override
	public Optional<User> authenticate(io.dropwizard.auth.basic.BasicCredentials credentials) throws AuthenticationException {
		System.out.println(credentials.getUsername() + " " + credentials.getPassword());

		Optional<User> user = userManager.findUserByEmail(credentials.getUsername());
		if (!user.isPresent()) return Optional.absent();

		Optional<BasicCredentials> storedCredentials = userManager.findCredentialsByUserId(user.get().getId());
		if (!storedCredentials.isPresent()) return Optional.absent();

		if (!authenticationUtils.checkPassword(credentials.getPassword(), storedCredentials.get())) return Optional.absent();
		return user;
	}

}
