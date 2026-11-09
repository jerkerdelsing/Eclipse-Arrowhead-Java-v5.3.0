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
package eu.arrowhead.blacklist.api.mqtt.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.service.DiscoveryService;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;

@Service
public class InternalBlacklistMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private DiscoveryService discoveryService;

	@Autowired
	private Normalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_BLACKLIST;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		logger.debug("InternalBlacklistMqttFilter is active");

		final String origin = request.getBaseTopic() + request.getOperation();
		final String systemName = request.getRequester();

		if (Utilities.isEmpty(systemName)) {
			// no system name in request
			throw new AuthException("Unknown requester system", origin);
		}

		if (!request.isSysOp()
				&& !isSelfCheck(request)
				&& !isLookup(request)
				&& discoveryService.check(systemName, origin)) {
			throw new ForbiddenException(systemName + " system is blacklisted", origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private boolean isLookup(final MqttRequestModel request) {
		return request.getBaseTopic().equals(BlacklistConstants.MQTT_API_DISCOVERY_BASE_TOPIC)
				&& request.getOperation().equals(Constants.SERVICE_OP_LOOKUP);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isSelfCheck(final MqttRequestModel request) {
		return request.getBaseTopic().equals(BlacklistConstants.MQTT_API_DISCOVERY_BASE_TOPIC)
				&& request.getOperation().equals(Constants.SERVICE_OP_CHECK)
				&& normalizer.normalizeSystemName(request.getPayload().toString()).equals(normalizer.normalizeSystemName(request.getRequester()));
	}
}