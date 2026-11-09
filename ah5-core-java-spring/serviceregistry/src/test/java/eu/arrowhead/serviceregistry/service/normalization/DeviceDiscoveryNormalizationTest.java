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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;

@ExtendWith(MockitoExtension.class)
public class DeviceDiscoveryNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceDiscoveryNormalization normalizer;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceRequestDTONullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeDeviceRequestDTO(null));

		assertEquals("DeviceRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceRequestDTONameNull() {
		final DeviceRequestDTO dto = new DeviceRequestDTO(null, null, null);

		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeDeviceRequestDTO(dto));

		assertEquals("DeviceRequestDTO name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceRequestDTONameEmpty() {
		final DeviceRequestDTO dto = new DeviceRequestDTO(" ", null, null);

		utilitiesMock.when(() -> Utilities.isEmpty(" ")).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeDeviceRequestDTO(dto));

		assertEquals("DeviceRequestDTO name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceRequestDTO() {
		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressValidator.detectType(anyString())).thenReturn(AddressType.MAC);

		assertAll(

				// nothing is empty
				() -> {
					final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", false), List.of("3a:7c:91:ef:2d:b6", "56:3e:c9:7a:11:84"));

					final List<AddressDTO> expectedAddresses = new ArrayList<AddressDTO>(2);
					expectedAddresses.add(new AddressDTO("MAC", "3a:7c:91:ef:2d:b6"));
					expectedAddresses.add(new AddressDTO("MAC", "56:3e:c9:7a:11:84"));

					final NormalizedDeviceRequestDTO expected = new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", false), expectedAddresses);

					final NormalizedDeviceRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(addressNormalizer, times(2)).normalize(anyString());
					verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of("3a:7c:91:ef:2d:b6", "56:3e:c9:7a:11:84")));
				},

				// addresses is empty
				() -> {
					final DeviceRequestDTO dto = new DeviceRequestDTO("TEST_DEVICE", Map.of("indoor", false), List.of());

					final NormalizedDeviceRequestDTO expected = new NormalizedDeviceRequestDTO("TEST_DEVICE", Map.of("indoor", false), new ArrayList<>());

					final NormalizedDeviceRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceRequestDTO(dto));
					assertEquals(expected, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testNormalizeDeviceLookupRequestDTO() {

		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		assertAll(

				// nothing is empty
				() -> {

					final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
					requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of("56:3e:c9:7a:11:84"), "mac  ", List.of(requirement));
					final DeviceLookupRequestDTO expected = new DeviceLookupRequestDTO(List.of("TEST_DEVICE"), List.of("56:3e:c9:7a:11:84"), "MAC", List.of(requirement));

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceLookupRequestDTO(dto));
					assertEquals(normalized, expected);
					verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
					verify(addressNormalizer, times(1)).normalize("56:3e:c9:7a:11:84");
				},

				// everything is empty
				() -> {
					final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of(), List.of(), EMPTY, List.of());
					final DeviceLookupRequestDTO expected = new DeviceLookupRequestDTO(null, null, null, List.of());

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceLookupRequestDTO(dto));
					assertEquals(normalized, expected);
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), times(1));
					utilitiesMock.verify(() -> Utilities.isEmpty(List.of()), times(2));
				},

				// dto is null
				() -> {
					final DeviceLookupRequestDTO expected = new DeviceLookupRequestDTO(null, null, null, null);

					final DeviceLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceLookupRequestDTO(null));
					assertEquals(normalized, expected);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDeviceName() {

		when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final String normalized = assertDoesNotThrow(() -> normalizer.normalizeDeviceName("GreenhouseGod"));
		assertEquals("GreenhouseGod", normalized);
		verify(deviceNameNormalizer, times(1)).normalize("GreenhouseGod");
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
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
