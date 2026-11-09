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
package eu.arrowhead.translationmanager.api.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.translationmanager.TranslationManagerConstants;
import eu.arrowhead.translationmanager.service.TranslationBridgeService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class TranslationBridgeMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private TranslationBridgeService service;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return TranslationManagerConstants.MQTT_API_BRIDGE_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("TranslationBridgeMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_DISCOVERY:
			final TranslationDiscoveryRequestDTO discoveryDTO = readPayload(request.getPayload(), TranslationDiscoveryRequestDTO.class);
			responsePayload = discovery(request.getRequester(), discoveryDTO);
			break;

		case Constants.SERVICE_OP_NEGOTIATION:
			final TranslationNegotiationRequestDTO negotiationDTO = readPayload(request.getPayload(), TranslationNegotiationRequestDTO.class);
			responseStatus = MqttStatus.CREATED;
			responsePayload = negotiation(request.getRequester(), negotiationDTO);
			break;

		case Constants.SERVICE_OP_ABORT:
			final String instanceId = readPayload(request.getPayload(), String.class);
			responseStatus = abort(request.getRequester(), instanceId);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TranslationDiscoveryResponseDTO discovery(final String requester, final TranslationDiscoveryRequestDTO discoveryDTO) {
		logger.debug("TranslationBridgeMqttHandler.discovery started");

		return service.discoveryOperation(requester, discoveryDTO, baseTopic() + Constants.SERVICE_OP_DISCOVERY);
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationNegotiationResponseDTO negotiation(final String requester, final TranslationNegotiationRequestDTO negotiationDTO) {
		logger.debug("TranslationBridgeMqttHandler.negotiation started");

		return service.negotiationOperation(requester, negotiationDTO, baseTopic() + Constants.SERVICE_OP_NEGOTIATION);
	}

	//-------------------------------------------------------------------------------------------------
	private MqttStatus abort(final String requester, final String instanceId) {
		logger.debug("TranslationBridgeMqttHandler.abort started");

		final boolean result = service.abortOperation(requester, requester, baseTopic() + Constants.SERVICE_OP_ABORT);

		return result ? MqttStatus.OK : MqttStatus.NO_CONTENT;
	}
}