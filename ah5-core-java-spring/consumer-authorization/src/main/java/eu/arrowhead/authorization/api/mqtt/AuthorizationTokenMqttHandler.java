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
package eu.arrowhead.authorization.api.mqtt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.service.AuthorizationTokenService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class AuthorizationTokenMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationTokenService authorizationTokenService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return AuthorizationConstants.MQTT_API_AUTHORIZATION_TOKEN_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("AuthorizationTokenMqttHandler.handle started...");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_GENERATE:
			final AuthorizationTokenGenerationRequestDTO generateDTO = readPayload(request.getPayload(), AuthorizationTokenGenerationRequestDTO.class);
			responsePayload = generate(request.getRequester(), generateDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_VERIFY:
			final String token = readPayload(request.getPayload(), String.class);
			responsePayload = verify(request.getRequester(), token);
			break;

		case Constants.SERVICE_OP_GET_PUBLIC_KEY:
			responsePayload = getPublicKey();
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_TOKEN_REGISTER_ENCRYPTION_KEY:
			final AuthorizationEncryptionKeyRegistrationRequestDTO keyDTO = readPayload(request.getPayload(), AuthorizationEncryptionKeyRegistrationRequestDTO.class);
			responsePayload = registerEncryptionKey(request.getRequester(), keyDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_TOKEN_UNREGISTER_ENCRYPTION_KEY:

			final boolean result = unregisterEncryptionKey(request.getRequester());
			if (!result) {
				responseStatus = MqttStatus.NO_CONTENT;
			}

			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenGenerationResponseDTO generate(final String requester, final AuthorizationTokenGenerationRequestDTO generateDTO) {
		logger.debug("AuthorizationTokenMqttHandler.generate started...");

		return authorizationTokenService.generate(requester, generateDTO, baseTopic() + Constants.SERVICE_OP_GENERATE);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenVerifyResponseDTO verify(final String requester, final String token) {
		logger.debug("AuthorizationTokenMqttHandler.verify started...");

		return authorizationTokenService.verify(requester, token, baseTopic() + Constants.SERVICE_OP_VERIFY);
	}

	//-------------------------------------------------------------------------------------------------
	private String getPublicKey() {
		logger.debug("AuthorizationTokenMqttHandler.getPublicKey started...");

		return authorizationTokenService.getPublicKey(baseTopic() + Constants.SERVICE_OP_GET_PUBLIC_KEY);
	}

	//-------------------------------------------------------------------------------------------------
	private String registerEncryptionKey(final String requester, final AuthorizationEncryptionKeyRegistrationRequestDTO keyDTO) {
		logger.debug("AuthorizationTokenMqttHandler.registerEncryptionKey started...");

		return authorizationTokenService.registerEncryptionKey(requester, keyDTO, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_TOKEN_REGISTER_ENCRYPTION_KEY);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean unregisterEncryptionKey(final String requester) {
		logger.debug("AuthorizationTokenMqttHandler.unregisterEncryptionKey started...");

		return authorizationTokenService.unregisterEncryptionKey(requester, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_TOKEN_UNREGISTER_ENCRYPTION_KEY);
	}
}