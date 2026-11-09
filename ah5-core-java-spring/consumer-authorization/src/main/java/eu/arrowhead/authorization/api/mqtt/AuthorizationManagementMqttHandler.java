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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.service.AuthorizationManagementService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class AuthorizationManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationManagementService mgmtService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return AuthorizationConstants.MQTT_API_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("AuthorizationManagementMqttHandler.handle started...");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_AUTHORIZATION_GRANT_POLICIES:
			final AuthorizationMgmtGrantListRequestDTO grantDTO = readPayload(request.getPayload(), AuthorizationMgmtGrantListRequestDTO.class);
			responsePayload = grantPolicies(request.getRequester(), grantDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_REVOKE_POLICIES:
			final List<String> instanceIds = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			revokePolicies(instanceIds);
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_QUERY_POLICIES:
			final AuthorizationQueryRequestDTO queryDTO = readPayload(request.getPayload(), AuthorizationQueryRequestDTO.class);
			responsePayload = queryPolicies(queryDTO);
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES:
			final AuthorizationVerifyListRequestDTO checkDTO = readPayload(request.getPayload(), AuthorizationVerifyListRequestDTO.class);
			responsePayload = checkPolicies(checkDTO);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationPolicyListResponseDTO grantPolicies(final String requester, final AuthorizationMgmtGrantListRequestDTO grantDTO) {
		logger.debug("AuthorizationManagementMqttHandler.grantPolicies started...");

		return mgmtService.grantPoliciesOperation(requester, grantDTO, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_GRANT_POLICIES);
	}

	//-------------------------------------------------------------------------------------------------
	private void revokePolicies(final List<String> instanceIds) {
		logger.debug("AuthorizationManagementMqttHandler.revokePolicies started...");

		mgmtService.revokePoliciesOperation(instanceIds, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_REVOKE_POLICIES);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationPolicyListResponseDTO queryPolicies(final AuthorizationQueryRequestDTO queryDTO) {
		logger.debug("AuthorizationManagementMqttHandler.queryPolicies started...");

		return mgmtService.queryPoliciesOperation(queryDTO, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_QUERY_POLICIES);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationVerifyListResponseDTO checkPolicies(final AuthorizationVerifyListRequestDTO checkDTO) {
		logger.debug("AuthorizationManagementMqttHandler.checkPolicies started...");

		return mgmtService.checkPoliciesOperation(checkDTO, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES);
	}
}