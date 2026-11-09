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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class MqttControllerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private MqttController controller;

	@Mock
	private MqttService mqttService;

	@Mock
	private MqttDispatcher mqttDispatcher;

	@Mock
	private SystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testListenInputNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> controller.listen(null));

		assertEquals("ServiceModel is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testListenInterfaceMismatch() throws MqttException {
		ReflectionTestUtils.setField(controller, "templateName", "generic_mqtt");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register"))
						.build())
				.build();

		assertDoesNotThrow(() -> controller.listen(serviceDiscoverySM));
		assertNull(ReflectionTestUtils.getField(controller, "client"));

		verify(mqttService, never()).connect(anyString(), anyString(), anyInt(), anyBoolean());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testListenConnectException() throws MqttException {
		ReflectionTestUtils.setField(controller, "templateName", "generic_mqtt");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.getMqttClientPassword()).thenReturn("123456");
		doThrow(MqttException.class).when(mqttService).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		doNothing().when(mqttDispatcher).revokeBaseTopic("arrowhead/serviceregistry/service-discovery/");

		assertNull(ReflectionTestUtils.getField(controller, "client"));
		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> controller.listen(serviceDiscoverySM));

		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getMqttClientPassword();
		verify(mqttService).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		verify(mqttDispatcher).revokeBaseTopic("arrowhead/serviceregistry/service-discovery/");

		assertTrue(ex.getMessage().startsWith("MQTT service listener creation failed for 'serviceDiscovery'"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testListenSubscribeException() throws MqttException {
		ReflectionTestUtils.setField(controller, "templateName", "generic_mqtt");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register"))
						.build())
				.build();

		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.getMqttClientPassword()).thenReturn("123456");
		doNothing().when(mqttService).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		when(mqttService.client(anyString())).thenReturn(clientMock);
		when(clientMock.getServerURI()).thenReturn("tcp://localhost:4763");
		doNothing().when(clientMock).setCallback(any(MqttCallback.class));
		doNothing().when(mqttDispatcher).addTopic("arrowhead/serviceregistry/service-discovery/register");
		doThrow(MqttException.class).when(clientMock).subscribe("arrowhead/serviceregistry/service-discovery/register");
		doNothing().when(mqttDispatcher).revokeBaseTopic("arrowhead/serviceregistry/service-discovery/");

		assertNull(ReflectionTestUtils.getField(controller, "client"));
		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> controller.listen(serviceDiscoverySM));

		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getMqttClientPassword();
		verify(mqttService).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		verify(mqttService).client(anyString());
		verify(clientMock).getServerURI();
		verify(clientMock).setCallback(any(MqttCallback.class));
		verify(mqttDispatcher).addTopic("arrowhead/serviceregistry/service-discovery/register");
		verify(clientMock).subscribe("arrowhead/serviceregistry/service-discovery/register");
		verify(mqttDispatcher).revokeBaseTopic("arrowhead/serviceregistry/service-discovery/");

		assertNotNull(ReflectionTestUtils.getField(controller, "client"));
		assertTrue(ex.getMessage().startsWith("MQTT service listener creation failed for 'serviceDiscovery'"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testListenOk() throws MqttException {
		ReflectionTestUtils.setField(controller, "templateName", "generic_mqtt");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register"))
						.build())
				.build();

		final MqttClient clientMock = Mockito.mock(MqttClient.class);

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.getMqttClientPassword()).thenReturn("123456");
		doNothing().when(mqttService).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		when(mqttService.client(anyString())).thenReturn(clientMock);
		when(clientMock.getServerURI()).thenReturn("tcp://localhost:4763");
		doNothing().when(clientMock).setCallback(any(MqttCallback.class));
		doNothing().when(mqttDispatcher).addTopic("arrowhead/serviceregistry/service-discovery/register");
		doNothing().when(clientMock).subscribe("arrowhead/serviceregistry/service-discovery/register");

		assertNull(ReflectionTestUtils.getField(controller, "client"));
		assertDoesNotThrow(() -> controller.listen(serviceDiscoverySM));

		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getMqttClientPassword();
		verify(mqttService).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		verify(mqttService).client(anyString());
		verify(clientMock).getServerURI();
		verify(clientMock).setCallback(any(MqttCallback.class));
		verify(mqttDispatcher).addTopic("arrowhead/serviceregistry/service-discovery/register");
		verify(clientMock).subscribe("arrowhead/serviceregistry/service-discovery/register");
		verify(mqttDispatcher, never()).revokeBaseTopic("arrowhead/serviceregistry/service-discovery/");

		assertNotNull(ReflectionTestUtils.getField(controller, "client"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testListenOkAlreadyExistingClient() throws MqttException {
		ReflectionTestUtils.setField(controller, "templateName", "generic_mqtt");
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		ReflectionTestUtils.setField(controller, "client", clientMock);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register"))
						.build())
				.build();

		doNothing().when(mqttDispatcher).addTopic("arrowhead/serviceregistry/service-discovery/register");
		doNothing().when(clientMock).subscribe("arrowhead/serviceregistry/service-discovery/register");

		assertDoesNotThrow(() -> controller.listen(serviceDiscoverySM));

		verify(sysInfo, never()).getSystemName();
		verify(sysInfo, never()).getMqttClientPassword();
		verify(mqttService, never()).connect(anyString(), eq("localhost"), eq(4763), eq("AH-ServiceRegistry"), eq("ServiceRegistry"), eq("123456"));
		verify(mqttService, never()).client(anyString());
		verify(clientMock, never()).getServerURI();
		verify(clientMock, never()).setCallback(any(MqttCallback.class));
		verify(mqttDispatcher).addTopic("arrowhead/serviceregistry/service-discovery/register");
		verify(clientMock).subscribe("arrowhead/serviceregistry/service-discovery/register");
		verify(mqttDispatcher, never()).revokeBaseTopic("arrowhead/serviceregistry/service-discovery/");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDisconnectClientNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> controller.disconnect());

		assertEquals("client is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testDisconnectUnsubscribeException() throws MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		ReflectionTestUtils.setField(controller, "client", clientMock);

		final Set<String> topicSet = Set.of("arrowhead/serviceregistry/service-discovery/register");
		final String[] topicArray = new String[] { "arrowhead/serviceregistry/service-discovery/register" };

		when(mqttDispatcher.getFullTopicSet()).thenReturn(topicSet);
		doThrow(MqttException.class).when(clientMock).unsubscribe(topicArray);

		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> controller.disconnect());

		verify(mqttDispatcher).getFullTopicSet();
		verify(clientMock).unsubscribe(topicArray);
		verify(mqttService, never()).disconnect(anyString());

		assertTrue(ex.getMessage().startsWith("Disconnecting MQTT Broker failed: "));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testDisconnectOk() throws MqttException {
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		ReflectionTestUtils.setField(controller, "client", clientMock);

		final Set<String> topicSet = Set.of("arrowhead/serviceregistry/service-discovery/register");
		final String[] topicArray = new String[] { "arrowhead/serviceregistry/service-discovery/register" };

		when(mqttDispatcher.getFullTopicSet()).thenReturn(topicSet);
		doNothing().when(clientMock).unsubscribe(topicArray);
		doNothing().when(mqttService).disconnect(anyString());

		assertDoesNotThrow(() -> controller.disconnect());

		verify(mqttDispatcher).getFullTopicSet();
		verify(clientMock).unsubscribe(topicArray);
		verify(mqttService).disconnect(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInit1() {
		assertNull(ReflectionTestUtils.getField(controller, "templateName"));

		when(sysInfo.isSslEnabled()).thenReturn(false);

		ReflectionTestUtils.invokeMethod(controller, "init");

		verify(sysInfo).isSslEnabled();

		assertEquals("generic_mqtt", ReflectionTestUtils.getField(controller, "templateName"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInit2() {
		assertNull(ReflectionTestUtils.getField(controller, "templateName"));

		when(sysInfo.isSslEnabled()).thenReturn(true);

		ReflectionTestUtils.invokeMethod(controller, "init");

		verify(sysInfo).isSslEnabled();

		assertEquals("generic_mqtts", ReflectionTestUtils.getField(controller, "templateName"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMessageArrived() {
		final MqttCallback callback = (MqttCallback) ReflectionTestUtils.invokeMethod(controller, "createMqttCallback", "tcp://localhost:4763");

		doNothing().when(mqttDispatcher).queueMessage(eq("arrowhead/serviceregistry/service-discovery/register"), any(MqttMessage.class));

		assertDoesNotThrow(() -> callback.messageArrived("arrowhead/serviceregistry/service-discovery/register", new MqttMessage()));

		verify(mqttDispatcher).queueMessage(eq("arrowhead/serviceregistry/service-discovery/register"), any(MqttMessage.class));
	}
}