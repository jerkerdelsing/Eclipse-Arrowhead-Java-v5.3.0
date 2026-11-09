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
package eu.arrowhead.authorization.service.normalization;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.authorization.service.utils.InstanceIdUtils;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Component
public class AuthorizationPolicyInstanceIdentifierNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	public String normalize(final String instanceId) {
		logger.debug("normalize authorization policy instance identitifer started...");

		if (Utilities.isEmpty(instanceId)) {
			return null;
		}

		final String[] parts = instanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP); // contains: level, cloud (one or two parts), provider, targetType, target
		final boolean isLocal = parts.length == InstanceIdUtils.INSTANCE_ID_MIN_PARTS && Defaults.DEFAULT_CLOUD.equalsIgnoreCase(parts[1].trim());

		final List<String> normalized = new ArrayList<>(InstanceIdUtils.INSTANCE_ID_MIN_PARTS);

		// level
		normalized.add(parts[0].trim().toUpperCase());

		// cloud
		final String cloudIdentifier = isLocal ? parts[1] : String.join(Constants.COMPOSITE_ID_DELIMITER, parts[1], parts[2]);
		normalized.add(cloudIdentifierNormalizer.normalize(cloudIdentifier));

		// provider
		final String provider = isLocal ? parts[2] : parts[3];
		normalized.add(systemNameNormalizer.normalize(provider));

		// target type
		final String targetType = isLocal ? parts[3] : parts[4];
		final String normalizedTargetType = targetType.trim().toUpperCase();
		normalized.add(normalizedTargetType);

		// target
		final String target = isLocal ? parts[4] : parts[5];
		if (AuthorizationTargetType.SERVICE_DEF.name().equals(normalizedTargetType)) {
			normalized.add(serviceDefNameNormalizer.normalize(target));
		} else {
			normalized.add(eventTypeNameNormalizer.normalize(target));
		}

		return String.join(Constants.COMPOSITE_ID_DELIMITER, normalized);
	}
}