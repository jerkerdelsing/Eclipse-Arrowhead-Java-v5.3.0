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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockListResponseDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.service.OrchestrationLockManagementService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class OrchestrationLockManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationLockManagementService lockMgmtService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return DynamicServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_LOCK_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("OrchestrationLockManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_ORCHESTRATION_CREATE:
			final OrchestrationLockListRequestDTO createReqDTO = readPayload(request.getPayload(), OrchestrationLockListRequestDTO.class);
			responsePayload = create(createReqDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_ORCHESTRATION_QUERY:
			final OrchestrationLockQueryRequestDTO queryReqDTO = readPayload(request.getPayload(), OrchestrationLockQueryRequestDTO.class);
			responsePayload = query(queryReqDTO);
			break;

		case Constants.SERVICE_OP_ORCHESTRATION_REMOVE:
			final String owner = request.getParams().get(DynamicServiceOrchestrationConstants.PARAM_NAME_OWNER);
			final List<String> removeReqDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			remove(owner, removeReqDTO);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);

	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationLockListResponseDTO create(final OrchestrationLockListRequestDTO dto) {
		logger.debug("OrchestrationLockManagementMqttHandler.create started");

		return lockMgmtService.create(dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_CREATE);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationLockListResponseDTO query(final OrchestrationLockQueryRequestDTO dto) {
		logger.debug("OrchestrationLockManagementMqttHandler.query started");

		return lockMgmtService.query(dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_QUERY);
	}

	//-------------------------------------------------------------------------------------------------
	private void remove(final String requesterSystem, final List<String> instanceIds) {
		logger.debug("OrchestrationLockManagementMqttHandler.remove started");
		lockMgmtService.remove(requesterSystem, instanceIds, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_REMOVE);
	}
}