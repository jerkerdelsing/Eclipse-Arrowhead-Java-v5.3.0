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
public class DeviceNameValidator {

	//=================================================================================================
	// members

	// snake-case naming convention, only allowed characters are upper-case ASCII letters and numbers and underscore
	private static final String DEVICE_NAME_REGEX_STRING = "([A-Z]{1})|(^[A-Z][0-9A-Z_]*[0-9A-Z]$)";
	private static final Pattern DEVICE_NAME_REGEX_PATTERN = Pattern.compile(DEVICE_NAME_REGEX_STRING);

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateDeviceName(final String name) {
		logger.debug("validateDeviceName started: {}", name);

		if (Utilities.isEmpty(name)
				|| !DEVICE_NAME_REGEX_PATTERN.matcher(name).matches()
				|| name.length() > Constants.DEVICE_NAME_MAX_LENGTH) {
			throw new InvalidParameterException("The specified device name does not match the naming convention: " + name);
		}
	}
}