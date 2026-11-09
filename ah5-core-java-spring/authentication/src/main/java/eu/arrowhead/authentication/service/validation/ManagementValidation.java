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
package eu.arrowhead.authentication.service.validation;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import eu.arrowhead.authentication.service.normalization.ManagementNormalization;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.IdentityQueryRequestDTO;
import eu.arrowhead.dto.IdentitySessionQueryRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class ManagementValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ManagementNormalization normalizer;

	@Autowired
	private AuthenticationMethods methods;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequester(final String requester, final String origin) {
		logger.debug("validateAndNormalizeRequester started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRequester(requester, origin);
		final String normalized = systemNameNormalizer.normalize(requester);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentityListMgmtRequestDTO validateAndNormalizeCreateIdentityList(final IdentityListMgmtCreateRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateIdentityList started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateCreateIdentityList(dto, origin);

		try {
			final NormalizedIdentityListMgmtRequestDTO result = normalizer.normalizeCreateIdentityList(dto);
			checkNameDuplications(result.identities().stream().map(ni -> ni.systemName()).toList(), origin);

			final IAuthenticationMethod method = result.authenticationMethod();
			result.identities().forEach(i -> {
				systemNameValidator.validateSystemName(i.systemName());
				method.validator().validateCredentials(i.credentials());
			});

			return result;
		} catch (final InvalidParameterException ex) {
			throw Utilities.isEmpty(ex.getOrigin()) ? new InvalidParameterException(ex.getMessage(), origin) : ex;
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> validateAndNormalizeUpdateIdentityListPhase1(final IdentityListMgmtUpdateRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateIdentityListPhase1 started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateUpdateIdentityListPhase1(dto, origin);

		final List<NormalizedIdentityMgmtRequestDTO> result = normalizer.normalizeUpdateIdentityListWithoutCredentials(dto);
		checkNameDuplications(result.stream().map(ni -> ni.systemName()).toList(), origin);

		try {
			result.forEach(i -> systemNameValidator.validateSystemName(i.systemName()));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedIdentityMgmtRequestDTO> validateAndNormalizeUpdateIdentityListPhase2(
			final IAuthenticationMethod authenticationMethod,
			final List<NormalizedIdentityMgmtRequestDTO> identities,
			final String origin) {
		logger.debug("validateAndNormalizeUpdateIdentityListPhase2 started...");
		Assert.notNull(authenticationMethod, "Authentication method is missing");
		Assert.isTrue(!Utilities.isEmpty(identities), "Identities list is missing or empty");
		Assert.isTrue(!Utilities.containsNull(identities), "Identities list contains null element");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			final List<NormalizedIdentityMgmtRequestDTO> normalized = normalizer.normalizeCredentials(authenticationMethod, identities);
			validateUpdateIdentityListPhase2(authenticationMethod, normalized);

			return normalized;
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveIdentities(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeRemoveIdentities started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateIdentityNames(names, origin);

		final List<String> normalized = normalizer.normalizeIdentifiableSystemNames(names);

		try {
			normalized.forEach(n -> systemNameValidator.validateSystemName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentityQueryRequestDTO validateAndNormalizeIdentityQueryRequest(final IdentityQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeIdentityQueryRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateIdentityQueryRequest(dto, origin);
		final NormalizedIdentityQueryRequestDTO normalized = normalizer.normalizeIdentityQueryRequest(dto);

		try {
			if (!Utilities.isEmpty(normalized.createdBy())) {
				systemNameValidator.validateSystemName(normalized.createdBy());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeCloseSessions(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeCloseSessions started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateIdentityNames(names, origin);
		final List<String> normalized = normalizer.normalizeIdentifiableSystemNames(names);

		try {
			normalized.forEach(n -> systemNameValidator.validateSystemName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedIdentitySessionQueryRequestDTO validateAndNormalizeSessionQueryRequest(final IdentitySessionQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeSessionQueryRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateSessionQueryRequest(dto, origin);

		return normalizer.normalizeSessionQueryRequest(dto);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateRequester(final String requester, final String origin) {
		logger.debug("validateRequester started...");

		if (Utilities.isEmpty(requester)) {
			throw new InvalidParameterException("Requester name is missing or empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateCreateIdentityList(final IdentityListMgmtCreateRequestDTO dto, final String origin) {
		logger.debug("validateCreateIdentityList started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.authenticationMethod())) {
			throw new InvalidParameterException("Authentication method is missing", origin);
		}

		final String authMethodName = dto.authenticationMethod().trim().toUpperCase();

		if (!Utilities.isEnumValue(authMethodName, AuthenticationMethod.class)) {
			throw new InvalidParameterException("Authentication method is invalid: " + dto.authenticationMethod(), origin);
		}

		final AuthenticationMethod methodType = AuthenticationMethod.valueOf(authMethodName);
		final IAuthenticationMethod method = methods.method(methodType);
		if (method == null) {
			throw new InvalidParameterException("Authentication method is unsupported: " + dto.authenticationMethod(), origin);
		}

		final List<IdentityMgmtRequestDTO> list = dto.identities();
		if (Utilities.isEmpty(list)) {
			throw new InvalidParameterException("Identity list is missing or empty", origin);
		}

		if (Utilities.containsNull(list)) {
			throw new InvalidParameterException("Identity list contains null element", origin);
		}

		for (final IdentityMgmtRequestDTO identity : list) {
			validateIdentity(identity, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateUpdateIdentityListPhase1(final IdentityListMgmtUpdateRequestDTO dto, final String origin) {
		logger.debug("validateUpdateIdentityListPhase1 started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		final List<IdentityMgmtRequestDTO> list = dto.identities();
		if (Utilities.isEmpty(list)) {
			throw new InvalidParameterException("Identity list is missing or empty", origin);
		}

		if (Utilities.containsNull(list)) {
			throw new InvalidParameterException("Identity list contains null element", origin);
		}

		for (final IdentityMgmtRequestDTO identity : list) {
			validateIdentity(identity, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateIdentity(final IdentityMgmtRequestDTO identity, final String origin) {
		logger.debug("validateIdentity started...");

		if (Utilities.isEmpty(identity.systemName())) {
			throw new InvalidParameterException("System name is missing or empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkNameDuplications(final List<String> names, final String origin) {
		logger.debug("checkNameDuplications started...");

		final Set<String> uniqueNames = new HashSet<>();
		for (final String name : names) {
			if (uniqueNames.contains(name)) {
				throw new InvalidParameterException("Duplicated system name: " + name, origin);
			}

			uniqueNames.add(name);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateUpdateIdentityListPhase2(final IAuthenticationMethod authenticationMethod, final List<NormalizedIdentityMgmtRequestDTO> identities) {
		logger.debug("validateUpdateIdentityListPhase2 started...");

		for (final NormalizedIdentityMgmtRequestDTO identity : identities) {
			authenticationMethod.validator().validateCredentials(identity.credentials());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateIdentityNames(final List<String> originalNames, final String origin) {
		logger.debug("validateIdentityNames started");

		if (Utilities.isEmpty(originalNames)) {
			throw new InvalidParameterException("Identifiable system name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(originalNames)) {
			throw new InvalidParameterException("Identifiable system name list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateIdentityQueryRequest(final IdentityQueryRequestDTO dto, final String origin) {
		logger.debug("validateIdentityQueryRequest started");

		if (dto != null) {
			// pagination
			pageValidator.validatePageParameter(dto.pagination(), System.SORTABLE_FIELDS_BY, origin);

			ZonedDateTime from = null;
			if (!Utilities.isEmpty(dto.creationFrom())) {
				try {
					from = Utilities.parseUTCStringToZonedDateTime(dto.creationFrom());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Minimum creation time has invalid time format", origin);
				}
			}

			ZonedDateTime to = null;
			if (!Utilities.isEmpty(dto.creationTo())) {
				try {
					to = Utilities.parseUTCStringToZonedDateTime(dto.creationTo());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Maximum creation time has invalid time format", origin);
				}
			}

			if (from != null && to != null && to.isBefore(from)) {
				throw new InvalidParameterException("Empty creation time interval", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateSessionQueryRequest(final IdentitySessionQueryRequestDTO dto, final String origin) {
		logger.debug("validateSessionQueryRequest started");

		if (dto != null) {
			// pagination
			pageValidator.validatePageParameter(dto.pagination(), ActiveSession.ACCEPTABLE_SORT_FIELDS, origin);

			ZonedDateTime from = null;
			if (!Utilities.isEmpty(dto.loginFrom())) {
				try {
					from = Utilities.parseUTCStringToZonedDateTime(dto.loginFrom());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Minimum login time has invalid time format", origin);
				}
			}

			ZonedDateTime to = null;
			if (!Utilities.isEmpty(dto.loginTo())) {
				try {
					to = Utilities.parseUTCStringToZonedDateTime(dto.loginTo());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Maximum login time has invalid time format", origin);
				}
			}

			if (from != null && to != null && to.isBefore(from)) {
				throw new InvalidParameterException("Empty login time interval", origin);
			}
		}
	}
}