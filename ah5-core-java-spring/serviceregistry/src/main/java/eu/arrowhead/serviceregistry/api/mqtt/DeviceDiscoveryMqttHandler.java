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
package eu.arrowhead.serviceregistry.api.mqtt;

import java.util.Map.Entry;

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
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.DeviceDiscoveryService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class DeviceDiscoveryMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private DeviceDiscoveryService ddService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return ServiceRegistryConstants.MQTT_API_DEVICE_DISCOVERY_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("DeviceDiscoveryMqttTopicHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_REGISTER:
			final DeviceRequestDTO registerDTO = readPayload(request.getPayload(), DeviceRequestDTO.class);
			final Pair<DeviceResponseDTO, MqttStatus> registerResult = register(registerDTO);
			responseStatus = registerResult.getRight();
			responsePayload = registerResult.getLeft();
			break;

		case Constants.SERVICE_OP_LOOKUP:
			final DeviceLookupRequestDTO lookupDTO = readPayload(request.getPayload(), DeviceLookupRequestDTO.class);
			responsePayload = lookup(lookupDTO);
			break;

		case Constants.SERVICE_OP_REVOKE:
			final String name = readPayload(request.getPayload(), String.class);
			responseStatus = revoke(name);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Pair<DeviceResponseDTO, MqttStatus> register(final DeviceRequestDTO dto) {
		logger.debug("DeviceDiscoveryMqttHandler.register started");

		final Entry<DeviceResponseDTO, Boolean> result = ddService.registerDevice(dto, baseTopic() + Constants.SERVICE_OP_REGISTER);

		return Pair.of(result.getKey(), result.getValue() ? MqttStatus.CREATED : MqttStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	private DeviceListResponseDTO lookup(final DeviceLookupRequestDTO dto) {
		logger.debug("DeviceDiscoveryMqttHandler.lookup started");

		return ddService.lookupDevice(dto, baseTopic() + Constants.SERVICE_OP_LOOKUP);
	}

	//-------------------------------------------------------------------------------------------------
	private MqttStatus revoke(final String deviceName) {
		logger.debug("DeviceDiscoveryMqttHandler.revoke started");

		final boolean result = ddService.revokeDevice(deviceName, baseTopic() + Constants.SERVICE_OP_REVOKE);

		return result ? MqttStatus.OK : MqttStatus.NO_CONTENT;
	}
}