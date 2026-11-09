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

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.validation.AuthorizationManagementValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class AuthorizationManagementService {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationManagementValidation validator;

	@Autowired
	private AuthorizationPolicyDbService dbService;

	@Autowired
	private AuthorizationPolicyEngine policyEngine;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private PageService pageService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO grantPoliciesOperation(final String requester, final AuthorizationMgmtGrantListRequestDTO dto, final String origin) {
		logger.debug("grantPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeSystemName(requester, origin);
		final List<NormalizedGrantRequest> normalizedList = validator.validateAndNormalizeGrantListRequest(dto, origin);

		try {
			final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.createMgmtLevelPoliciesInBulk(normalizedRequester, normalizedList);

			return dtoConverter.convertMgmtLevelPolicyListToResponse(result);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void revokePoliciesOperation(final List<String> instanceIds, final String origin) {
		logger.debug("revokePoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalizedList = validator.validateAndNormalizeRevokePoliciesInput(instanceIds, origin);

		try {
			dbService.deletePoliciesByInstanceIds(normalizedList);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO queryPoliciesOperation(final AuthorizationQueryRequestDTO dto, final String origin) {
		logger.debug("queryPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final NormalizedQueryRequest normalized = validator.validateAndNormalizeQueryRequest(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(
				dto.pagination(),
				Direction.ASC,
				normalized.level() == AuthorizationLevel.MGMT ? AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY : AuthProviderPolicyHeader.SORTABLE_FIELDS_BY,
				AuthPolicyHeader.DEFAULT_SORT_FIELD,
				origin);

		try {
			if (normalized.level() == AuthorizationLevel.MGMT) {
				final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, normalized);

				return dtoConverter.convertMgmtLevelPolicyPageToResponse(result);
			} else {
				final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(
						pageRequest,
						normalized);

				return dtoConverter.convertProviderLevelPolicyPageToResponse(result);
			}
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationVerifyListResponseDTO checkPoliciesOperation(final AuthorizationVerifyListRequestDTO dto, final String origin) {
		logger.debug("checkPoliciesOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<NormalizedVerifyRequest> normalized = validator.validateAndNormalizeVerifyListRequest(dto, origin);

		try {
			final List<Pair<NormalizedVerifyRequest, Boolean>> result = policyEngine.checkAccess(normalized);

			return dtoConverter.convertCheckResultListToResponse(result);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}