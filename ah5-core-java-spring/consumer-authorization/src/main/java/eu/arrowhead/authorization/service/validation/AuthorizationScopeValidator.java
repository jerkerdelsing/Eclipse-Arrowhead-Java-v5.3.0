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
package eu.arrowhead.authorization.service.validation;

import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;

@Component
public class AuthorizationScopeValidator {

	//=================================================================================================
	// members

	// kebab-case naming convention, only allowed characters are lower-case ASCII letters and numbers and hyphen
	private static final String SCOPE_REGEX_STRING = "([a-z]{1})|(^[a-z][0-9a-z\\-]*[0-9a-z]$)";
	private static final Pattern SCOPE_REGEX_PATTERN = Pattern.compile(SCOPE_REGEX_STRING);

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateScope(final String scope) {
		logger.debug("validateScope started: {}", scope);

		if (Defaults.DEFAULT_AUTHORIZATION_SCOPE.equals(scope)) {
			return;
		}

		if (Utilities.isEmpty(scope)
				|| !SCOPE_REGEX_PATTERN.matcher(scope).matches()
				|| scope.length() > Constants.SCOPE_MAX_LENGTH) {
			throw new InvalidParameterException("The specified scope does not match the naming convention: " + scope);
		}
	}
}