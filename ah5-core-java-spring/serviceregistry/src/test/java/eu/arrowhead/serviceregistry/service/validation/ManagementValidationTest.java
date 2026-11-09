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
package eu.arrowhead.serviceregistry.service.validation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
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
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.ManagementNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:FileLength")
public class ManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementValidation validator;

	@Mock
	private AddressValidator addressTypeValidator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private VersionValidator versionValidator;

	@Mock
	private InterfaceValidator interfaceValidator;

	@Mock
	private DeviceNameValidator deviceNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private ManagementNormalization normalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	// expected error messages
	private static final String MISSING_PAYLOAD = "Request payload is missing";
	private static final String EMPTY_PAYLOAD = "Request payload is empty";
	private static final String NULL_DEVICE = "Device list contains null element";
	private static final String EMPTY_DEVICE_NAME = "Device name is empty";
	private static final String DUPLICATE_DEVICE_NAME_PREFIX = "Duplicated device name: ";
	private static final String MISSING_ADDRESS_LIST = "At least one device address is needed for every device";
	private static final String MISSING_ADDRESS = "Address is missing";
	private static final String NULL_OR_EMPTY_DEVICE_NAME = "Device name list contains null or empty element";
	private static final String NULL_OR_EMPTY_ADDRESS = "Address list contains null or empty element";
	private static final String INVALID_ADDRESS_TYPE_PREFIX = "Invalid address type: ";
	private static final String NULL_METADATA_REQUIREMENT = "Metadata requirement list contains null element";
	private static final String MISSING_DEVICE_NAME_LIST = "Device name list is missing or empty";
	private static final String EMPTY_SERVICE_DEF_LIST = "Service definition name list is empty";
	private static final String NULL_OR_EMPTY_SERVICE_DEF = "Service definition name list contains null or empty element";
	private static final String DUPLICATED_SERVICE_DEF_NAME_PREFIX = "Duplicated service defitition name: ";
	private static final String MISSING_SERVICE_DEF_NAME_LIST = "Service definition name list is missing or empty";
	private static final String SYSTEM_LIST_NULL_ELEMENT = "System list contains null element";
	private static final String EMPTY_SYSTEM_NAME = "System name is empty";
	private static final String DUPLICATED_SYSTEM_NAME_PREFIX = "Duplicated system name: ";
	private static final String MISSING_ADDRESS_VALUE = "Address value is missing";
	private static final String NO_ADDRESS_PROVIDED = "At least one system address is needed for every system";
	private static final String NULL_OR_EMPTY_SYSTEM_NAME = "System name list contains null or empty element";
	private static final String NULL_OR_EMPTY_VERSION = "Version list contains null or empty element";
	private static final String MISSING_SYSTEM_NAME_LIST = "System name list is missing or empty";
	private static final String EMPTY_SERVICE_DEFINITION = "Service definition name is empty";
	private static final String DUPLICATE_INSTANCE_PREFIX = "Duplicated instance: ";
	private static final String INVALID_EXPIRATION_FORMAT = "Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)";
	private static final String INVALID_EXPIRATION_DATE = "Expiration time is in the past";
	private static final String EMPTY_INTF_LIST = "Service interface list is empty";
	private static final String MISSING_TEMPLATE_NAME = "Interface template name is missing";
	private static final String MISSING_POLICY = "Interface policy is missing";
	private static final String INVALID_POLICY = "Invalid interface policy";
	private static final String MISSING_PROPERTIES = "Interface properties are missing";
	private static final String EMPTY_INSTANCE_ID = "Instance id is empty";
	private static final String DUPLICATED_INSTANCE_ID_PREFIX = "Duplicated instance id: ";
	private static final String EMPTY_INSTANCE_ID_LIST = "Instance id list is empty";
	private static final String MANDATORY_FILTER_MISSING = "One of the following filters must be used: 'instanceIds', 'providerNames', 'serviceDefinitionNames'";
	private static final String NULL_OR_EMPTY_INSTANCE_ID = "Instance id list contains null or empty element";
	private static final String NULL_OR_EMPTY_PROVIDER_NAME = "Provider name list contains null or empty element";
	private static final String INVALID_ALIVES_AT = "Alive time has an invalid time format";
	private static final String NULL_METADATA_REQ = "Metadata requirements list contains null element";
	private static final String NULL_OR_EMPTY_ADDRESS_TYPE = "Address type list contains null or empty element";
	private static final String INVALID_ADDRESS_TYPE_ELEMENT_PREFIX = "Address type list contains invalid element: ";
	private static final String NULL_OR_EMPTY_TEMPTLATE = "Interface template list contains null or empty element";
	private static final String NULL_PROPERTY_REQUIREMENT = "Interface property requirements list contains null element";
	private static final String NULL_OR_EMPTY_POLICY = "Policy list contains null or empty element";
	private static final String INVALID_POLICY_PREFIX = "Policy list contains invalid element: ";
	private static final String NULL_TEMPLATE = "Interface template list contains null element";
	private static final String EMPTY_TEMPLATE_NAME = "Interface template name is empty";
	private static final String DUPLICATED_TEMPLATE_NAME_PREFIX = "Duplicated interface template name: ";
	private static final String EMPTY_TEMPLATE_PROTOCOL = "Interface template protocol is empty";
	private static final String NULL_TEMPLATE_PROPERTY = "Interface template contains null property";
	private static final String EMPTY_TEMPLATE_PROPERTY_NAME = "Interface template property name is empty";
	private static final String INVALID_TEMPLATE_PROPERTY_NAME = "Invalid interface template property name: {}, it should not contain . character";
	private static final String DUPLICATED_TEMPLATE_PROPERTY_NAME_PREFIX = "Duplicated interface template property name: ";
	private static final String EMPTY_VALIDATOR = "Interface template property validator is empty while validator params are defined";
	private static final String NULL_OR_EMPTY_VALIDATOR_PARAMETER = "Interface template property validator parameter list contains null or empty element";
	private static final String EMPTY_INTERFACE_TEMPLATE_NAME = "Interface template name list contains empty element";
	private static final String EMPTY_PROTOCOL = "Interface template protocol list contains empty element";
	private static final String MISSING_TEMPLATE_NAME_LIST = "Interface template name list is missing or empty";
	private static final String NULL_OR_EMPTY_TEMPLATE_NAME = "Interface template name list contains null or empty element";

	//=================================================================================================
	// methods

	// DEVICE

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceEmptyPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(List.of()), "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceNullElement() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(NULL_DEVICE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceEmptyName() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO(EMPTY, Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceDuplicateName() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(new DeviceRequestDTO(" test_device ", Map.of("indoor", false), List.of("02:00:00:00:00:02")));

		when(deviceNameNormalizer.normalize("TEST_DEVICE")).thenReturn("TEST_DEVICE");
		when(deviceNameNormalizer.normalize(" test_device ")).thenReturn("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(DUPLICATE_DEVICE_NAME_PREFIX + "TEST_DEVICE", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceMissingAddresses() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of()));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceMissingAddress() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01", EMPTY)));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDeviceValidateMetadata() {

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

		assertAll(

				// invalid metadata
				() -> {
					final List<DeviceRequestDTO> devicesInvalidMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("in.door", true), List.of("02:00:00:00:00:01")));

					metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true))).thenThrow(new InvalidParameterException("Validation error"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devicesInvalidMetadata), "test origin"));
					assertEquals("Validation error", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)));
				},

				// empty metadata
				() -> {
					final List<DeviceRequestDTO> devicesEmptyMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of(), List.of("02:00:00:00:00:01")));
					final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of(), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
					when(normalizer.normalizeDeviceRequestDTOList(devicesEmptyMetadata)).thenReturn(expected);

					metadataValidationMock.reset();

					final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devicesEmptyMetadata), "test origin"));
					assertEquals(normalized, expected);
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)), never());
					metadataValidationMock.close();
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateDevicesOk() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);

		final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeDeviceRequestDTOList(devices);
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:00:00:00:01");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateDevicesThrowsException() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// update

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceEmptyPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(List.of()), "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceNullElement() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(NULL_DEVICE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceEmptyName() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO(EMPTY, Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceDuplicateName() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(2);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));
		devices.add(new DeviceRequestDTO(" test_device ", Map.of("indoor", false), List.of("02:00:00:00:00:02")));

		when(deviceNameNormalizer.normalize("TEST_DEVICE")).thenReturn("TEST_DEVICE");
		when(deviceNameNormalizer.normalize(" test_device ")).thenReturn("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(DUPLICATE_DEVICE_NAME_PREFIX + "TEST_DEVICE", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceMissingAddresses() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of()));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceMissingAddress() {

		final List<DeviceRequestDTO> devices = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01", EMPTY)));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(MISSING_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpdateDeviceValidateMetadata() {

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

		assertAll(

				// invalid metadata
				() -> {
					final List<DeviceRequestDTO> devicesInvalidMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of("in.door", true), List.of("02:00:00:00:00:01")));

					metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true))).thenThrow(new InvalidParameterException("Validation error"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devicesInvalidMetadata), "test origin"));
					assertEquals("Validation error", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)));
				},

				// empty metadata
				() -> {
					final List<DeviceRequestDTO> devicesEmptyMetadata = List.of(new DeviceRequestDTO("TEST_DEVICE", Map.of(), List.of("02:00:00:00:00:01")));
					final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of(), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
					when(normalizer.normalizeDeviceRequestDTOList(devicesEmptyMetadata)).thenReturn(expected);

					metadataValidationMock.reset();

					final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devicesEmptyMetadata), "test origin"));
					assertEquals(normalized, expected);
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", true)), never());
					metadataValidationMock.close();
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateDevicesOk() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);

		final List<NormalizedDeviceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeDeviceRequestDTOList(devices);
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:00:00:00:01");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateDevicesThrowsException() {

		final List<DeviceRequestDTO> devices = new ArrayList<DeviceRequestDTO>(1);
		devices.add(new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:00:00:00:01")));

		final List<NormalizedDeviceRequestDTO> expected = List.of(new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of(new AddressDTO("MAC", "02:00:00:00:00:01"))));
		when(normalizer.normalizeDeviceRequestDTOList(devices)).thenReturn(expected);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST_DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateDevices(new DeviceListRequestDTO(devices), "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryDevicesMissingName() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of(EMPTY),
				List.of("02:00:00:00:00:01"),
				"MAC",
				List.of(metadataReq));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryDevicesMissingAddress() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST_DEVICE"),
				List.of(EMPTY),
				"MAC",
				List.of(metadataReq));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryDevicesInvalidAddressType() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST_DEVICE"),
				List.of("02:00:00:00:00:01"),
				"MäC",
				List.of(metadataReq));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "MäC", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryDevicesNullMetadataRequirement() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(2);
		requirements.add(metadataReq);
		requirements.add(null);

		utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST_DEVICE"),
				List.of("02:00:00:00:00:01"),
				"MAC",
				requirements);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
		assertEquals(NULL_METADATA_REQUIREMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeQueryDevicesOk() {

		assertAll(

				// dto is null
				() -> {

					final DeviceQueryRequestDTO expected = new DeviceQueryRequestDTO(null, null, null, null, null);
					when(normalizer.normalizeDeviceQueryRequestDTO(null)).thenReturn(expected);

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(null, "test origin"));
					assertEquals(expected, normalized);
				},

				// everything is empty
				() -> {

					final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(null, null, null, null, null);
					when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
					assertEquals(dto, normalized);

				},

				// nothing is empty
				() -> {

					final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
					metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TEST_DEVICE"),
							List.of("02:00:00:00:00:01"),
							"MAC",
							List.of(metadataReq));
					when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
					assertEquals(dto, normalized);
					verify(pageValidator, times(1)).validatePageParameter(new PageDTO(10, 20, "ASC", "id"), Device.SORTABLE_FIELDS_BY, "test origin");
					verify(normalizer, times(1)).normalizeDeviceQueryRequestDTO(dto);
					verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
					verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:00:00:00:01");
				},

				// address type is present, but addresses are empty
				() -> {

					final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
					metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TEST_DEVICE"),
							List.of(),
							"MAC",
							List.of(metadataReq));
					when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);

					final DeviceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));
					assertEquals(dto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeQueryDevicesThrowsException() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final DeviceQueryRequestDTO dto = new DeviceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TEST|DEVICE"),
				List.of("02:00:00:00:00:01"),
				"MAC",
				List.of(metadataReq));

		when(normalizer.normalizeDeviceQueryRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST|DEVICE");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryDevices(dto, "test origin"));

		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveDevicesMissingNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveDevices(List.of(), "test origin"));
		assertEquals(MISSING_DEVICE_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveDevicesMissingName() {

		final List<String> deviceNames = new ArrayList<String>(2);
		deviceNames.add("TEST_DEVICE");
		deviceNames.add(EMPTY);

		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(deviceNames)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveDevices(deviceNames, "test origin"));
		assertEquals(NULL_OR_EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(deviceNames));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveDevicesOk() {

		final List<String> deviceNames = List.of("TEST_DEVICE1\n", "TEST_DEVICE2\n");
		final List<String> expected = List.of("TEST_DEVICE1", "TEST_DEVICE2");

		when(normalizer.normalizeDeviceNames(deviceNames)).thenReturn(expected);

		final List<String> normalized = validator.validateAndNormalizeRemoveDevices(deviceNames, "test origin");
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeDeviceNames(deviceNames);
		verify(deviceNameValidator, times(2)).validateDeviceName(anyString());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveDevicesThrowsExcetion() {

		final List<String> deviceNames = List.of("TEST_DEVICE1\n", "TEST_DEVICE2\n");
		final List<String> expected = List.of("TEST_DEVICE1", "TEST_DEVICE2");

		when(normalizer.normalizeDeviceNames(deviceNames)).thenReturn(expected);
		lenient().doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("TEST_DEVICE2");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveDevices(deviceNames, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE DEFINITION

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsNullDto() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsEmptyList() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(EMPTY_SERVICE_DEF_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsNullOrEmptyName() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of(EMPTY));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_SERVICE_DEF, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceDefinitionsDuplicatedName() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("temperature info", "temperatureInfo"));
		when(serviceDefNameNormalizer.normalize(any())).thenReturn("temperatureInfo");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(DUPLICATED_SERVICE_DEF_NAME_PREFIX + "temperatureInfo", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceDefinitionsOk() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("temperatureInfo", "alertService"));
		final List<String> expected = List.of("temperatureInfo", "alertService");

		when(serviceDefNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeCreateServiceDefinitions(any())).thenReturn(List.of("temperatureInfo", "alertService"));

		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals(expected, normalized);
		verify(normalizer, times(1)).normalizeCreateServiceDefinitions(any());
		verify(serviceDefNameValidator, times(2)).validateServiceDefinitionName(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceDefinitionsThrowsException() {

		final ServiceDefinitionListRequestDTO dto = new ServiceDefinitionListRequestDTO(List.of("temperatureInfo", "alertService"));

		when(serviceDefNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeCreateServiceDefinitions(any())).thenReturn(List.of("temperatureInfo", "alertService"));
		lenient().doThrow(new InvalidParameterException("Validation error")).when(serviceDefNameValidator).validateServiceDefinitionName("alertService");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceDefinitions(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryServiceDefinitions() {

		assertAll(

				// dto is null
				() -> {
					assertDoesNotThrow(() -> validator.validateQueryServiceDefinitions(null, "test origin"));
					verify(pageValidator, never()).validatePageParameter(any(), any(), any());
				},

				// dto is not null
				() -> {
					final PageDTO dto = new PageDTO(10, 20, "ASC", "id");
					assertDoesNotThrow(() -> validator.validateQueryServiceDefinitions(dto, "test origin"));
					verify(pageValidator, times(1)).validatePageParameter(any(), any(), eq("test origin"));
				});
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceDefinitionsMissingNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceDefinitions(List.of(), "test origin"));
		assertEquals(MISSING_SERVICE_DEF_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceDefinitionsMissingName() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceDefinitions(List.of(EMPTY), "test origin"));
		assertEquals(NULL_OR_EMPTY_SERVICE_DEF, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveServiceDefinitionsOk() {

		final List<String> names = List.of("temperatureInfo", "alertService");
		when(normalizer.normalizeRemoveServiceDefinitions(any())).thenReturn(names);

		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin"));
		assertEquals(names, normalized);
		verify(normalizer, times(1)).normalizeRemoveServiceDefinitions(any());
		verify(serviceDefNameValidator, times(2)).validateServiceDefinitionName(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveServiceDefinitionsThrowsException() {

		final List<String> names = List.of("temperatureInfo", "alertService");
		when(normalizer.normalizeRemoveServiceDefinitions(any())).thenReturn(names);
		lenient().doThrow(new InvalidParameterException("Validation error")).when(serviceDefNameValidator).validateServiceDefinitionName("alertService");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceDefinitions(names, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SYSTEM

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsEmptyPayload() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsNullElement() {

		final SystemRequestDTO element = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> listWithNull = new ArrayList<SystemRequestDTO>(2);
		listWithNull.add(element);
		listWithNull.add(null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(listWithNull);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(SYSTEM_LIST_NULL_ELEMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsEmptyName() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(new SystemRequestDTO(EMPTY, Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsDuplicatedName() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("Temperature Provider", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenReturn("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(DUPLICATED_SYSTEM_NAME_PREFIX + "TemperatureProvider", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsMissingAddress() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com", EMPTY), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(MISSING_ADDRESS_VALUE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsNoAddressProvided() {
		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(), null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals(NO_ADDRESS_PROVIDED, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateSystemsInvalidMetadata() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("in.door", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false))).thenThrow(new InvalidParameterException("Validation error"));
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false)));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateSystemsOk() {

		// not empty metadata, not empty addresses
		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("192.168.20.21"), EMPTY);
		// empty metadata, empty addresses
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final NormalizedSystemRequestDTO expected1 = new NormalizedSystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.20.21")), null);
		final NormalizedSystemRequestDTO expected2 = new NormalizedSystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(expected1, expected2));

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateSystems(new SystemListRequestDTO(List.of(element1, element2)), "test origin"));
		assertEquals(List.of(expected1, expected2), normalized);
		verify(systemNameValidator, times(2)).validateSystemName(anyString());
		verify(versionValidator, times(2)).validateNormalizedVersion(anyString());
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(any(), anyString());
		verify(deviceNameValidator, times(1)).validateDeviceName(anyString());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(any()), times(1));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateSystemsThrowsException() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final NormalizedSystemRequestDTO normalizedSystem = new NormalizedSystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.10.20")), "TEST_DEVICE");

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(normalizedSystem));
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// update

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsEmptyPayload() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsNullElement() {

		final SystemRequestDTO element = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> listWithNull = new ArrayList<SystemRequestDTO>(2);
		listWithNull.add(element);
		listWithNull.add(null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(listWithNull);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(SYSTEM_LIST_NULL_ELEMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsEmptyName() {

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(new SystemRequestDTO(EMPTY, Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsDuplicatedName() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("Temperature Provider", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenReturn("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(DUPLICATED_SYSTEM_NAME_PREFIX + "TemperatureProvider", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsMissingAddress() {

		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of("indoor", true), "1.1.0", List.of("greenhouse.com", EMPTY), "TEST_DEVICE");
		final List<SystemRequestDTO> systemList = new ArrayList<SystemRequestDTO>(2);
		systemList.add(element1);
		systemList.add(element2);
		final SystemListRequestDTO dto = new SystemListRequestDTO(systemList);

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(MISSING_ADDRESS_VALUE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsNoAddressProvided() {
		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(), null);
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals(NO_ADDRESS_PROVIDED, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateSystemsInvalidMetadata() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperatureProvider1", Map.of("in.door", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false))).thenThrow(new InvalidParameterException("Validation error"));
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in.door", false)));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateSystemsOk() {

		// not empty metadata, not empty addresses
		final SystemRequestDTO element1 = new SystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of("192.168.20.21"), EMPTY);
		// empty metadata, empty addresses
		final SystemRequestDTO element2 = new SystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final NormalizedSystemRequestDTO expected1 = new NormalizedSystemRequestDTO("TemperatureProvider1", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.20.21")), null);
		final NormalizedSystemRequestDTO expected2 = new NormalizedSystemRequestDTO("TemperatureProvider2", Map.of(), "1.1.0", List.of(), "TEST_DEVICE1");

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(expected1, expected2));

		final List<NormalizedSystemRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateSystems(new SystemListRequestDTO(List.of(element1, element2)), "test origin"));
		assertEquals(List.of(expected1, expected2), normalized);
		verify(systemNameValidator, times(2)).validateSystemName(anyString());
		verify(versionValidator, times(2)).validateNormalizedVersion(anyString());
		verify(addressTypeValidator, times(1)).validateNormalizedAddress(any(), anyString());
		verify(deviceNameValidator, times(1)).validateDeviceName(anyString());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(any()), times(1));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUpdateSystemsThrowsException() {

		final SystemRequestDTO system = new SystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of("192.168.10.20"), "TEST_DEVICE");
		final NormalizedSystemRequestDTO normalizedSystem = new NormalizedSystemRequestDTO("TemperätureProvider", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.10.20")), "TEST_DEVICE");

		final SystemListRequestDTO dto = new SystemListRequestDTO(List.of(system));

		when(systemNameNormalizer.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(normalizer.normalizeSystemRequestDTOs(any())).thenReturn(List.of(normalizedSystem));
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateSystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQuerySystemsNullOrEmptySystemName() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of(EMPTY),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQuerySystemsNullOrEmptyAddress() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of(EMPTY),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQuerySystemsInvalidAddressType() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV5",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "IPV5", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEnumValue("IPV5", AddressType.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQuerySystemsNullMetadataRequirement() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(2);
		requirements.add(requirement);
		requirements.add(null);
		utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				requirements,
				List.of("5.0.0"),
				List.of("TEST_DEVICE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_METADATA_REQUIREMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQuerySystemsNullOrEmptyVersion() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of(EMPTY),
				List.of("TEST_DEVICE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_VERSION, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQuerySystemsNullOrEmptyDeviceName() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of(EMPTY));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_DEVICE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeQuerySystemsOk() {

		Assertions.assertAll(

				// nothing is null
				() -> {
					final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
					requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider1"),
							List.of("192.168.4.1"),
							"IPV4",
							List.of(requirement),
							List.of("5.0.0"),
							List.of("TEST_DEVICE"));

					when(normalizer.normalizeSystemQueryRequestDTO(dto)).then(invocation -> invocation.getArgument(0));

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
					verify(pageValidator, times(1)).validatePageParameter(any(), any(), eq("test origin"));
					verify(normalizer, times(1)).normalizeSystemQueryRequestDTO(dto);
					verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider1");
					verify(addressTypeValidator, times(1)).validateNormalizedAddress(AddressType.IPV4, "192.168.4.1");
					verify(versionValidator, times(1)).validateNormalizedVersion("5.0.0");
					verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
					assertEquals(dto, normalized);
				},

				// dto is null
				() -> {
					final SystemQueryRequestDTO expected = new SystemQueryRequestDTO(null, null, null, null, null, null, null);
					when(normalizer.normalizeSystemQueryRequestDTO(null)).thenReturn(expected);

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(null, "test origin"));
					assertEquals(expected, normalized);
				},

				// everything is null
				() -> {
					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(null, null, null, null, null, null, null);
					when(normalizer.normalizeSystemQueryRequestDTO(dto)).thenReturn(dto);

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
					assertEquals(dto, normalized);
				},

				// address type is present, but addresses are empty
				() -> {
					final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(null, null, null, "IPV4", null, null, null);
					when(normalizer.normalizeSystemQueryRequestDTO(dto)).thenReturn(dto);

					final SystemQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
					assertEquals(dto, normalized);
				});

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeQuerySystemsThrowsException() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemQueryRequestDTO dto = new SystemQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperätureProvider1"),
				List.of("192.168.4.1"),
				"IPV4",
				List.of(requirement),
				List.of("5.0.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemQueryRequestDTO(dto)).then(invocation -> invocation.getArgument(0));
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider1");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuerySystems(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveSystemsEmptyNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveSystems(List.of(), "test origin"));
		assertEquals(MISSING_SYSTEM_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveSystemsNullOrEmptySystemName() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveSystems(List.of(EMPTY), "test origin"));
		assertEquals(NULL_OR_EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveSystemsOk() {

		when(normalizer.normalizeRemoveSystemNames(any())).thenAnswer(invocation -> invocation.getArgument(0));
		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRemoveSystems(List.of("TemperatureProvider1", "TemperatureProvider2"), "test origin"));

		assertEquals(List.of("TemperatureProvider1", "TemperatureProvider2"), normalized);
		verify(normalizer, times(1)).normalizeRemoveSystemNames(any());
		verify(systemNameValidator, times(2)).validateSystemName(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveSystemsThrowsException() {

		when(normalizer.normalizeRemoveSystemNames(any())).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperätureProvider2");
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveSystems(List.of("TemperatureProvider1", "TemperätureProvider2"), "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// SERVICE INTSTANCE

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceInstancesMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceInstancesEmptyPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(new ServiceInstanceCreateListRequestDTO(List.of()), "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceInstancesEmptySystemName() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
				EMPTY,
				"temperatureInfo",
				"1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
		assertEquals(EMPTY_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateServiceInstancesEmptyServiceDefinition() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
				"TemperatureProvider",
				EMPTY,
				"1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
		assertEquals(EMPTY_SERVICE_DEFINITION, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateCreateServiceInstancesDuplicatedInstance() {

		final ServiceInstanceInterfaceRequestDTO intf1 = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceInterfaceRequestDTO intf2 = new ServiceInstanceInterfaceRequestDTO("generic_https", "https", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceRequestDTO instance1 = new ServiceInstanceRequestDTO(
				"TemperatureProvider",
				"temperatureInfo",
				"1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf1));

		final ServiceInstanceRequestDTO instance2 = new ServiceInstanceRequestDTO(
				"TemperatureProvider",
				"temperatureInfo",
				"1.0.0",
				"2031-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf2));

		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance1, instance2));

		when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
		when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(instance1.expiresAt())).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(instance2.expiresAt())).thenReturn(ZonedDateTime.of(2031, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
		assertEquals(DUPLICATE_INSTANCE_PREFIX + "TemperatureProvider|temperatureInfo|1.0.0", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateCreateServiceInstancesInvalidExpiration() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		assertAll(

				// invalid format
				() -> {

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"2030.11.04.",
							Map.of("indoor", true),
							List.of(intf));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

					when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
					when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
					when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030.11.04.")).thenThrow(DateTimeException.class);

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(INVALID_EXPIRATION_FORMAT, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// invalid time
				() -> {

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"1990-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intf));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

					when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
					when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
					when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("1990-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(1990, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(INVALID_EXPIRATION_DATE, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateCreateServiceInstancesEmptyServiceInterfaceList() {

		final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
				"TemperatureProvider",
				"temperatureInfo",
				"1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of());

		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

		when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
		when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
		assertEquals(EMPTY_INTF_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateCreateServiceInstancesInvalidInterface() {

		when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
		when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

		assertAll(

				// missing template name
				() -> {
					final ServiceInstanceInterfaceRequestDTO intfMissingName = new ServiceInstanceInterfaceRequestDTO(EMPTY, "http", "NONE", Map.of("accessPort", 8080));

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intfMissingName));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(MISSING_TEMPLATE_NAME, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// missing policy
				() -> {
					final ServiceInstanceInterfaceRequestDTO intfMissingPolicy = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", EMPTY, Map.of("accessPort", 8080));

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intfMissingPolicy));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(MISSING_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// invalid policy
				() -> {
					final ServiceInstanceInterfaceRequestDTO invalidPolicy = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "MAGIC_TOKEN_AUTH", Map.of("accessPort", 8080));

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(invalidPolicy));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(INVALID_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// missing properties
				() -> {
					final ServiceInstanceInterfaceRequestDTO intfMissingProperties = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of());

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intfMissingProperties));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(MISSING_PROPERTIES, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeCreateServiceInstancesOk() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
		when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");

		assertAll(

				// nothing is empty
				() -> {
					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intf));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));
					final List<ServiceInstanceRequestDTO> expected = List.of(instance);
					when(normalizer.normalizeCreateServiceInstances(dto)).thenReturn(expected);
					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

					final List<ServiceInstanceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(expected, normalized);
					verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
					verify(serviceDefNameValidator, times(1)).validateServiceDefinitionName("temperatureInfo");
					verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
					verify(interfaceValidator, times(1)).validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(intf));
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("indoor", true)));
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("accessPort", 8080)));
					metadataValidationMock.close();
				},

				// empty expires at and metadata
				() -> {
					resetUtilitiesMock();

					final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
							"TemperatureProvider",
							"temperatureInfo",
							"1.0.0",
							EMPTY,
							Map.of(),
							List.of(intf));

					final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));
					final List<ServiceInstanceRequestDTO> expected = List.of(instance);
					when(normalizer.normalizeCreateServiceInstances(dto)).thenReturn(expected);
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

					final List<ServiceInstanceRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime(anyString()), never());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(any()), times(1)); // only interface properties
					metadataValidationMock.close();
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeCreateServiceInstancesThrowsException() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
				"TemperatureProvider",
				"temperatureInfo",
				"1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));
		final List<ServiceInstanceRequestDTO> expected = List.of(instance);
		when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
		when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
		when(normalizer.normalizeCreateServiceInstances(dto)).thenReturn(expected);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeCreateServiceInstancesNotOfferablePolicy() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "TRANSLATION_BRIDGE_TOKEN_AUTH", Map.of("accessPort", 8080));

		final ServiceInstanceRequestDTO instance = new ServiceInstanceRequestDTO(
				"TemperatureProvider",
				"temperatureInfo",
				"1.0.0",
				null,
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceCreateListRequestDTO dto = new ServiceInstanceCreateListRequestDTO(List.of(instance));
		final List<ServiceInstanceRequestDTO> expected = List.of(instance);
		when(systemNameNormalizer.normalize("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(serviceDefNameNormalizer.normalize("temperatureInfo")).thenReturn("temperatureInfo");
		when(versionNormalizer.normalize("1.0.0")).thenReturn("1.0.0");
		utilitiesMock.when(() -> Utilities.isEnumValue("TRANSLATION_BRIDGE_TOKEN_AUTH", ServiceInterfacePolicy.class)).thenReturn(true);
		when(normalizer.normalizeCreateServiceInstances(dto)).thenReturn(expected);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateServiceInstances(dto, "test origin"));
		assertEquals("Invalid interface policy: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// update

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateServiceInstancesMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateServiceInstancesEmptyPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(new ServiceInstanceUpdateListRequestDTO(List.of()), "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUpdateServiceInstancesEmptyInstanceId() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
				EMPTY,
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
		assertEquals(EMPTY_INSTANCE_ID, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateUpdateServiceInstancesDuplicatedInstanceId() {

		final ServiceInstanceInterfaceRequestDTO intf1 = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		final ServiceInstanceInterfaceRequestDTO intf2 = new ServiceInstanceInterfaceRequestDTO("generic_https", "https", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceUpdateRequestDTO instance1 = new ServiceInstanceUpdateRequestDTO(
				"TemperatureProvider|temperatureInfo|1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf1));

		final ServiceInstanceUpdateRequestDTO instance2 = new ServiceInstanceUpdateRequestDTO(
				"TemperatureProvider|temperatureInfo|1.0.0",
				"2031-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf2));

		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance1, instance2));

		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(instance1.expiresAt())).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(instance2.expiresAt())).thenReturn(ZonedDateTime.of(2031, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
		assertEquals(DUPLICATED_INSTANCE_ID_PREFIX + "TemperatureProvider|temperatureInfo|1.0.0", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateUpdateServiceInstancesInvalidExpiration() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));
		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");

		assertAll(

				// invalid format
				() -> {

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"2030.11.04.",
							Map.of("indoor", true),
							List.of(intf));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030.11.04.")).thenThrow(DateTimeException.class);

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(INVALID_EXPIRATION_FORMAT, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// invalid time
				() -> {

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"1990-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intf));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("1990-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(1990, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(INVALID_EXPIRATION_DATE, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateUpdateServiceInstancesEmptyServiceInterfaceList() {

		final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
				"TemperatureProvider|temperatureInfo|1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of());

		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));
		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");

		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
		assertEquals(EMPTY_INTF_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateUpdateServiceInstancesInvalidInterface() {

		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

		assertAll(

				// missing template name
				() -> {
					final ServiceInstanceInterfaceRequestDTO intfMissingName = new ServiceInstanceInterfaceRequestDTO(EMPTY, "http", "NONE", Map.of("accessPort", 8080));

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intfMissingName));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(MISSING_TEMPLATE_NAME, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// missing policy
				() -> {
					final ServiceInstanceInterfaceRequestDTO intfMissingPolicy = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", EMPTY, Map.of("accessPort", 8080));

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intfMissingPolicy));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(MISSING_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// invalid policy
				() -> {
					final ServiceInstanceInterfaceRequestDTO invalidPolicy = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "MAGIC_TOKEN_AUTH", Map.of("accessPort", 8080));

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(invalidPolicy));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(INVALID_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// missing properties
				() -> {
					final ServiceInstanceInterfaceRequestDTO intfMissingProperties = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of());

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intfMissingProperties));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(MISSING_PROPERTIES, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeUpdateServiceInstancesOk() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");

		assertAll(

				// nothing is empty
				() -> {
					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							"2030-11-04T01:53:02Z",
							Map.of("indoor", true),
							List.of(intf));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));
					final List<ServiceInstanceUpdateRequestDTO> expected = List.of(instance);
					when(normalizer.normalizeUpdateServiceInstances(dto)).thenReturn(expected);
					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

					final List<ServiceInstanceUpdateRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(expected, normalized);
					verify(serviceInstanceIdentifierValidator, times(1)).validateServiceInstanceIdentifier("TemperatureProvider|temperatureInfo|1.0.0");
					verify(interfaceValidator, times(1)).validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(intf));
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("indoor", true)));
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("accessPort", 8080)));
					metadataValidationMock.close();
				},

				// empty expires at and metadata
				() -> {
					resetUtilitiesMock();

					final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
							"TemperatureProvider|temperatureInfo|1.0.0",
							EMPTY,
							Map.of(),
							List.of(intf));

					final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));
					final List<ServiceInstanceUpdateRequestDTO> expected = List.of(instance);
					when(normalizer.normalizeUpdateServiceInstances(dto)).thenReturn(expected);
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

					final List<ServiceInstanceUpdateRequestDTO> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime(anyString()), never());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(any()), times(1)); // only interface properties
					metadataValidationMock.close();
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeUpdateServiceInstancesThrowsException() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
				"TemperatureProvider|temperatureInfo|1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));
		final List<ServiceInstanceUpdateRequestDTO> expected = List.of(instance);
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");
		when(normalizer.normalizeUpdateServiceInstances(dto)).thenReturn(expected);
		doThrow(new InvalidParameterException("Validation error")).when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TemperatureProvider|temperatureInfo|1.0.0");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeUpdateServiceInstancesNotOfferablePolicy() {

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "TRANSLATION_BRIDGE_TOKEN_AUTH", Map.of("accessPort", 8080));

		final ServiceInstanceUpdateRequestDTO instance = new ServiceInstanceUpdateRequestDTO(
				"TemperatureProvider|temperatureInfo|1.0.0",
				"2030-11-04T01:53:02Z",
				Map.of("indoor", true),
				List.of(intf));

		final ServiceInstanceUpdateListRequestDTO dto = new ServiceInstanceUpdateListRequestDTO(List.of(instance));
		final List<ServiceInstanceUpdateRequestDTO> expected = List.of(instance);
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2030-11-04T01:53:02Z")).thenReturn(ZonedDateTime.of(2030, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
		utilitiesMock.when(() -> Utilities.isEnumValue("TRANSLATION_BRIDGE_TOKEN_AUTH", ServiceInterfacePolicy.class)).thenReturn(true);
		when(serviceInstanceIdentifierNormalizer.normalize("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");
		when(normalizer.normalizeUpdateServiceInstances(dto)).thenReturn(expected);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUpdateServiceInstances(dto, "test origin"));
		assertEquals("Invalid interface policy: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceInstancesEmptyInstanceIds() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceInstances(List.of(), "test origin"));
		assertEquals(EMPTY_INSTANCE_ID_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveServiceInstancesOk() {

		final List<String> instanceIds = List.of("TemperatureProvider|temperatureInfo|1.0.0", "TemperatureProvider|temperatureInfo|1.0.1");
		when(normalizer.normalizeRemoveServiceInstances(instanceIds)).thenReturn(instanceIds);

		final List<String> normalized = validator.validateAndNormalizeRemoveServiceInstances(instanceIds, "test origin");
		assertEquals(instanceIds, normalized);
		verify(normalizer, times(1)).normalizeRemoveServiceInstances(instanceIds);
		verify(serviceInstanceIdentifierValidator, times(2)).validateServiceInstanceIdentifier(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveServiceInstancesThrowsException() {

		final List<String> instanceIds = List.of("TemperatureProvider|temperatureInfo|1.0.0", "TemperätureProvider|temperatureInfo|1.0.1");
		when(normalizer.normalizeRemoveServiceInstances(instanceIds)).thenReturn(instanceIds);
		lenient().doThrow(new InvalidParameterException("Validation error")).when(serviceInstanceIdentifierValidator).validateServiceInstanceIdentifier("TemperätureProvider|temperatureInfo|1.0.1");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveServiceInstances(instanceIds, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryServiceInstancesMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesMandatoryFilterMissing() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of(),
				List.of(),
				List.of(),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(MANDATORY_FILTER_MISSING, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullOrEmptyInstanceId() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of(EMPTY),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_INSTANCE_ID, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullOrEmptyProviderName() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of(EMPTY),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_PROVIDER_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullOrEmptyServiceDefinition() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of(EMPTY),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_SERVICE_DEF, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullOrEmptyVersion() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of(EMPTY),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_VERSION, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesInvalidAliveTime() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2029-11-04")).thenThrow(DateTimeException.class);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(INVALID_ALIVES_AT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullMetadataRequirement() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(2);
		requirements.add(metadataReq);
		requirements.add(null);

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				requirements,
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_METADATA_REQ, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesInvalidAddressType() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(

				// null or empty element
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of(EMPTY),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(NULL_OR_EMPTY_ADDRESS_TYPE, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
				},

				// invalid element
				() -> {
					resetUtilitiesMock();
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPv5"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(INVALID_ADDRESS_TYPE_ELEMENT_PREFIX + "IPv5", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEnumValue("IPV5", AddressType.class));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullOrEmptyTemplate() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of(EMPTY),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_TEMPTLATE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of(EMPTY)));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesNullPropertyRequirement() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<MetadataRequirementDTO> intfRequirements = new ArrayList<MetadataRequirementDTO>(2);
		intfRequirements.add(intfReq);
		intfRequirements.add(null);

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				intfRequirements,
				List.of("NONE"));

		utilitiesMock.when(() -> Utilities.containsNull(intfRequirements)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals(NULL_PROPERTY_REQUIREMENT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNull(intfRequirements));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateQueryServiceInstancesInvalidPolicy() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(

				// null or empty element
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of(EMPTY));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(NULL_OR_EMPTY_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// invalid element
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("MAGIC_TOKEN_AUTH"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(INVALID_POLICY_PREFIX + "MAGIC_TOKEN_AUTH", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeQueryServiceInstancesOk() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(

				// nothing is empty
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					when(normalizer.normalizeQueryServiceInstances(dto)).thenReturn(dto);

					final ServiceInstanceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(normalized, dto);
					verify(pageValidator, times(1)).validatePageParameter(any(), any(), eq("test origin"));
					verify(serviceInstanceIdentifierValidator, times(1)).validateServiceInstanceIdentifier("TemperatureProvider|temperatureInfo|1.0.0");
					verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
					verify(serviceDefNameNormalizer, times(1)).normalize("temperatureInfo");
					verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
					verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName("generic_http");
				},

				// almost everything is empty
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							null, List.of(), List.of(), List.of("temperatureInfo"), List.of(), null, List.of(), List.of(), List.of(), List.of(), List.of());
					final ServiceInstanceQueryRequestDTO expected = new ServiceInstanceQueryRequestDTO(
							null, List.of(), List.of(), List.of("temperatureInfo"), List.of(), "", List.of(), List.of(), List.of(), List.of(), List.of());

					when(normalizer.normalizeQueryServiceInstances(dto)).thenReturn(expected);

					final ServiceInstanceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(normalized, expected);
				},

				// only serivce definition is empty
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of(),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					when(normalizer.normalizeQueryServiceInstances(dto)).thenReturn(dto);

					final ServiceInstanceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(normalized, dto);
				},

				// only service instance id is empty
				() -> {
					final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
							new PageDTO(10, 20, "ASC", "id"),
							List.of(),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2029-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					when(normalizer.normalizeQueryServiceInstances(dto)).thenReturn(dto);

					final ServiceInstanceQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
					assertEquals(normalized, dto);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void validateAndNormalizeQueryServiceInstancesThrowsException() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		when(normalizer.normalizeQueryServiceInstances(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void validateAndNormalizeQueryServiceInstancesNotOfferablePolicy() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceQueryRequestDTO dto = new ServiceInstanceQueryRequestDTO(
				new PageDTO(10, 20, "ASC", "id"),
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2029-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("TRANSLATION_BRIDGE_TOKEN_AUTH"));

		when(normalizer.normalizeQueryServiceInstances(dto)).thenReturn(dto);
		utilitiesMock.when(() -> Utilities.isEnumValue("TRANSLATION_BRIDGE_TOKEN_AUTH", ServiceInterfacePolicy.class)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryServiceInstances(dto, "test origin"));
		assertEquals("Invalid interface policy: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// INTERFACE

	// create

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesEmptyPayload() {

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesNullTemplate() {

		final List<ServiceInterfaceTemplateRequestDTO> templates = new ArrayList<ServiceInterfaceTemplateRequestDTO>(1);
		templates.add(null);
		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(templates);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(NULL_TEMPLATE, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesEmptyTemplateName() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO(EMPTY, "http", List.of(propRequirement));
		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_TEMPLATE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesDuplicatedTemplate() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template1 = new ServiceInterfaceTemplateRequestDTO("generic_http", "http1.1", List.of(propRequirement));
		final ServiceInterfaceTemplateRequestDTO template2 = new ServiceInterfaceTemplateRequestDTO("generic_http", "http2.0", List.of());
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template1, template2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(DUPLICATED_TEMPLATE_NAME_PREFIX + "generic_http", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesEmptyProtocol() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", EMPTY, List.of(propRequirement));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_TEMPLATE_PROTOCOL, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesNullProperty() {

		final List<ServiceInterfaceTemplatePropertyDTO> requirements = new ArrayList<ServiceInterfaceTemplatePropertyDTO>(1);
		requirements.add(null);

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", requirements);
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(NULL_TEMPLATE_PROPERTY, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesEmptyPropertyName() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				EMPTY,
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_TEMPLATE_PROPERTY_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesInvalidPropertyName() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"http.operations",
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(INVALID_TEMPLATE_PROPERTY_NAME.replace("{}", "http.operations"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesDuplicatePropertyName() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement1 = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplatePropertyDTO propRequirement2 = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				false,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement1, propRequirement2));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(DUPLICATED_TEMPLATE_PROPERTY_NAME_PREFIX + "generic_http|operations", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesEmptyValidator() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				EMPTY,
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_VALIDATOR, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateCreateInterfaceTemplatesNullOrEmptyValidatorParam() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"not_empty_string_set",
				List.of(EMPTY));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals(NULL_OR_EMPTY_VALIDATOR_PARAMETER, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify((() -> Utilities.containsNullOrEmpty(List.of(EMPTY))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateInterfaceTemplatesOk() {

		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		assertAll(

				// nothing is empty
				() -> {
					final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
							"operations",
							true,
							"not_empty_string_set",
							List.of("operation"));

					final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));

					final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));
					when(normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto)).thenReturn(dto);

					final ServiceInterfaceTemplateListRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
					assertEquals(dto, normalized);
					verify(normalizer, times(1)).normalizeServiceInterfaceTemplateListRequestDTO(dto);
					verify(interfaceValidator, times(1)).validateNormalizedInterfaceTemplates(List.of(template));
				},

				// requirements is empty
				() -> {

					final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of());

					final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));
					when(normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto)).thenReturn(dto);

					final ServiceInterfaceTemplateListRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
					assertEquals(dto, normalized);
				},

				// validator params is empty
				() -> {

					final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
							"operations",
							true,
							"not_empty_string_set",
							List.of());

					final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));

					final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));
					when(normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto)).thenReturn(dto);

					final ServiceInterfaceTemplateListRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
					assertEquals(dto, normalized);
				});

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateInterfaceTemplatesThrowsException() {

		final ServiceInterfaceTemplatePropertyDTO propRequirement = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"not_empty_string_set",
				List.of("operation"));

		final ServiceInterfaceTemplateRequestDTO template = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propRequirement));
		when(interfaceTemplateNameNormalizer.normalize("generic_http")).thenReturn("generic_http");

		final ServiceInterfaceTemplateListRequestDTO dto = new ServiceInterfaceTemplateListRequestDTO(List.of(template));
		when(normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto)).thenReturn(dto);

		doThrow(new InvalidParameterException("Validation error")).when(interfaceValidator).validateNormalizedInterfaceTemplates(List.of(template));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateInterfaceTemplates(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// query

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryInterfaceTemplatesEmptyTemplateName() {

		final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of(EMPTY), List.of("tcp"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_INTERFACE_TEMPLATE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of(EMPTY)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateQueryInterfaceTemplatesNullOrEmptyProtocol() {

		final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_mqtt"), List.of(EMPTY));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin"));
		assertEquals(EMPTY_PROTOCOL, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of(EMPTY)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInterfaceTemplatesOk() {

		assertAll(

				// nothing is null
				() -> {
					final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_mqtt"), List.of("tcp"));
					when(normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto)).thenReturn(dto);

					final ServiceInterfaceTemplateQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin"));
					assertEquals(dto, normalized);
					verify(pageValidator, times(1)).validatePageParameter(any(), any(), eq("test origin"));
					verify(normalizer, times(1)).normalizeServiceInterfaceTemplateQueryRequestDTO(dto);
					verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName("generic_mqtt");
				},

				// everything is null
				() -> {
					final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(null, List.of(), List.of());
					final ServiceInterfaceTemplateQueryRequestDTO expected = new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>());
					when(normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto)).thenReturn(dto);

					final ServiceInterfaceTemplateQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin"));
					assertEquals(expected, normalized);
				},

				// dto is null
				() -> {
					final ServiceInterfaceTemplateQueryRequestDTO expected = new ServiceInterfaceTemplateQueryRequestDTO(null, new ArrayList<>(), new ArrayList<>());
					when(normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(null)).thenReturn(expected);

					final ServiceInterfaceTemplateQueryRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryInterfaceTemplates(null, "test origin"));
					assertEquals(expected, normalized);
				});

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInterfaceTemplatesThrowsException() {

		final ServiceInterfaceTemplateQueryRequestDTO dto = new ServiceInterfaceTemplateQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of("generic_mqtt"), List.of("tcp"));
		when(normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("Validation error")).when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_mqtt");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryInterfaceTemplates(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// remove

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveInterfaceTemplatesMissingTemplateNameList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveInterfaceTemplates(List.of(), "test origin"));
		assertEquals(MISSING_TEMPLATE_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveInterfaceTemplatesNullOrEmptyTemplateName() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveInterfaceTemplates(List.of(EMPTY), "test origin"));
		assertEquals(NULL_OR_EMPTY_TEMPLATE_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of(EMPTY)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveInterfaceTemplatesOk() {

		final List<String> templateNames = List.of("generic_mqtt", "generic_http");
		when(normalizer.normalizeRemoveInterfaceTemplates(templateNames)).thenReturn(templateNames);

		final List<String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRemoveInterfaceTemplates(templateNames, "test origin"));
		assertEquals(templateNames, normalized);
		verify(normalizer, times(1)).normalizeRemoveInterfaceTemplates(templateNames);
		verify(interfaceTemplateNameValidator, times(2)).validateInterfaceTemplateName(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveInterfaceTemplatesThrowsException() {

		final List<String> templateNames = List.of("generic_mqtt", "generic_http");
		when(normalizer.normalizeRemoveInterfaceTemplates(templateNames)).thenReturn(templateNames);
		lenient().doThrow(new InvalidParameterException("Validation error")).when(interfaceTemplateNameValidator).validateInterfaceTemplateName("generic_http");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemoveInterfaceTemplates(templateNames, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
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
	@SuppressWarnings("checkstyle:magicnumber")
	private static void createUtilitiesMock() {
		utilitiesMock = mockStatic(Utilities.class);

		// mock common cases
		utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((List<String>) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEnumValue("MAC", AddressType.class)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEnumValue("IPV4", AddressType.class)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty((List<String>) List.of(EMPTY))).thenReturn(true);
		utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.of(2025, 8, 4, 1, 53, 2, 0, ZoneId.of("UTC"))); // fictive date of testing
		utilitiesMock.when(() -> Utilities.isEnumValue("NONE", ServiceInterfacePolicy.class)).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
