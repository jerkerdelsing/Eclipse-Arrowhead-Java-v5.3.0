/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.deviceqosevaluator.security;

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
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

// Asks ConsumerAuthorization whether a consumer may invoke a qualityEvaluation operation; decisions are cached briefly,
// failures are not cached and refuse.
@Service
public class DeviceQoSEvaluatorAuthorizationService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	private final ArrowheadHttpService httpService;
	private final LongSupplier clock;
	private final Map<String, Decision> cache = new ConcurrentHashMap<>();

	private record Decision(boolean allowed, long expiresAt) {
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public DeviceQoSEvaluatorAuthorizationService(final ArrowheadHttpService httpService) {
		this(httpService, System::currentTimeMillis);
	}

	//-------------------------------------------------------------------------------------------------
	DeviceQoSEvaluatorAuthorizationService(final ArrowheadHttpService httpService, final LongSupplier clock) {
		this.httpService = httpService;
		this.clock = clock;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isAuthorized(final String consumer, final String serviceDefinition, final String operation) {
		final String key = consumer + "|" + serviceDefinition + "|" + operation;
		final long now = clock.getAsLong();
		final Decision cached = cache.get(key);
		if (cached != null && cached.expiresAt() > now) {
			return cached.allowed();
		}

		final AuthorizationVerifyRequestDTO payload = new AuthorizationVerifyRequestDTO(
				Constants.SYS_NAME_DEVICE_QOS_EVALUATOR,
				consumer,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF.name(),
				serviceDefinition,
				operation);
		try {
			final Boolean allowed = httpService.consumeService(Constants.SERVICE_DEF_AUTHORIZATION, Constants.SERVICE_OP_VERIFY, Boolean.class, payload);
			final boolean result = Boolean.TRUE.equals(allowed);
			cache.put(key, new Decision(result, now + DeviceQoSEvaluatorConstants.SERVICE_AUTHORIZATION_CACHE_MILLIS));
			return result;
		} catch (final Exception ex) {
			// fail closed and do not cache, so the next request asks again
			logger.error("Authorization check failed for {}: {}", key, ex.getMessage());
			logger.debug(ex);
			return false;
		}
	}
}
