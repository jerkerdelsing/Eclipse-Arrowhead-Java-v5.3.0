/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.authentication.method.password;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.IAuthenticationMethodService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class PasswordAuthenticationMethodService implements IAuthenticationMethodService {

	//=================================================================================================
	// members

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private IdentityDbService identityDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean verifyCredentials(final System system, final Map<String, String> credentials) throws InternalServerError {
		logger.debug("PasswordAuthenticationMethodService.verifyCredentials started...");
		Assert.notNull(system, "system is null");
		Assert.notNull(credentials, "credentials is null");
		Assert.isTrue(!Utilities.isEmpty(credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD)), "password field is missing or empty");

		final String rawPassword = credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD);
		final Optional<PasswordAuthentication> passwordOpt = identityDbService.getPasswordAuthenticationBySystem(system);
		if (passwordOpt.isEmpty()) {
			// database inconsistency
			logger.error("System {} has no assigned password", system.getName());
			return false;
		}

		return encoder.matches(rawPassword, passwordOpt.get().getPassword());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void changeCredentials(final System system, final Map<String, String> oldCredentials, final Map<String, String> newCredentials)
			throws InvalidParameterException, InternalServerError {
		logger.debug("PasswordAuthenticationMethodService.changeCredentials started...");
		Assert.notNull(system, "system is null");
		Assert.notNull(newCredentials, "newCredentials is null");
		Assert.isTrue(!Utilities.isEmpty(newCredentials.get(PasswordAuthenticationMethod.KEY_PASSWORD)), "new password field is missing or empty");

		final String encodedPassword = encoder.encode(newCredentials.get(PasswordAuthenticationMethod.KEY_PASSWORD));
		identityDbService.changePassword(system, encodedPassword);
	}
}