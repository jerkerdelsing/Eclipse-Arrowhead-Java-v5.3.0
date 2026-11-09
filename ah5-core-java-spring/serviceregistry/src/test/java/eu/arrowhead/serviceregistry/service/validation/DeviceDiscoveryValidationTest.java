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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.DeviceDiscoveryNormalization;

@ExtendWith(MockitoExtension.class)
public class DeviceDiscoveryValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceDiscoveryValidation validator;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private DeviceNameValidator deviceNameValidator;

	@Mock
	private DeviceDiscoveryNormalization normalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	private final NormalizedDeviceRequestDTO testNormalizedDto = new NormalizedDeviceRequestDTO(
			"DUMMY_DEVICE",
			Map.of(),
			List.of(new AddressDTO("MAC", "02:00:5e:00:53:af")));

	// expected error messages
	private static final String MISSING_PAYLOAD = "Request payload is missing";
	private static final String EMPTY_NAME = "Device name is empty";
	private static final String NO_ADDRESS = "At least one device address is needed";
	private static final String MISSING_ADDRESS = "Address is missing";
	private static final String NAME_LIST_CONTAINS_NULL_OR_EMPTY = "Device name list contains null or empty element";
	private static final String ADDRESS_LIST_CONTAINS_NULL_OR_EMPTY = "Address list contains null or empty element";
	private static final String INVALID_ADDRESS_TYPE_PREFIX = "Invalid address type: ";
	private static final String METADATA_CONTAINS_NULL = "Metadata requirement list contains null element";

	//=================================================================================================
	// methods

	// REGISTER DEVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceMissingPayload() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceMissingDeviceName() {

		final DeviceRequestDTO dto = new DeviceRequestDTO(EMPTY, Map.of(), List.of("02:00:5e:00:53:af"));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(EMPTY_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceNoAddress() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of(), List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(NO_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceMissingAddress() {

		final List<String> addresses = new ArrayList<String>();
		addresses.add("02:00:5e:00:53:af");
		addresses.add(EMPTY);

		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of(), addresses);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(MISSING_ADDRESS, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceInvalidMetadata() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of("in door", true), List.of("02:00:5e:00:53:af"));

		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("in door", true))).thenThrow(new InvalidParameterException("Validation error"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("in door", true)));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterDeviceOk() {

		when(normalizer.normalizeDeviceRequestDTO(any())).thenReturn(testNormalizedDto);

		Assertions.assertAll(

				// not empty metadata
				() -> {
					final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:5e:00:53:af"));
					final NormalizedDeviceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
					verify(deviceNameValidator, times(1)).validateDeviceName(anyString());
					verify(addressValidator, times(1)).validateNormalizedAddress(any(), anyString());
					assertEquals(testNormalizedDto, normalized);
				},

				// empty metadata
				() -> {
					final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of(), List.of("02:00:5e:00:53:af"));
					final NormalizedDeviceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
					assertEquals(testNormalizedDto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterDeviceThrowsException() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", true), List.of("02:00:5e:00:53:af"));
		when(normalizer.normalizeDeviceRequestDTO(dto)).thenReturn(testNormalizedDto);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName(anyString());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterDevice(dto, "test origin"));
		assertEquals(ex.getMessage(), "Validation error");
		assertEquals("test origin", ex.getOrigin());
	}

	// LOOKUP DEVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceNameListContainsNullOrEmpty() {

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of(EMPTY), List.of("02:00:5e:00:53:af"), "MAC", List.of());
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(dto.deviceNames())).thenReturn(true);
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupDevice(dto, "test origin"));
		assertEquals(NAME_LIST_CONTAINS_NULL_OR_EMPTY, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(dto.deviceNames()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceAddressListContainsNullOrEmpty() {

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of(EMPTY), "MAC", List.of());
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(dto.addresses())).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupDevice(dto, "test origin"));
		assertEquals(ADDRESS_LIST_CONTAINS_NULL_OR_EMPTY, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(dto.deviceNames()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceAddressTypeInvalid() {

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of("02:00:5e:00:53:af"), "MäC", List.of());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupDevice(dto, "test origin"));
		assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "MäC", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceMetadataContainsNull() {

		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(1);
		requirements.add(null);
		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of("02:00:5e:00:53:af"), "MAC", requirements);
		utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEnumValue(dto.addressType(), AddressType.class)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupDevice(dto, "test origin"));
		assertEquals(METADATA_CONTAINS_NULL, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateLookupDeviceOk() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		Assertions.assertAll(

				// nothing is empty
				() -> {
					final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of("02:00:5e:00:53:af"), "MAC", List.of(requirement));
					utilitiesMock.when(() -> Utilities.isEnumValue(dto.addressType(), AddressType.class)).thenReturn(true);
					when(normalizer.normalizeDeviceLookupRequestDTO(dto)).thenReturn(dto);

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupDevice(dto, "test origin"));
					verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
					verify(addressValidator, times(1)).validateNormalizedAddress(AddressType.MAC, "02:00:5e:00:53:af");
					assertEquals(dto, normalized);
				},

				// dto is null
				() -> {
					final DeviceLookupRequestDTO expected = new DeviceLookupRequestDTO(null, null, null, null);
					when(normalizer.normalizeDeviceLookupRequestDTO(null)).thenReturn(expected);

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupDevice(null, "test origin"));
					assertEquals(expected, normalized);
				},

				// everything is empty
				() -> {
					final DeviceLookupRequestDTO expected = new DeviceLookupRequestDTO(null, null, null, List.of());
					final DeviceLookupRequestDTO dtoWithEmptyLists = new DeviceLookupRequestDTO(List.of(), List.of(), EMPTY, List.of());
					when(normalizer.normalizeDeviceLookupRequestDTO(dtoWithEmptyLists)).thenReturn(expected);

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupDevice(dtoWithEmptyLists, "test origin"));
					assertEquals(expected, normalized);
				},

				// address list is empty, address type is not
				() -> {
					final DeviceLookupRequestDTO expected = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), null, "MAC", List.of(requirement));
					Mockito.reset(addressValidator);
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.isEnumValue("MAC", AddressType.class)).thenReturn(true);
					final DeviceLookupRequestDTO dtoWithEmptyAddressList = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of(), "MAC", List.of(requirement));
					when(normalizer.normalizeDeviceLookupRequestDTO(dtoWithEmptyAddressList)).thenReturn(expected);

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupDevice(dtoWithEmptyAddressList, "test origin"));
					verify(addressValidator, never()).validateNormalizedAddress(any(), anyString());
					assertEquals(normalized, expected);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateLookupDeviceThrowsException() {

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("DEVICE$$$"), List.of("02:00:5e:00:53:af"), "MAC", List.of());
		utilitiesMock.when(() -> Utilities.isEnumValue(dto.addressType(), AddressType.class)).thenReturn(true);
		when(normalizer.normalizeDeviceLookupRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("DEVICE$$$");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupDevice(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// REVOKE DEVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeDeviceEmptyName() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeDevice(EMPTY, "test origin"));
		assertEquals(EMPTY_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateAndNormalizeRevokeDeviceOk() {
		when(normalizer.normalizeDeviceName("TEST_DEVICE")).thenReturn("TEST_DEVICE");

		final String normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRevokeDevice("TEST_DEVICE", "test origin"));
		verify(normalizer, times(1)).normalizeDeviceName("TEST_DEVICE");
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
		assertEquals("TEST_DEVICE", normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateAndNormalizeRevokeDeviceThrowsException() {
		when(normalizer.normalizeDeviceName("DEVICE$$$")).thenReturn("DEVICE$$$");
		doThrow(new InvalidParameterException("Validation error")).when(deviceNameValidator).validateDeviceName("DEVICE$$$");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeDevice("DEVICE$$$", "test origin"));
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
	private static void createUtilitiesMock() {
		utilitiesMock = mockStatic(Utilities.class);

		// mock common cases
		utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((List<String>) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
