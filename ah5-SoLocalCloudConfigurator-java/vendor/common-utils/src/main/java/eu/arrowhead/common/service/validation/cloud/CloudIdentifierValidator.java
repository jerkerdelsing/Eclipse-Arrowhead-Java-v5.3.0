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
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;

@Component
public class CloudIdentifierValidator {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator; // the two parts of the cloud identifier should follow the naming convention of systems

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateCloudIdentifier(final String identifier) {
		logger.debug("validateCloudIdentifier started: {}", identifier);

		if (Utilities.isEmpty(identifier)) {
			throw new InvalidParameterException("The specified cloud identifier does not match the naming convention: " + identifier);

		}

		if (!Defaults.DEFAULT_CLOUD.equals(identifier)) {
			// accepted format <CloudName><delimiter><OrganizationName>
			final String[] parts = identifier.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
			if (parts.length != 2) {
				throw new InvalidParameterException("The specified cloud identifier does not match the naming convention: " + identifier);
			}

			for (final String part : parts) {
				systemNameValidator.validateSystemName(part);
			}
		}
	}
}