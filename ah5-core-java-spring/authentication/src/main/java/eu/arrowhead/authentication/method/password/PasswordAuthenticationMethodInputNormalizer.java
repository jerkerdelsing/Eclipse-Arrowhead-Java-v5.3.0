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

import eu.arrowhead.authentication.method.IAuthenticationMethodInputNormalizer;
import eu.arrowhead.common.Utilities;

@Service
public class PasswordAuthenticationMethodInputNormalizer implements IAuthenticationMethodInputNormalizer {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public Map<String, String> normalizeCredentials(final Map<String, String> credentials) {
		logger.debug("PasswordAuthenticationMethodInputNormalizer.normalizeCredentials started...");

		if (credentials == null
				|| !credentials.containsKey(PasswordAuthenticationMethod.KEY_PASSWORD)
				|| Utilities.isEmpty(credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD))) {
			return credentials;
		}

		return Map.of(PasswordAuthenticationMethod.KEY_PASSWORD, credentials.get(PasswordAuthenticationMethod.KEY_PASSWORD).trim());
	}
}