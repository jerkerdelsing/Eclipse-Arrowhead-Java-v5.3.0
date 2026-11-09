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
package eu.arrowhead.common;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SystemInfo.PublicConfigurationKeysAndDefaults;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class SystemInfoTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TestSystemInfo sysInfo;

	@InjectMocks
	private TestSystemInfo2 sysInfo2;

	@Mock
	private SSLProperties sslProperties;

	@Mock
	protected SystemNameNormalizer systemNameNormalizer;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private PublicConfigurationKeysAndDefaults config;

	@Mock
	private Helper helper;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		final Map<String, Object> context = new HashMap<>();
		ReflectionTestUtils.setField(sysInfo, "arrowheadContext", context);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetIdentityTokenNotOutsourced() {
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.DECLARED);

		assertNull(sysInfo.getIdentityToken());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetIdentityTokenOutsourcedButNotLogged() {
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.OUTSOURCED);

		assertNull(sysInfo.getIdentityToken());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testGetIdentityTokenOutsourcedAndLogged() {
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.OUTSOURCED);
		final Map<String, Object> context = (Map<String, Object>) ReflectionTestUtils.getField(sysInfo, "arrowheadContext");
		context.put("identity-token", "testToken");

		assertEquals("testToken", sysInfo.getIdentityToken());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetManagementWhitelistEmpty() {
		assertTrue(sysInfo.getManagementWhitelist().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetManagementWhitelistAlreadyNormalized() {
		ReflectionTestUtils.setField(sysInfo, "managementWhitelist", List.of("testSystem", "TestSystem2"));
		final List<String> nList = List.of("TestSystem", "TestSystem2");
		ReflectionTestUtils.setField(sysInfo, "normalizedManagementWhitelist", nList);

		assertEquals(nList, sysInfo.getManagementWhitelist());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetManagementWhitelistOnDemandNormalization() {
		ReflectionTestUtils.setField(sysInfo, "managementWhitelist", List.of("testSystem", "", "TestSystem2"));
		final List<String> nList = List.of("TestSystem", "TestSystem2");

		when(systemNameNormalizer.normalize(anyString())).thenReturn("TestSystem", "TestSystem2");

		assertEquals(nList, sysInfo.getManagementWhitelist());

		verify(systemNameNormalizer).normalize("testSystem");
		verify(systemNameNormalizer).normalize("TestSystem2");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBlacklistCheckExcludeListEmpty() {
		assertTrue(sysInfo.getBlacklistCheckExcludeList().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBlacklistCheckExcludeListAlreadyNormalized() {
		ReflectionTestUtils.setField(sysInfo, "blacklistCheckExcludeList", List.of("testSystem", "TestSystem2"));
		final List<String> nList = List.of("TestSystem", "TestSystem2");
		ReflectionTestUtils.setField(sysInfo, "normalizedBlacklistCheckExcludeList", nList);

		assertEquals(nList, sysInfo.getBlacklistCheckExcludeList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetBlacklistCheckExcludeListOnDemandNormalization() {
		ReflectionTestUtils.setField(sysInfo, "blacklistCheckExcludeList", List.of("testSystem", "", "TestSystem2"));
		final List<String> nList = List.of("TestSystem", "TestSystem2");

		when(systemNameNormalizer.normalize(anyString())).thenReturn("TestSystem", "TestSystem2");

		assertEquals(nList, sysInfo.getBlacklistCheckExcludeList());

		verify(systemNameNormalizer).normalize("testSystem");
		verify(systemNameNormalizer).normalize("TestSystem2");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsSslEnabledNoProperties() {
		ReflectionTestUtils.setField(sysInfo, "sslProperties", null);

		assertFalse(sysInfo.isSslEnabled());

		ReflectionTestUtils.setField(sysInfo, "sslProperties", sslProperties);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsSslEnabledWithProperties() {
		when(sslProperties.isSslEnabled()).thenReturn(false, true);

		assertAll("isSslEnabled",
				() -> assertFalse(sysInfo.isSslEnabled()),
				() -> assertTrue(sysInfo.isSslEnabled()));

		verify(sslProperties, times(2)).isSslEnabled();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAddress() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");

		when(addressNormalizer.normalize("localhost")).thenReturn("localhost");

		assertEquals("localhost", sysInfo.getAddress());

		verify(addressNormalizer).normalize("localhost");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPublicKeyNotAvailable() {
		assertEquals("", sysInfo.getPublicKey());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void getPublicKeyAvailable() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = KeyStore.getInstance("pkcs12");
		final ClassPathResource resource = new ClassPathResource("certs/ConsumerAuthorization.p12");
		keyStore.load(resource.getInputStream(), "123456".toCharArray());
		final X509Certificate serverCertificate = SecurityUtilities.getCertificateFromKeyStore(keyStore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		final Map<String, Object> context = (Map<String, Object>) ReflectionTestUtils.getField(sysInfo, "arrowheadContext");
		context.put("server.public.key", serverCertificate.getPublicKey());

		final String expected = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA7D8z7doc95vY0uAx8JXwvrcl+Q7MykFoFIF1tn4fesvPIXo5eCGDS8FCONW0S5igQ+l00GdN/SlE0o85lI08TvepGEkTOtm1J+hsAHRD"
				+ "65OpPTjzWDVzP4+GzjZSUJl41iBDSW1YHgiFG8P2TqaTqNScrfLtKyekSzy/m24uh+zX5tjNoJ4GdSUeTNttHUuCH39MBxEo5E6KpzFGbC4105WHIH1MGWozOrZ3k7udvCLbCTvZ8PFtbDN4Ymjir0PE+6E2N4I+ka"
				+ "gL1Py/DmNpKvLLI6m+YWJh2ErOAc56ThVvbCDeLOihacb26Y9Icrda1jOa30/xGsS3CmFLIpZjWwIDAQAB";

		final String result = sysInfo.getPublicKey();

		assertEquals(expected, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitNoSystemName() {
		when(helper.getSystemName()).thenReturn(null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(sysInfo2, "init"));

		verify(helper).getSystemName();

		assertEquals("System name is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitNoDomainAddress() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));

		assertEquals("'domainAddress' is missing or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitOutsourcedButNoCredentials() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.OUTSOURCED);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));

		assertEquals("No credentials are specified to login to the authentication system", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitMqttEnabledButNoBrokerAddress() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "mqttEnabled", true);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));

		assertEquals("MQTT Broker address is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitMqttEnabledButNoBrokerPort() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "mqttEnabled", true);
		ReflectionTestUtils.setField(sysInfo, "mqttBrokerAddress", "localhost");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));

		assertEquals("MQTT Broker port is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitNoConfigOk() {
		ReflectionTestUtils.setField(sysInfo2, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.OUTSOURCED);
		ReflectionTestUtils.setField(sysInfo2, "mqttEnabled", false);
		ReflectionTestUtils.setField(sysInfo2, "authenticatorCredentials", Map.of("Authentication", "123456"));

		when(helper.getSystemName()).thenReturn("TestSystem");
		doNothing().when(helper).customInitChecker();

		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo2, "configDefaultsMap")).isEmpty());
		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(sysInfo2, "init"));
		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo2, "configDefaultsMap")).isEmpty());

		verify(helper).getSystemName();
		verify(helper).customInitChecker();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitNoDefaultClassOk() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.DECLARED);
		ReflectionTestUtils.setField(sysInfo, "mqttEnabled", true);
		ReflectionTestUtils.setField(sysInfo, "mqttBrokerAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "mqttBrokerPort", 1883);

		when(config.defaultsClass()).thenReturn(null);

		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo, "configDefaultsMap")).isEmpty());
		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));
		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo, "configDefaultsMap")).isEmpty());

		verify(config).defaultsClass();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testInitNoConfigKeysOk() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.DECLARED);
		ReflectionTestUtils.setField(sysInfo, "mqttEnabled", false);
		ReflectionTestUtils.setField(sysInfo2, "authenticatorCredentials", Map.of("Authentication", "123456"));

		when(config.defaultsClass()).thenReturn((Class) TestDefaults.class);
		when(config.configKeys()).thenReturn(Set.of());

		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo, "configDefaultsMap")).isEmpty());
		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));
		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo, "configDefaultsMap")).isEmpty());

		verify(config).defaultsClass();
		verify(config).configKeys();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testInitOk() {
		ReflectionTestUtils.setField(sysInfo, "domainAddress", "localhost");
		ReflectionTestUtils.setField(sysInfo, "authenticationPolicy", AuthenticationPolicy.DECLARED);
		ReflectionTestUtils.setField(sysInfo, "mqttEnabled", false);
		ReflectionTestUtils.setField(sysInfo2, "authenticatorCredentials", Map.of("Authentication", "123456"));

		when(config.defaultsClass()).thenReturn((Class) TestDefaults.class);
		when(config.configKeys()).thenReturn(Set.of("no.such-field", "not.a.static-field", "valid.field"));

		assertTrue(((Map<String, String>) ReflectionTestUtils.getField(sysInfo, "configDefaultsMap")).isEmpty());
		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(sysInfo, "init"));
		final Map<String, String> configMap = (Map<String, String>) ReflectionTestUtils.getField(sysInfo, "configDefaultsMap");
		assertFalse(configMap.isEmpty());
		assertTrue(configMap.containsKey("no.such-field"));
		assertNull(configMap.get("no.such-field"));
		assertTrue(configMap.containsKey("not.a.static-field"));
		assertNull(configMap.get("not.a.static-field"));
		assertEquals("b", configMap.get("valid.field"));

		verify(config, times(2)).defaultsClass();
		verify(config, times(2)).configKeys();
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	private static final class TestDefaults {

		//=================================================================================================
		// members

		@SuppressWarnings({ "unused", "checkstyle:MemberName", "checkstyle:VisibilityModifier" })
		public final String NOT_A_STATIC__FIELD_DEFAULT = "a";
		@SuppressWarnings("unused")
		public static final String VALID_FIELD_DEFAULT = "b";

	}

	//-------------------------------------------------------------------------------------------------
	@Service
	private static class TestSystemInfo extends SystemInfo {

		//=================================================================================================
		// members

		@Autowired
		private PublicConfigurationKeysAndDefaults config;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getSystemName() {
			return "TestSystem";
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public SystemModel getSystemModel() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public List<ServiceModel> getServices() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
			return config;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static final class TestSystemInfo2 extends TestSystemInfo {

		//=================================================================================================
		// members

		@Autowired
		private Helper helper;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getSystemName() {
			return helper.getSystemName();
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		protected void customInit() {
			helper.customInitChecker();
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static final class Helper {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public String getSystemName() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		public void customInitChecker() {
		};
	}
}