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
import eu.arrowhead.deviceqosevaluator.service.DeviceQualityDataManagementService;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;
import eu.arrowhead.dto.QoSDeviceStatQueryResponseDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class DeviceQualityDataManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQualityDataManagementService mgmtService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return DeviceQoSEvaluatorConstants.MQTT_API_DEVICE_QUALITY_DATA_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("DeviceQualityDataManagementMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_QUERY:
			final QoSDeviceStatQueryRequestDTO requestPayload = readPayload(request.getPayload(), QoSDeviceStatQueryRequestDTO.class);
			responsePayload = query(requestPayload);
			break;

		case Constants.SERVICE_OP_RELOAD:
			responsePayload = reload();
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, MqttStatus.OK, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private QoSDeviceStatQueryResponseDTO query(final QoSDeviceStatQueryRequestDTO requestPayload) {
		logger.debug("DeviceQualityDataManagementMqttHandler.query started");

		return mgmtService.query(requestPayload, baseTopic() + Constants.SERVICE_OP_QUERY);
	}

	//-------------------------------------------------------------------------------------------------
	private String reload() {
		logger.debug("DeviceQualityDataManagementMqttHandler.reload started");

		return mgmtService.reload(baseTopic() + Constants.SERVICE_OP_RELOAD);
	}

}
