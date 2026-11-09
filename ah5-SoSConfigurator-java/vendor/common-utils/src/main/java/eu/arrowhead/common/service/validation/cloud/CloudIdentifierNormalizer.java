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
package eu.arrowhead.common.service.validation.cloud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@Component
public class CloudIdentifierNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer; // the two parts of the cloud identifier should follow the naming convention of systems

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalize(final String cloudIdentifier) {
		logger.debug("normalize cloud identitifer started...");

		if (Utilities.isEmpty(cloudIdentifier)) {
			return null;
		}

		if (Defaults.DEFAULT_CLOUD.equalsIgnoreCase(cloudIdentifier.trim())) {
			return Defaults.DEFAULT_CLOUD;
		}

		final String[] parts = cloudIdentifier.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		for (int i = 0; i < parts.length; ++i) {
			parts[i] = systemNameNormalizer.normalize(parts[i]);
		}

		return String.join(Constants.COMPOSITE_ID_DELIMITER, parts);
	}
}