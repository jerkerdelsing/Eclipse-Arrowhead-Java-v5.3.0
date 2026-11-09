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
package eu.arrowhead.serviceregistry.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryService;
import eu.arrowhead.serviceregistry.service.SystemDiscoveryService;

@ExtendWith(MockitoExtension.class)
public class ServiceRegistryApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceRegistryApplicationInitListener listener;

	@Mock
	private SystemDiscoveryService sysdService;

	@Mock
	private ServiceDiscoveryService sdService;

	@Mock
	private ServiceInterfaceAddressPropertyProcessor serviceInterfaceAddressPropertyProcessor;

	@Mock
	private ServiceRegistrySystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitStandaloneModeTrue() {
		ReflectionTestUtils.setField(listener, "standaloneMode", true);

		assertDoesNotThrow(() -> listener.customInit(null));

		verify(sysdService, never()).revokeSystem(anyString(), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCustomInitNullServiceList() {
		ReflectionTestUtils.setField(listener, "standaloneMode", false);
		ReflectionTestUtils.setField(listener, "registeredServices", Set.of());

		final SystemModel sysModel = new SystemModel(Map.of(), "1.0.0", List.of("localhost"), null);
		final SystemRequestDTO dto = new SystemRequestDTO("ServiceRegistry", Map.of(), "1.0.0", List.of("localhost"), null);
		final SystemResponseDTO result = new SystemResponseDTO(
				"ServiceRegistry",
				Map.of(),
				"1.0.0",
				List.of(new AddressDTO(AddressType.HOSTNAME.name(), "localhost")),
				null,
				"2025-11-11T12:49:02Z",
				"2025-11-11T12:49:02Z");

		final List<String> addressAliases = List.of("addresses", "accessAddresses");

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysdService.revokeSystem("ServiceRegistry", "INIT")).thenReturn(true);
		when(sysInfo.getSystemModel()).thenReturn(sysModel);
		when(sysdService.registerSystem(dto, "INIT")).thenReturn(Map.entry(result, true));
		when(sysInfo.getServices()).thenReturn(null);
		when(sysInfo.getServiceAddressAliases()).thenReturn(addressAliases);
		doNothing().when(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(addressAliases);

		assertDoesNotThrow(() -> listener.customInit(null));

		verify(sysInfo, times(3)).getSystemName();
		verify(sysdService).revokeSystem("ServiceRegistry", "INIT");
		verify(sysInfo).getSystemModel();
		verify(sysdService).registerSystem(dto, "INIT");
		verify(sysInfo).getServices();
		verify(sdService, never()).registerService(any(ServiceInstanceRequestDTO.class), eq("INIT"));
		verify(sysInfo).getServiceAddressAliases();
		verify(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(addressAliases);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testCustomInitServiceListNonePolicy() {
		ReflectionTestUtils.setField(listener, "standaloneMode", false);
		ReflectionTestUtils.setField(listener, "registeredServices", new HashSet<>());

		final SystemModel sysModel = new SystemModel(Map.of(), "1.0.0", List.of("localhost"), null);
		final SystemRequestDTO dto = new SystemRequestDTO("ServiceRegistry", Map.of(), "1.0.0", List.of("localhost"), null);
		final SystemResponseDTO result = new SystemResponseDTO(
				"ServiceRegistry",
				Map.of(),
				"1.0.0",
				List.of(new AddressDTO(AddressType.HOSTNAME.name(), "localhost")),
				null,
				"2025-11-11T12:49:02Z",
				"2025-11-11T12:49:02Z");

		final HttpOperationModel register = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REGISTER_PATH)
				.build();
		final HttpOperationModel lookup = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REVOKE_PATH)
				.build();
		final HttpInterfaceModel intfModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
				.basePath(ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH)
				.operation(Constants.SERVICE_OP_REGISTER, register)
				.operation(Constants.SERVICE_OP_LOOKUP, lookup)
				.operation(Constants.SERVICE_OP_REVOKE, revoke)
				.build();
		final ServiceModel serviceModel = new ServiceModel("deviceDiscovery", "1.0.0", List.of(intfModel), Map.of());

		final Map<String, Object> intfProps = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 8443,
				"basePath", "/serviceregistry/device-discovery",
				"operations", Map.of(
						"register", register,
						"lookup", lookup,
						"revoke", revoke));

		final ServiceInstanceInterfaceRequestDTO intfRequest = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", ServiceInterfacePolicy.NONE.name(), intfProps);
		final ServiceInstanceRequestDTO siRequest = new ServiceInstanceRequestDTO("ServiceRegistry", "deviceDiscovery", "1.0.0", null, Map.of(), List.of(intfRequest));
		final ServiceInstanceInterfaceResponseDTO intfResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", ServiceInterfacePolicy.NONE.name(), intfProps);
		final ServiceInstanceResponseDTO siResponse = new ServiceInstanceResponseDTO(
				"ServiceRegistry|deviceDiscovery|1.0.0",
				result,
				new ServiceDefinitionResponseDTO("deviceDiscovery", "2025-11-11T12:49:02Z", "2025-11-11T12:49:02Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intfResponse),
				"2025-11-11T12:49:02Z",
				"2025-11-11T12:49:02Z");

		final List<String> addressAliases = List.of("addresses", "accessAddresses");

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysdService.revokeSystem("ServiceRegistry", "INIT")).thenReturn(true);
		when(sysInfo.getSystemModel()).thenReturn(sysModel);
		when(sysdService.registerSystem(dto, "INIT")).thenReturn(Map.entry(result, true));
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sdService.registerService(siRequest, "INIT")).thenReturn(siResponse);
		when(sysInfo.getServiceAddressAliases()).thenReturn(addressAliases);
		doNothing().when(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(addressAliases);

		assertDoesNotThrow(() -> listener.customInit(null));

		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		assertEquals(1, registeredServices.size());
		assertEquals("ServiceRegistry|deviceDiscovery|1.0.0", registeredServices.iterator().next());

		verify(sysInfo, times(4)).getSystemName();
		verify(sysdService).revokeSystem("ServiceRegistry", "INIT");
		verify(sysInfo).getSystemModel();
		verify(sysdService).registerSystem(dto, "INIT");
		verify(sysInfo, times(2)).getServices();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sdService).registerService(siRequest, "INIT");
		verify(sysInfo).getServiceAddressAliases();
		verify(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(addressAliases);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testCustomInitServiceListCertPolicy() {
		ReflectionTestUtils.setField(listener, "standaloneMode", false);
		ReflectionTestUtils.setField(listener, "registeredServices", new HashSet<>());

		final SystemModel sysModel = new SystemModel(Map.of(), "1.0.0", List.of("localhost"), null);
		final SystemRequestDTO dto = new SystemRequestDTO("ServiceRegistry", Map.of(), "1.0.0", List.of("localhost"), null);
		final SystemResponseDTO result = new SystemResponseDTO(
				"ServiceRegistry",
				Map.of(),
				"1.0.0",
				List.of(new AddressDTO(AddressType.HOSTNAME.name(), "localhost")),
				null,
				"2025-11-11T12:49:02Z",
				"2025-11-11T12:49:02Z");

		final HttpOperationModel register = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REGISTER_PATH)
				.build();
		final HttpOperationModel lookup = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH)
				.build();
		final HttpOperationModel revoke = new HttpOperationModel.Builder()
				.method(HttpMethod.DELETE.name())
				.path(ServiceRegistryConstants.HTTP_API_OP_REVOKE_PATH)
				.build();
		final HttpInterfaceModel intfModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
				.basePath(ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH)
				.operation(Constants.SERVICE_OP_REGISTER, register)
				.operation(Constants.SERVICE_OP_LOOKUP, lookup)
				.operation(Constants.SERVICE_OP_REVOKE, revoke)
				.build();
		final ServiceModel serviceModel = new ServiceModel("deviceDiscovery", "1.0.0", List.of(intfModel), Map.of());

		final Map<String, Object> intfProps = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 8443,
				"basePath", "/serviceregistry/device-discovery",
				"operations", Map.of(
						"register", register,
						"lookup", lookup,
						"revoke", revoke));

		final ServiceInstanceInterfaceRequestDTO intfRequest = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", ServiceInterfacePolicy.CERT_AUTH.name(), intfProps);
		final ServiceInstanceRequestDTO siRequest = new ServiceInstanceRequestDTO("ServiceRegistry", "deviceDiscovery", "1.0.0", null, Map.of(), List.of(intfRequest));
		final ServiceInstanceInterfaceResponseDTO intfResponse = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", ServiceInterfacePolicy.CERT_AUTH.name(), intfProps);
		final ServiceInstanceResponseDTO siResponse = new ServiceInstanceResponseDTO(
				"ServiceRegistry|deviceDiscovery|1.0.0",
				result,
				new ServiceDefinitionResponseDTO("deviceDiscovery", "2025-11-11T12:49:02Z", "2025-11-11T12:49:02Z"),
				"1.0.0",
				null,
				Map.of(),
				List.of(intfResponse),
				"2025-11-11T12:49:02Z",
				"2025-11-11T12:49:02Z");

		final List<String> addressAliases = List.of("addresses", "accessAddresses");

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysdService.revokeSystem("ServiceRegistry", "INIT")).thenReturn(true);
		when(sysInfo.getSystemModel()).thenReturn(sysModel);
		when(sysdService.registerSystem(dto, "INIT")).thenReturn(Map.entry(result, true));
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(sdService.registerService(siRequest, "INIT")).thenReturn(siResponse);
		when(sysInfo.getServiceAddressAliases()).thenReturn(addressAliases);
		doNothing().when(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(addressAliases);

		assertDoesNotThrow(() -> listener.customInit(null));

		final Set<String> registeredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		assertEquals(1, registeredServices.size());
		assertEquals("ServiceRegistry|deviceDiscovery|1.0.0", registeredServices.iterator().next());

		verify(sysInfo, times(4)).getSystemName();
		verify(sysdService).revokeSystem("ServiceRegistry", "INIT");
		verify(sysInfo).getSystemModel();
		verify(sysdService).registerSystem(dto, "INIT");
		verify(sysInfo, times(2)).getServices();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sdService).registerService(siRequest, "INIT");
		verify(sysInfo).getServiceAddressAliases();
		verify(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(addressAliases);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomDestroyStandaloneModeTrue() {
		ReflectionTestUtils.setField(listener, "standaloneMode", true);

		assertDoesNotThrow(() -> listener.customDestroy());

		verify(sdService, never()).revokeService(anyString(), anyString(), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testCustomDestroyExceptionsHandled() {
		ReflectionTestUtils.setField(listener, "standaloneMode", false);
		final Set<String> registeredServices = new HashSet<>(1);
		registeredServices.add("ServiceRegistry|deviceDiscovery|1.0.0");
		ReflectionTestUtils.setField(listener, "registeredServices", registeredServices);

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sdService.revokeService("ServiceRegistry", "ServiceRegistry|deviceDiscovery|1.0.0", "INIT")).thenThrow(InternalServerError.class);

		assertDoesNotThrow(() -> listener.customDestroy());
		final Set<String> currentRegisteredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		assertEquals(1, currentRegisteredServices.size());
		assertEquals("ServiceRegistry|deviceDiscovery|1.0.0", currentRegisteredServices.iterator().next());

		verify(sysInfo).getSystemName();
		verify(sdService).revokeService("ServiceRegistry", "ServiceRegistry|deviceDiscovery|1.0.0", "INIT");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testCustomDestroyOk() {
		ReflectionTestUtils.setField(listener, "standaloneMode", false);
		final Set<String> registeredServices = new HashSet<>(1);
		registeredServices.add("ServiceRegistry|deviceDiscovery|1.0.0");
		ReflectionTestUtils.setField(listener, "registeredServices", registeredServices);

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sdService.revokeService("ServiceRegistry", "ServiceRegistry|deviceDiscovery|1.0.0", "INIT")).thenReturn(true);

		assertDoesNotThrow(() -> listener.customDestroy());
		final Set<String> currentRegisteredServices = (Set<String>) ReflectionTestUtils.getField(listener, "registeredServices");
		assertEquals(0, currentRegisteredServices.size());

		verify(sysInfo).getSystemName();
		verify(sdService).revokeService("ServiceRegistry", "ServiceRegistry|deviceDiscovery|1.0.0", "INIT");
	}
}