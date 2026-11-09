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
package eu.arrowhead.common.collector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.common.intf.properties.validators.HttpOperationsValidator;
import eu.arrowhead.common.intf.properties.validators.NotEmptyStringSetValidator;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class HttpCollectorDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private HttpCollectorDriver driver;

	@Mock
	private HttpService httpService;

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private PropertyValidators validators;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.SR_ONLY);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInit() {
		assertDoesNotThrow(() -> driver.init());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceServiceDefNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.acquireService(null, "generic_http", "ProviderName"));

		assertEquals("service definition is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceServiceDefEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.acquireService("", "generic_http", "ProviderName"));

		assertEquals("service definition is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceUnsupportedInterface() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> driver.acquireService("testService", "custom_http", "ProviderName"));

		assertTrue(ex.getMessage().startsWith("This collector only supports the following interfaces: "));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceSROnlyNoMatch() {
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final ServiceModel result = driver.acquireService("testService", "generic_http", null);

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<ServiceInstanceLookupRequestDTO> payloadCaptor = ArgumentCaptor.forClass(ServiceInstanceLookupRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());

		assertNull(result);
		assertEquals("https://localhost:8443/serviceregistry/service-discovery/lookup?verbose=false", uriCaptor.getValue().toUriString());
		final ServiceInstanceLookupRequestDTO payload = payloadCaptor.getValue();
		assertEquals(1, payload.serviceDefinitionNames().size());
		assertEquals("testService", payload.serviceDefinitionNames().get(0));
		assertEquals(1, payload.interfaceTemplateNames().size());
		assertEquals("generic_http", payload.interfaceTemplateNames().get(0));
		assertNull(payload.providerNames());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceSROnlyHttpNoOperations() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"basePath", "/test"));
		final ServiceInstanceResponseDTO responseEntity = new ServiceInstanceResponseDTO(
				"ProviderName|testService|1.0.0",
				new SystemResponseDTO(
						"ProviderName",
						Map.of(),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-07-30T08:00:00Z",
						"2025-07-30T08:00:00Z"),
				new ServiceDefinitionResponseDTO("testService", "2025-06-25T08:00:00Z", "2025-06-25T08:00:00Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				"2025-07-30T08:00:05Z",
				"2025-07-30T08:00:05Z");

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(responseEntity), 1));

		final ServiceModel result = driver.acquireService("testService", "generic_http", "ProviderName");

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo).getSystemName();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<ServiceInstanceLookupRequestDTO> payloadCaptor = ArgumentCaptor.forClass(ServiceInstanceLookupRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());

		assertNull(result);
		assertEquals("http://localhost:8443/serviceregistry/service-discovery/lookup?verbose=false", uriCaptor.getValue().toUriString());
		final ServiceInstanceLookupRequestDTO payload = payloadCaptor.getValue();
		assertEquals(1, payload.serviceDefinitionNames().size());
		assertEquals("testService", payload.serviceDefinitionNames().get(0));
		assertEquals(1, payload.interfaceTemplateNames().size());
		assertEquals("generic_http", payload.interfaceTemplateNames().get(0));
		assertEquals(1, payload.providerNames().size());
		assertEquals("ProviderName", payload.providerNames().get(0));
		assertEquals(1, headerCaptor.getValue().size());
		assertEquals("Bearer SYSTEM//ConsumerName", headerCaptor.getValue().get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceSROnlyHttpOk() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"basePath", "/test",
						"operations", Map.of("op", Map.of("path", "/op", "method", "POST"))));
		final ServiceInstanceInterfaceResponseDTO intf2 = new ServiceInstanceInterfaceResponseDTO(
				"generic_mqtt",
				"tcp",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12346,
						"baseTopic", "test/",
						"operations", Set.of("op")));
		final ServiceInstanceResponseDTO responseEntity = new ServiceInstanceResponseDTO(
				"ProviderName|testService|1.0.0",
				new SystemResponseDTO(
						"ProviderName",
						Map.of(),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-07-30T08:00:00Z",
						"2025-07-30T08:00:00Z"),
				new ServiceDefinitionResponseDTO("testService", "2025-06-25T08:00:00Z", "2025-06-25T08:00:00Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf, intf2),
				"2025-07-30T08:00:05Z",
				"2025-07-30T08:00:05Z");

		final HttpOperationsValidator httpOperationsValidatorMock = Mockito.mock(HttpOperationsValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(responseEntity), 1));
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(httpOperationsValidatorMock);
		when(httpOperationsValidatorMock.validateAndNormalize(anyMap())).thenReturn(Map.of("op", new HttpOperationModel.Builder().path("/op").method("POST").build()));

		final ServiceModel result = driver.acquireService("testService", "generic_http", "ProviderName");

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo).getSystemName();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<ServiceInstanceLookupRequestDTO> payloadCaptor = ArgumentCaptor.forClass(ServiceInstanceLookupRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(httpOperationsValidatorMock).validateAndNormalize(anyMap());

		assertEquals("testService", result.serviceDefinition());
		assertEquals("1.0.0", result.version());
		assertEquals(1, result.interfaces().size());
		HttpInterfaceModel resultIntf = (HttpInterfaceModel) result.interfaces().get(0);
		assertEquals("generic_http", resultIntf.templateName());
		assertEquals("http", resultIntf.protocol());
		assertEquals("localhost", resultIntf.accessAddresses().get(0));
		assertEquals(12345, resultIntf.accessPort());
		assertEquals("/test", resultIntf.basePath());
		assertEquals(1, resultIntf.operations().size());
		HttpOperationModel opModel = resultIntf.operations().get("op");
		assertEquals("/op", opModel.path());
		assertEquals("POST", opModel.method());
		assertEquals("http://localhost:8443/serviceregistry/service-discovery/lookup?verbose=false", uriCaptor.getValue().toUriString());
		final ServiceInstanceLookupRequestDTO payload = payloadCaptor.getValue();
		assertEquals(1, payload.serviceDefinitionNames().size());
		assertEquals("testService", payload.serviceDefinitionNames().get(0));
		assertEquals(1, payload.interfaceTemplateNames().size());
		assertEquals("generic_http", payload.interfaceTemplateNames().get(0));
		assertEquals(1, payload.providerNames().size());
		assertEquals("ProviderName", payload.providerNames().get(0));
		assertEquals(1, headerCaptor.getValue().size());
		assertEquals("Bearer SYSTEM//ConsumerName", headerCaptor.getValue().get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceSROnlyMqttNoOperations() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_mqtt",
				"tcp",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"baseTopic", "test/"));
		final ServiceInstanceResponseDTO responseEntity = new ServiceInstanceResponseDTO(
				"ProviderName|testService|1.0.0",
				new SystemResponseDTO(
						"ProviderName",
						Map.of(),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-07-30T08:00:00Z",
						"2025-07-30T08:00:00Z"),
				new ServiceDefinitionResponseDTO("testService", "2025-06-25T08:00:00Z", "2025-06-25T08:00:00Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				"2025-07-30T08:00:05Z",
				"2025-07-30T08:00:05Z");

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(responseEntity), 1));

		final ServiceModel result = driver.acquireService("testService", "generic_mqtt", "ProviderName");

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo).getSystemName();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<ServiceInstanceLookupRequestDTO> payloadCaptor = ArgumentCaptor.forClass(ServiceInstanceLookupRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());

		assertNull(result);
		assertEquals("http://localhost:8443/serviceregistry/service-discovery/lookup?verbose=false", uriCaptor.getValue().toUriString());
		final ServiceInstanceLookupRequestDTO payload = payloadCaptor.getValue();
		assertEquals(1, payload.serviceDefinitionNames().size());
		assertEquals("testService", payload.serviceDefinitionNames().get(0));
		assertEquals(1, payload.interfaceTemplateNames().size());
		assertEquals("generic_mqtt", payload.interfaceTemplateNames().get(0));
		assertEquals(1, payload.providerNames().size());
		assertEquals("ProviderName", payload.providerNames().get(0));
		assertEquals(1, headerCaptor.getValue().size());
		assertEquals("Bearer SYSTEM//ConsumerName", headerCaptor.getValue().get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceSROnlyMqttOk() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"basePath", "/test",
						"operations", Map.of("op", Map.of("path", "/op", "method", "POST"))));
		final ServiceInstanceInterfaceResponseDTO intf2 = new ServiceInstanceInterfaceResponseDTO(
				"generic_mqtt",
				"tcp",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12346,
						"baseTopic", "test/",
						"operations", Set.of("op")));
		final ServiceInstanceResponseDTO responseEntity = new ServiceInstanceResponseDTO(
				"ProviderName|testService|1.0.0",
				new SystemResponseDTO(
						"ProviderName",
						Map.of(),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-07-30T08:00:00Z",
						"2025-07-30T08:00:00Z"),
				new ServiceDefinitionResponseDTO("testService", "2025-06-25T08:00:00Z", "2025-06-25T08:00:00Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf, intf2),
				"2025-07-30T08:00:05Z",
				"2025-07-30T08:00:05Z");

		final NotEmptyStringSetValidator operationsValidatorMock = Mockito.mock(NotEmptyStringSetValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(responseEntity), 1));
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_STRING_SET)).thenReturn(operationsValidatorMock);
		when(operationsValidatorMock.validateAndNormalize(anySet(), eq("OPERATION"))).thenReturn(Set.of("op"));

		final ServiceModel result = driver.acquireService("testService", "generic_mqtt", "ProviderName");

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo).getSystemName();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<ServiceInstanceLookupRequestDTO> payloadCaptor = ArgumentCaptor.forClass(ServiceInstanceLookupRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_STRING_SET);
		verify(operationsValidatorMock).validateAndNormalize(anySet(), eq("OPERATION"));

		assertEquals("testService", result.serviceDefinition());
		assertEquals("1.0.0", result.version());
		assertEquals(1, result.interfaces().size());
		MqttInterfaceModel resultIntf = (MqttInterfaceModel) result.interfaces().get(0);
		assertEquals("generic_mqtt", resultIntf.templateName());
		assertEquals("tcp", resultIntf.protocol());
		assertEquals("localhost", resultIntf.accessAddresses().get(0));
		assertEquals(12346, resultIntf.accessPort());
		assertEquals("test/", resultIntf.baseTopic());
		assertEquals(1, resultIntf.operations().size());
		assertEquals("op", resultIntf.operations().iterator().next());
		assertEquals("http://localhost:8443/serviceregistry/service-discovery/lookup?verbose=false", uriCaptor.getValue().toUriString());
		final ServiceInstanceLookupRequestDTO payload = payloadCaptor.getValue();
		assertEquals(1, payload.serviceDefinitionNames().size());
		assertEquals("testService", payload.serviceDefinitionNames().get(0));
		assertEquals(1, payload.interfaceTemplateNames().size());
		assertEquals("generic_mqtt", payload.interfaceTemplateNames().get(0));
		assertEquals(1, payload.providerNames().size());
		assertEquals("ProviderName", payload.providerNames().get(0));
		assertEquals(1, headerCaptor.getValue().size());
		assertEquals("Bearer SYSTEM//ConsumerName", headerCaptor.getValue().get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceLookupOrchestrationFailure() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.SR_AND_ORCH);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		assertNull(ReflectionTestUtils.getField(driver, "orchestrationCache"));
		final ServiceModel result = driver.acquireService("testService", "generic_http", null);
		assertNull(ReflectionTestUtils.getField(driver, "orchestrationCache"));

		verify(sysInfo, times(4)).isSslEnabled();
		verify(sysInfo, times(3)).getServiceRegistryAddress();
		verify(sysInfo, times(3)).getServiceRegistryPort();
		verify(sysInfo, times(3)).getIdentityToken();
		verify(sysInfo, times(3)).getAuthenticationPolicy();
		verify(sysInfo, times(3)).getSystemName();
		verify(httpService, times(3)).sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap());

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceLookupOrchestrationSuccessNoMatch() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.SR_AND_ORCH);

		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_https",
				"https",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 8441,
						"basePath", "/serviceorchestration/orchestration",
						"operations", Map.of("pull", Map.of("path", "/pull", "method", "POST"))));
		final ServiceInstanceResponseDTO orchLookupResponse = new ServiceInstanceResponseDTO(
				"DynamicServiceOrchestration|serviceOrchestration|1.0.0",
				new SystemResponseDTO(
						"DynamicServiceOrchestration",
						Map.of(),
						"5.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-07-30T08:00:00Z",
						"2025-07-30T08:00:00Z"),
				new ServiceDefinitionResponseDTO("serviceOrchestration", "2025-06-25T08:00:00Z", "2025-06-25T08:00:00Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intf),
				"2025-07-30T08:00:05Z",
				"2025-07-30T08:00:05Z");

		final HttpOperationsValidator httpOperationsValidatorMock = Mockito.mock(HttpOperationsValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(orchLookupResponse), 1));
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(httpOperationsValidatorMock);
		when(httpOperationsValidatorMock.validateAndNormalize(anyMap())).thenReturn(Map.of("pull", new HttpOperationModel.Builder().path("/pull").method("POST").build()));
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), any(OrchestrationRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new OrchestrationResponseDTO(List.of(), List.of()));

		assertNull(ReflectionTestUtils.getField(driver, "orchestrationCache"));
		final ServiceModel result = driver.acquireService("testService", "generic_https", null);
		final ServiceModel orchCache = (ServiceModel) ReflectionTestUtils.getField(driver, "orchestrationCache");
		assertNotNull(orchCache);
		assertEquals("serviceOrchestration", orchCache.serviceDefinition());

		verify(sysInfo, times(5)).isSslEnabled();
		verify(sysInfo, times(2)).getServiceRegistryAddress();
		verify(sysInfo, times(2)).getServiceRegistryPort();
		verify(sysInfo, times(3)).getIdentityToken();
		verify(sysInfo, times(3)).getAuthenticationPolicy();
		verify(httpService, times(2)).sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap());
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(httpOperationsValidatorMock).validateAndNormalize(anyMap());

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<OrchestrationRequestDTO> payloadCaptor = ArgumentCaptor.forClass(OrchestrationRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());

		assertNull(result);
		assertEquals("https://localhost:8441/serviceorchestration/orchestration/pull", uriCaptor.getValue().toUriString());
		final OrchestrationRequestDTO payload = payloadCaptor.getValue();
		assertEquals("testService", payload.serviceRequirement().serviceDefinition());
		assertEquals(1, payload.serviceRequirement().interfaceTemplateNames().size());
		assertEquals("generic_https", payload.serviceRequirement().interfaceTemplateNames().get(0));
		assertTrue(payload.orchestrationFlags().get("MATCHMAKING"));
		assertFalse(payload.orchestrationFlags().get("ALLOW_INTERCLOUD"));
		assertFalse(payload.orchestrationFlags().get("ALLOW_TRANSLATION"));
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceCachedOrchestrationSuccessNoOperations() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.SR_AND_ORCH);

		final ServiceModel orchModel = new ServiceModel.Builder()
				.serviceDefinition("serviceOrchestration")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(8441)
						.basePath("/serviceorchestration/orchestration")
						.operation("pull", new HttpOperationModel.Builder().path("/pull").method("POST").build())
						.build())
				.build();
		ReflectionTestUtils.setField(driver, "orchestrationCache", orchModel);

		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"basePath", "/test"));

		final OrchestrationResultDTO orchResult = new OrchestrationResultDTO(
				"TestProvider|testService|1.0.0",
				null,
				"TestProvider",
				"testService",
				"1.0.0",
				null,
				null,
				Map.of(),
				List.of(intf),
				Map.of());

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), any(OrchestrationRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new OrchestrationResponseDTO(List.of(orchResult), List.of()));

		ServiceModel orchCache = (ServiceModel) ReflectionTestUtils.getField(driver, "orchestrationCache");
		assertNotNull(ReflectionTestUtils.getField(driver, "orchestrationCache"));
		assertEquals("serviceOrchestration", orchCache.serviceDefinition());
		final ServiceModel result = driver.acquireService("testService", "generic_http", "ProviderName");
		orchCache = (ServiceModel) ReflectionTestUtils.getField(driver, "orchestrationCache");
		assertNotNull(orchCache);
		assertEquals("serviceOrchestration", orchCache.serviceDefinition());

		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo, times(2)).getIdentityToken();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(2)).getSystemName();
		verify(httpService).sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap());

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<OrchestrationRequestDTO> payloadCaptor = ArgumentCaptor.forClass(OrchestrationRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());

		assertNull(result);
		assertEquals("http://localhost:8441/serviceorchestration/orchestration/pull", uriCaptor.getValue().toUriString());
		final OrchestrationRequestDTO payload = payloadCaptor.getValue();
		assertEquals("testService", payload.serviceRequirement().serviceDefinition());
		assertEquals(1, payload.serviceRequirement().interfaceTemplateNames().size());
		assertEquals("generic_http", payload.serviceRequirement().interfaceTemplateNames().get(0));
		assertEquals(1, payload.serviceRequirement().preferredProviders().size());
		assertEquals("ProviderName", payload.serviceRequirement().preferredProviders().get(0));
		assertTrue(payload.orchestrationFlags().get("MATCHMAKING"));
		assertFalse(payload.orchestrationFlags().get("ALLOW_INTERCLOUD"));
		assertFalse(payload.orchestrationFlags().get("ALLOW_TRANSLATION"));
		assertTrue(payload.orchestrationFlags().get("ONLY_PREFERRED"));
		assertEquals(1, headerCaptor.getValue().size());
		assertEquals("Bearer SYSTEM//ConsumerName", headerCaptor.getValue().get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceCachedOrchestrationOk() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.SR_AND_ORCH);

		final ServiceModel orchModel = new ServiceModel.Builder()
				.serviceDefinition("serviceOrchestration")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(8441)
						.basePath("/serviceorchestration/orchestration")
						.operation("pull", new HttpOperationModel.Builder().path("/pull").method("POST").build())
						.build())
				.build();
		ReflectionTestUtils.setField(driver, "orchestrationCache", orchModel);

		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"basePath", "/test",
						"operations", Map.of("op", Map.of("path", "/op", "method", "POST"))));

		final OrchestrationResultDTO orchResult = new OrchestrationResultDTO(
				"TestProvider|testService|1.0.0",
				null,
				"TestProvider",
				"testService",
				"1.0.0",
				null,
				null,
				Map.of(),
				List.of(intf),
				Map.of());

		final HttpOperationsValidator httpOperationsValidatorMock = Mockito.mock(HttpOperationsValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(httpOperationsValidatorMock);
		when(httpOperationsValidatorMock.validateAndNormalize(anyMap())).thenReturn(Map.of("op", new HttpOperationModel.Builder().path("/op").method("POST").build()));
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), any(OrchestrationRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new OrchestrationResponseDTO(List.of(orchResult), List.of()));

		ServiceModel orchCache = (ServiceModel) ReflectionTestUtils.getField(driver, "orchestrationCache");
		assertNotNull(ReflectionTestUtils.getField(driver, "orchestrationCache"));
		assertEquals("serviceOrchestration", orchCache.serviceDefinition());
		final ServiceModel result = driver.acquireService("testService", "generic_http", "ProviderName");
		orchCache = (ServiceModel) ReflectionTestUtils.getField(driver, "orchestrationCache");
		assertNotNull(orchCache);
		assertEquals("serviceOrchestration", orchCache.serviceDefinition());

		verify(sysInfo, times(3)).isSslEnabled();
		verify(sysInfo).getServiceRegistryAddress();
		verify(sysInfo).getServiceRegistryPort();
		verify(sysInfo, times(2)).getIdentityToken();
		verify(sysInfo, times(2)).getAuthenticationPolicy();
		verify(sysInfo, times(2)).getSystemName();
		verify(httpService).sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap());

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<OrchestrationRequestDTO> payloadCaptor = ArgumentCaptor.forClass(OrchestrationRequestDTO.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), payloadCaptor.capture(), isNull(), headerCaptor.capture());
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(httpOperationsValidatorMock).validateAndNormalize(anyMap());

		assertEquals("testService", result.serviceDefinition());
		assertEquals("1.0.0", result.version());
		assertEquals(1, result.interfaces().size());
		HttpInterfaceModel resultIntf = (HttpInterfaceModel) result.interfaces().get(0);
		assertEquals("generic_http", resultIntf.templateName());
		assertEquals("http", resultIntf.protocol());
		assertEquals("localhost", resultIntf.accessAddresses().get(0));
		assertEquals(12345, resultIntf.accessPort());
		assertEquals("/test", resultIntf.basePath());
		assertEquals(1, resultIntf.operations().size());
		HttpOperationModel opModel = resultIntf.operations().get("op");
		assertEquals("/op", opModel.path());
		assertEquals("POST", opModel.method());
		assertEquals("http://localhost:8441/serviceorchestration/orchestration/pull", uriCaptor.getValue().toUriString());
		final OrchestrationRequestDTO payload = payloadCaptor.getValue();
		assertEquals("testService", payload.serviceRequirement().serviceDefinition());
		assertEquals(1, payload.serviceRequirement().interfaceTemplateNames().size());
		assertEquals("generic_http", payload.serviceRequirement().interfaceTemplateNames().get(0));
		assertEquals(1, payload.serviceRequirement().preferredProviders().size());
		assertEquals("ProviderName", payload.serviceRequirement().preferredProviders().get(0));
		assertTrue(payload.orchestrationFlags().get("MATCHMAKING"));
		assertFalse(payload.orchestrationFlags().get("ALLOW_INTERCLOUD"));
		assertFalse(payload.orchestrationFlags().get("ALLOW_TRANSLATION"));
		assertTrue(payload.orchestrationFlags().get("ONLY_PREFERRED"));
		assertEquals(1, headerCaptor.getValue().size());
		assertEquals("Bearer SYSTEM//ConsumerName", headerCaptor.getValue().get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testAcquireServiceOrchestrationModeOrchestratesWithOperation() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.ORCHESTRATION);
		ReflectionTestUtils.setField(driver, "bootstrapServiceNames", new String[] { "serviceDiscovery", " identity ", "serviceOrchestration" });
		ReflectionTestUtils.setField(driver, "orchestrationTimeToLive", 60L);
		driver.init();

		final ServiceModel orchModel = new ServiceModel.Builder()
				.serviceDefinition("serviceOrchestration")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(8441)
						.basePath("/serviceorchestration/orchestration")
						.operation("pull", new HttpOperationModel.Builder().path("/pull").method("POST").build())
						.build())
				.build();
		ReflectionTestUtils.setField(driver, "orchestrationCache", orchModel);

		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of(
						"accessAddresses", List.of("localhost"),
						"accessPort", 12345,
						"basePath", "/test",
						"operations", Map.of("op", Map.of("path", "/op", "method", "POST"))));
		final OrchestrationResultDTO orchResult = new OrchestrationResultDTO("TestProvider|testService|1.0.0", null, "TestProvider", "testService", "1.0.0", null, null, Map.of(), List.of(intf), Map.of());
		final HttpOperationsValidator httpOperationsValidatorMock = Mockito.mock(HttpOperationsValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(httpOperationsValidatorMock);
		when(httpOperationsValidatorMock.validateAndNormalize(anyMap())).thenReturn(Map.of("op", new HttpOperationModel.Builder().path("/op").method("POST").build()));
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), any(OrchestrationRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new OrchestrationResponseDTO(List.of(orchResult), List.of()));

		final ServiceModel result = driver.acquireService("testService", "generic_http", null, "op");

		// no ServiceRegistry lookup: the service is not a bootstrap service
		verify(httpService, never()).sendRequest(any(UriComponents.class), any(HttpMethod.class), eq(ServiceInstanceListResponseDTO.class), any(), any(), anyMap());
		final ArgumentCaptor<OrchestrationRequestDTO> payloadCaptor = ArgumentCaptor.forClass(OrchestrationRequestDTO.class);
		verify(httpService).sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(OrchestrationResponseDTO.class), payloadCaptor.capture(), isNull(), anyMap());

		assertEquals("testService", result.serviceDefinition());
		final OrchestrationRequestDTO payload = payloadCaptor.getValue();
		assertEquals(List.of("op"), payload.serviceRequirement().operations());
		assertTrue(Utilities.isEmpty(payload.serviceRequirement().preferredProviders()));
		assertFalse(payload.orchestrationFlags().containsKey("ONLY_PREFERRED"));
		assertEquals(60000, driver.cacheTimeToLiveMillis("testService"));
		assertEquals(0, driver.cacheTimeToLiveMillis("identity"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireServiceOrchestrationModeLooksUpBootstrapService() {
		ReflectionTestUtils.setField(driver, "mode", HttpCollectorMode.ORCHESTRATION);
		ReflectionTestUtils.setField(driver, "bootstrapServiceNames", new String[] { "identity", "serviceOrchestration" });
		driver.init();

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServiceRegistryAddress()).thenReturn("localhost");
		when(sysInfo.getServiceRegistryPort()).thenReturn(8443);
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		assertNull(driver.acquireService("identity", "generic_http", "Authentication", "identity-login"));

		verify(httpService).sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(ServiceInstanceListResponseDTO.class), any(ServiceInstanceLookupRequestDTO.class), isNull(), anyMap());
		verify(httpService, never()).sendRequest(any(UriComponents.class), any(HttpMethod.class), eq(OrchestrationResponseDTO.class), any(), any(), anyMap());
	}
}