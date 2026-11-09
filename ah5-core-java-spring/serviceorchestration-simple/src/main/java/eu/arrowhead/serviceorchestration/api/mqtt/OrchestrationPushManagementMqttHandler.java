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

import com.fasterxml.jackson.core.type.TypeReference;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.service.OrchestrationPushManagementService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class OrchestrationPushManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationPushManagementService pushService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return SimpleStoreServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_PUSH_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("OrchestrationPushManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
			case Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE:
				final OrchestrationSubscriptionListRequestDTO subscribeReqDTO = readPayload(request.getPayload(), OrchestrationSubscriptionListRequestDTO.class);
				responsePayload = pushSubscribe(request.getRequester(), subscribeReqDTO);
				responseStatus = MqttStatus.CREATED;
				break;

			case Constants.SERVICE_OP_ORCHESTRATION_TRIGGER:
				final OrchestrationPushTriggerDTO triggerReqDTO = readPayload(request.getPayload(), OrchestrationPushTriggerDTO.class);
				responsePayload = pushTrigger(request.getRequester(), triggerReqDTO);
				responseStatus = MqttStatus.CREATED;
				break;

			case Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE:
				final List<String> unsubscribeReqDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() {
				});
				pushUnsubscribe(request.getRequester(), unsubscribeReqDTO);
				break;

			case Constants.SERVICE_OP_ORCHESTRATION_QUERY:
				final OrchestrationSubscriptionQueryRequestDTO queryReqDTO = readPayload(request.getPayload(), OrchestrationSubscriptionQueryRequestDTO.class);
				responsePayload = pushQuery(queryReqDTO);
				break;

			default:
				throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSubscriptionListResponseDTO pushSubscribe(final String requesterSystem, final OrchestrationSubscriptionListRequestDTO dto) {
		logger.debug("OrchestrationPushManagementMqttHandler.pushSubscribe started");

		return pushService.pushSubscribe(requesterSystem, dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_SUBSCRIBE);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationPushJobListResponseDTO pushTrigger(final String requesterSystem, final OrchestrationPushTriggerDTO dto) {
		logger.debug("OrchestrationPushManagementMqttHandler.pushTrigger started");

		return pushService.pushTrigger(requesterSystem, dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_TRIGGER);
	}

	//-------------------------------------------------------------------------------------------------
	private void pushUnsubscribe(final String requesterSystem, final List<String> ids) {
		logger.debug("OrchestrationPushManagementMqttHandler.pushUnsubscribe started");

		pushService.pushUnsubscribe(requesterSystem, ids, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_UNSUBSCRIBE);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSubscriptionListResponseDTO pushQuery(final OrchestrationSubscriptionQueryRequestDTO dto) {
		logger.debug("OrchestrationPushManagementMqttHandler.pushQuery started");

		return pushService.queryPushSubscriptions(dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_QUERY);
	}
}