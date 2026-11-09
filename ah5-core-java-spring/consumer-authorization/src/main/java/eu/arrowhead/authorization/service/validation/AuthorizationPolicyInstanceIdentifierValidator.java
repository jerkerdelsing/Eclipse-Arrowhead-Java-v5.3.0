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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.authorization.service.utils.InstanceIdUtils;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierValidator;
import eu.arrowhead.common.service.validation.name.EventTypeNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Component
public class AuthorizationPolicyInstanceIdentifierValidator {

	//=================================================================================================
	// members

	@Autowired
	private CloudIdentifierValidator cloudIdentifierValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private EventTypeNameValidator eventTypeNameValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	public void validateInstanceIdentifier(final String identifier) {
		logger.debug("validateInstanceIdentifier started: {}", identifier);

		if (Utilities.isEmpty(identifier)
				|| identifier.length() > Constants.AUTHORIZATION_POLICY_ID_MAX_LENGTH) {
			throw new InvalidParameterException("The specified instance identifier does not match the naming convention: " + identifier);

		}

		final String[] parts = identifier.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP); // contains: level, cloud (one or two parts), provider, targetType, target
		final boolean isLocal = parts.length == InstanceIdUtils.INSTANCE_ID_MIN_PARTS && Defaults.DEFAULT_CLOUD.equals(parts[1]);

		final int acceptedCount = isLocal ? InstanceIdUtils.INSTANCE_ID_MIN_PARTS : InstanceIdUtils.INSTANCE_ID_MIN_PARTS + 1;
		if (parts.length != acceptedCount) {
			throw new InvalidParameterException("The specified instance identifier does not match the naming convention: " + identifier);
		}

		// level
		final AuthorizationLevel level = AuthorizationLevel.fromPrefix(parts[0]);
		if (level == null) {
			throw new InvalidParameterException("The specified instance identifier does not match the naming convention: " + identifier);
		}

		// cloud
		final String cloudIdentifier = isLocal ? parts[1] : String.join(Constants.COMPOSITE_ID_DELIMITER, parts[1], parts[2]);
		cloudIdentifierValidator.validateCloudIdentifier(cloudIdentifier);

		// provider
		final String provider = isLocal ? parts[2] : parts[3];
		systemNameValidator.validateSystemName(provider);

		// target type
		final String targetTypeStr = isLocal ? parts[3] : parts[4];
		AuthorizationTargetType targetType = null;
		try {
			targetType = AuthorizationTargetType.valueOf(targetTypeStr);
		} catch (final IllegalArgumentException __) {
			throw new InvalidParameterException("The specified instance identifier does not match the naming convention: " + identifier);
		}

		// target
		final String target = isLocal ? parts[4] : parts[5];
		if (AuthorizationTargetType.SERVICE_DEF == targetType) {
			serviceDefNameValidator.validateServiceDefinitionName(target);
		} else {
			eventTypeNameValidator.validateEventTypeName(target);
		}
	}
}