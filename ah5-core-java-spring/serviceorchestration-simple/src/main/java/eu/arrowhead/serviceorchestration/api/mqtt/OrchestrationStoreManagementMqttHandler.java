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
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.service.OrchestrationStoreManagementService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class OrchestrationStoreManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationStoreManagementService storeManagementService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return SimpleStoreServiceOrchestrationConstants.MQTT_API_ORCHESTRATION_STORE_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("OrchestrationStoreManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
			case Constants.SERVICE_OP_ORCHESTRATION_QUERY:
				final OrchestrationSimpleStoreQueryRequestDTO queryReqDTO = readPayload(request.getPayload(), OrchestrationSimpleStoreQueryRequestDTO.class);
				responsePayload = query(queryReqDTO);
				break;

			case Constants.SERVICE_OP_ORCHESTRATION_CREATE:
				final OrchestrationSimpleStoreListRequestDTO createReqDTO = readPayload(request.getPayload(), OrchestrationSimpleStoreListRequestDTO.class);
				responsePayload = create(createReqDTO, request.getRequester());
				responseStatus = MqttStatus.CREATED;
				break;

			case Constants.SERVICE_OP_ORCHESTRATION_MODIFY_PRIORITIES:
				final PriorityRequestDTO priorityReqDTO = readPayload(request.getPayload(), PriorityRequestDTO.class);
				responsePayload = modifyPriorities(priorityReqDTO, request.getRequester());
				break;

			case Constants.SERVICE_OP_ORCHESTRATION_REMOVE:
				final List<String> removeReq = readPayload(request.getPayload(), new TypeReference<List<String>>() {
				});
				remove(removeReq);
				break;

			default:
				throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreListResponseDTO query(final OrchestrationSimpleStoreQueryRequestDTO dto) {
		logger.debug("OrchestrationStoreManagementMqttHandler.query started");

		return storeManagementService.querySimpleStoreEntries(dto, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_QUERY);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreListResponseDTO create(final OrchestrationSimpleStoreListRequestDTO dto, final String requesterSystemName) {
		logger.debug("OrchestrationStoreManagementMqttHandler.create started");

		return storeManagementService.createSimpleStoreEntries(dto, requesterSystemName, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_CREATE);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSimpleStoreListResponseDTO modifyPriorities(final PriorityRequestDTO dto, final String requesterSystemName) {
		logger.debug("OrchestrationStoreManagementMqttHandler.modifyPriorities started");

		return storeManagementService.modifyPriorities(dto, requesterSystemName, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_MODIFY_PRIORITIES);
	}

	//-------------------------------------------------------------------------------------------------
	private void remove(final List<String> uuids) {
		logger.debug("OrchestrationStoreManagementMqttHandler.remove started");

		storeManagementService.removeSimpleStoreEntries(uuids, baseTopic() + Constants.SERVICE_OP_ORCHESTRATION_REMOVE);
	}

}
