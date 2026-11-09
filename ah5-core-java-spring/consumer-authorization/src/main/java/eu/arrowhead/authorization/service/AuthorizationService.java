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
package eu.arrowhead.authorization.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.utils.InstanceIdUtils;
import eu.arrowhead.authorization.service.validation.AuthorizationValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationLookupRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class AuthorizationService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationValidation validator;

	@Autowired
	private AuthorizationPolicyDbService dbService;

	@Autowired
	private AuthorizationPolicyEngine policyEngine;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<AuthorizationPolicyResponseDTO, Boolean> grantOperation(final String provider, final AuthorizationGrantRequestDTO dto, final String origin) {
		logger.debug("grantOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedProvider = validator.validateAndNormalizeSystemName(provider, origin);
		final NormalizedGrantRequest normalized = validator.validateAndNormalizeGrantRequest(normalizedProvider, dto, origin);

		try {
			final Pair<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>, Boolean> result = dbService.createProviderLevelPolicy(normalized); // second is whether we created a new policy or not

			return Pair.of(
					dtoConverter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, result.getFirst()),
					result.getSecond());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeOperation(final String identifiedSystemName, final String instanceId, final String origin) {
		logger.debug("revokeOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final Pair<String, String> normalized = validator.validateAndNormalizeRevokeInput(identifiedSystemName, instanceId, origin); // contains normalized system name and instance id in that order

		if (!InstanceIdUtils.retrieveProviderName(normalized.getSecond()).equals(normalized.getFirst())) {
			throw new ForbiddenException("Revoking other systems' policy is forbidden", origin);
		}

		try {
			return dbService.deleteProviderLevelPolicyByInstanceId(normalized.getSecond());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO lookupOperation(final String provider, final AuthorizationLookupRequestDTO dto, final String origin) {
		logger.debug("lookupOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedProvider = validator.validateAndNormalizeSystemName(provider, origin);
		final NormalizedLookupRequest normalized = validator.validateAndNormalizeLookupRequest(normalizedProvider, dto, origin);

		try {
			final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(
					PageRequest.of(0, Integer.MAX_VALUE, Direction.ASC, AuthProviderPolicyHeader.DEFAULT_SORT_FIELD),
					normalized);

			return dtoConverter.convertProviderLevelPolicyPageToResponse(result);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean verifyOperation(final String requester, final AuthorizationVerifyRequestDTO dto, final String origin) {
		logger.debug("verifyOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requester, origin);
		final NormalizedVerifyRequest normalized = validator.validateAndNormalizeVerifyRequest(normalizedRequester, dto, origin);

		return policyEngine.isAccessGranted(normalized);
	}
}