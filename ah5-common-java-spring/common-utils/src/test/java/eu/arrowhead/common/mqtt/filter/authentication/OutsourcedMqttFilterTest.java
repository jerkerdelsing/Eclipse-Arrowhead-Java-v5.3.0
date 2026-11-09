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
package eu.arrowhead.common.mqtt.filter.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.filter.authorization.BlacklistMqttFilterTest.DummyMqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class OutsourcedMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OutsourcedMqttFilter filter;

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Spy
	private ObjectMapper mapper;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private ServiceCollector collector;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOrder() {
		assertEquals(15, filter.order());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticationLookup() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("identity")
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, payload));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterNullAuthKey() {
		final String authKey = null;
		final MqttRequestModel request = new MqttRequestModel("test-base/", "test-operation", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		when(sysInfo.getSystemName()).thenReturn("TestSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();

		assertEquals("No authentication info has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterEmptyAuthKey() {
		final String authKey = "";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "register", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();

		assertEquals("No authentication info has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthKeyWithWrongPrefix() {
		final String authKey = "SYSTEM//RequesterSystem";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(null);

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticatorKeysNotSupported() {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of());
		final String authKey = "AUTHENTICATOR-KEY//RequesterSystem//some-hash";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();
		serviceDiscoverySM.interfaces().clear();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticatorKeysMissingPart() {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("Authentication", "key"));
		final String authKey = "AUTHENTICATOR-KEY//RequesterSystem";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new DummyMqttInterfaceModel("generic_mqtt"))
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticatorNoRelatedSecretKey() {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("Authentication", "key"));
		final String authKey = "AUTHENTICATOR-KEY//RequesterSystem//hash";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/system-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
		verify(systemNameNormalizer).normalize("RequesterSystem");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticatorHashMismatch() throws JsonProcessingException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", "key"));
		final String authKey = "AUTHENTICATOR-KEY//RequesterSystem//hash";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
		verify(mapper).writeValueAsString("payload");
		verify(mapper).readValue("\"payload\"", ServiceInstanceLookupRequestDTO.class);
		verify(systemNameNormalizer).normalize("RequesterSystem");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticatorInvalidSecretKey() throws JsonProcessingException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", ""));
		final String authKey = "AUTHENTICATOR-KEY//RequesterSystem//hash";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, null));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
		verify(systemNameNormalizer).normalize("RequesterSystem");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterAuthenticatorKeyOk() throws JsonProcessingException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", "secretKey"));
		final String authKey = "AUTHENTICATOR-KEY//RequesterSystem//d13e0ffb717e848fd5c4945e7c9fd4500664422899759afef850d5b85dcb5de8";
		final Map<String, Object> payload = Map.of("providerNames", List.of("Authentication"));
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, payload));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		assertDoesNotThrow(() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
		verify(mapper).writeValueAsString(payload);
		verify(mapper).readValue(anyString(), eq(ServiceInstanceLookupRequestDTO.class));
		verify(systemNameNormalizer).normalize("RequesterSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterTokenTooMuchParts() throws JsonProcessingException {
		final String authKey = "IDENTITY-TOKEN//token//other";
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionNames(List.of("identity", "identityManagement"))
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, payload));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterTokenFalseResult() throws JsonProcessingException {
		final String authKey = "IDENTITY-TOKEN//testtoken";
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionNames(List.of("identityManagement"))
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", authKey, "response", 0, null, payload));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(httpService.consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("testtoken"))).thenReturn(new IdentityVerifyResponseDTO(false, null, null, null, null));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
		verify(httpService).consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("testtoken"));

		assertEquals("Invalid authentication info", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void doFilterTokenTrueResult() throws JsonProcessingException {
		final String authKey = "IDENTITY-TOKEN//testtoken";
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "register", new MqttRequestTemplate("trace", authKey, "response", 0, null, "payload"));

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(httpService.consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("testtoken"))).thenReturn(new IdentityVerifyResponseDTO(
				true,
				"RequesterSystem",
				true,
				"2025-07-18T12:00:00Z",
				"2025-07-18T12:05:00Z"));

		assertDoesNotThrow(() -> filter.doFilter(authKey, request));

		verify(sysInfo).getSystemName();
		verify(httpService).consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("testtoken"));

		assertEquals("RequesterSystem", request.getRequester());
		assertTrue(request.isSysOp());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitNoRawKeys() {
		ReflectionTestUtils.setField(filter, "rawSecretKeys", Map.of());
		ReflectionTestUtils.invokeMethod(filter, "init");

		verify(systemNameNormalizer, never()).normalize(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitOk() {
		ReflectionTestUtils.setField(filter, "rawSecretKeys", Map.of("System1", " Key1", "System2", "Key2 "));

		when(systemNameNormalizer.normalize("System1")).thenReturn("System1");
		when(systemNameNormalizer.normalize("System2")).thenReturn("System2");

		ReflectionTestUtils.invokeMethod(filter, "init");

		verify(systemNameNormalizer, times(2)).normalize(anyString());

		final Map<String, String> result = (Map<String, String>) ReflectionTestUtils.getField(filter, "secretKeys");
		assertEquals("Key1", result.get("System1"));
		assertEquals("Key2", result.get("System2"));
	}
}