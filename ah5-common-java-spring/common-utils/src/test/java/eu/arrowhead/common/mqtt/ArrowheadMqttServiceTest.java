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
package eu.arrowhead.common.mqtt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class ArrowheadMqttServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ArrowheadMqttService service;

	@Mock
	private MqttService mqttService;

	@Spy
	private ObjectMapper mapper;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeAddressNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.subscribe(null, 1234, false, "/test"));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeAddressEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.subscribe("", 1234, false, "/test"));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeTopicNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.subscribe("localhost", 1234, false, null));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeTopicEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.subscribe("localhost", 1234, false, ""));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeSubscriptionIsCached() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";

		final MqttSubscriptionHandler handlerMock = Mockito.mock(MqttSubscriptionHandler.class);
		ReflectionTestUtils.setField(service, "subscriptionMap", Map.of(connectionId, handlerMock));

		when(handlerMock.addSubscription("/test")).thenReturn(new LinkedBlockingQueue<>());

		assertDoesNotThrow(() -> service.subscribe("localhost", 1234, false, "/test"));

		verify(handlerMock).addSubscription("/test");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeConnectionError() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";

		when(mqttService.client(connectionId)).thenReturn(null);
		doThrow(MqttException.class).when(mqttService).connect(connectionId, "localhost", 1234, false);

		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> service.subscribe("localhost", 1234, false, "/test"));

		verify(mqttService).client(connectionId);
		verify(mqttService).connect(connectionId, "localhost", 1234, false);

		assertTrue(ex.getMessage().startsWith("MQTT subscribe failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeConnectionIsOpen() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";

		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		when(mqttService.client(connectionId)).thenReturn(clientMock);
		when(clientMock.isConnected()).thenReturn(true);
		doNothing().when(clientMock).setCallback(any(MqttCallback.class));
		doNothing().when(clientMock).subscribe("/test");

		assertDoesNotThrow(() -> service.subscribe("localhost", 1234, false, "/test"));

		verify(mqttService).client(connectionId);
		verify(clientMock).isConnected();
		verify(clientMock).setCallback(any(MqttCallback.class));
		verify(clientMock).subscribe("/test");

		@SuppressWarnings("unchecked")
		final Map<String, MqttSubscriptionHandler> map = (Map<String, MqttSubscriptionHandler>) ReflectionTestUtils.getField(service, "subscriptionMap");

		assertTrue(map.containsKey(connectionId));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSubscribeNewConnection() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";

		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		when(mqttService.client(connectionId)).thenReturn(null, clientMock);
		doNothing().when(mqttService).connect(connectionId, "localhost", 1234, false);

		when(clientMock.isConnected()).thenReturn(true);
		doNothing().when(clientMock).setCallback(any(MqttCallback.class));
		doNothing().when(clientMock).subscribe("/test");

		assertDoesNotThrow(() -> service.subscribe("localhost", 1234, false, "/test"));

		verify(mqttService, times(2)).client(connectionId);
		verify(mqttService).connect(connectionId, "localhost", 1234, false);
		verify(clientMock).isConnected();
		verify(clientMock).setCallback(any(MqttCallback.class));
		verify(clientMock).subscribe("/test");

		@SuppressWarnings("unchecked")
		final Map<String, MqttSubscriptionHandler> map = (Map<String, MqttSubscriptionHandler>) ReflectionTestUtils.getField(service, "subscriptionMap");

		assertTrue(map.containsKey(connectionId));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeAddressNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.unsubscribe(null, 1234, false, "/test"));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeAddressEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.unsubscribe("", 1234, false, "/test"));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeTopicNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.unsubscribe("localhost", 1234, false, null));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeTopicEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.unsubscribe("localhost", 1234, false, ""));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeNoSubscription() throws MqttException {
		assertDoesNotThrow(() -> service.unsubscribe("localhost", 1234, false, "/test"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeConnectionError() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";
		@SuppressWarnings("unchecked")
		final Map<String, MqttSubscriptionHandler> map = (Map<String, MqttSubscriptionHandler>) ReflectionTestUtils.getField(service, "subscriptionMap");
		final MqttSubscriptionHandler handlerMock = Mockito.mock(MqttSubscriptionHandler.class);
		map.put(connectionId, handlerMock);

		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		doNothing().when(handlerMock).removeSubscription("/test");
		when(handlerMock.getSubscribedTopics()).thenReturn(Set.of());
		when(mqttService.client(connectionId)).thenReturn(clientMock);
		doThrow(MqttException.class).when(clientMock).disconnect();

		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> service.unsubscribe("localhost", 1234, false, "/test"));

		verify(handlerMock).removeSubscription("/test");
		verify(handlerMock).getSubscribedTopics();
		verify(mqttService).client(connectionId);
		verify(clientMock).disconnect();

		assertTrue(ex.getMessage().startsWith("MQTT unsubscribe failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeLastTopic() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";
		@SuppressWarnings("unchecked")
		final Map<String, MqttSubscriptionHandler> map = (Map<String, MqttSubscriptionHandler>) ReflectionTestUtils.getField(service, "subscriptionMap");
		final MqttSubscriptionHandler handlerMock = Mockito.mock(MqttSubscriptionHandler.class);
		map.put(connectionId, handlerMock);

		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		doNothing().when(handlerMock).removeSubscription("/test");
		when(handlerMock.getSubscribedTopics()).thenReturn(Set.of());
		when(mqttService.client(connectionId)).thenReturn(clientMock);
		doNothing().when(clientMock).disconnect();

		assertDoesNotThrow(() -> service.unsubscribe("localhost", 1234, false, "/test"));

		verify(handlerMock).removeSubscription("/test");
		verify(handlerMock).getSubscribedTopics();
		verify(mqttService).client(connectionId);
		verify(clientMock).disconnect();

		assertTrue(map.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnsubscribeNotLastTopic() throws MqttException {
		final String connectionId = "bG9jYWxob3N0MTIzNGZhbHNl";
		@SuppressWarnings("unchecked")
		final Map<String, MqttSubscriptionHandler> map = (Map<String, MqttSubscriptionHandler>) ReflectionTestUtils.getField(service, "subscriptionMap");
		final MqttSubscriptionHandler handlerMock = Mockito.mock(MqttSubscriptionHandler.class);
		map.put(connectionId, handlerMock);

		doNothing().when(handlerMock).removeSubscription("/test");
		when(handlerMock.getSubscribedTopics()).thenReturn(Set.of("/other"));

		assertDoesNotThrow(() -> service.unsubscribe("localhost", 1234, false, "/test"));

		verify(handlerMock).removeSubscription("/test");
		verify(handlerMock).getSubscribedTopics();

		assertTrue(map.containsKey(connectionId));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishBaseTopicNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.publish(null, "test-operation", "sender", MqttQoS.AT_LEAST_ONCE, "payload"));

		assertEquals("baseTopic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishBaseTopicEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.publish("", "test-operation", "sender", MqttQoS.AT_LEAST_ONCE, "payload"));

		assertEquals("baseTopic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishOperationNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.publish("test/", null, "sender", MqttQoS.AT_LEAST_ONCE, "payload"));

		assertEquals("operation is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishOperationEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.publish("test/", "", "sender", MqttQoS.AT_LEAST_ONCE, "payload"));

		assertEquals("operation is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishClientNull() {
		when(mqttService.client(anyString())).thenReturn(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.publish("test/", "test-operation", "sender", MqttQoS.AT_LEAST_ONCE, "payload"));

		verify(mqttService).client(anyString());

		assertEquals("Main broker is not initialized", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishInternalServerError() throws JsonProcessingException, MqttPersistenceException, MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(mqttService.client(anyString())).thenReturn(clientMock);
		when(mapper.writeValueAsBytes(any(Object.class))).thenThrow(JsonProcessingException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> service.publish("test/", "test-operation", "sender", null, "payload"));

		verify(mqttService).client(anyString());
		verify(mapper).writeValueAsBytes(any(Object.class));
		verify(clientMock, never()).publish(anyString(), any(MqttMessage.class));

		assertTrue(ex.getMessage().startsWith("MQTT service publish message creation failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishExternalServerError() throws JsonProcessingException, MqttPersistenceException, MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(mqttService.client(anyString())).thenReturn(clientMock);
		doThrow(MqttException.class).when(clientMock).publish(eq("test/test-operation"), any(MqttMessage.class));

		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> service.publish("test/", "test-operation", "sender", null, "payload"));

		verify(mqttService).client(anyString());
		verify(mapper).writeValueAsBytes(any(Object.class));
		verify(clientMock).publish(eq("test/test-operation"), any(MqttMessage.class));

		assertTrue(ex.getMessage().startsWith("MQTT service publish failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPublishOk() throws JsonProcessingException, MqttPersistenceException, MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(mqttService.client(anyString())).thenReturn(clientMock);
		doNothing().when(clientMock).publish(eq("test/test-operation"), any(MqttMessage.class));

		assertDoesNotThrow(() -> service.publish("test/", "test-operation", "sender", MqttQoS.AT_LEAST_ONCE, "payload"));

		verify(mqttService).client(anyString());
		verify(mapper).writeValueAsBytes(any(Object.class));
		verify(clientMock).publish(eq("test/test-operation"), any(MqttMessage.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testResponseTopicNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.response("receiver", null, "trace", MqttQoS.EXACTLY_ONCE, MqttStatus.OK, "payload"));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testResponseTopicEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.response("receiver", "", "trace", MqttQoS.EXACTLY_ONCE, MqttStatus.OK, "payload"));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testResponseClientNull() {
		when(mqttService.client(anyString())).thenReturn(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.response("receiver", "topic", "trace", MqttQoS.EXACTLY_ONCE, MqttStatus.OK, "payload"));

		verify(mqttService).client(anyString());

		assertEquals("Main broker is not initialized", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testResponseInternalServerError() throws JsonProcessingException, MqttPersistenceException, MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(mqttService.client(anyString())).thenReturn(clientMock);
		when(mapper.writeValueAsBytes(any(Object.class))).thenThrow(JsonProcessingException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> service.response("receiver", "topic", "trace", MqttQoS.EXACTLY_ONCE, MqttStatus.OK, null));

		verify(mqttService).client(anyString());
		verify(mapper).writeValueAsBytes(any(Object.class));
		verify(clientMock, never()).publish(anyString(), any(MqttMessage.class));

		assertTrue(ex.getMessage().startsWith("MQTT service response message creation failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testResponseExternalServerError() throws JsonProcessingException, MqttPersistenceException, MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(mqttService.client(anyString())).thenReturn(clientMock);
		doThrow(MqttException.class).when(clientMock).publish(eq("topic"), any(MqttMessage.class));

		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> service.response("receiver", "topic", "trace", null, MqttStatus.OK, "payload"));

		verify(mqttService).client(anyString());
		verify(mapper).writeValueAsBytes(any(Object.class));
		verify(clientMock).publish(eq("topic"), any(MqttMessage.class));

		assertTrue(ex.getMessage().startsWith("MQTT service response failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testResponseOk() throws JsonProcessingException, MqttPersistenceException, MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(mqttService.client(anyString())).thenReturn(clientMock);
		doNothing().when(clientMock).publish(eq("topic"), any(MqttMessage.class));

		assertDoesNotThrow(() -> service.response("receiver", "topic", "trace", MqttQoS.AT_MOST_ONCE, MqttStatus.OK, "payload"));

		verify(mqttService).client(anyString());
		verify(mapper).writeValueAsBytes(any(Object.class));
		verify(clientMock).publish(eq("topic"), any(MqttMessage.class));
	}
}