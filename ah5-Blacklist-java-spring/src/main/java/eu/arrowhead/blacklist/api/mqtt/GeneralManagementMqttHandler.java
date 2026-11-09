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
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class GeneralManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return BlacklistConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("GeneralManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_GET_LOG:
			final LogRequestDTO getLogDTO = readPayload(request.getPayload(), LogRequestDTO.class);
			responsePayload = getLog(getLogDTO);
			break;

		case Constants.SERVICE_OP_GET_CONFIG:
			final List<String> getConfigDTO = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			responsePayload = getConfig(getConfigDTO);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, MqttStatus.OK, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private LogEntryListResponseDTO getLog(final LogRequestDTO dto) {
		logger.debug("GeneralManagementMqttHandler.getLog started");

		return logService.getLogEntries(dto, baseTopic() + Constants.SERVICE_OP_GET_LOG);
	}

	//-------------------------------------------------------------------------------------------------
	private KeyValuesDTO getConfig(final List<String> dto) {
		logger.debug("GeneralManagementMqttHandler.getConfig started");

		return configService.getConfig(dto, baseTopic() + Constants.SERVICE_OP_GET_CONFIG);
	}
}