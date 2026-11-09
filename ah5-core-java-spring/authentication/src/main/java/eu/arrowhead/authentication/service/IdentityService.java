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
package eu.arrowhead.authentication.service;

import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.authentication.method.AuthenticationMethods;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.IdentityLoginData;
import eu.arrowhead.authentication.service.validation.IdentityValidation;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class IdentityService {

	//=================================================================================================
	// members

	@Autowired
	private IdentityValidation validator;

	@Autowired
	private IdentityDbService dbService;

	@Autowired
	private AuthenticationMethods methods;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityLoginData loginOperation(final IdentityRequestDTO dto, final boolean noSession, final String origin) {
		logger.debug("login operation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// Phase 1: authentication method independent steps
		IdentityRequestDTO normalized = validator.validateAndNormalizeLoginServicePhase1(dto, origin);
		Optional<System> systemOpt = Optional.empty();

		try {
			systemOpt = dbService.getSystemByName(normalized.systemName());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}

		if (systemOpt.isEmpty()) {
			// system is not known
			throw new AuthException("Invalid name and/or credentials", origin);
		}

		final System system = systemOpt.get();

		// Phase 2: authentication method dependent steps
		normalized = validator.validateAndNormalizeLoginServicePhase2(normalized, system.getAuthenticationMethod(), origin);

		final IAuthenticationMethod method = methods.method(system.getAuthenticationMethod());
		Assert.notNull(method, "Authentication method implementation not found: " + system.getAuthenticationMethod().name());

		boolean verified = false;
		try {
			verified = method.service().verifyCredentials(system, normalized.credentials());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		}

		// Phase 3: authentication method independent steps
		if (!verified) {
			throw new AuthException("Invalid name and/or credentials", origin);
		}

		if (noSession) {
			// no need to create session, we just want to check credentials
			return new IdentityLoginData(normalized, system, null);
		}

		final String token = UUID.randomUUID().toString();
		try {
			final ActiveSession session = dbService.createOrUpdateSession(system, token);

			return new IdentityLoginData(
					normalized,
					system,
					new IdentityLoginResponseDTO(token, Utilities.convertZonedDateTimeToUTCString(session.getExpirationTime())));
		} catch (final InternalServerError ex) {
			method.service().rollbackCredentialsVerification(system, normalized.credentials(), ex.getMessage());
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void logoutOperation(final IdentityRequestDTO dto, final String origin) {
		logger.debug("logout operation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// check if request is coming from a verified system
		final IdentityLoginData data = loginOperation(dto, true, origin);

		final AuthenticationMethod methodType = data.system().getAuthenticationMethod();
		final IAuthenticationMethod method = methods.method(methodType);

		try {
			dbService.removeSession(data.normalizedRequest().systemName());
			method.service().logout(data.system(), data.normalizedRequest().credentials());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void changeOperation(final IdentityChangeRequestDTO dto, final String origin) {
		logger.debug("change operation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		// check if request is coming from a verified system
		final IdentityLoginData data = loginOperation(new IdentityRequestDTO(dto.systemName(), dto.credentials()), true, origin);

		final AuthenticationMethod methodType = data.system().getAuthenticationMethod();
		final IAuthenticationMethod method = methods.method(methodType);

		// further normalization & validation
		IdentityChangeRequestDTO normalized = new IdentityChangeRequestDTO(data.normalizedRequest().systemName(), data.normalizedRequest().credentials(), dto.newCredentials());

		try {
			normalized = validator.validateAndNormalizeChangeServicePhase2(normalized, methodType, origin);
		} catch (final ArrowheadException ex) {
			method.service().rollbackCredentialsVerification(data.system(), normalized.credentials(), ex.getMessage());
			throw ex;
		}

		// changing credentials
		try {
			method.service().changeCredentials(data.system(), normalized.credentials(), normalized.newCredentials());
			method.service().rollbackCredentialsVerification(data.system(), normalized.newCredentials(), "Credentials changed");
		} catch (final ArrowheadException ex) {
			method.service().rollbackCredentialsVerification(data.system(), normalized.credentials(), ex.getMessage());

			if (ex instanceof InvalidParameterException) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			} else if (ex instanceof InternalServerError) {
				throw new InternalServerError(ex.getMessage(), origin);
			} else if (ex instanceof ExternalServerError) {
				throw new ExternalServerError(ex.getMessage(), origin);
			} else {
				// should never happen
				throw new ArrowheadException(ex.getMessage(), origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityVerifyResponseDTO verifyOperation(final String token, final String origin) {
		logger.debug("verify operation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedToken = validator.validateAndNormalizeIdentityToken(token, origin);

		try {
			final Optional<ActiveSession> sessionOpt = dbService.getSessionByToken(normalizedToken);
			if (sessionOpt.isEmpty()) {
				return new IdentityVerifyResponseDTO(false, null, null, null, null);
			}

			final ActiveSession session = sessionOpt.get();

			return new IdentityVerifyResponseDTO(
					true,
					session.getSystem().getName(),
					session.getSystem().isSysop(),
					Utilities.convertZonedDateTimeToUTCString(session.getLoginTime()),
					Utilities.convertZonedDateTimeToUTCString(session.getExpirationTime()));
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}