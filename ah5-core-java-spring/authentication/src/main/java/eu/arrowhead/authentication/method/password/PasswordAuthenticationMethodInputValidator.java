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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputValidator;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class PasswordAuthenticationMethodInputValidator implements IAuthenticationMethodInputValidator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void validateCredentials(final Map<String, String> credentials) throws InvalidParameterException {
		logger.debug("PasswordAuthenticationMethodInputValidator.validateCredentials started...");

		if (Utilities.isEmpty(credentials)
				|| !credentials.containsKey(PasswordAuthenticationMethod.KEY_PASSWORD)) {
			throw new InvalidParameterException("Missing credentials");
		}

		if (Utilities.isEmpty(credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD))) {
			throw new InvalidParameterException("Missing or empty password");
		}

		if (credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD).length() > AuthenticationConstants.PASSWORD_MAX_LENGTH) {
			throw new InvalidParameterException("Password is too long, maximum length is " + AuthenticationConstants.PASSWORD_MAX_LENGTH + " characters");
		}
	}
}