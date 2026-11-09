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
import eu.arrowhead.authorization.AuthorizationDefaults;
import eu.arrowhead.authorization.service.AuthorizationTokenManagementService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyListResponseDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class AuthorizationTokenManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationTokenManagementService mgmtService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return AuthorizationConstants.MQTT_API_TOKEN_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("AuthorizationTokenManagementMqttHandler.handle started...");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS:
			final AuthorizationTokenGenerationMgmtListRequestDTO generateDTO = readPayload(request.getPayload(), AuthorizationTokenGenerationMgmtListRequestDTO.class);
			final boolean unbound = request.getParams().containsKey(Constants.UNBOUND)
					? Boolean.valueOf(request.getParams().get(Constants.UNBOUND))
					: Boolean.valueOf(AuthorizationDefaults.DEFAULT_UNBOUND_VALUE);
			responsePayload = generateTokens(request.getRequester(), generateDTO, unbound);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_QUERY_TOKENS:
			final AuthorizationTokenQueryRequestDTO queryDTO = readPayload(request.getPayload(), AuthorizationTokenQueryRequestDTO.class);
			responsePayload = queryTokens(queryDTO);
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_REVOKE_TOKENS:
			final List<String> tokenReferences = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			revokeTokens(tokenReferences);
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_ADD_ENCRYPTION_KEYS:
			final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO encryptionKeysDTO = readPayload(request.getPayload(), AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO.class);
			responsePayload = addEncryptionKeys(encryptionKeysDTO);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_AUTHORIZATION_REMOVE_ENCRYPTION_KEYS:
			final List<String> systemNames = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			removeEncryptionKeys(systemNames);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenMgmtListResponseDTO generateTokens(final String requester, final AuthorizationTokenGenerationMgmtListRequestDTO generateDTO, final boolean unbound) {
		logger.debug("AuthorizationTokenManagementMqttHandler.generateTokens started...");

		return mgmtService.generateTokensOperation(requester, generateDTO, unbound, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenMgmtListResponseDTO queryTokens(final AuthorizationTokenQueryRequestDTO queryDTO) {
		logger.debug("AuthorizationTokenManagementMqttHandler.queryTokens started...");

		return mgmtService.queryTokensOperation(queryDTO, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_QUERY_TOKENS);
	}

	//-------------------------------------------------------------------------------------------------
	private void revokeTokens(final List<String> tokenReferences) {
		logger.debug("AuthorizationTokenManagementMqttHandler.revokeTokens started...");

		mgmtService.revokeTokensOperation(tokenReferences, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_REVOKE_TOKENS);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationMgmtEncryptionKeyListResponseDTO addEncryptionKeys(final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO encryptionKeysDTO) {
		logger.debug("AuthorizationTokenManagementMqttHandler.addEncryptionKeys started...");

		return mgmtService.addEncryptionKeysOperation(encryptionKeysDTO, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_ADD_ENCRYPTION_KEYS);
	}

	//-------------------------------------------------------------------------------------------------
	private void removeEncryptionKeys(final List<String> systemNames) {
		logger.debug("AuthorizationTokenManagementMqttHandler.removeEncryptionKeys started...");
		mgmtService.removeEncryptionKeysOperation(systemNames, baseTopic() + Constants.SERVICE_OP_AUTHORIZATION_REMOVE_ENCRYPTION_KEYS);

	}
}