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
package eu.arrowhead.common.service.validation.version;

import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InvalidParameterException;

@Component
public class VersionValidator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	private static final String ERROR_MSG_PREFIX = "Version verification failure: ";
	private static final String DIGITS_ONLY_REGEX_STRING = "[0-9]+";
	private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile(DIGITS_ONLY_REGEX_STRING);
	private static final char DOT = '.';
	private static final String DOT_REGEX = "\\.";
	private static final int NUMBER_OF_SEPARATORS = 2;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedVersion(final String version) {
		logger.debug("Validate version started: {}", version);

		// check size
		if (version.length() > Constants.SYSTEM_VERSION_MAX_LENGTH) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + "version is too long");
		}

		// check the number of the dot characters
		if (version.chars().filter(ch -> ch == DOT).count() != NUMBER_OF_SEPARATORS) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + "version must contain exactly " + NUMBER_OF_SEPARATORS + " separator "  + DOT + " characters");
		}

		// check the number of version numbers
		final String[] chunksOfVersion = version.split(DOT_REGEX);
		if (chunksOfVersion.length != NUMBER_OF_SEPARATORS + 1) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + "version must contain exactly " + (NUMBER_OF_SEPARATORS + 1) + " numbers");
		}

		for (int i = 0; i < chunksOfVersion.length; ++i) {
			if (!DIGITS_ONLY_PATTERN.matcher(chunksOfVersion[i]).matches()) {
				throw new InvalidParameterException(ERROR_MSG_PREFIX + "the version should only contain numbers and separator " + DOT + " characters");
			}
		}
	}
}