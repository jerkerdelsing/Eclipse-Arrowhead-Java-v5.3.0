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
package eu.arrowhead.common.mqtt.handler;

import java.io.IOException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.mqtt.ArrowheadMqttService;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.enums.ExceptionType;

@Service
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class MqttHandlerUtils {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	protected ArrowheadMqttService ahMqttService;

	@Autowired
	protected ObjectMapper mapper;

	@Autowired
	private ServiceOperationNameNormalizer operationNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, MqttRequestModel> parseMqttMessage(final MqttMessageContainer msgContainer) {
		logger.debug("parseMqttMessage started...");
		Assert.notNull(msgContainer, "MqttMessageContainer is null");

		if (msgContainer.getMessage() == null) {
			throw new InvalidParameterException("Invalid message template: null message");
		}

		try {
			final MqttRequestTemplate template = mapper.readValue(msgContainer.getMessage().getPayload(), MqttRequestTemplate.class);
			return new ImmutablePair<>(
					template.authentication(),
					new MqttRequestModel(msgContainer.getBaseTopic(), operationNameNormalizer.normalize(msgContainer.getOperation()), template));
		} catch (final IOException ex) {
			throw new InvalidParameterException("Invalid message template. Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T readPayload(final Object payload, final Class<T> dtoClass) {
		logger.debug("readPayload started...");

		if (payload == null) {
			return null;
		}

		if (dtoClass.isInstance(payload)) {
			return dtoClass.cast(payload);
		}

		try {
			return mapper.readValue(mapper.writeValueAsString(payload), dtoClass);
		} catch (final IOException ex) {
			throw new InvalidParameterException("Could not parse payload. Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T readPayload(final Object payload, final TypeReference<T> dtoTypeRef) {
		logger.debug("readPayload started...");

		if (payload == null) {
			return null;
		}

		try {
			return mapper.readValue(mapper.writeValueAsString(payload), dtoTypeRef);
		} catch (final IOException ex) {
			throw new InvalidParameterException("Could not parse payload. Reason: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void successResponse(final MqttRequestModel request, final MqttStatus status, final Object response) {
		logger.debug("successResponse started...");

		if (!Utilities.isEmpty(request.getResponseTopic())) {
			ahMqttService.response(
					request.getRequester(),
					request.getResponseTopic(),
					request.getTraceId(),
					request.getQosRequirement(),
					status,
					response);
		} else {
			logger.debug("No MQTT response topic was defined for success response");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void errorResponse(final Exception ex, final MqttRequestModel request) {
		logger.debug("errorResponse started...");

		if (request == null) {
			logger.error("MQTT error occured, without request model being parsed");
			logger.debug(ex);
			return;
		}

		if (Utilities.isEmpty(request.getResponseTopic())) {
			logger.error("MQTT request error occured, but no response topic has been defined");
			logger.debug(ex);
			return;
		}

		final ExceptionType exType = calculateExceptionType(ex);

		final ErrorMessageDTO dto = new ErrorMessageDTO(
				ex.getMessage(),
				exType.getErrorCode(),
				exType,
				request.getBaseTopic() + request.getOperation());

		ahMqttService.response(
				request.getRequester(),
				request.getResponseTopic(),
				request.getTraceId(),
				request.getQosRequirement(),
				calculateStatusFromExceptionType(exType),
				dto);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private MqttStatus calculateStatusFromExceptionType(final ExceptionType exType) {
		logger.debug("calculateStatusFromExceptionType started...");

		switch (exType) {
		case AUTH:
			return MqttStatus.UNAUTHORIZED;
		case FORBIDDEN:
			return MqttStatus.FORBIDDEN;
		case INVALID_PARAMETER:
			return MqttStatus.BAD_REQUEST;
		case DATA_NOT_FOUND:
			return MqttStatus.NOT_FOUND;
		case EXTERNAL_SERVER_ERROR:
			return MqttStatus.EXTERNAL_SERVER_ERROR;
		case TIMEOUT:
			return MqttStatus.TIMEOUT;
		case LOCKED:
			return MqttStatus.LOCKED;
		default:
			return MqttStatus.INTERNAL_SERVER_ERROR;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private ExceptionType calculateExceptionType(final Exception ex) {
		logger.debug("calculateExceptionType started...");

		if (!(ex instanceof ArrowheadException)) {
			return ExceptionType.INTERNAL_SERVER_ERROR;
		}

		return ((ArrowheadException) ex).getExceptionType();
	}
}
