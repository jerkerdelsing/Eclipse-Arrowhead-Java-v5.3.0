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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.mqtt.handler.MqttTopicHandler;
import eu.arrowhead.common.mqtt.model.MqttMessageContainer;

@ExtendWith(MockitoExtension.class)
public class MqttDispatcherTest {

	//=================================================================================================
	// members

	private final MqttDispatcher dispatcher = new MqttDispatcher();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setUp() {
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		fullTopicSet.clear();
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");
		baseTopicQueueMap.clear();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddTopicInputNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.addTopic(null));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddTopicInputEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.addTopic(""));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddTopicInvalidBaseTopic() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.addTopic("nobasetopic"));

		assertEquals("Invalid base topic. It can't be empty and must end with /", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAddTopicKnownBaseTopic() {
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		fullTopicSet.add("basetopic/other-operation");
		baseTopicQueueMap.put("basetopic/", new LinkedBlockingQueue<>());

		assertDoesNotThrow(() -> dispatcher.addTopic("basetopic/operation"));

		assertTrue(fullTopicSet.contains("basetopic/operation"));
		assertEquals(1, baseTopicQueueMap.size());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAddTopicNoHandlerForTopic() {
		ReflectionTestUtils.setField(dispatcher, "handlers", List.of());
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.addTopic("basetopic/operation"));

		assertEquals("No service handler exists for topic: basetopic/operation", ex.getMessage());
		assertTrue(fullTopicSet.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAddTopicOkHandlerAlive() {
		final MqttTopicHandler handlerMock = Mockito.mock(MqttTopicHandler.class);
		ReflectionTestUtils.setField(dispatcher, "handlers", List.of(handlerMock));

		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		when(handlerMock.baseTopic()).thenReturn("basetopic/");
		doNothing().when(handlerMock).init(any(BlockingQueue.class));
		when(handlerMock.isAlive()).thenReturn(true);

		assertDoesNotThrow(() -> dispatcher.addTopic("basetopic/operation"));

		verify(handlerMock).baseTopic();
		verify(handlerMock).init(any(BlockingQueue.class));
		verify(handlerMock).isAlive();
		verify(handlerMock, never()).start();

		assertTrue(fullTopicSet.contains("basetopic/operation"));
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAddTopicOkHandlerNotAlive() {
		final MqttTopicHandler handlerMock = Mockito.mock(MqttTopicHandler.class);
		ReflectionTestUtils.setField(dispatcher, "handlers", List.of(handlerMock));

		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		when(handlerMock.baseTopic()).thenReturn("basetopic/");
		doNothing().when(handlerMock).init(any(BlockingQueue.class));
		when(handlerMock.isAlive()).thenReturn(false);
		doNothing().when(handlerMock).start();

		assertDoesNotThrow(() -> dispatcher.addTopic("basetopic/operation"));

		verify(handlerMock).baseTopic();
		verify(handlerMock).init(any(BlockingQueue.class));
		verify(handlerMock).isAlive();
		verify(handlerMock).start();

		assertTrue(fullTopicSet.contains("basetopic/operation"));
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeBaseTopicInputNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.revokeBaseTopic(null));

		assertEquals("baseTopic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeBaseTopicInputEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.revokeBaseTopic(""));

		assertEquals("baseTopic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRevokeBaseTopicUnknownBaseTopic() {
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		fullTopicSet.add("basetopic/operation");
		baseTopicQueueMap.put("basetopic/", new LinkedBlockingQueue<>());

		assertTrue(fullTopicSet.contains("basetopic/operation"));
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
		assertDoesNotThrow(() -> dispatcher.revokeBaseTopic("otherbasetopic/"));
		assertTrue(fullTopicSet.contains("basetopic/operation"));
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRevokeBaseTopicNoHandler() {
		ReflectionTestUtils.setField(dispatcher, "handlers", List.of());
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		fullTopicSet.add("basetopic/operation");
		fullTopicSet.add("basetopic/operation-2");
		baseTopicQueueMap.put("basetopic/", new LinkedBlockingQueue<>());

		assertEquals(2, fullTopicSet.size());
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
		assertDoesNotThrow(() -> dispatcher.revokeBaseTopic("basetopic/"));
		assertTrue(fullTopicSet.isEmpty());
		assertTrue(baseTopicQueueMap.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRevokeBaseTopicHandlerNotAlive() {
		final MqttTopicHandler handlerMock = Mockito.mock(MqttTopicHandler.class);
		ReflectionTestUtils.setField(dispatcher, "handlers", List.of(handlerMock));
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		fullTopicSet.add("basetopic/operation");
		fullTopicSet.add("basetopic/operation-2");
		baseTopicQueueMap.put("basetopic/", new LinkedBlockingQueue<>());

		when(handlerMock.baseTopic()).thenReturn("basetopic/");
		when(handlerMock.isAlive()).thenReturn(false);

		assertEquals(2, fullTopicSet.size());
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
		assertDoesNotThrow(() -> dispatcher.revokeBaseTopic("basetopic/"));

		verify(handlerMock).baseTopic();
		verify(handlerMock).isAlive();
		verify(handlerMock, never()).interrupt();

		assertTrue(fullTopicSet.isEmpty());
		assertTrue(baseTopicQueueMap.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRevokeBaseTopicHandlerAlive() {
		final MqttTopicHandler handlerMock = Mockito.mock(MqttTopicHandler.class);
		ReflectionTestUtils.setField(dispatcher, "handlers", List.of(handlerMock));
		final Set<String> fullTopicSet = (Set<String>) ReflectionTestUtils.getField(dispatcher, "fullTopicSet");
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");

		fullTopicSet.add("basetopic/operation");
		fullTopicSet.add("basetopic/operation-2");
		baseTopicQueueMap.put("basetopic/", new LinkedBlockingQueue<>());

		when(handlerMock.baseTopic()).thenReturn("basetopic/");
		when(handlerMock.isAlive()).thenReturn(true);
		doNothing().when(handlerMock).interrupt();

		assertEquals(2, fullTopicSet.size());
		assertTrue(baseTopicQueueMap.containsKey("basetopic/"));
		assertDoesNotThrow(() -> dispatcher.revokeBaseTopic("basetopic/"));

		verify(handlerMock).baseTopic();
		verify(handlerMock).isAlive();
		verify(handlerMock).interrupt();

		assertTrue(fullTopicSet.isEmpty());
		assertTrue(baseTopicQueueMap.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueueMessageTopicNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.queueMessage(null, new MqttMessage()));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueueMessageTopicEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.queueMessage("", new MqttMessage()));

		assertEquals("topic is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueueMessageUnknownBaseTopic() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> dispatcher.queueMessage("basetopic/operation", new MqttMessage()));

		assertEquals("unknown base topic", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testQueueMessageOk() {
		final BlockingQueue queueMock = Mockito.mock(BlockingQueue.class);
		final Map<String, BlockingQueue<MqttMessageContainer>> baseTopicQueueMap = (Map<String, BlockingQueue<MqttMessageContainer>>) ReflectionTestUtils.getField(dispatcher, "baseTopicQueueMap");
		baseTopicQueueMap.put("basetopic/", queueMock);

		when(queueMock.add(any(MqttMessageContainer.class))).thenReturn(true);

		assertDoesNotThrow(() -> dispatcher.queueMessage("basetopic/operation", new MqttMessage()));

		verify(queueMock).add(any(MqttMessageContainer.class));
	}

}