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
package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceNormalizer;

@SuppressWarnings("checkstyle:magicnumber")
@ExtendWith(MockitoExtension.class)
public class ManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementNormalization normalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private InterfaceNormalizer interfaceNormalizer;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	//=================================================================================================
	// members

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemRequestDTOsNotEmptyList() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressValidator.detectType("192.168.4.4")).thenReturn(AddressType.IPV4);

		// empty addresses
		final SystemRequestDTO dto1 = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "TEST_DEVICE");
		final NormalizedSystemRequestDTO expected1 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", new ArrayList<>(), "TEST_DEVICE");

		// empty device name
		final SystemRequestDTO dto2 = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of("192.168.4.4"), EMPTY);
		final NormalizedSystemRequestDTO expected2 = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), null);

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTOs(new SystemListRequestDTO(List.of(dto1, dto2))));
		assertEquals(List.of(expected1, expected2), normalized);
		verify(systemNameNormalizer, times(2)).normalize(anyString());
		verify(versionNormalizer, times(2)).normalize(anyString());
		verify(addressNormalizer, times(1)).normalize(anyString());
		verify(deviceNameNormalizer, times(1)).normalize(anyString());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), times(1));
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), times(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemRequestDTOsEmptyList() {

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTOs(new SystemListRequestDTO(List.of())));
		assertEquals(new ArrayList<>(), normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testNormalizeSystemQueryRequestDTO() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		assertAll(

				// nothing is empty
				() -> {
					final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
					requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureConsumer"), List.of("192.168.4.4"), "IPv4 \n", List.of(requirement), List.of("5.0.0"), List.of("TEST_DEVICE"));
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"), List.of("TemperatureConsumer"), List.of("192.168.4.4"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("TEST_DEVICE"));

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(systemNameNormalizer, times(1)).normalize("TemperatureConsumer");
					verify(versionNormalizer, times(1)).normalize("5.0.0");
					verify(addressNormalizer, times(1)).normalize("192.168.4.4");
					verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
				},

				// everything is empty
				() -> {
					resetUtilitiesMock();
					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(null, List.of(), List.of(), EMPTY, List.of(), List.of(), List.of());
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(null, null, null, null, List.of(), null, null);

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(List.of())), times(4));
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(EMPTY)), times(1));

				},

				// dto is empty
				() -> {
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(null, null, null, null, null, null, null);
					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemQueryRequestDTO(null));
					assertEquals(expected, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRemoveSystemNames() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> toNormalize = new ArrayList<>(3);
		toNormalize.add("TemperatureProvider");
		toNormalize.add(null);
		toNormalize.add("TemperatureConsumer");

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeRemoveSystemNames(toNormalize));
		assertEquals(List.of("TemperatureProvider", "TemperatureConsumer"), normalized);
	}

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeDeviceRequestDTOListNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeDeviceRequestDTOList(null));

		assertEquals("DeviceRequestDTO list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeDeviceRequestDTOListNameNull() {
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeDeviceRequestDTOList(List.of(new DeviceRequestDTO(null, null, null))));

		assertEquals("Device name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeDeviceRequestDTOListNameEmpty() {
		utilitiesMock.when(() -> Utilities.isEmpty("")).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeDeviceRequestDTOList(List.of(new DeviceRequestDTO("", null, null))));

		assertEquals("Device name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceRequestDTOList() {

		assertAll(

				// nothing is empty
				() -> {
					final DeviceRequestDTO dto1 = new DeviceRequestDTO("TEST_DEVICE1", Map.of("indoor", false), List.of("3a:7c:91:ef:2d:b6", "56:3e:c9:7a:11:84"));
					final List<AddressDTO> expectedAddresses = new ArrayList<AddressDTO>(2);
					expectedAddresses.add(new AddressDTO("MAC", "3a:7c:91:ef:2d:b6"));
					expectedAddresses.add(new AddressDTO("MAC", "56:3e:c9:7a:11:84"));
					final NormalizedDeviceRequestDTO expected1 = new NormalizedDeviceRequestDTO("TEST_DEVICE1", Map.of("indoor", false), expectedAddresses);

					final DeviceRequestDTO dto2 = new DeviceRequestDTO("TEST_DEVICE2", Map.of("indoor", true), List.of("3a:7c:91:ef:2d:b7"));
					final NormalizedDeviceRequestDTO expected2 = new NormalizedDeviceRequestDTO("TEST_DEVICE2", Map.of("indoor", true), List.of(new AddressDTO("MAC", "3a:7c:91:ef:2d:b7")));

					when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
					when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
					when(addressValidator.detectType(anyString())).thenReturn(AddressType.MAC);

					final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceRequestDTOList(List.of(dto1, dto2)));
					assertEquals(List.of(expected1, expected2), normalized);
					verify(addressNormalizer, times(3)).normalize(anyString());
					verify(deviceNameNormalizer, times(2)).normalize(anyString());
					utilitiesMock.verify(() -> Utilities.isEmpty("TEST_DEVICE1"));
					utilitiesMock.verify(() -> Utilities.isEmpty("TEST_DEVICE2"));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of("3a:7c:91:ef:2d:b6", "56:3e:c9:7a:11:84")));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of("3a:7c:91:ef:2d:b7")));
				},

				// address is empty
				() -> {

					Mockito.reset(addressNormalizer);
					final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE2", Map.of("indoor", true), List.of());
					final NormalizedDeviceRequestDTO expected = new NormalizedDeviceRequestDTO("TEST_DEVICE2", Map.of("indoor", true), new ArrayList<>());

					when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

					final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceRequestDTOList(List.of(dto)));
					assertEquals(List.of(expected), normalized);
					verify(addressNormalizer, never()).normalize(anyString());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceQueryRequestDTO() {

		assertAll(

				// nothing is null
				() -> {
					when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
					when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

					final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
					requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TEST_DEVICE"), List.of("56:3e:c9:7a:11:84"), "mac ", List.of(requirement));
					final DeviceQueryRequestDTO expected = new DeviceQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("TEST_DEVICE"), List.of("56:3e:c9:7a:11:84"), "MAC", List.of(requirement));

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
					verify(addressNormalizer, times(1)).normalize("56:3e:c9:7a:11:84");
				},

				// everything is null
				() -> {
					final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(null, List.of(), List.of(), EMPTY, List.of());
					final DeviceQueryRequestDTO expected = new DeviceQueryRequestDTO(null, null, null, null, List.of());

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceQueryRequestDTO(dto));
					assertEquals(expected, normalized);
				},

				// dto is null
				() -> {
					final DeviceQueryRequestDTO expected = new DeviceQueryRequestDTO(null, null, null, null, null);

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceQueryRequestDTO(null));
					assertEquals(expected, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceNames() {

		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> deviceNames = new ArrayList<String>(2);
		deviceNames.add("TEST_DEVICE");
		deviceNames.add(EMPTY);

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceNames(deviceNames));
		assertEquals(List.of("TEST_DEVICE"), normalized);
		verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	// SERVICE DEFINITIONS

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateServiceDefinitions() {

		when(serviceDefNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> definitions = new ArrayList<String>(2);
		definitions.add("alertService");
		definitions.add("temperatureManagement");

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeCreateServiceDefinitions(new ServiceDefinitionListRequestDTO(definitions)));
		assertEquals(definitions, normalized);
		verify(serviceDefNameNormalizer, times(2)).normalize(anyString());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRemoveServiceDefinitions() {

		when(serviceDefNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> names = new ArrayList<String>(2);
		names.add("alertService");
		names.add("temperatureManagement");

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeRemoveServiceDefinitions(names));
		assertEquals(names, normalized);
		verify(serviceDefNameNormalizer, times(2)).normalize(anyString());
	}

	// SERVICE INSTANCES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateServiceInstances() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(serviceDefNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(interfaceNormalizer.normalizeInterfaceDTO(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ServiceInstanceInterfaceRequestDTO intf1 = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceInterfaceRequestDTO intf2 = new ServiceInstanceInterfaceRequestDTO("generic_https", "https", "NONE", Map.of("accessPort", 8080));

		// nothing is empty
		final ServiceInstanceRequestDTO dto1 = new ServiceInstanceRequestDTO("AlertProvider1", "alertService", "16.4.3", "2025-11-04T01:53:02Z\n\n", Map.of("indoor", true), List.of(intf1));
		final ServiceInstanceRequestDTO dto1Expected = new ServiceInstanceRequestDTO("AlertProvider1", "alertService", "16.4.3", "2025-11-04T01:53:02Z", Map.of("indoor", true), List.of(intf1));

		// expires at is empty
		final ServiceInstanceRequestDTO dto2 = new ServiceInstanceRequestDTO("AlertProvider2", "alertService", "16.4.0", EMPTY, Map.of("indoor", false), List.of(intf2));
		final ServiceInstanceRequestDTO dto2Expected = new ServiceInstanceRequestDTO("AlertProvider2", "alertService", "16.4.0", "", Map.of("indoor", false), List.of(intf2));

		final List<ServiceInstanceRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeCreateServiceInstances(new ServiceInstanceCreateListRequestDTO(List.of(dto1, dto2))));
		assertEquals(List.of(dto1Expected, dto2Expected), normalized);
		verify(systemNameNormalizer, times(2)).normalize(anyString());
		verify(serviceDefNameNormalizer, times(2)).normalize(anyString());
		verify(versionNormalizer, times(2)).normalize(anyString());
		verify(interfaceNormalizer, times(2)).normalizeInterfaceDTO(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUpdateServiceInstances() {

		when(serviceInstanceIdentifierNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(interfaceNormalizer.normalizeInterfaceDTO(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ServiceInstanceInterfaceRequestDTO intf1 = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceInterfaceRequestDTO intf2 = new ServiceInstanceInterfaceRequestDTO("generic_https", "https", "NONE", Map.of("accessPort", 8080));

		// nothing is empty
		final ServiceInstanceUpdateRequestDTO dto1 = new ServiceInstanceUpdateRequestDTO("AlertProvider1|alertService|16.4.3", "2025-11-04T01:53:02Z\n\n", Map.of("indoor", true), List.of(intf1));
		final ServiceInstanceUpdateRequestDTO dto1Expected = new ServiceInstanceUpdateRequestDTO("AlertProvider1|alertService|16.4.3", "2025-11-04T01:53:02Z", Map.of("indoor", true), List.of(intf1));

		// expires at is empty
		final ServiceInstanceUpdateRequestDTO dto2 = new ServiceInstanceUpdateRequestDTO("AlertProvider2|alertService|16.4.0", EMPTY, Map.of("indoor", false), List.of(intf2));
		final ServiceInstanceUpdateRequestDTO dto2Expected = new ServiceInstanceUpdateRequestDTO("AlertProvider2|alertService|16.4.0", "", Map.of("indoor", false), List.of(intf2));

		final List<ServiceInstanceUpdateRequestDTO> normalized = assertDoesNotThrow(() -> normalizer.normalizeUpdateServiceInstances(new ServiceInstanceUpdateListRequestDTO(List.of(dto1, dto2))));
		assertEquals(List.of(dto1Expected, dto2Expected), normalized);
		verify(serviceInstanceIdentifierNormalizer, times(2)).normalize(anyString());
		verify(interfaceNormalizer, times(2)).normalizeInterfaceDTO(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRemoveServiceInstances() {

		when(serviceInstanceIdentifierNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> original = List.of("AlertProvider1|alertService|16.4.3", "AlertProvider2|alertService|16.4.0");

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeRemoveServiceInstances(original));
		assertEquals(original, normalized);
		verify(serviceInstanceIdentifierNormalizer, times(2)).normalize(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQueryServiceInstances() {

		when(serviceInstanceIdentifierNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(serviceDefNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(interfaceTemplateNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		assertAll(

				// nothing is null
				() -> {
					final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
					metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
					intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("AlertProvider|alertService|16.4.3"),
							List.of("AlertProvider"),
							List.of("alertService"),
							List.of("16.4.3"),
							"\t  2025-11-04T01:53:02Z \n",
							List.of(metadataReq),
							List.of("IPv4  "),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("none \t"));

					final ServiceInstanceQueryRequestDTO expected = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("AlertProvider|alertService|16.4.3"),
							List.of("AlertProvider"),
							List.of("alertService"),
							List.of("16.4.3"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					final ServiceInstanceQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeQueryServiceInstances(dto));
					assertEquals(expected, normalized);
					verify(serviceInstanceIdentifierNormalizer, times(1)).normalize("AlertProvider|alertService|16.4.3");
					verify(systemNameNormalizer, times(1)).normalize("AlertProvider");
					verify(serviceDefNameNormalizer, times(1)).normalize("alertService");
					verify(versionNormalizer, times(1)).normalize("16.4.3");
					verify(interfaceTemplateNameNormalizer, times(1)).normalize("generic_http");
				},

				// everything is null
				() -> {
					resetUtilitiesMock();

					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(null, List.of(), List.of(), List.of(), List.of(), EMPTY, List.of(), List.of(), List.of(), List.of(), List.of());
					final ServiceInstanceQueryRequestDTO expected = new ServiceInstanceQueryRequestDTO(
							null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

					final ServiceInstanceQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeQueryServiceInstances(dto));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(EMPTY)), times(1));
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(List.of())), times(9));
				});
	}

	// INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInterfaceTemplateListRequestDTO() {

		when(interfaceNormalizer.normalizeTemplateDTO(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO("operations", true, "not_empty_string_set", List.of("operation"));
		final List<ServiceInterfaceTemplatePropertyDTO> propRequirementList = List.of(propRequirement);

		final ServiceInterfaceTemplateRequestDTO instance = new ServiceInterfaceTemplateRequestDTO("generic_mqtt", "mqtt", propRequirementList);
		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(instance));

		final ServiceInterfaceTemplateListRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto));
		assertEquals(dto, normalized);
		verify(interfaceNormalizer, times(1)).normalizeTemplateDTO(instance);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInterfaceTemplateQueryRequestDTO() {

		assertAll(

				// nothing is empty
				() -> {
					when(interfaceTemplateNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

					final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_http"), List.of("\tHTTP"));
					final ServiceInterfaceTemplateQueryRequestDTO expected = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_http"), List.of("http"));

					final ServiceInterfaceTemplateQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(interfaceTemplateNameNormalizer, times(1)).normalize("generic_http");
				},

				// everything is empty
				() -> {
					resetUtilitiesMock();
					final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(null, null, null);
					final ServiceInterfaceTemplateQueryRequestDTO expected = new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>());

					final ServiceInterfaceTemplateQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty((List<String>) null), times(2));
				},

				// dto is null
				() -> {
					final ServiceInterfaceTemplateQueryRequestDTO expected = new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>());
					final ServiceInterfaceTemplateQueryRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(null));
					assertEquals(expected, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRemoveInterfaceTemplates() {

		when(interfaceTemplateNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<String> names = List.of("generic_http", "generic_mqtt");

		final List<String> normalized = assertDoesNotThrow(() -> normalizer.normalizeRemoveInterfaceTemplates(names));
		assertEquals(names, normalized);
		verify(interfaceTemplateNameNormalizer, times(2)).normalize(anyString());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	private static void initializeUtilitiesMock() {
		createUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	private void resetUtilitiesMockBeforeEach() {
		resetUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	private void resetUtilitiesMock() {
		if (utilitiesMock != null) {
			utilitiesMock.close();
		}
		createUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	private static void createUtilitiesMock() {
		utilitiesMock = mockStatic(Utilities.class);

		// mock common cases
		utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((List<String>) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
