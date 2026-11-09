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
package eu.arrowhead.serviceorchestration.api.mqtt;

import org.apache.commons.lang3.tuple.Pair;
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
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.service.OrchestrationService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class OrchestrationMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationService orchService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return DynamicServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("OrchestrationMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_ORCHESTRATION_PULL:
			final OrchestrationRequestDTO pullReqDTO = readPayload(request.getPayload(), OrchestrationRequestDTO.class);
			responsePayload = pull(request.getRequester(), pullReqDTO);
			break;

		case Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE:
			final OrchestrationSubscriptionRequestDTO subscribeReqDTO = readPayload(request.getPayload(), OrchestrationSubscriptionRequestDTO.class);
			final String triggerStr = request.getParams().getOrDefault(DynamicServiceOrchestrationConstants.PARAM_NAME_TRIGGER, Boolean.FALSE.toString());
			final Pair<Boolean, String> subscribeResult = pushSubscribe(request.getRequester(), subscribeReqDTO, Boolean.valueOf(triggerStr));
			responseStatus = subscribeResult.getLeft() ? MqttStatus.CREATED : MqttStatus.OK;
			responsePayload = subscribeResult.getRight();
			break;

		case Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE:
			final String unsubscribeReqId = readPayload(request.getPayload(), String.class);
			boolean unsubscribeResult = pushUnsubscribe(request.getRequester(), unsubscribeReqId);
			responseStatus = unsubscribeResult ? MqttStatus.OK : MqttStatus.NO_CONTENT;
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO pull(final String requesterSystem, final OrchestrationRequestDTO dto) {
		logger.debug("OrchestrationMqttHandler.pull started");

		return orchService.pull(requesterSystem, dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_PULL);
	}

	//-------------------------------------------------------------------------------------------------
	private Pair<Boolean, String> pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionRequestDTO dto, final boolean trigger) {
		logger.debug("OrchestrationMqttHandler.pushSubscribe started");

		return orchService.pushSubscribe(requesterSystem, dto, trigger, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean pushUnsubscribe(final String requesterSystem, final String id) {
		logger.debug("OrchestrationMqttHandler.pushUnsubscribe started");

		return orchService.pushUnsubscribe(requesterSystem, id, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE);
	}
}