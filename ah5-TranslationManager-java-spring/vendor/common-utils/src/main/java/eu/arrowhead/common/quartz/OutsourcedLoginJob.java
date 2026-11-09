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
package eu.arrowhead.common.quartz;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;

@Component
@DisallowConcurrentExecution
public class OutsourcedLoginJob implements Job {

	//=================================================================================================
	// members

	private static final int MULTIPLICATOR = 5;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(Constants.$AUTHENTICATOR_LOGIN_INTERVAL_WD)
	private long interval;

	@Autowired
	private SystemInfo systemInfo;

	@Autowired
	private ArrowheadHttpService httpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("Login job called...");

		loginIfNeeded();
	}

	//-------------------------------------------------------------------------------------------------
	// Logs in when there is no valid token yet or it is due for renewal. Also called directly at start-up, before the
	// system registers itself, because a system may start its Quartz scheduler only later (spring.quartz.auto-startup=false).
	public synchronized void loginIfNeeded() {
		try {
			final ZonedDateTime now = Utilities.utcNow();
			final ZonedDateTime renewalThreshold = (ZonedDateTime) systemInfo.getArrowheadContext().get(Constants.KEY_IDENTITY_RENEWAL_THRESHOLD);
			// check whether we need to login or not
			if (renewalThreshold == null || renewalThreshold.isBefore(now)) {
				doLogin();
			}
		} catch (final ArrowheadException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void doLogin() {
		logger.debug("doLogin started...");

		try {
			final IdentityLoginResponseDTO response = httpService.consumeService(Constants.SERVICE_DEF_IDENTITY, Constants.SERVICE_OP_IDENTITY_LOGIN, IdentityLoginResponseDTO.class, getPayload());
			final ZonedDateTime expiration = Utilities.parseUTCStringToZonedDateTime(response.expirationTime());
			systemInfo.getArrowheadContext().put(Constants.KEY_IDENTITY_TOKEN, response.token());
			final ZonedDateTime renewalThreshold = expiration.minus(MULTIPLICATOR * interval + 1, ChronoUnit.MILLIS);
			systemInfo.getArrowheadContext().put(Constants.KEY_IDENTITY_RENEWAL_THRESHOLD, renewalThreshold);
			logger.info("(Re-)Login is successful");
		} catch (final AuthException ex) {
			// Keep the job scheduled and retry at the next interval: the refusal can be transient (Authentication still
			// starting, or the identity not yet provisioned when this system starts first, e.g. an outsourced ServiceRegistry).
			logger.error("After login, the Authentication server responds with: {} (retrying at the next interval)", ex.getMessage());
			logger.debug(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private IdentityRequestDTO getPayload() {
		logger.debug("getPayload started...");

		return new IdentityRequestDTO(
				systemInfo.getSystemName(),
				systemInfo.getAuthenticatorCredentials());
	}
}