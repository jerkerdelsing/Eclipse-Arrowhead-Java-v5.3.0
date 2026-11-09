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
package eu.arrowhead.authorization.service.engine;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@Service
public class AuthorizationPolicyEngine {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationPolicyDbService dbService;

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isAccessGranted(final NormalizedVerifyRequest request) {
		logger.debug("isAccessGranted started...");
		Assert.notNull(request, "request is null");

		// 1. Collect policies from the MGMT level
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> relevantMgmtLevelPolicies = dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request);

		// 2. Evaluate MGMT level policies
		Result result = evalPolicies(request.consumer(), relevantMgmtLevelPolicies);

		if (Result.UNDECIDED == result) {
			// 3. Collect policies from the PROVIDER level
			final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> relevantProviderLevelPolicies = dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

			// 4. Evaluate PROVIDER level policies
			result = evalPolicies(request.consumer(), relevantProviderLevelPolicies);
		}

		return result.toBoolean();
	}

	//-------------------------------------------------------------------------------------------------
	public List<Pair<NormalizedVerifyRequest, Boolean>> checkAccess(final List<NormalizedVerifyRequest> requestList) {
		logger.debug("checkAccess started...");
		Assert.isTrue(!Utilities.isEmpty(requestList), "Request list is missing");
		Assert.isTrue(!Utilities.containsNull(requestList), "Request list contains null element");

		return requestList
				.stream()
				.map(request -> Pair.of(request, isAccessGranted(request)))
				.toList();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Result evalPolicies(final String consumer, final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> policiesOpt) {
		logger.debug("evalPolicies started...");

		if (policiesOpt.isEmpty()) {
			// no policies
			return Result.UNDECIDED;
		}

		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> policies = policiesOpt.get();
		Map<String, Object> consumerMetadata = null;
		final boolean needConsumerMetadata = needConsumerMetadata(policies.getSecond());
		if (needConsumerMetadata) {
			consumerMetadata = acquireConsumerMetadata(consumer);
		}

		for (final AuthPolicy policy : policies.getSecond()) {
			if (!evalPolicy(consumer, consumerMetadata, policy)) {
				return Result.FALSE;
			}
		}

		return Result.TRUE;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean evalPolicy(final String consumer, final Map<String, Object> consumerMetadata, final AuthPolicy policy) {
		logger.debug("evalPolicy started...");

		final AuthorizationPolicyDTO policyDTO = dtoConverter.convertAuthPolicyToDTO(policy);

		switch (policyDTO.policyType()) {
		case ALL:
			return true;
		case BLACKLIST:
			return !policyDTO.policyList().contains(consumer);
		case WHITELIST:
			return policyDTO.policyList().contains(consumer);
		case SYS_METADATA:
			return MetadataRequirementsMatcher.isMetadataMatch(consumerMetadata, policyDTO.policyMetadataRequirement());
		default:
			logger.error("Unknown policy type: {}", policyDTO.policyType().name());
			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private boolean needConsumerMetadata(final List<AuthPolicy> policies) {
		logger.debug("needConsumerMetadata started...");

		return policies
				.stream()
				.anyMatch(p -> AuthorizationPolicyType.SYS_METADATA == p.getPolicyType());
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> acquireConsumerMetadata(final String consumer) {
		logger.debug("acquireConsumerMetadata started...");

		final SystemLookupRequestDTO payload = new SystemLookupRequestDTO(List.of(consumer), null, null, null, null, null);
		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(1);
		queryParams.put(Constants.VERBOSE, List.of(String.valueOf(false)));

		try {
			final SystemListResponseDTO response = httpService.consumeService(
					Constants.SERVICE_DEF_SYSTEM_DISCOVERY,
					Constants.SERVICE_OP_LOOKUP,
					SystemListResponseDTO.class,
					payload,
					queryParams);

			if (Utilities.isEmpty(response.entries())) {
				return null;
			}

			return response
					.entries()
					.get(0)
					.metadata();
		} catch (final Exception ex) {
			// something happened during network communication
			logger.error(ex.getMessage());
			logger.debug(ex);

			return null;
		}
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private enum Result {
		TRUE, FALSE, UNDECIDED;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public boolean toBoolean() {
			return this == TRUE;
		}
	}
}