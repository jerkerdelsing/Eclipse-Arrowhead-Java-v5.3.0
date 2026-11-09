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
package eu.arrowhead.authentication.api.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.service.IdentityService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class IdentityMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private IdentityService identityService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return AuthenticationConstants.MQTT_API_IDENTITY_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("IdentityMqttHandler.handle started...");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_IDENTITY_LOGIN:
			final IdentityRequestDTO loginDTO = readPayload(request.getPayload(), IdentityRequestDTO.class);
			responsePayload = login(loginDTO);
			break;

		case Constants.SERVICE_OP_IDENTITY_LOGOUT:
			final IdentityRequestDTO logoutDTO = readPayload(request.getPayload(), IdentityRequestDTO.class);
			logout(logoutDTO);
			break;

		case Constants.SERVICE_OP_IDENTITY_CHANGE:
			final IdentityChangeRequestDTO changeDTO = readPayload(request.getPayload(), IdentityChangeRequestDTO.class);
			change(changeDTO);
			break;

		case Constants.SERVICE_OP_IDENTITY_VERIFY:
			final String token = readPayload(request.getPayload(), String.class);
			responsePayload = verify(token);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private IdentityLoginResponseDTO login(final IdentityRequestDTO dto) {
		logger.debug("IdentityMqttHandler.login started...");

		return identityService.loginOperation(dto, false, baseTopic() + Constants.SERVICE_OP_IDENTITY_LOGIN).response();
	}

	//-------------------------------------------------------------------------------------------------
	private void logout(final IdentityRequestDTO dto) {
		logger.debug("IdentityMqttHandler.logout started...");

		identityService.logoutOperation(dto, baseTopic() + Constants.SERVICE_OP_IDENTITY_LOGOUT);
	}

	//-------------------------------------------------------------------------------------------------
	private void change(final IdentityChangeRequestDTO dto) {
		logger.debug("IdentityMqttHandler.change started...");

		identityService.changeOperation(dto, baseTopic() + Constants.SERVICE_OP_IDENTITY_CHANGE);
	}

	//-------------------------------------------------------------------------------------------------
	private IdentityVerifyResponseDTO verify(final String token) {
		logger.debug("IdentityMqttHandler.verify started...");

		return identityService.verifyOperation(token, baseTopic() + Constants.SERVICE_OP_IDENTITY_VERIFY);
	}
}