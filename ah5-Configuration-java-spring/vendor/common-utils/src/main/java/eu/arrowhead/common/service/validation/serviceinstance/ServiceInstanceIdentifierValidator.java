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
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;

@Component
public class ServiceInstanceIdentifierValidator {

	//=================================================================================================
	// members

	private static final int IDENTIFIER_LENGTH = 3;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private VersionValidator versionValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateServiceInstanceIdentifier(final String identifier) {
		logger.debug("validateServiceInstanceIdentifier started: {}", identifier);

		if (Utilities.isEmpty(identifier)) {
			throw new InvalidParameterException("The specified service instance identifier does not match the naming convention: " + identifier);

		}

		// accepted format <ProviderName><delimiter><serviceDefinitionName><delimiter><semantic version>
		final String[] parts = identifier.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		if (parts.length != IDENTIFIER_LENGTH) {
			throw new InvalidParameterException("The specified service instance identifier does not match the naming convention: " + identifier);
		}

		systemNameValidator.validateSystemName(parts[0]);
		serviceDefNameValidator.validateServiceDefinitionName(parts[1]);
		versionValidator.validateNormalizedVersion(parts[2]);
	}
}