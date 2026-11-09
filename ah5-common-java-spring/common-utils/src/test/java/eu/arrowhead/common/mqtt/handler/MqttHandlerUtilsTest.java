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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.exception.TimeoutException;
import eu.arrowhead.common.mqtt.ArrowheadMqttService;
import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.common.mqtt.MqttStatus;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.MqttRequestTemplate;

@ExtendWith(MockitoExtension.class)
public class MqttHandlerUtilsTest {

	//=================================================================================================
	// members

	@InjectMocks
	private MqttHandlerUtils utils;

	@Mock
	protected ArrowheadMqttService ahMqttService;

	@Spy
	protected ObjectMapper mapper;

	@Spy
	private ServiceOperationNameNormalizer operationNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseMqttMessageNullMsgContainer() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> utils.parseMqttMessage(null));

		assertEquals("MqttMessageContainer is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseMqttMessageNullMsg() {
		final MqttMessageContainer msgContainer = new MqttMessageContainer("test/test-operation", new MqttMessage());
		ReflectionTestUtils.setField(msgContainer, "message", null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> utils.parseMqttMessage(msgContainer));

		assertEquals("Invalid message template: null message", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseMqttMessageInvalidMessageTemplate() throws StreamReadException, DatabindException, IOException {
		final MqttMessageContainer msgContainer = new MqttMessageContainer("test/test-operation", new MqttMessage("not a JSON".getBytes()));
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> utils.parseMqttMessage(msgContainer));

		verify(mapper).readValue(any(byte[].class), eq(MqttRequestTemplate.class));

		assertTrue(ex.getMessage().startsWith("Invalid message template. Reason: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseMqttMessageOk() throws StreamReadException, DatabindException, IOException {
		final MqttRequestTemplate template = new MqttRequestTemplate("trace", "authTest", "response", 1, null, "test payload");
		final String templateJson = Utilities.toJson(template);
		final byte[] templateBytes = templateJson.getBytes();

		final MqttMessageContainer msgContainer = new MqttMessageContainer("test/test-operation", new MqttMessage(templateBytes));
		final Pair<String, MqttRequestModel> pair = utils.parseMqttMessage(msgContainer);

		verify(mapper).readValue(templateBytes, MqttRequestTemplate.class);
		verify(operationNameNormalizer).normalize("test-operation");

		assertEquals("authTest", pair.getLeft());
		assertEquals("test/", pair.getRight().getBaseTopic());
		assertEquals("test-operation", pair.getRight().getOperation());
		assertEquals("trace", pair.getRight().getTraceId());
		assertEquals("response", pair.getRight().getResponseTopic());
		assertEquals(1, pair.getRight().getQosRequirement().value());
		assertEquals("test payload", pair.getRight().getPayload());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadClassNullInput() {
		final MqttRequestTemplate result = utils.readPayload(null, MqttRequestTemplate.class);
		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadClassTemplateInput() {
		final MqttRequestTemplate template = new MqttRequestTemplate("trace", "auth", "response", 2, null, "payload");

		final MqttRequestTemplate result = utils.readPayload(template, MqttRequestTemplate.class);
		assertEquals(template, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadClassMapInput() throws JsonProcessingException {
		final Map<String, Object> templateMap = Map.of("traceId", "trace", "authentication", "auth", "responseTopic", "response", "qosRequirement", 2, "payload", "payload");

		final MqttRequestTemplate result = utils.readPayload(templateMap, MqttRequestTemplate.class);

		verify(mapper).writeValueAsString(anyMap());
		verify(mapper).readValue(anyString(), eq(MqttRequestTemplate.class));

		assertEquals("trace", result.traceId());
		assertEquals("auth", result.authentication());
		assertEquals("response", result.responseTopic());
		assertEquals(2, result.qosRequirement());
		assertEquals("payload", result.payload());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadClassException() throws JsonProcessingException {
		final Map<String, Object> wrongMap = Map.of("a", "b");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> utils.readPayload(wrongMap, MqttRequestTemplate.class));

		verify(mapper).writeValueAsString(anyMap());
		verify(mapper).readValue(anyString(), eq(MqttRequestTemplate.class));

		assertTrue(ex.getMessage().startsWith("Could not parse payload. Reason: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadTypeRefNullInput() {
		final TypeReference<MqttRequestTemplate> typeRef = new TypeReference<MqttRequestTemplate>() {
		};
		final MqttRequestTemplate result = utils.readPayload(null, typeRef);
		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadTypeRefMapInput() throws JsonProcessingException {
		final TypeReference<MqttRequestTemplate> typeRef = new TypeReference<MqttRequestTemplate>() {
		};
		final Map<String, Object> templateMap = Map.of("traceId", "trace", "authentication", "auth", "responseTopic", "response", "qosRequirement", 2, "payload", "payload");

		final MqttRequestTemplate result = utils.readPayload(templateMap, typeRef);

		verify(mapper).writeValueAsString(anyMap());
		verify(mapper).readValue(anyString(), eq(typeRef));

		assertEquals("trace", result.traceId());
		assertEquals("auth", result.authentication());
		assertEquals("response", result.responseTopic());
		assertEquals(2, result.qosRequirement());
		assertEquals("payload", result.payload());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testReadPayloadTypeRefException() throws JsonProcessingException {
		final TypeReference<MqttRequestTemplate> typeRef = new TypeReference<MqttRequestTemplate>() {
		};
		final Map<String, Object> wrongMap = Map.of("a", "b");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> utils.readPayload(wrongMap, typeRef));

		verify(mapper).writeValueAsString(anyMap());
		verify(mapper).readValue(anyString(), eq(typeRef));

		assertTrue(ex.getMessage().startsWith("Could not parse payload. Reason: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSuccessResponseNullResponseTopic() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "auth", null, 0, Map.of(), "payload"));

		assertDoesNotThrow(() -> utils.successResponse(request, MqttStatus.OK, null));

		verify(ahMqttService, never()).response(anyString(), anyString(), anyString(), any(MqttQoS.class), any(MqttStatus.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSuccessResponseOk() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "auth", "response", 2, Map.of(), "payload"));
		request.setRequester("requester");
		final String response = "ACK";

		doNothing().when(ahMqttService).response("requester", "response", "trace", MqttQoS.valueOf(2), MqttStatus.OK, response);

		assertDoesNotThrow(() -> utils.successResponse(request, MqttStatus.OK, response));

		verify(ahMqttService).response("requester", "response", "trace", MqttQoS.valueOf(2), MqttStatus.OK, response);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testErrorResponseNullRequestNull() {
		assertDoesNotThrow(() -> utils.errorResponse(new RuntimeException("test"), null));

		verify(ahMqttService, never()).response(anyString(), anyString(), anyString(), any(MqttQoS.class), any(MqttStatus.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testErrorResponseNullResponseTopic() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "auth", null, 0, Map.of(), "payload"));

		assertDoesNotThrow(() -> utils.errorResponse(new RuntimeException("test"), request));

		verify(ahMqttService, never()).response(anyString(), anyString(), anyString(), any(MqttQoS.class), any(MqttStatus.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testErrorResponseOk() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "auth", "response", 0, Map.of(), "payload"));
		request.setRequester("requester");

		doNothing().when(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), any(MqttStatus.class), any(ErrorMessageDTO.class));

		assertDoesNotThrow(() -> utils.errorResponse(new RuntimeException("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new AuthException("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new ForbiddenException("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new InvalidParameterException("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new DataNotFoundException("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new ExternalServerError("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new TimeoutException("test"), request));
		assertDoesNotThrow(() -> utils.errorResponse(new LockedException("test"), request));

		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.INTERNAL_SERVER_ERROR), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.UNAUTHORIZED), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.FORBIDDEN), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.BAD_REQUEST), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.NOT_FOUND), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.EXTERNAL_SERVER_ERROR), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.TIMEOUT), any(ErrorMessageDTO.class));
		verify(ahMqttService).response(eq("requester"), eq("response"), eq("trace"), eq(MqttQoS.valueOf(0)), eq(MqttStatus.LOCKED), any(ErrorMessageDTO.class));
	}
}