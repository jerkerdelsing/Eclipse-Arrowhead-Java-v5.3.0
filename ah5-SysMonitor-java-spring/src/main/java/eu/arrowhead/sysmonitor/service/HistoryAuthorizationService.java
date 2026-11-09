/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.sysmonitor.SysMonitorConstants;

// Asks ConsumerAuthorization whether a system may invoke a sysMonitorHistory operation. As in the native implementation, only
// granted decisions are cached (30 s); a refusal or a failed check is asked again on the next request (fail closed).
@Service
public class HistoryAuthorizationService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	private final ArrowheadHttpService httpService;
	private final LongSupplier clock;
	private final Map<String, Long> granted = new ConcurrentHashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public HistoryAuthorizationService(final ArrowheadHttpService httpService) {
		this(httpService, System::currentTimeMillis);
	}

	//-------------------------------------------------------------------------------------------------
	HistoryAuthorizationService(final ArrowheadHttpService httpService, final LongSupplier clock) {
		this.httpService = httpService;
		this.clock = clock;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorized(final String consumer, final String operation) {
		final String key = consumer + "|" + operation;
		final long now = clock.getAsLong();
		if (granted.getOrDefault(key, 0L) > now) {
			return true;
		}

		final AuthorizationVerifyRequestDTO payload = new AuthorizationVerifyRequestDTO(
				SysMonitorConstants.SYSTEM_NAME,
				consumer,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF.name(),
				SysMonitorConstants.SERVICE_DEF_HISTORY,
				operation);
		try {
			final boolean allowed = Boolean.TRUE.equals(httpService.consumeService(Constants.SERVICE_DEF_AUTHORIZATION, Constants.SERVICE_OP_VERIFY, Boolean.class, payload));
			if (allowed) {
				granted.put(key, now + SysMonitorConstants.AUTHORIZATION_CACHE_MILLIS);
			}

			return allowed;
		} catch (final Exception ex) {
			logger.error("Authorization check failed for {}: {}", key, ex.getMessage());
			logger.debug(ex);
			return false;
		}
	}
}
