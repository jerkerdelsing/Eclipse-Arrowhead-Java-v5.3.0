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
package eu.arrowhead.authentication.mqtt.filter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;

public class InternalAuthenticationMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	private static final List<String> tokenlessOperations = List.of(
			Constants.SERVICE_OP_IDENTITY_LOGIN,
			Constants.SERVICE_OP_IDENTITY_LOGOUT,
			Constants.SERVICE_OP_IDENTITY_CHANGE,
			Constants.SERVICE_OP_ECHO);

	@Autowired
	private IdentityDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHENTICATION;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		initializeRequestAttributes(request);

		if (needTokenCheck(request.getOperation())) {
			final String token = processAuthKey(authKey);
			if (Utilities.isEmpty(token)) {
				throw new AuthException("Missing identity token");
			}

			Optional<ActiveSession> sessionOpt = Optional.empty();
			try {
				sessionOpt = dbService.getSessionByToken(token.trim());
			} catch (final InternalServerError ex) {
				throw new AuthException("Session problem", ex);
			}

			if (sessionOpt.isEmpty()) {
				throw new AuthException("Invalid identity token");
			}

			final ActiveSession session = sessionOpt.get();
			final ZonedDateTime now = Utilities.utcNow();
			if (session.getExpirationTime().isBefore(now)) {
				// expired session
				throw new AuthException("Invalid identity token");
			}

			request.setRequester(session.getSystem().getName());
			request.setSysOp(session.getSystem().isSysop());
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void initializeRequestAttributes(final MqttRequestModel request) {
		request.setRequester(null);
		request.setSysOp(false);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean needTokenCheck(final String currentOperation) {
		for (final String operation : tokenlessOperations) {
			if (operation.equals(currentOperation)) {
				return false;
			}
		}

		return true;
	}

	//-------------------------------------------------------------------------------------------------
	private String processAuthKey(final String authKey) {
		if (Utilities.isEmpty(authKey)) {
			throw new AuthException("No authentication info has been provided");
		}

		final String[] split = authKey.split(Constants.AUTHENTICATION_KEY_DELIMITER);
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_PREFIX_IDENTITY_TOKEN)) {
			throw new AuthException("Invalid authentication info");
		}

		return split[1];
	}
}