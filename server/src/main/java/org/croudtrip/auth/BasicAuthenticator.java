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

package org.croudtrip.auth;


import com.google.common.base.Optional;

import org.croudtrip.api.account.User;
import org.croudtrip.account.UserManager;

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
