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
package eu.arrowhead.deviceqosevaluator.api.mqtt;

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
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.service.QualityEvaluationService;
import eu.arrowhead.dto.QoSEvaluationFilterResponseDTO;
import eu.arrowhead.dto.QoSEvaluationRequestDTO;
import eu.arrowhead.dto.QoSEvaluationSortResponseDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class QualityEvaluationMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private QualityEvaluationService qualityEvaluationService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return DeviceQoSEvaluatorConstants.MQTT_API_QUALITY_EVALUATION_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("QualityEvaluationMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		final QoSEvaluationRequestDTO requestPayload = readPayload(request.getPayload(), QoSEvaluationRequestDTO.class);
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_FILTER:
			responsePayload = filter(requestPayload);
			break;

		case Constants.SERVICE_OP_SORT:
			responsePayload = sort(requestPayload);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, MqttStatus.OK, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private QoSEvaluationFilterResponseDTO filter(final QoSEvaluationRequestDTO dto) {
		logger.debug("QualityEvaluationMqttHandler.filter started");

		return qualityEvaluationService.filter(dto, baseTopic() + Constants.SERVICE_OP_FILTER);
	}

	//-------------------------------------------------------------------------------------------------
	private QoSEvaluationSortResponseDTO sort(final QoSEvaluationRequestDTO dto) {
		logger.debug("QualityEvaluationMqttHandler.sort started");

		return qualityEvaluationService.sort(dto, baseTopic() + Constants.SERVICE_OP_SORT);
	}

}
