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
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryService;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class ServiceDiscoveryMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDiscoveryService sdService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return ServiceRegistryConstants.MQTT_API_SERVICE_DISCOVERY_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("ServiceDiscoveryMqttHandler.handle started");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_REGISTER:
			final ServiceInstanceCreateRequestDTO registerDTO = readPayload(request.getPayload(), ServiceInstanceCreateRequestDTO.class);
			responsePayload = register(request.getRequester(), registerDTO);
			break;

		case Constants.SERVICE_OP_LOOKUP:
			final ServiceInstanceLookupRequestDTO lookupDTO = readPayload(request.getPayload(), ServiceInstanceLookupRequestDTO.class);
			final Boolean verbose = Boolean.valueOf(request.getParams().get("verbose"));
			final String restrictedAttribute = request.getAttribute(ServiceRegistryConstants.REQUEST_ATTR_RESTRICTED_SERVICE_LOOKUP);
			final Boolean restricted = restrictedAttribute == null || Boolean.valueOf(restrictedAttribute);
			responsePayload = lookup(
					lookupDTO,
					verbose != null
						? verbose
						: Boolean.valueOf(ServiceRegistryConstants.VERBOSE_PARAM_DEFAULT),
					restricted);
			break;

		case Constants.SERVICE_OP_REVOKE:
			final String instanceId = readPayload(request.getPayload(), String.class);
			responseStatus = revoke(request.getRequester(), instanceId);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO register(final String identifiedRequester, final ServiceInstanceCreateRequestDTO dto) {
		logger.debug("ServiceDiscoveryMqttHandler.register started");

		return sdService.registerService(new ServiceInstanceRequestDTO(
				identifiedRequester,
				dto.serviceDefinitionName(),
				dto.version(),
				dto.expiresAt(),
				dto.metadata(),
				dto.interfaces()),
				baseTopic() + Constants.SERVICE_OP_REGISTER);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceListResponseDTO lookup(final ServiceInstanceLookupRequestDTO dto, final boolean verbose, final boolean restricted) {
		logger.debug("ServiceDiscoveryMqttHandler.lookup started");

		return sdService.lookupServices(dto, verbose, restricted, baseTopic() + Constants.SERVICE_OP_LOOKUP);
	}

	//-------------------------------------------------------------------------------------------------
	private MqttStatus revoke(final String identifiedRequester, final String instanceId) {
		logger.debug("ServiceDiscoveryMqttHandler.revoke started");

		final boolean result = sdService.revokeService(identifiedRequester, instanceId, baseTopic() + Constants.SERVICE_OP_REVOKE);

		return result ? MqttStatus.OK : MqttStatus.NO_CONTENT;
	}
}