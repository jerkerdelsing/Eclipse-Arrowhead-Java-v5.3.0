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
package eu.arrowhead.common.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.MqttController;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class ApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TestApplicationInitListener listener;

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private ServiceCollector serviceCollector;

	@Mock
	private ArrowheadHttpService arrowheadHttpService;

	@Mock
	private MqttController mqttController;

	@Mock
	private Helper helper;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		@SuppressWarnings("unchecked")
		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		registeredServices.clear();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventInvalidServerConfiguration() {
		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);

		final Throwable ex = assertThrows(ConfigurationException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(2)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();

		assertEquals("Authentication policy cannot be 'CERTIFICATE' while SSL is disabled", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventKeystoreTypeEmpty() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of("server.standalone.mode", true));
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("");

		assertFalse((boolean) ReflectionTestUtils.getField(listener, "standaloneMode"));

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo).getSslProperties();
		verify(propsMock).getKeyStoreType();

		assertTrue((boolean) ReflectionTestUtils.getField(listener, "standaloneMode"));
		assertEquals("server.ssl.key-store-type is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventKeystoreNull() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of());
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo, times(2)).getSslProperties();
		verify(propsMock).getKeyStoreType();
		verify(propsMock).getKeyStore();

		assertEquals("server.ssl.key-store is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventKeystoreNotExists() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of());
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystoreMock = Mockito.mock(Resource.class);

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(false);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo, times(3)).getSslProperties();
		verify(propsMock).getKeyStoreType();
		verify(propsMock, times(2)).getKeyStore();
		verify(keystoreMock).exists();

		assertEquals("server.ssl.key-store file is not found", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventKeystorePasswordNull() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of());
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystoreMock = Mockito.mock(Resource.class);

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(propsMock.getKeyStorePassword()).thenReturn(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo, times(4)).getSslProperties();
		verify(propsMock).getKeyStoreType();
		verify(propsMock, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(propsMock).getKeyStorePassword();

		assertEquals("server.ssl.key-store-password is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventKeyAliasEmpty() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of());
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystoreMock = Mockito.mock(Resource.class);

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("");

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo, times(5)).getSslProperties();
		verify(propsMock).getKeyStoreType();
		verify(propsMock, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(propsMock).getKeyStorePassword();
		verify(propsMock).getKeyAlias();

		assertEquals("server.ssl.key-alias is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventKeyPasswordNull() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of());
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystoreMock = Mockito.mock(Resource.class);

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystoreMock);
		when(keystoreMock.exists()).thenReturn(true);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("alias");
		when(propsMock.getKeyPassword()).thenReturn(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo, times(6)).getSslProperties();
		verify(propsMock).getKeyStoreType();
		verify(propsMock, times(2)).getKeyStore();
		verify(keystoreMock).exists();
		verify(propsMock).getKeyStorePassword();
		verify(propsMock).getKeyAlias();
		verify(propsMock).getKeyPassword();

		assertEquals("server.ssl.key-password is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventServerCertificateNotFound() {
		ReflectionTestUtils.setField(listener, "arrowheadContext", Map.of());
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ConsumerAuthorization.p12");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("alias");
		when(propsMock.getKeyPassword()).thenReturn("123456");

		final Throwable ex = assertThrows(ServiceConfigurationError.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo, times(10)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(2)).getKeyAlias();
		verify(propsMock).getKeyPassword();

		assertEquals("Cannot find server certificate in the specified key store", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventServerPrivateKeyNotAccessible() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ConsumerAuthorization.p12");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");

		assertTrue(context.isEmpty());

		try (MockedStatic<SecurityUtilities> secUtilMock = Mockito.mockStatic(SecurityUtilities.class)) {
			secUtilMock.when(() -> SecurityUtilities.getCertificateFromKeyStore(any(KeyStore.class), eq("ConsumerAuthorization.TestCloud.Company.arrowhead.eu"))).thenCallRealMethod();
			secUtilMock.when(() -> SecurityUtilities.getPrivateKeyFromKeyStore(any(KeyStore.class), eq("ConsumerAuthorization.TestCloud.Company.arrowhead.eu"), eq("123456"))).thenReturn(null);

			final Throwable ex = assertThrows(ServiceConfigurationError.class,
					() -> listener.onApplicationEvent(null));

			assertEquals(2, context.size());
			assertTrue(context.containsKey("server.certificate"));
			assertTrue(context.containsKey("server.public.key"));

			verify(sysInfo).getSystemName();
			verify(sysInfo).getServerPort();
			verify(sysInfo, times(3)).isSslEnabled();
			verify(sysInfo).getAuthenticationPolicy();
			verify(sysInfo, times(12)).getSslProperties();
			verify(propsMock, times(2)).getKeyStoreType();
			verify(propsMock, times(3)).getKeyStore();
			verify(propsMock, times(2)).getKeyStorePassword();
			verify(propsMock, times(3)).getKeyAlias();
			verify(propsMock, times(2)).getKeyPassword();
			secUtilMock.verify(() -> SecurityUtilities.getCertificateFromKeyStore(any(KeyStore.class), eq("ConsumerAuthorization.TestCloud.Company.arrowhead.eu")));
			secUtilMock.verify(() -> SecurityUtilities.getPrivateKeyFromKeyStore(any(KeyStore.class), eq("ConsumerAuthorization.TestCloud.Company.arrowhead.eu"), eq("123456")));

			assertEquals("Cannot find private key in the specified key store", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventNotArrowheadServerCertificate() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/wrong.p12");

		when(sysInfo.getSystemName()).thenReturn("wrong");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("wrong.rubin.aitia.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");

		final Throwable ex = assertThrows(AuthException.class,
				() -> listener.onApplicationEvent(null));

		assertEquals(3, context.size());
		assertTrue(context.containsKey("server.private.key"));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();

		assertEquals("Server certificate is not compliant with the Arrowhead certificate structure, common name and profile type not found", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventNotSystemCertificate() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/testoperator.p12");

		when(sysInfo.getSystemName()).thenReturn("testoperator");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("testoperator.testcloud.company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");

		final Throwable ex = assertThrows(AuthException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();

		assertEquals("Server certificate is not compliant with the Arrowhead certificate structure, invalid profile type: OPERATOR", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventNotWrongCN() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/wrongcn.p12");

		when(sysInfo.getSystemName()).thenReturn("TestSystem");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("TestSystem.Something.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");

		final Throwable ex = assertThrows(AuthException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();

		assertEquals("Server CN (TestSystem.Something.TestCloud.Company.arrowhead.eu) is not compliant with the Arrowhead certificate structure, since it does not have 5 parts", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventStandAloneOk() {
		final Map<Object, Object> context = new HashMap<>();
		context.put("server.standalone.mode", true);

		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ConsumerAuthorization.p12");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");
		when(sysInfo.isMqttApiEnabled()).thenReturn(false);
		doNothing().when(helper).customInitCheck();

		assertDoesNotThrow(() -> listener.onApplicationEvent(null));

		assertEquals(5, context.size());
		assertEquals("ConsumerAuthorization.TestCloud.Company.arrowhead.eu", context.get("server.common.name"));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();
		verify(sysInfo).isMqttApiEnabled();
		verify(helper).customInitCheck();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventStandAloneNoMqttController() {
		final Map<Object, Object> context = new HashMap<>();
		context.put("server.standalone.mode", true);

		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		ReflectionTestUtils.setField(listener, "mqttController", null);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ConsumerAuthorization.p12");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();
		verify(sysInfo).isMqttApiEnabled();

		assertEquals(5, context.size());
		assertEquals("ConsumerAuthorization.TestCloud.Company.arrowhead.eu", context.get("server.common.name"));
		assertEquals("mqttController is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventServiceRegistryOk() {
		final Map<Object, Object> context = new HashMap<>();

		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ServiceRegistry.p12");

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("ServiceRegistry.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");
		when(sysInfo.isMqttApiEnabled()).thenReturn(false);
		doNothing().when(helper).customInitCheck();

		assertDoesNotThrow(() -> listener.onApplicationEvent(null));

		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();
		verify(sysInfo).isMqttApiEnabled();
		verify(helper).customInitCheck();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventServiceRegistryForbidden() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.OUTSOURCED);
		when(sysInfo.getAuthenticatorLoginDelay()).thenReturn(1L);
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry")).thenThrow(new ForbiddenException("test forbidden"));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(4)).isSslEnabled();
		verify(sysInfo, times(3)).getAuthenticationPolicy();
		verify(sysInfo).getAuthenticatorLoginDelay();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry");

		assertEquals("test forbidden", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventServiceRegistryAuthException() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ConsumerAuthorization.p12");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_https", "ServiceRegistry")).thenThrow(new AuthException("test auth"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> listener.onApplicationEvent(null));

		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(4)).isSslEnabled();
		verify(sysInfo, times(3)).getAuthenticationPolicy();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();
		verify(sysInfo, never()).getAuthenticatorLoginDelay();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_https", "ServiceRegistry");

		assertEquals("test auth", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventRegisterToServiceRegistryConsumerOk() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		final ServiceModel systemDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(8443)
						.basePath("/serviceregistry/system-discovery")
						.operation("register", new HttpOperationModel("/register", "POST"))
						.build())
				.build();

		final SystemModel systemModel = new SystemModel.Builder()
				.address("localhost")
				.version("1.0.0")
				.build();

		when(sysInfo.getSystemName()).thenReturn("TestConsumer");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry")).thenReturn(systemDiscoverySM);
		when(arrowheadHttpService.consumeService("systemDiscovery", "revoke", "ServiceRegistry", Void.class)).thenReturn(null);
		when(sysInfo.getSystemModel()).thenReturn(systemModel);
		when(arrowheadHttpService.consumeService(eq("systemDiscovery"), eq("register"), eq("ServiceRegistry"), eq(SystemResponseDTO.class), any(SystemRegisterRequestDTO.class)))
				.thenReturn(new SystemResponseDTO("TestConsumer", Map.of(), "1.0.0", List.of(new AddressDTO("HOSTNAME", "localhost")), null, "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"));
		when(sysInfo.getServices()).thenReturn(null);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		doNothing().when(helper).customInitCheck();

		assertDoesNotThrow(() -> listener.onApplicationEvent(null));

		verify(sysInfo, times(3)).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(4)).isSslEnabled();
		verify(sysInfo, times(3)).getAuthenticationPolicy();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry");
		verify(arrowheadHttpService).consumeService("systemDiscovery", "revoke", "ServiceRegistry", Void.class);
		verify(sysInfo).getSystemModel();
		verify(arrowheadHttpService).consumeService(eq("systemDiscovery"), eq("register"), eq("ServiceRegistry"), eq(SystemResponseDTO.class), any(SystemRegisterRequestDTO.class));
		verify(sysInfo, times(2)).getServices();
		verify(sysInfo).isMqttApiEnabled();
		verify(helper).customInitCheck();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventRegisterToServiceRegistryProviderOk() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		@SuppressWarnings("unchecked")
		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");

		final ServiceModel systemDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(8443)
						.basePath("/serviceregistry/system-discovery")
						.operation("register", new HttpOperationModel("/register", "POST"))
						.build())
				.build();

		final SystemModel systemModel = new SystemModel.Builder()
				.address("localhost")
				.version("1.0.0")
				.build();

		final List<ServiceModel> services = List.of(
				new ServiceModel.Builder()
						.serviceDefinition("testService")
						.version("1.0.0")
						.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
								.accessAddress("localhost")
								.accessPort(12345)
								.basePath("/base/path")
								.operation("test-operation", new HttpOperationModel("/test", "POST"))
								.build())
						.build());

		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry")).thenReturn(systemDiscoverySM);
		when(arrowheadHttpService.consumeService("systemDiscovery", "revoke", "ServiceRegistry", Void.class)).thenReturn(null);
		when(sysInfo.getSystemModel()).thenReturn(systemModel);
		when(arrowheadHttpService.consumeService(eq("systemDiscovery"), eq("register"), eq("ServiceRegistry"), eq(SystemResponseDTO.class), any(SystemRegisterRequestDTO.class)))
				.thenReturn(new SystemResponseDTO("TestProvider", Map.of(), "1.0.0", List.of(new AddressDTO("HOSTNAME", "localhost")), null, "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"));
		when(sysInfo.getServices()).thenReturn(services);
		when(arrowheadHttpService.consumeService(eq("serviceDiscovery"), eq("register"), eq("ServiceRegistry"), eq(ServiceInstanceResponseDTO.class), any(ServiceInstanceCreateRequestDTO.class)))
				.thenReturn(new ServiceInstanceResponseDTO(
						"TestProvider|testService|1.0.0",
						new SystemResponseDTO("TestProvider", Map.of(), "1.0.0", List.of(new AddressDTO("HOSTNAME", "localhost")), null, "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"),
						new ServiceDefinitionResponseDTO("testService", "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"),
						"1.0.0",
						null,
						Map.of(),
						List.of(new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "NONE", Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345, "basePath", "/base/path",
								"operations", Map.of("test-operation", new HttpOperationModel("/test", "POST"))))),
						"2025-07-24T08:00:00Z",
						"2025-07-24T08:00:00Z"));
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		doNothing().when(mqttController).listen(any(ServiceModel.class));
		doNothing().when(helper).customInitCheck();

		assertTrue(registeredServices.isEmpty());
		assertDoesNotThrow(() -> listener.onApplicationEvent(null));

		verify(sysInfo, times(3)).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(4)).isSslEnabled();
		verify(sysInfo, times(4)).getAuthenticationPolicy();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry");
		verify(arrowheadHttpService).consumeService("systemDiscovery", "revoke", "ServiceRegistry", Void.class);
		verify(sysInfo).getSystemModel();
		verify(arrowheadHttpService).consumeService(eq("systemDiscovery"), eq("register"), eq("ServiceRegistry"), eq(SystemResponseDTO.class), any(SystemRegisterRequestDTO.class));
		verify(sysInfo, times(4)).getServices();
		verify(arrowheadHttpService).consumeService(eq("serviceDiscovery"), eq("register"), eq("ServiceRegistry"), eq(ServiceInstanceResponseDTO.class), any(ServiceInstanceCreateRequestDTO.class));
		verify(sysInfo).isMqttApiEnabled();
		verify(mqttController).listen(services.get(0));
		verify(helper).customInitCheck();

		assertEquals(1, registeredServices.size());
		assertEquals("TestProvider|testService|1.0.0", registeredServices.iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOnApplicationEventRegisterToServiceRegistryProviderOkCertifcatePolicy() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		@SuppressWarnings("unchecked")
		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");

		final SSLProperties propsMock = Mockito.mock(SSLProperties.class);
		final Resource keystore = new ClassPathResource("certs/ConsumerAuthorization.p12");

		final ServiceModel systemDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https")
						.accessAddress("localhost")
						.accessPort(8443)
						.basePath("/serviceregistry/system-discovery")
						.operation("register", new HttpOperationModel("/register", "POST"))
						.build())
				.build();

		final SystemModel systemModel = new SystemModel.Builder()
				.address("localhost")
				.version("1.0.0")
				.build();

		final List<ServiceModel> services = List.of(
				new ServiceModel.Builder()
						.serviceDefinition("testService")
						.version("1.0.0")
						.serviceInterface(new HttpInterfaceModel.Builder("generic_https")
								.accessAddress("localhost")
								.accessPort(12345)
								.basePath("/base/path")
								.operation("test-operation", new HttpOperationModel("/test", "POST"))
								.build())
						.build());

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(sysInfo.getServerPort()).thenReturn(12345);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sysInfo.getSslProperties()).thenReturn(propsMock);
		when(propsMock.getKeyStoreType()).thenReturn("pkcs12");
		when(propsMock.getKeyStore()).thenReturn(keystore);
		when(propsMock.getKeyStorePassword()).thenReturn("123456");
		when(propsMock.getKeyAlias()).thenReturn("ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		when(propsMock.getKeyPassword()).thenReturn("123456");
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_https", "ServiceRegistry")).thenReturn(systemDiscoverySM);
		when(arrowheadHttpService.consumeService("systemDiscovery", "revoke", "ServiceRegistry", Void.class)).thenReturn(null);
		when(sysInfo.getSystemModel()).thenReturn(systemModel);
		when(arrowheadHttpService.consumeService(eq("systemDiscovery"), eq("register"), eq("ServiceRegistry"), eq(SystemResponseDTO.class), any(SystemRegisterRequestDTO.class)))
				.thenReturn(new SystemResponseDTO("ConsumerAuthorization", Map.of(), "1.0.0", List.of(new AddressDTO("HOSTNAME", "localhost")), null, "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"));
		when(sysInfo.getServices()).thenReturn(services);
		when(arrowheadHttpService.consumeService(eq("serviceDiscovery"), eq("register"), eq("ServiceRegistry"), eq(ServiceInstanceResponseDTO.class), any(ServiceInstanceCreateRequestDTO.class)))
				.thenReturn(new ServiceInstanceResponseDTO(
						"ConsumerAuthorization|testService|1.0.0",
						new SystemResponseDTO("ConsumerAuthorization", Map.of(), "1.0.0", List.of(new AddressDTO("HOSTNAME", "localhost")), null, "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"),
						new ServiceDefinitionResponseDTO("testService", "2025-07-24T08:00:00Z", "2025-07-24T08:00:00Z"),
						"1.0.0",
						null,
						Map.of(),
						List.of(new ServiceInstanceInterfaceResponseDTO("generic_https", "http", "CERT_AUTH", Map.of("accessAddresses", List.of("localhost"), "accessPort", 12345,
								"basePath", "/base/path", "operations", Map.of("test-operation", new HttpOperationModel("/test", "POST"))))),
						"2025-07-24T08:00:00Z",
						"2025-07-24T08:00:00Z"));
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		doNothing().when(mqttController).listen(any(ServiceModel.class));
		doNothing().when(helper).customInitCheck();

		assertTrue(registeredServices.isEmpty());
		assertDoesNotThrow(() -> listener.onApplicationEvent(null));

		verify(sysInfo, times(3)).getSystemName();
		verify(sysInfo).getServerPort();
		verify(sysInfo, times(12)).getSslProperties();
		verify(propsMock, times(2)).getKeyStoreType();
		verify(propsMock, times(3)).getKeyStore();
		verify(propsMock, times(2)).getKeyStorePassword();
		verify(propsMock, times(3)).getKeyAlias();
		verify(propsMock, times(2)).getKeyPassword();
		verify(sysInfo, times(4)).getAuthenticationPolicy();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_https", "ServiceRegistry");
		verify(arrowheadHttpService).consumeService("systemDiscovery", "revoke", "ServiceRegistry", Void.class);
		verify(sysInfo).getSystemModel();
		verify(arrowheadHttpService).consumeService(eq("systemDiscovery"), eq("register"), eq("ServiceRegistry"), eq(SystemResponseDTO.class), any(SystemRegisterRequestDTO.class));
		verify(sysInfo, times(4)).getServices();
		verify(arrowheadHttpService).consumeService(eq("serviceDiscovery"), eq("register"), eq("ServiceRegistry"), eq(ServiceInstanceResponseDTO.class), any(ServiceInstanceCreateRequestDTO.class));
		verify(sysInfo).isMqttApiEnabled();
		verify(mqttController).listen(services.get(0));
		verify(helper).customInitCheck();

		assertEquals(1, registeredServices.size());
		assertEquals("ConsumerAuthorization|testService|1.0.0", registeredServices.iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDestroySimpleOk() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);
		ReflectionTestUtils.setField(listener, "standaloneMode", true);

		doNothing().when(helper).customDestroyCheck();
		when(sysInfo.isMqttApiEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);

		assertDoesNotThrow(() -> listener.destroy());

		verify(helper).customDestroyCheck();
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(mqttController, never()).disconnect();
		verify(arrowheadHttpService, never()).consumeService(anyString(), anyString(), eq(Void.TYPE), any(IdentityRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDestroyExceptionsHandled() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		@SuppressWarnings("unchecked")
		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		registeredServices.add("TestProvider|testService|1.0.0");

		final ServiceModel systemDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https")
						.accessAddress("localhost")
						.accessPort(8443)
						.basePath("/serviceregistry/system-discovery")
						.operation("register", new HttpOperationModel("/register", "POST"))
						.build())
				.build();

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry")).thenReturn(systemDiscoverySM);
		when(arrowheadHttpService.consumeService("serviceDiscovery", "revoke", "ServiceRegistry", Void.class, List.of("TestProvider|testService|1.0.0"))).thenThrow(ArrowheadException.class);
		doThrow(ArrowheadException.class).when(helper).customDestroyCheck();
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		doThrow(ExternalServerError.class).when(mqttController).disconnect();
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.OUTSOURCED);
		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getAuthenticatorCredentials()).thenReturn(Map.of("password", "123456"));
		when(arrowheadHttpService.consumeService(eq("identity"), eq("identity-logout"), eq(Void.TYPE), any(IdentityRequestDTO.class))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> listener.destroy());

		verify(sysInfo).isSslEnabled();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry");
		verify(arrowheadHttpService).consumeService("serviceDiscovery", "revoke", "ServiceRegistry", Void.class, List.of("TestProvider|testService|1.0.0"));
		verify(helper).customDestroyCheck();
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(mqttController).disconnect();
		verify(sysInfo, times(2)).getSystemName();
		verify(sysInfo).getAuthenticatorCredentials();
		verify(arrowheadHttpService).consumeService(eq("identity"), eq("identity-logout"), eq(Void.TYPE), any(IdentityRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDestroyComplexOk() {
		final Map<Object, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(listener, "arrowheadContext", context);

		@SuppressWarnings("unchecked")
		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		registeredServices.add("TestProvider|testService|1.0.0");

		final ServiceModel systemDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https")
						.accessAddress("localhost")
						.accessPort(8443)
						.basePath("/serviceregistry/system-discovery")
						.operation("register", new HttpOperationModel("/register", "POST"))
						.build())
				.build();

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry")).thenReturn(systemDiscoverySM);
		when(arrowheadHttpService.consumeService("serviceDiscovery", "revoke", "ServiceRegistry", Void.class, List.of("TestProvider|testService|1.0.0"))).thenReturn(null);
		doNothing().when(helper).customDestroyCheck();
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		doNothing().when(mqttController).disconnect();
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.OUTSOURCED);
		when(sysInfo.getSystemName()).thenReturn("TestProvider");
		when(sysInfo.getAuthenticatorCredentials()).thenReturn(Map.of("password", "123456"));
		when(arrowheadHttpService.consumeService(eq("identity"), eq("identity-logout"), eq(Void.TYPE), any(IdentityRequestDTO.class))).thenReturn(null);

		assertDoesNotThrow(() -> listener.destroy());

		verify(sysInfo).isSslEnabled();
		verify(serviceCollector).getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry");
		verify(arrowheadHttpService).consumeService("serviceDiscovery", "revoke", "ServiceRegistry", Void.class, List.of("TestProvider|testService|1.0.0"));
		verify(helper).customDestroyCheck();
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(mqttController).disconnect();
		verify(sysInfo, times(3)).getSystemName();
		verify(sysInfo).getAuthenticatorCredentials();
		verify(arrowheadHttpService).consumeService(eq("identity"), eq("identity-logout"), eq(Void.TYPE), any(IdentityRequestDTO.class));

		assertTrue(registeredServices.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	// testing the private method directly because it is called with constant values which causes a long running time
	@Test
	public void testCheckServiceRegistryConnectionConnectProblem() {
		when(serviceCollector.getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry")).thenThrow(new ArrowheadException("connection problem"));

		final Throwable ex = assertThrows(ArrowheadException.class,
				() -> ReflectionTestUtils.invokeMethod(listener, "checkServiceRegistryConnection", false, 1, 1));

		verify(serviceCollector, times(2)).getServiceModel("systemDiscovery", "generic_http", "ServiceRegistry");

		assertEquals("connection problem", ex.getMessage());
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@Service
	private static final class Helper {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public void customInitCheck() {
		};

		//-------------------------------------------------------------------------------------------------
		public void customDestroyCheck() {
		};
	}

	//-------------------------------------------------------------------------------------------------
	@Service
	private static final class TestApplicationInitListener extends ApplicationInitListener {

		//=================================================================================================
		// members

		@Autowired
		private Helper helper;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@Override
		protected void customInit(final ContextRefreshedEvent event) throws InterruptedException, ConfigurationException {
			super.customInit(event);
			helper.customInitCheck();
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		protected void customDestroy() {
			helper.customDestroyCheck();
			super.customDestroy();
		}
	}
}