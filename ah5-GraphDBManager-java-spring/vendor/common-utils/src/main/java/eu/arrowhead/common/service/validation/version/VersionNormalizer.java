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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;

@Component
public class VersionNormalizer {

	//=================================================================================================
	// members

	private static final String DEFAULT_MAJOR = "1";
	private static final String DEFAULT_MINOR = "0";
	private static final String DEFAULT_PATCH = "0";
	private static final String DOT = ".";
	private static final String DOT_REGEX = "\\.";
	private static final String DEFAULT_VERSION = DEFAULT_MAJOR + DOT + DEFAULT_MINOR + DOT + DEFAULT_PATCH;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalize(final String version) {
		logger.debug("normalize version started...");

		if (Utilities.isEmpty(version)) {
			return DEFAULT_VERSION;
		}

		final String candidate = version.trim();

		final String[] chunks = candidate.split(DOT_REGEX);
		final int numberOfChunks = chunks.length;
		if (numberOfChunks == 1) {
			return chunks[0] + DOT + DEFAULT_MINOR + DOT + DEFAULT_PATCH;
		}

		if (numberOfChunks == 2) {
			return chunks[0] + DOT + chunks[1] + DOT + DEFAULT_PATCH;
		}

		return candidate;
	}
}