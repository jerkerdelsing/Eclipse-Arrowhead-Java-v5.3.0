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
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.service.AuthorizationService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationLookupRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class AuthorizationMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationService authorizationService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return AuthorizationConstants.MQTT_API_AUTHORIZATION_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("AuthorizationMqttHandler.handle started...");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_GRANT:
			final AuthorizationGrantRequestDTO grantDTO = readPayload(request.getPayload(), AuthorizationGrantRequestDTO.class);
			final Pair<AuthorizationPolicyResponseDTO, Boolean> grantResult = grant(request.getRequester(), grantDTO);
			responsePayload = grantResult.getFirst();
			responseStatus = grantResult.getSecond() ? MqttStatus.CREATED : MqttStatus.OK;
			break;

		case Constants.SERVICE_OP_REVOKE:
			final String instanceId = readPayload(request.getPayload(), String.class);
			revoke(request.getRequester(), instanceId);
			break;

		case Constants.SERVICE_OP_LOOKUP:
			final AuthorizationLookupRequestDTO lookupDTO = readPayload(request.getPayload(), AuthorizationLookupRequestDTO.class);
			responsePayload = lookup(request.getRequester(), lookupDTO);
			break;

		case Constants.SERVICE_OP_VERIFY:
			final AuthorizationVerifyRequestDTO verifyDTO = readPayload(request.getPayload(), AuthorizationVerifyRequestDTO.class);
			responsePayload = verify(request.getRequester(), verifyDTO);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Pair<AuthorizationPolicyResponseDTO, Boolean> grant(final String requester, final AuthorizationGrantRequestDTO grantDTO) {
		logger.debug("AuthorizationMqttHandler.grant started...");

		return authorizationService.grantOperation(requester, grantDTO, baseTopic() + Constants.SERVICE_OP_GRANT);
	}

	//-------------------------------------------------------------------------------------------------
	private void revoke(final String requester, final String instanceId) {
		logger.debug("AuthorizationMqttHandler.revoke started...");

		authorizationService.revokeOperation(requester, instanceId, baseTopic() + Constants.SERVICE_OP_REVOKE);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationPolicyListResponseDTO lookup(final String requester, final AuthorizationLookupRequestDTO lookupDTO) {
		logger.debug("AuthorizationMqttHandler.lookup started...");

		return authorizationService.lookupOperation(requester, lookupDTO, baseTopic() + Constants.SERVICE_OP_LOOKUP);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean verify(final String requester, final AuthorizationVerifyRequestDTO verifyDTO) {
		logger.debug("AuthorizationMqttHandler.verify started...");

		return authorizationService.verifyOperation(requester, verifyDTO, baseTopic() + Constants.SERVICE_OP_VERIFY);
	}
}