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
package eu.arrowhead.authentication.service.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityQueryRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentitySessionQueryRequestDTO;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.IdentityQueryRequestDTO;
import eu.arrowhead.dto.IdentitySessionQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class ManagementNormalization {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private PageService pageService;

	@Autowired
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentityListMgmtRequestDTO normalizeCreateIdentityList(final IdentityListMgmtCreateRequestDTO dto) {
		logger.debug("normalizeIdentityList started...");
		Assert.notNull(dto, "Payload is missing");
		Assert.isTrue(!Utilities.isEmpty(dto.identities()), "Identities list is missing or empty");
		Assert.isTrue(!Utilities.containsNull(dto.identities()), "Identities list contains null element");

		final AuthenticationMethod normalizedAuthenticationMethod = authenticationMethodFromString(dto.authenticationMethod());
		final IAuthenticationMethod method = methods.method(normalizedAuthenticationMethod);
		Assert.notNull(method, "Authentication method is unsupported");

		final List<NormalizedIdentityMgmtRequestDTO> result = new ArrayList<>(dto.identities().size());
		for (final IdentityMgmtRequestDTO identity : dto.identities()) {
			final String normalizedSystem = systemNameNormalizer.normalize(identity.systemName());
			final Map<String, String> normalizedCredentials = method.normalizer().normalizeCredentials(identity.credentials());
			final boolean normalizedSysop = identity.sysop() == null ? false : identity.sysop().booleanValue();

			result.add(new NormalizedIdentityMgmtRequestDTO(
					normalizedSystem,
					normalizedCredentials,
					normalizedSysop));
		}

		return new NormalizedIdentityListMgmtRequestDTO(method, result);
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> normalizeUpdateIdentityListWithoutCredentials(final IdentityListMgmtUpdateRequestDTO dto) {
		logger.debug("normalizeUpdateIdentityListWithoutCredentials started...");
		Assert.notNull(dto, "Payload is missing");
		Assert.isTrue(!Utilities.isEmpty(dto.identities()), "Identities list is missing or empty");
		Assert.isTrue(!Utilities.containsNull(dto.identities()), "Identities list contains null element");

		final List<NormalizedIdentityMgmtRequestDTO> result = new ArrayList<>(dto.identities().size());
		for (final IdentityMgmtRequestDTO identity : dto.identities()) {
			final String normalizedSystem = systemNameNormalizer.normalize(identity.systemName());
			final boolean normalizedSysop = identity.sysop() == null ? false : identity.sysop().booleanValue();

			result.add(new NormalizedIdentityMgmtRequestDTO(
					normalizedSystem,
					identity.credentials(),
					normalizedSysop));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> normalizeCredentials(final IAuthenticationMethod authenticationMethod, final List<NormalizedIdentityMgmtRequestDTO> identities) {
		logger.debug("normalizeCredentials started...");
		Assert.notNull(authenticationMethod, "Authentication method is missing");
		Assert.isTrue(!Utilities.isEmpty(identities), "Identities list is missing or empty");
		Assert.isTrue(!Utilities.containsNull(identities), "Identities list contains null element");

		final List<NormalizedIdentityMgmtRequestDTO> result = new ArrayList<>(identities.size());
		for (final NormalizedIdentityMgmtRequestDTO identity : identities) {
			final Map<String, String> normalizedCredentials = authenticationMethod.normalizer().normalizeCredentials(identity.credentials());

			result.add(new NormalizedIdentityMgmtRequestDTO(
					identity.systemName(),
					normalizedCredentials,
					identity.sysop()));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeIdentifiableSystemNames(final List<String> originalNames) {
		logger.debug("normalizeIdentifiableSystemNames started");
		Assert.notNull(originalNames, "name list is null");

		return originalNames.stream()
				.map(n -> systemNameNormalizer.normalize(n))
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentityQueryRequestDTO normalizeIdentityQueryRequest(final IdentityQueryRequestDTO dto) {
		logger.debug("normalizeIdentityQueryRequest started");

		if (dto == null) {
			return new NormalizedIdentityQueryRequestDTO(
					pageService.getPageRequest(null, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter"),
					null,
					null,
					null,
					null,
					null,
					null);
		}

		return new NormalizedIdentityQueryRequestDTO(
				pageService.getPageRequest(dto.pagination(), System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, "does not matter"),
				Utilities.isEmpty(dto.namePart()) ? null : dto.namePart().trim(), // not a system name, just a part
				dto.isSysop(),
				Utilities.isEmpty(dto.createdBy()) ? null : systemNameNormalizer.normalize(dto.createdBy()),
				Utilities.isEmpty(dto.creationFrom()) ? null : Utilities.parseUTCStringToZonedDateTime(dto.creationFrom()),
				Utilities.isEmpty(dto.creationTo()) ? null : Utilities.parseUTCStringToZonedDateTime(dto.creationTo()),
				dto.hasSession());
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentitySessionQueryRequestDTO normalizeSessionQueryRequest(final IdentitySessionQueryRequestDTO dto) {
		logger.debug("normalizeSessionQueryRequest started");

		if (dto == null) {
			return new NormalizedIdentitySessionQueryRequestDTO(
					pageService.getPageRequest(null, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter"),
					null,
					null,
					null);
		}

		final PageDTO pagination = dto.pagination();
		final PageDTO normalizedPageDTO = pagination != null
				&& !Utilities.isEmpty(pagination.sortField())
				&& ActiveSession.SYSTEM_NAME_ALTERNATIVES.contains(dto.pagination().sortField().trim().toLowerCase())
						? new PageDTO(pagination.page(), pagination.size(), pagination.direction(), ActiveSession.SORT_NAME_SYSTEM_NAME)
						: pagination;

		return new NormalizedIdentitySessionQueryRequestDTO(
				pageService.getPageRequest(normalizedPageDTO, ActiveSession.SORTABLE_FIELDS_BY, ActiveSession.DEFAULT_SORT_FIELD, "does not matter"),
				Utilities.isEmpty(dto.namePart()) ? null : dto.namePart().trim(), // not a system name, just a part
				Utilities.isEmpty(dto.loginFrom()) ? null : Utilities.parseUTCStringToZonedDateTime(dto.loginFrom()),
				Utilities.isEmpty(dto.loginTo()) ? null : Utilities.parseUTCStringToZonedDateTime(dto.loginTo()));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthenticationMethod authenticationMethodFromString(final String str) {
		logger.debug("authenticationMethodFromString started...");
		Assert.isTrue(!Utilities.isEmpty(str), "Authentication method is missing or empty");

		final String authMethodName = str.trim().toUpperCase();
		Assert.isTrue(Utilities.isEnumValue(authMethodName, AuthenticationMethod.class), "Authentication method is invalid");

		return AuthenticationMethod.valueOf(authMethodName);
	}
}