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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@Service
public class AuthorizationPolicyRequestValidator {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateAuthorizationPolicy(final AuthorizationPolicyRequestDTO dto, final boolean isDefault, final String origin) {
		logger.debug("validateAuthorizationPolicy started...");

		if (dto == null) {
			throw new InvalidParameterException((isDefault ? "Default p" : "P") + "olicy is missing", origin);
		}

		// policy type
		if (Utilities.isEmpty(dto.policyType())) {
			throw new InvalidParameterException("Policy type is missing", origin);
		}

		final String policyTypeName = dto.policyType().trim().toUpperCase();

		if (!Utilities.isEnumValue(policyTypeName, AuthorizationPolicyType.class)) {
			throw new InvalidParameterException("Policy type is invalid: " + dto.policyType(), origin);
		}

		final AuthorizationPolicyType policyType = AuthorizationPolicyType.valueOf(policyTypeName);

		switch (policyType) {
		case BLACKLIST:
		case WHITELIST:
			validateList(dto.policyList(), origin);
			break;
		case SYS_METADATA:
			validateMetadataRequirement(dto.policyMetadataRequirement(), origin);
			break;
		case ALL:
			// do nothing
			break;
		default:
			throw new InvalidParameterException("Unknown policy type: " + policyTypeName, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedAuthorizationPolicy(final NormalizedAuthorizationPolicyRequest dto) {
		logger.debug("validateNormalizedAuthorizationPolicy started...");

		switch (dto.policyType()) {
		case BLACKLIST:
		case WHITELIST:
			validateNormalizedList(dto.policyList());
			break;
		case SYS_METADATA:
		case ALL:
			// do nothing
			break;
		default:
			throw new InvalidParameterException("Unknown policy type: " + dto.policyType().name());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateList(final List<String> policyList, final String origin) {
		logger.debug("validateList started...");

		if (Utilities.isEmpty(policyList)) {
			throw new InvalidParameterException("List is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(policyList)) {
			throw new InvalidParameterException("List contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateMetadataRequirement(final MetadataRequirementDTO policyMetadataRequirement, final String origin) {
		logger.debug("validateMetadataRequirement started...");

		if (Utilities.isEmpty(policyMetadataRequirement)) {
			throw new InvalidParameterException("Metadata requirements is empty", origin);

		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedList(final List<String> policyList) {
		logger.debug("validateNormalizedList started...");

		for (final String systemName : policyList) {
			systemNameValidator.validateSystemName(systemName);
		}
	}
}