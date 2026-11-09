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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@Service
public class AuthorizationPolicyRequestNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedAuthorizationPolicyRequest normalize(final AuthorizationPolicyRequestDTO dto) {
		logger.debug("AuthorizationPolicyRequestNormalizer.normalize started...");

		if (dto == null) {
			return null;
		}

		final AuthorizationPolicyType policyType = AuthorizationPolicyType.valueOf(dto.policyType().trim().toUpperCase());
		List<String> list = null;
		MetadataRequirementDTO metadataRequirement = null;

		switch (policyType) {
		case ALL:
			break;
		case BLACKLIST:
		case WHITELIST:
			list = normalizeSystemList(dto.policyList());
			break;
		case SYS_METADATA:
			metadataRequirement = dto.policyMetadataRequirement();
			break;
		default:
			throw new InvalidParameterException("Unknown policy type: " + policyType.name());
		}

		return new NormalizedAuthorizationPolicyRequest(
				policyType,
				list,
				metadataRequirement);

	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<String> normalizeSystemList(final List<String> systemList) {
		logger.debug("normalizeSystemList started...");
		final Set<String> set = systemList
				.stream()
				.map(sys -> systemNameNormalizer.normalize(sys))
				.collect(Collectors.toSet()); // to remove any duplicates

		return new ArrayList<>(set);
	}
}