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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SSLProperties;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class MqttServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private MqttService service;

	@Mock
	private SSLProperties sslProperties;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testClientInputNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.client(null));

		assertEquals("connectId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testClientInputEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.client(""));

		assertEquals("connectId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testClientUnknownId() {
		assertNull(service.client("unknownConnectId"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testClientKnownId() {
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		clientMap.put("knownConnectId", clientMock);

		final MqttClient result = service.client("knownConnectId");
		assertEquals(clientMock, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDisconnectUnknownConnectId() {
		assertDoesNotThrow(() -> service.disconnect("unknown"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDisconnectKnownConnectId() throws MqttException {
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		clientMap.put("knownConnectId", clientMock);

		doNothing().when(clientMock).disconnect();

		assertEquals(1, clientMap.size());
		assertDoesNotThrow(() -> service.disconnect("knownConnectId"));
		assertTrue(clientMap.isEmpty());

		verify(clientMock).disconnect();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect4ConnectIdNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.connect(null, "localhost", 4763, false));

		assertEquals("connectId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect4ConnectIdEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.connect("", "localhost", 4763, false));

		assertEquals("connectId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6AddressNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.connect("connectId", null, 4763, "clientId", "SystemName", "123456"));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6AddressEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.connect("connectId", "", 4763, "clientId", "SystemName", "123456"));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConnect4SSLProblem1() throws MqttException {
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		clientMap.put("connectId", clientMock);

		doNothing().when(clientMock).disconnect();
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn(null);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, true));

		verify(clientMock).disconnect();
		verify(sslProperties).isSslEnabled();
		verify(sslProperties).getKeyStoreType();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.key-store-type is not defined", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConnect6SSLProblem2() throws MqttException {
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		final MqttClient clientMock = Mockito.mock(MqttClient.class);
		clientMap.put("connectId", clientMock);

		doNothing().when(clientMock).disconnect();
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		when(sslProperties.getKeyStore()).thenReturn(null);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", ""));

		verify(clientMock).disconnect();
		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties).getKeyStore();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.key-store is not defined", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLProblem3() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final Resource keystoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(false);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456"));

		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties, times(2)).getKeyStore();
		verify(keystoreMock).exists();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.key-store file is not found", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLProblem4() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final Resource keystoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(sslProperties.getKeyStorePassword()).thenReturn(null);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456"));

		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(sslProperties).getKeyStorePassword();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.key-store-password is not defined", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLProblem5() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final Resource keystoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(sslProperties.getKeyStorePassword()).thenReturn("123456");
		when(sslProperties.getKeyPassword()).thenReturn(null);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456"));

		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(sslProperties).getKeyStorePassword();
		verify(sslProperties).getKeyPassword();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.key-password is not defined", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLProblem6() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final Resource keystoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(sslProperties.getKeyStorePassword()).thenReturn("123456");
		when(sslProperties.getKeyPassword()).thenReturn("123456");
		when(sslProperties.getTrustStore()).thenReturn(null);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456"));

		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(sslProperties).getKeyStorePassword();
		verify(sslProperties).getKeyPassword();
		verify(sslProperties).getTrustStore();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.trust-store is not defined", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLProblem7() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final Resource keystoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(sslProperties.getKeyStorePassword()).thenReturn("123456");
		when(sslProperties.getKeyPassword()).thenReturn("123456");
		final Resource truststoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getTrustStore()).thenReturn(truststoreMock);
		when(truststoreMock.exists()).thenReturn(false);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456"));

		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(sslProperties).getKeyStorePassword();
		verify(sslProperties).getKeyPassword();
		verify(sslProperties, times(2)).getTrustStore();
		verify(truststoreMock).exists();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.trust-store file is not found", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLProblem8() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final Resource keystoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(sslProperties.getKeyStorePassword()).thenReturn("123456");
		when(sslProperties.getKeyPassword()).thenReturn("123456");
		final Resource truststoreMock = Mockito.mock(Resource.class);
		when(sslProperties.getTrustStore()).thenReturn(truststoreMock);
		when(truststoreMock.exists()).thenReturn(true);
		when(sslProperties.getTrustStorePassword()).thenReturn(null);

		final MqttException ex = assertThrows(MqttException.class,
				() -> service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456"));

		verify(sslProperties, times(2)).isSslEnabled();
		verify(sslProperties).getKeyStoreType();
		verify(sslProperties, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(sslProperties).getKeyStorePassword();
		verify(sslProperties).getKeyPassword();
		verify(sslProperties, times(2)).getTrustStore();
		verify(truststoreMock).exists();
		verify(sslProperties).getTrustStorePassword();

		assertEquals(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex.getReasonCode());
		assertEquals("server.ssl.trust-store-password is not defined", ex.getCause().getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6SSLOk() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(true);
		when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
		final ClassPathResource keystoreResource = new ClassPathResource("certs/ConsumerAuthorization.p12");
		when(sslProperties.getKeyStore()).thenReturn(keystoreResource);
		when(sslProperties.getKeyStorePassword()).thenReturn("123456");
		when(sslProperties.getKeyPassword()).thenReturn("123456");
		final ClassPathResource truststoreResource = new ClassPathResource("certs/truststore.p12");
		when(sslProperties.getTrustStore()).thenReturn(truststoreResource);
		when(sslProperties.getTrustStorePassword()).thenReturn("123456");

		@SuppressWarnings("unchecked")
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		assertTrue(clientMap.isEmpty());

		try (MockedConstruction<MqttClient> constructorMock = Mockito.mockConstruction(MqttClient.class,
				(mock, context) -> {
					doNothing().when(mock).connect(any(MqttConnectOptions.class));
				})) {
			service.connect("connectId", "localhost", 4763, "clientId", "SystemName", "123456");

			verify(sslProperties, times(2)).isSslEnabled();
			verify(sslProperties, times(3)).getKeyStoreType();
			verify(sslProperties, times(3)).getKeyStore();
			verify(sslProperties, times(3)).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();
			verify(sslProperties, times(3)).getTrustStore();
			verify(sslProperties, times(2)).getTrustStorePassword();
			final MqttClient clientMock = constructorMock.constructed().get(0);
			verify(clientMock).connect(any(MqttConnectOptions.class));

			assertEquals(1, clientMap.size());
			assertEquals(clientMock, clientMap.get("connectId"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect6TCPOk() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(false);

		@SuppressWarnings("unchecked")
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		assertTrue(clientMap.isEmpty());

		try (MockedConstruction<MqttClient> constructorMock = Mockito.mockConstruction(MqttClient.class,
				(mock, context) -> {
					doNothing().when(mock).connect(any(MqttConnectOptions.class));
				})) {
			service.connect("connectId", "localhost", 4763, null, "SystemName", "123456");

			final MqttClient clientMock = constructorMock.constructed().get(0);
			verify(clientMock).connect(any(MqttConnectOptions.class));

			assertEquals(1, clientMap.size());
			assertEquals(clientMock, clientMap.get("connectId"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConnect4TCPOk() throws MqttException {
		when(sslProperties.isSslEnabled()).thenReturn(false);

		@SuppressWarnings("unchecked")
		final Map<String, MqttClient> clientMap = (Map<String, MqttClient>) ReflectionTestUtils.getField(service, "clientMap");
		assertTrue(clientMap.isEmpty());

		try (MockedConstruction<MqttClient> constructorMock = Mockito.mockConstruction(MqttClient.class,
				(mock, context) -> {
					doNothing().when(mock).connect(any(MqttConnectOptions.class));
				})) {
			service.connect("connectId", "localhost", 4763, false);

			final MqttClient clientMock = constructorMock.constructed().get(0);
			verify(clientMock).connect(any(MqttConnectOptions.class));

			assertEquals(1, clientMap.size());
			assertEquals(clientMock, clientMap.get("connectId"));
		}
	}
}