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
package eu.arrowhead.blacklist.api.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.service.DiscoveryService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class DiscoveryMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private DiscoveryService discoveryService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return BlacklistConstants.MQTT_API_DISCOVERY_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("DiscoveryMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_CHECK:
			final String systemName = readPayload(request.getPayload(), String.class);
			responsePayload = check(systemName);
			break;

		case Constants.SERVICE_OP_LOOKUP:
			responsePayload = lookup(request.getRequester());
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, MqttStatus.OK, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	// check
	//-------------------------------------------------------------------------------------------------
	private boolean check(final String systemName) {
		logger.debug("DiscoveryMqttHandler.check started");

		return discoveryService.check(systemName, baseTopic() + Constants.SERVICE_OP_CHECK);
	}

	// lookup
	//-------------------------------------------------------------------------------------------------
	private BlacklistEntryListResponseDTO lookup(final String identifiedRequester) {
		logger.debug("DiscoveryMqttHandler.lookup started");

		return discoveryService.lookup(identifiedRequester, baseTopic() + Constants.SERVICE_OP_LOOKUP);
	}
}