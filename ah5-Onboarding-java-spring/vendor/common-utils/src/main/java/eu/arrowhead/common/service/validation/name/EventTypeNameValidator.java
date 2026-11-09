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
package eu.arrowhead.common.service.validation.name;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class EventTypeNameValidator {

	//=================================================================================================
	// members

	// camelCase naming convention, only allowed characters are lower- and upper-case ASCII letters and numbers
	private static final String EVENT_TYPE_NAME_REGEX_STRING = "([a-z]{1})|(^[a-z][0-9A-Za-z]+$)";
	private static final Pattern EVENT_TYPE_NAME_REGEX_PATTERN = Pattern.compile(EVENT_TYPE_NAME_REGEX_STRING);

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateEventTypeName(final String name) {
		logger.debug("validateEventTypeName started: {}", name);

		if (Utilities.isEmpty(name)
				|| !EVENT_TYPE_NAME_REGEX_PATTERN.matcher(name).matches()
				|| name.length() > Constants.EVENT_TYPE_NAME_MAX_LENGTH) {
			throw new InvalidParameterException("The specified event type name does not match the naming convention: " + name);
		}
	}
}