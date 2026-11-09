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
package eu.arrowhead.common.service.validation.serviceinstance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;

@Component
public class ServiceInstanceIdentifierNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private VersionNormalizer versionNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalize(final String serviceInstanceId) {
		logger.debug("normalize service instance identitifer started...");

		if (serviceInstanceId == null) {
			return null;
		}

		final String[] parts = serviceInstanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		parts[0] = systemNameNormalizer.normalize(parts[0]);

		if (parts.length > 1) {
			parts[1] = serviceDefNameNormalizer.normalize(parts[1]);
		}

		if (parts.length > 2) {
			parts[2] = versionNormalizer.normalize(parts[2]);
		}

		return String.join(Constants.COMPOSITE_ID_DELIMITER, parts);
	}
}