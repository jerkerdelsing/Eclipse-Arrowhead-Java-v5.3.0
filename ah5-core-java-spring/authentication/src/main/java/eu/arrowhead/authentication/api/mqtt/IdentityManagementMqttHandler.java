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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.service.ManagementService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityListMgmtUpdateRequestDTO;
import eu.arrowhead.dto.IdentityQueryRequestDTO;
import eu.arrowhead.dto.IdentitySessionListMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionQueryRequestDTO;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class IdentityManagementMqttHandler extends MqttTopicHandler {

	//=================================================================================================
	// members

	@Autowired
	private ManagementService mgmtService;

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String baseTopic() {
		return AuthenticationConstants.MQTT_API_MANAGEMENT_BASE_TOPIC;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void handle(final MqttRequestModel request) throws ArrowheadException {
		logger.debug("IdentityManagementMqttHandler.handle started...");
		Assert.isTrue(request.getBaseTopic().equals(baseTopic()), "MQTT topic-handler mismatch");

		MqttStatus responseStatus = MqttStatus.OK;
		Object responsePayload = null;

		switch (request.getOperation()) {
		case Constants.SERVICE_OP_IDENTITY_MGMT_CREATE:
			final IdentityListMgmtCreateRequestDTO createDto = readPayload(request.getPayload(), IdentityListMgmtCreateRequestDTO.class);
			responsePayload = create(request.getRequester(), createDto);
			responseStatus = MqttStatus.CREATED;
			break;

		case Constants.SERVICE_OP_IDENTITY_MGMT_UPDATE:
			final IdentityListMgmtUpdateRequestDTO updateDto = readPayload(request.getPayload(), IdentityListMgmtUpdateRequestDTO.class);
			responsePayload = update(request.getRequester(), updateDto);
			break;

		case Constants.SERVICE_OP_IDENTITY_MGMT_REMOVE:
			final List<String> systemsToRemove = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			remove(systemsToRemove);
			break;

		case Constants.SERVICE_OP_IDENTITY_MGMT_QUERY:
			final IdentityQueryRequestDTO queryDto = readPayload(request.getPayload(), IdentityQueryRequestDTO.class);
			responsePayload = query(queryDto);
			break;

		case Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_CLOSE:
			final List<String> systemsToCloseTheirSession = readPayload(request.getPayload(), new TypeReference<List<String>>() {
			});
			closeSessions(systemsToCloseTheirSession);
			break;

		case Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_QUERY:
			final IdentitySessionQueryRequestDTO sessionQueryDto = readPayload(request.getPayload(), IdentitySessionQueryRequestDTO.class);
			responsePayload = querySessions(sessionQueryDto);
			break;

		default:
			throw new InvalidParameterException("Unknown operation: " + request.getOperation());
		}

		successResponse(request, responseStatus, responsePayload);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private IdentityListMgmtResponseDTO create(final String requester, final IdentityListMgmtCreateRequestDTO dto) {
		logger.debug("IdentityManagementMqttHandler.create started...");

		return mgmtService.createIdentitiesOperation(requester, dto, baseTopic() + Constants.SERVICE_OP_IDENTITY_MGMT_CREATE);
	}

	//-------------------------------------------------------------------------------------------------
	private IdentityListMgmtResponseDTO update(final String requester, final IdentityListMgmtUpdateRequestDTO dto) {
		logger.debug("IdentityManagementMqttHandler.update started...");

		return mgmtService.updateIdentitiesOperation(requester, dto, baseTopic() + Constants.SERVICE_OP_IDENTITY_MGMT_UPDATE);
	}

	//-------------------------------------------------------------------------------------------------
	private void remove(final List<String> names) {
		logger.debug("IdentityManagementMqttHandler.remove started...");

		mgmtService.removeIdentitiesOperation(names, baseTopic() + Constants.SERVICE_OP_IDENTITY_MGMT_REMOVE);
	}

	//-------------------------------------------------------------------------------------------------
	private IdentityListMgmtResponseDTO query(final IdentityQueryRequestDTO dto) {
		logger.debug("IdentityManagementMqttHandler.query started...");

		return mgmtService.queryIdentitiesOperation(dto, baseTopic() + Constants.SERVICE_OP_IDENTITY_MGMT_QUERY);
	}

	//-------------------------------------------------------------------------------------------------
	private void closeSessions(final List<String> names) {
		logger.debug("IdentityManagementMqttHandler.closeSessions started...");

		mgmtService.closeSessionsOperation(names, baseTopic() + Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_CLOSE);
	}

	//-------------------------------------------------------------------------------------------------
	private IdentitySessionListMgmtResponseDTO querySessions(final IdentitySessionQueryRequestDTO dto) {
		logger.debug("IdentityManagementMqttHandler.querySessions started...");

		return mgmtService.querySessionsOperation(dto, baseTopic() + Constants.SERVICE_OP_IDENTITY_MGMT_SESSION_QUERY);
	}
}