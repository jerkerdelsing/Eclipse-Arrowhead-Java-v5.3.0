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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.service.ManagementService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class ManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private ManagementService managementService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return BlacklistConstants.MQTT_API_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("ManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_BLACKLIST_QUERY:
			final BlacklistQueryRequestDTO queryDto = readPayload(request.getPayload(), BlacklistQueryRequestDTO.class);
			responsePayload = query(queryDto);
			break;

		case Constants.SERVICE_OP_BLACKLIST_CREATE:
			final BlacklistCreateListRequestDTO createDto = readPayload(request.getPayload(), BlacklistCreateListRequestDTO.class);
			responsePayload = create(createDto, request.getRequester());
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_BLACKLIST_REMOVE:
			final List<String> systemNames = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			remove(systemNames, request.isSysOp(), request.getRequester());
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	// query
	//-------------------------------------------------------------------------------------------------
	private BlacklistEntryListResponseDTO query(final BlacklistQueryRequestDTO dto) {
		logger.debug("ManagementMqttHandler.query started");

		return managementService.query(dto, baseTopic() + Constants.SERVICE_OP_BLACKLIST_QUERY);
	}

	// create
	//-------------------------------------------------------------------------------------------------
	private BlacklistEntryListResponseDTO create(final BlacklistCreateListRequestDTO dto, final String identifiedRequester) {
		logger.debug("ManagementMqttHandler.create started");

		return managementService.create(dto, identifiedRequester, baseTopic() + Constants.SERVICE_OP_BLACKLIST_CREATE);
	}

	// remove
	//-------------------------------------------------------------------------------------------------
	private void remove(final List<String> systemNames, final boolean isSysop, final String identifiedRequester) {
		logger.debug("ManagementMqttHandler.remove started");

		managementService.remove(systemNames, isSysop, identifiedRequester, baseTopic() + Constants.SERVICE_OP_BLACKLIST_REMOVE);
	}
}