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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class IdentityValidation {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private AuthenticationMethods methods;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public IdentityRequestDTO validateAndNormalizeLoginServicePhase1(final IdentityRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLoginServicePhase1 started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateLoginServicePhase1(dto, origin);
		final String normalized = systemNameNormalizer.normalize(dto.systemName());

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return new IdentityRequestDTO(normalized, dto.credentials());
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityRequestDTO validateAndNormalizeLoginServicePhase2(final IdentityRequestDTO dto, final AuthenticationMethod authenticationMethod, final String origin) {
		logger.debug("validateAndNormalizeLoginServicePhase2 started...");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(authenticationMethod, "authentication method is null");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final IAuthenticationMethod method = methods.method(authenticationMethod);
		if (method == null) {
			throw new InternalServerError("Unsupported authentication method: " + authenticationMethod.name(), origin);
		}

		try {
			final Map<String, String> normalizedCredentials = method.normalizer().normalizeCredentials(dto.credentials());
			method.validator().validateCredentials(normalizedCredentials);

			return new IdentityRequestDTO(dto.systemName(), normalizedCredentials);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityChangeRequestDTO validateAndNormalizeChangeServicePhase2(final IdentityChangeRequestDTO dto, final AuthenticationMethod authenticationMethod, final String origin) {
		logger.debug("validateAndNormalizeChangeServicePhase2 started...");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(authenticationMethod, "authentication method is null");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// at this point only new credentials are not validated or normalized
		final IAuthenticationMethod method = methods.method(authenticationMethod);
		if (method == null) {
			throw new InternalServerError("Unsupported authentication method: " + authenticationMethod.name(), origin);
		}

		try {
			final Map<String, String> normalizedNewCredentials = method.normalizer().normalizeCredentials(dto.newCredentials());
			method.validator().validateCredentials(normalizedNewCredentials);

			return new IdentityChangeRequestDTO(dto.systemName(), dto.credentials(), normalizedNewCredentials);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeIdentityToken(final String token, final String origin) {
		logger.debug("validateAndNormalizeIdentityToken started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(token)) {
			throw new InvalidParameterException("Token is missing or empty", origin);
		}

		return token.trim();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateLoginServicePhase1(final IdentityRequestDTO dto, final String origin) {
		logger.debug("validateLoginServicePhase1 started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systemName())) {
			throw new InvalidParameterException("System name is empty", origin);
		}
	}
}