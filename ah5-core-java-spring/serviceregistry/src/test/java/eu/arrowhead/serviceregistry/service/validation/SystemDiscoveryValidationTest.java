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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.SystemDiscoveryNormalization;

@ExtendWith(MockitoExtension.class)
public class SystemDiscoveryValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDiscoveryValidation validator;

	@Mock
	private SystemDiscoveryNormalization normalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private DeviceNameValidator deviceNameValidator;

	@Mock
	private VersionValidator versionValidator;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	// expected error messages
	private static final String MISSING_PAYLOAD = "Request payload is missing";
	private static final String MISSING_SYSTEM_NAME = "System name is empty";
	private static final String MISSING_ADDRESS = "Address is missing";
	private static final String AT_LEAST_ONE_ADDRESS_NEEDED = "At least one system address is needed for every system";
	private static final String INVALID_SYS_NAME_LIST = "System name list contains null or empty element";
	private static final String INVALID_ADDRESS_LIST = "Address list contains null or empty element";
	private static final String INVALID_ADDRESS_TYPE_PREFIX = "Invalid address type: ";
	private static final String INVALID_METADATA = "Metadata requirement list contains null element";
	private static final String INVALID_VERSION_LIST = "Version list contains null element";
	private static final String INVALID_DEVICE_LIST = "Device name list contains null or empty element";

	private final NormalizedSystemRequestDTO testNormalizedRequestDto = new NormalizedSystemRequestDTO(
			"DummyProvider",
			Map.of("dummy", true),
			"1.0.0",
			List.of(new AddressDTO("HOSTNAME", "dummy.eu")),
			"DUMMY_DEVICE"); // the actual values don't matter

	private final SystemLookupRequestDTO testNormalizedLookupDto = new SystemLookupRequestDTO(
			List.of("DummyProvider"),
			List.of("192.168.0.3"),
			"IPV4",
			List.of(),
			List.of("1.0.0"),
			List.of("DUMMY_DEVICE")); // the actual values don't matter

	//=================================================================================================
	// methods

	// REGISTER SYSTEM

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemMissingPalyoad() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
			validator.validateAndNormalizeRegisterSystem(null, "test origin");
		});
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemMissingSystemName() {

		// create test dtos
		final SystemRequestDTO dtoWithMissingName = new SystemRequestDTO(EMPTY, Map.of("indoor", true), "1.0.0", List.of(), "TEST_DEVICE");
		final SystemRequestDTO dtoOk = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of(), "TEST_DEVICE");

		assertAll("Validate system name",
				() -> {
					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
						validator.validateAndNormalizeRegisterSystem(dtoWithMissingName, "test origin");
					});
					assertEquals(MISSING_SYSTEM_NAME, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
				},
				() -> {
					when(normalizer.normalizeSystemRequestDTO(dtoOk)).thenReturn(testNormalizedRequestDto);
					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dtoOk, "test origin"));
					assertEquals(testNormalizedRequestDto, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty("TemperatureProvider"));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemValidateAddresses() {

		assertAll("Validate addresses",

				// dto with empty list
				() -> {

					final List<String> emptyList = List.of();
					final SystemRequestDTO dtoWithEmptyList = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", emptyList, "TEST_DEVICE");

					when(normalizer.normalizeSystemRequestDTO(dtoWithEmptyList)).thenReturn(testNormalizedRequestDto);

					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dtoWithEmptyList, "test origin"));
					assertEquals(testNormalizedRequestDto, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
				},

				// dto with empty element containing list
				() -> {

					final List<String> emptyElementList = List.of("192.168.0.1", EMPTY);
					final SystemRequestDTO dtoWithEmptyElement = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", emptyElementList, "TEST_DEVICE");

					resetUtilitiesMock();
					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
						validator.validateAndNormalizeRegisterSystem(dtoWithEmptyElement, "test origin");
					});
					assertEquals(MISSING_ADDRESS, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty("192.168.0.1"));
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemValidateAtLeastOneAddress() {

		assertAll("At least one address is present",

				// nothing is empty
				() -> {
					resetUtilitiesMock();

					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("192.168.0.1"), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedRequestDto);
					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(testDto, "test origin"));

					assertEquals(testNormalizedRequestDto, normalized);
					assertTrue(isEmptyCalledAtLeastOnce("TEST_DEVICE", List.of("192.168.0.1")));

				},

				// device name is empty
				() -> {
					resetUtilitiesMock();

					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("192.168.0.1"), EMPTY);
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedRequestDto);
					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(testDto, "test origin"));

					assertEquals(testNormalizedRequestDto, normalized);
					assertTrue(isEmptyCalledAtLeastOnce(EMPTY, List.of("192.168.0.1")));
				},

				// address list is empty
				() -> {
					resetUtilitiesMock();

					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of(), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(testDto)).thenReturn(testNormalizedRequestDto);
					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(testDto, "test origin"));

					assertEquals(testNormalizedRequestDto, normalized);
					assertTrue(isEmptyCalledAtLeastOnce("TEST_DEVICE", List.of()));

				},

				// everything is empty
				() -> {
					resetUtilitiesMock();
					final SystemRequestDTO testDto = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of(), EMPTY);
					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
						validator.validateAndNormalizeRegisterSystem(testDto, "test origin");
					});
					assertEquals(AT_LEAST_ONE_ADDRESS_NEEDED, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					assertTrue(isEmptyCalledAtLeastOnce(EMPTY, List.of()));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterSystemValidateMetadata() {

		assertAll("Validate metadata",

				// without metadata
				() -> {
					utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);

					// mock metadata validation
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

					final SystemRequestDTO dtoWithoutMetadata = new SystemRequestDTO("TemperatureProvider", Map.of(), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(dtoWithoutMetadata)).thenReturn(testNormalizedRequestDto);

					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dtoWithoutMetadata, "test origin"));

					utilitiesMock.verify(() -> Utilities.isEmpty(Map.of()), atLeastOnce());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of()), never());
					assertEquals(testNormalizedRequestDto, normalized);

					// reset mock
					metadataValidationMock.close();
				},

				// with metadata
				() -> {
					resetUtilitiesMock();

					// mock metadata validation
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);

					final SystemRequestDTO dtoWithMetadata = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
					when(normalizer.normalizeSystemRequestDTO(dtoWithMetadata)).thenReturn(testNormalizedRequestDto);

					final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dtoWithMetadata, "test origin"));

					utilitiesMock.verify(() -> Utilities.isEmpty(Map.of("indoor", true)), atLeastOnce());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("indoor", true)), times(1));
					assertEquals(testNormalizedRequestDto, normalized);

					// reset mock
					metadataValidationMock.close();
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterSystemOk() {

		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), "TEST_DEVICE");
		final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of(new AddressDTO("HOSTNAME", "greenhouse.com")), "TEST_DEVICE");

		when(normalizer.normalizeSystemRequestDTO(dto)).thenReturn(expected);

		final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dto, "test origin"));
		verify(normalizer, times(1)).normalizeSystemRequestDTO(dto);
		verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
		verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
		verify(addressValidator, times(1)).validateNormalizedAddress(AddressType.HOSTNAME, "greenhouse.com");
		verify(deviceNameValidator, times(1)).validateDeviceName("TEST_DEVICE");
		assertEquals(expected, normalized);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterSystemEmptyDeviceName() {

		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), EMPTY);
		final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of(new AddressDTO("HOSTNAME", "greenhouse.com")), EMPTY);

		when(normalizer.normalizeSystemRequestDTO(dto)).thenReturn(expected);

		final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterSystem(dto, "test origin"));
		verify(deviceNameValidator, never()).validateDeviceName("TEST_DEVICE");
		assertEquals(expected, normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRegisterSystemThrowsExeption() {

		final SystemRequestDTO dto = new SystemRequestDTO("TemperatureProvider", Map.of("indoor", true), "1.0.0", List.of("greenhouse.com"), EMPTY);

		when(normalizer.normalizeSystemRequestDTO(dto)).thenReturn(testNormalizedRequestDto);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName(anyString());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
			validator.validateAndNormalizeRegisterSystem(dto, "test origin");
		});

		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// LOOKUP SYSTEM

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemSystemListContainsNullOrEmpty() {

		// dtos for the possible cases
		final SystemLookupRequestDTO dtoEmptyList = new SystemLookupRequestDTO(
				List.of(),
				List.of("192.168.6.6"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoContainsNullOrEmpty = new SystemLookupRequestDTO(
				List.of("TemperatureProvider", EMPTY),
				List.of("192.168.6.6"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoOk = new SystemLookupRequestDTO(
				List.of("TemperatureProvider", "TemperatureConsumer"),
				List.of("192.168.6.6"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);

		Assertions.assertAll(

				// list is empty
				() -> {
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoEmptyList, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
					assertEquals(testNormalizedLookupDto, normalized);
				},

				// list contains null or empty
				() -> {
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.containsNullOrEmpty(List.of("TemperatureProvider", EMPTY))).thenReturn(true);

					final InvalidParameterException ex = assertThrows(
							InvalidParameterException.class,
							() -> validator.validateAndNormalizeLookupSystem(dtoContainsNullOrEmpty, "test origin"));
					assertEquals(INVALID_SYS_NAME_LIST, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of("TemperatureProvider", EMPTY)));
				},

				// list does not contain null or empty
				() -> {
					resetUtilitiesMock();
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoOk, "test origin"));
					utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of("TemperatureProvider", "TemperatureConsumer")));
					assertEquals(testNormalizedLookupDto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemAddressListContainsNullOrEmpty() {

		// dtos for the possible cases
		final SystemLookupRequestDTO dtoEmptyList = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of(),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoContainsNullOrEmpty = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6", EMPTY),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoOk = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6", "192.168.6.7"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);

		Assertions.assertAll(

				// list is empty
				() -> {
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoEmptyList, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
					assertEquals(testNormalizedLookupDto, normalized);
				},

				// list contains null or empty
				() -> {
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.containsNullOrEmpty(List.of("192.168.6.6", EMPTY))).thenReturn(true);

					final InvalidParameterException ex = assertThrows(
							InvalidParameterException.class,
							() -> validator.validateAndNormalizeLookupSystem(dtoContainsNullOrEmpty, "test origin"));
					assertEquals(INVALID_ADDRESS_LIST, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of("192.168.6.6", EMPTY)));
				},

				// list does not contain null or empty
				() -> {
					resetUtilitiesMock();

					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoOk, "test origin"));
					utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of("192.168.6.6", "192.168.6.7")));
					assertEquals(testNormalizedLookupDto, normalized);
				});

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemInvalidAddressType() {

		// dtos for the possible cases
		final SystemLookupRequestDTO dtoEmpty = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.0.1"),
				EMPTY,
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoInvalid = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6"),
				"IPV8",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoValid = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6", "192.168.6.7"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);

		Assertions.assertAll(

				// empty address type
				() -> {
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoEmpty, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
					utilitiesMock.verify(() -> Utilities.isEnumValue(EMPTY, AddressType.class), never());
					assertEquals(testNormalizedLookupDto, normalized);
				},

				// invalid address type
				() -> {
					resetUtilitiesMock();

					final InvalidParameterException ex = assertThrows(
							InvalidParameterException.class,
							() -> validator.validateAndNormalizeLookupSystem(dtoInvalid, "test origin"));
					assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "IPV8", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEnumValue("IPV8", AddressType.class));
				},

				// valid address type
				() -> {
					resetUtilitiesMock();
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoValid, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEnumValue("IPV4", AddressType.class));
					assertEquals(testNormalizedLookupDto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testLookupSystemMetadataContainsNull() {

		// dtos for the possible cases
		final SystemLookupRequestDTO dtoEmpty = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.0.1"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final MetadataRequirementDTO req1 = new MetadataRequirementDTO();
		req1.put("priority", Map.of("op", "LESS_THAN", "value", 10));
		final List<MetadataRequirementDTO> requirements = new ArrayList<MetadataRequirementDTO>(2);
		requirements.add(req1);
		requirements.add(null);
		final SystemLookupRequestDTO dtoContainsNull = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6"),
				"IPV4",
				requirements,
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		final MetadataRequirementDTO req2 = new MetadataRequirementDTO();
		req2.put("margin-of-error", Map.of("op", "LESS_THAN_OR_EQUALS_TO", "value", 0.5));
		final SystemLookupRequestDTO dtoValid = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6", "192.168.6.7"),
				"IPV4",
				List.of(req1, req2),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);

		Assertions.assertAll(

				// empty metadata
				() -> {
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoEmpty, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
					utilitiesMock.verify(() -> Utilities.containsNull(List.of()), never());
					assertEquals(testNormalizedLookupDto, normalized);
				},

				// metadata contains null
				() -> {
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.containsNull(requirements)).thenReturn(true);

					final InvalidParameterException ex = assertThrows(
							InvalidParameterException.class,
							() -> validator.validateAndNormalizeLookupSystem(dtoContainsNull, "test origin"));
					assertEquals(INVALID_METADATA, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(requirements));
					utilitiesMock.verify(() -> Utilities.containsNull(requirements));
				},

				// valid metadata
				() -> {
					resetUtilitiesMock();

					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoValid, "test origin"));
					utilitiesMock.verify(() -> Utilities.containsNull(List.of(req1, req2)));
					assertEquals(testNormalizedLookupDto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemVersionListContainsNull() {

		// dtos for the possible cases
		final SystemLookupRequestDTO dtoEmpty = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.0.1"),
				"IPV4",
				List.of(),
				List.of(),
				List.of("TEST_DEVICE"));

		final List<String> versions = new ArrayList<String>(2);
		versions.add("2.0.2");
		versions.add(null);
		final SystemLookupRequestDTO dtoContainsNull = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6"),
				"IPV4",
				List.of(),
				versions,
				List.of("TEST_DEVICE"));

		final SystemLookupRequestDTO dtoValid = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6", "192.168.6.7"),
				"IPV4",
				List.of(),
				List.of("1.0.0", "1.1.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);

		Assertions.assertAll(

				// empty version list
				() -> {
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoEmpty, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
					utilitiesMock.verify(() -> Utilities.containsNull(List.of()), never());
					assertEquals(testNormalizedLookupDto, normalized);
				},

				// version list contains null
				() -> {
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.containsNull(versions)).thenReturn(true);

					final InvalidParameterException ex = assertThrows(
							InvalidParameterException.class,
							() -> validator.validateAndNormalizeLookupSystem(dtoContainsNull, "test origin"));
					assertEquals(INVALID_VERSION_LIST, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(versions));
					utilitiesMock.verify(() -> Utilities.containsNull(versions));
				},

				// version list does not contain null
				() -> {
					resetUtilitiesMock();

					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoValid, "test origin"));
					utilitiesMock.verify(() -> Utilities.containsNull(List.of("1.0.0", "1.1.0")));
					assertEquals(testNormalizedLookupDto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupSystemDeviceListContainsNullOrEmpty() {

		// dtos for the possible cases
		final SystemLookupRequestDTO dtoEmpty = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.0.1"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of());

		final List<String> deviceNames = new ArrayList<String>(2);
		deviceNames.add("TEST_DEVICE");
		deviceNames.add(null);
		final SystemLookupRequestDTO dtoContainsNull = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				deviceNames);

		final SystemLookupRequestDTO dtoValid = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.6.6", "192.168.6.7"),
				"IPV4",
				List.of(),
				List.of("1.0.0"),
				List.of("TEST_DEVICE1", "TEST_DEVICE2"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);

		Assertions.assertAll(

				// empty list
				() -> {
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoEmpty, "test origin"));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), atLeastOnce());
					utilitiesMock.verify(() -> Utilities.containsNull(List.of()), never());
					assertEquals(testNormalizedLookupDto, normalized);
				},

				// list contains null or empty
				() -> {
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.containsNullOrEmpty(deviceNames)).thenReturn(true);

					final InvalidParameterException ex = assertThrows(
							InvalidParameterException.class,
							() -> validator.validateAndNormalizeLookupSystem(dtoContainsNull, "test origin"));
					assertEquals(INVALID_DEVICE_LIST, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(deviceNames));
					utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(deviceNames));
				},

				// list does not contain null or empty
				() -> {
					resetUtilitiesMock();

					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(dtoValid, "test origin"));
					utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(List.of("TEST_DEVICE1", "TEST_DEVICE2")));
					assertEquals(testNormalizedLookupDto, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeLookupSystemOk() {

		// metadata for the dto
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		Assertions.assertAll(

				// valid dto
				() -> {
					final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(List.of("TemperatureProvider"), List.of("192.168.0.1"), "IPV4", List.of(requirement), List.of("1.0.0"), List.of("TEST_DEVICE"));
					when(normalizer.normalizeSystemLookupRequestDTO(dto)).thenReturn(testNormalizedLookupDto);
					validator.validateAndNormalizeLookupSystem(dto, "test origin");

					verify(normalizer, times(1)).normalizeSystemLookupRequestDTO(dto);
					verify(systemNameValidator, times(1)).validateSystemName(anyString());
					verify(addressValidator, times(1)).validateNormalizedAddress(any(), anyString());
					verify(versionValidator, times(1)).validateNormalizedVersion(anyString());
					verify(deviceNameValidator, times(1)).validateDeviceName(anyString());
				},

				// null dto
				() -> {
					final SystemLookupRequestDTO expected = new SystemLookupRequestDTO(null, null, null, null, null, null);
					when(normalizer.normalizeSystemLookupRequestDTO(null)).thenReturn(expected);
					final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupSystem(null, "test origin"));
					assertEquals(expected, normalized);
				},

				// address type is empty, should not validate addresses
				() -> {
					resetUtilitiesMock();
					Mockito.reset(addressValidator);
					final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(List.of("TemperatureProvider"), List.of("192.168.0.1"), EMPTY, List.of(requirement), List.of("1.0.0"), List.of("TEST_DEVICE"));
					final SystemLookupRequestDTO expected = new SystemLookupRequestDTO(List.of("TemperatureProvider"), List.of("192.168.0.1"), null, List.of(requirement), List.of("1.0.0"), List.of("TEST_DEVICE"));
					when(normalizer.normalizeSystemLookupRequestDTO(dto)).thenReturn(expected);

					validator.validateAndNormalizeLookupSystem(dto, "test origin");
					verify(addressValidator, never()).validateNormalizedAddress(any(), anyString());
				},

				// address list is empty, should not validate addresses
				() -> {
					resetUtilitiesMock();
					Mockito.reset(addressValidator);
					final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(List.of("TemperatureProvider"), List.of(), "IPV4", List.of(requirement), List.of("1.0.0"), List.of("TEST_DEVICE"));
					final SystemLookupRequestDTO expected = new SystemLookupRequestDTO(List.of("TemperatureProvider"), null, "IPV4", List.of(requirement), List.of("1.0.0"), List.of("TEST_DEVICE"));
					when(normalizer.normalizeSystemLookupRequestDTO(dto)).thenReturn(expected);
					validator.validateAndNormalizeLookupSystem(dto, "test origin");
					verify(addressValidator, never()).validateNormalizedAddress(any(), anyString());
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateAndNormalizeLookupSystemThrowsExeption() {

		// metadata for the dto
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(
				List.of("TemperatureProvider"),
				List.of("192.168.0.1"),
				"IPV4",
				List.of(requirement),
				List.of("1.0.0"),
				List.of("TEST_DEVICE"));

		when(normalizer.normalizeSystemLookupRequestDTO(any())).thenReturn(testNormalizedLookupDto);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName(anyString());

		final InvalidParameterException ex = assertThrows(
				InvalidParameterException.class,
				() -> validator.validateAndNormalizeLookupSystem(dto, "test origin"));

		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// REVOKE SYSTEM

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeSystemEmptySystemName() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeSystem(EMPTY, "test origin"));
		assertEquals(MISSING_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeSystemOk() {
		final String systemName = "TemperatureProvider";
		when(normalizer.normalizeRevokeSystemName(systemName)).thenReturn(systemName);
		validator.validateAndNormalizeRevokeSystem(systemName, "test origin");
		verify(normalizer, times(1)).normalizeRevokeSystemName(systemName);
		verify(systemNameValidator, times(1)).validateSystemName(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeSystemThrowsException() {
		final String systemName = "invalid provider name";
		when(normalizer.normalizeRevokeSystemName(systemName)).thenReturn(systemName);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName(anyString());
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeSystem(systemName, "test origin"));
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
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty((List<String>) null)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEnumValue("IPV4", AddressType.class)).thenReturn(true);
		final List<String> listWithNull = new ArrayList<String>(1);
		listWithNull.add(null);
		utilitiesMock.when(() -> Utilities.containsNull(listWithNull)).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isEmptyCalledAtLeastOnce(final String argString, final List<String> argStringList) {

		boolean calledAtLeastOnce = false;

		try {
			utilitiesMock.verify(() -> Utilities.isEmpty(argString), atLeastOnce());
			calledAtLeastOnce = true;
		} catch (final Exception ex) {
			// intentionally do nothing
		}
		try {
			utilitiesMock.verify(() -> Utilities.isEmpty(argStringList), atLeastOnce());
			calledAtLeastOnce = true;
		} catch (final Exception ex) {
			// intentionally do nothing
		}

		return calledAtLeastOnce;
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
