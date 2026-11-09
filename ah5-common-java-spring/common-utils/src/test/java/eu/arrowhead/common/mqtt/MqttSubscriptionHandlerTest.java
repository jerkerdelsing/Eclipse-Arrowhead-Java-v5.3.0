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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.ExternalServerError;

@ExtendWith(MockitoExtension.class)
public class MqttSubscriptionHandlerTest {

	//=================================================================================================
	// members

	private MqttSubscriptionHandler handler;

	@Mock
	private MqttClient client;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		if (handler == null) {
			when(client.isConnected()).thenReturn(true);
			when(client.getServerURI()).thenReturn("tcp://localhost:4763");
			doNothing().when(client).setCallback(any(MqttCallback.class));
			handler = new MqttSubscriptionHandler("connectId", client);
			verify(client).isConnected();
			verify(client).getServerURI();
			verify(client).setCallback(any(MqttCallback.class));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConstructorNullClient() {
		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> new MqttSubscriptionHandler("otherConnectId", null));

		assertEquals("Cannot initialize MqttSubscriptionHandler, because client is not connected", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConstructorNotConnectedClient() {
		final MqttClient otherClient = Mockito.mock(MqttClient.class);
		when(otherClient.isConnected()).thenReturn(false);
		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> new MqttSubscriptionHandler("otherConnectId", otherClient));

		verify(otherClient).isConnected();

		assertEquals("Cannot initialize MqttSubscriptionHandler, because client is not connected", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAddSubscriptionOk() throws MqttException {
		final Map<String, LinkedBlockingQueue<MqttMessage>> topicQueueMap = (Map<String, LinkedBlockingQueue<MqttMessage>>) ReflectionTestUtils.getField(handler, "topicQueueMap");

		doNothing().when(client).subscribe("baseTopic/operation");

		assertTrue(topicQueueMap.isEmpty());
		assertDoesNotThrow(() -> handler.addSubscription("baseTopic/operation"));
		assertTrue(topicQueueMap.containsKey("baseTopic/operation"));

		verify(client).subscribe("baseTopic/operation");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSubscriptionNoMatch() throws MqttException {
		doNothing().when(client).unsubscribe("baseTopic/operation");

		assertDoesNotThrow(() -> handler.removeSubscription("baseTopic/operation"));

		verify(client).unsubscribe("baseTopic/operation");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRemoveSubscriptionMatch() throws MqttException {
		final Map<String, LinkedBlockingQueue<MqttMessage>> topicQueueMap = (Map<String, LinkedBlockingQueue<MqttMessage>>) ReflectionTestUtils.getField(handler, "topicQueueMap");
		final LinkedBlockingQueue<MqttMessage> queueMock = Mockito.mock(LinkedBlockingQueue.class);
		topicQueueMap.put("baseTopic/operation", queueMock);

		doNothing().when(client).unsubscribe("baseTopic/operation");
		doNothing().when(queueMock).clear();

		assertEquals(queueMock, topicQueueMap.get("baseTopic/operation"));
		assertDoesNotThrow(() -> handler.removeSubscription("baseTopic/operation"));

		verify(client).unsubscribe("baseTopic/operation");
		verify(queueMock).clear();

		assertTrue(topicQueueMap.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testMessageArrived() {
		final Map<String, LinkedBlockingQueue<MqttMessage>> topicQueueMap = (Map<String, LinkedBlockingQueue<MqttMessage>>) ReflectionTestUtils.getField(handler, "topicQueueMap");
		final LinkedBlockingQueue<MqttMessage> queueMock = Mockito.mock(LinkedBlockingQueue.class);
		topicQueueMap.put("baseTopic/operation", queueMock);

		final MqttCallback callback = (MqttCallback) ReflectionTestUtils.invokeMethod(handler, "createMqttCallback", "tcp://localhost:4763");

		when(queueMock.add(any(MqttMessage.class))).thenReturn(true);

		assertDoesNotThrow(() -> callback.messageArrived("baseTopic/operation", new MqttMessage()));

		verify(queueMock).add(any(MqttMessage.class));
	}
}