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
import eu.arrowhead.dto.TranslationAbortMgmtResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.TranslationQueryListResponseDTO;
import eu.arrowhead.dto.TranslationQueryRequestDTO;
import eu.arrowhead.translationmanager.TranslationManagerConstants;
import eu.arrowhead.translationmanager.service.TranslationBridgeManagementService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class TranslationBridgeManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private TranslationBridgeManagementService mgmtService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return TranslationManagerConstants.MQTT_API_BRIDGE_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("TranslationBridgeManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_DISCOVERY:
			final TranslationDiscoveryMgmtRequestDTO discoveryDTO = readPayload(request.getPayload(), TranslationDiscoveryMgmtRequestDTO.class);
			responsePayload = discovery(request.getRequester(), discoveryDTO);
			break;

		case Constants.SERVICE_OP_NEGOTIATION:
			final TranslationNegotiationMgmtRequestDTO negotiationDTO = readPayload(request.getPayload(), TranslationNegotiationMgmtRequestDTO.class);
			responseStatus = MqttStatus.CREATED;
			responsePayload = negotiation(negotiationDTO);
			break;

		case Constants.SERVICE_OP_ABORT:
			final List<String> ids = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			responsePayload = abort(ids);
			break;

		case Constants.SERVICE_OP_QUERY:
			final TranslationQueryRequestDTO queryDTO = readPayload(request.getPayload(), TranslationQueryRequestDTO.class);
			responsePayload = query(queryDTO);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TranslationDiscoveryResponseDTO discovery(final String requester, final TranslationDiscoveryMgmtRequestDTO discoveryDTO) {
		logger.debug("TranslationBridgeManagementMqttHandler.discovery started");

		return mgmtService.discoveryOperation(requester, discoveryDTO, baseTopic() + Constants.SERVICE_OP_DISCOVERY);
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationNegotiationResponseDTO negotiation(final TranslationNegotiationMgmtRequestDTO negotiationDTO) {
		logger.debug("TranslationBridgeManagementMqttHandler.negotiation started");

		return mgmtService.negotiationOperation(negotiationDTO, baseTopic() + Constants.SERVICE_OP_NEGOTIATION);
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationAbortMgmtResponseDTO abort(final List<String> ids) {
		logger.debug("TranslationBridgeManagementMqttHandler.abort started");

		return mgmtService.abortOperation(ids, baseTopic() + Constants.SERVICE_OP_ABORT);
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationQueryListResponseDTO query(final TranslationQueryRequestDTO queryDTO) {
		logger.debug("TranslationBridgeManagementMqttHandler.query started");

		return mgmtService.queryOperation(queryDTO, baseTopic() + Constants.SERVICE_OP_QUERY);
	}
}