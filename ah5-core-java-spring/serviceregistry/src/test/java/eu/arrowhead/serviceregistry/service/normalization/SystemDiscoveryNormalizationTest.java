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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
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
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@ExtendWith(MockitoExtension.class)
public class SystemDiscoveryNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SystemDiscoveryNormalization normalizer;

	@Mock
	private AddressValidator addressValidator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private DeviceNameNormalizer deviceNameNormalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemRequestDTO() {

    	when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(addressValidator.detectType("192.168.4.4")).thenReturn(AddressType.IPV4);

		assertAll(
			// nothing is empty
			() -> {

				final SystemRequestDTO dto = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of("192.168.4.4"), "TEST_DEVICE");
				final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), "TEST_DEVICE");

				final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTO(dto));
				assertEquals(expected, normalized);
				verify(systemNameNormalizer, times(1)).normalize("TemperatureConsumer");
				verify(versionNormalizer, times(1)).normalize("1.0.0");
				verify(addressNormalizer, times(1)).normalize("192.168.4.4");
				verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
			},

			// addresses are empty
			() -> {
				resetUtilitiesMock();
				final SystemRequestDTO dto = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(), "TEST_DEVICE");
				final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", new ArrayList<>(), "TEST_DEVICE");

				final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTO(dto));
				assertEquals(expected, normalized);
				utilitiesMock.verify(() -> Utilities.isEmpty(List.of()));

			},

			// device name is empty
			() -> {
				resetUtilitiesMock();
				final SystemRequestDTO dto = new SystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of("192.168.4.4"), EMPTY);
				final NormalizedSystemRequestDTO expected = new NormalizedSystemRequestDTO("TemperatureConsumer", Map.of("indoor", false), "1.0.0", List.of(new AddressDTO("IPV4", "192.168.4.4")), null);

				final NormalizedSystemRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemRequestDTO(dto));
				assertEquals(expected, normalized);
				utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testNormalizeSystemLookupRequestDTO() {

    	when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(addressNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    	when(deviceNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		assertAll(

			// nothing is empty
			() -> {
				final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
				requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

				final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(List.of("TemperatureConsumer"), List.of("192.168.4.4"), "IPv4 \n", List.of(requirement), List.of("5.0.0"), List.of("TEST_DEVICE"));
				final SystemLookupRequestDTO expected = new SystemLookupRequestDTO(List.of("TemperatureConsumer"), List.of("192.168.4.4"), "IPV4", List.of(requirement), List.of("5.0.0"), List.of("TEST_DEVICE"));

				final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemLookupRequestDTO(dto));
				assertEquals(expected, normalized);
				verify(systemNameNormalizer, times(1)).normalize("TemperatureConsumer");
				verify(versionNormalizer, times(1)).normalize("5.0.0");
				verify(addressNormalizer, times(1)).normalize("192.168.4.4");
				verify(deviceNameNormalizer, times(1)).normalize("TEST_DEVICE");
			},

			// everything is empty
			() -> {
				resetUtilitiesMock();
				final SystemLookupRequestDTO dto = new SystemLookupRequestDTO(List.of(), List.of(), EMPTY, List.of(), List.of(), List.of());
				final SystemLookupRequestDTO expected = new SystemLookupRequestDTO(null, null, null, List.of(), null, null);

				final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemLookupRequestDTO(dto));
				assertEquals(expected, normalized);
				utilitiesMock.verify(() -> Utilities.isEmpty(eq(List.of())), times(4));
				utilitiesMock.verify(() -> Utilities.isEmpty(eq(EMPTY)), times(1));
			},

			// dto is empty
			() -> {
				final SystemLookupRequestDTO expected = new SystemLookupRequestDTO(null, null, null, null, null, null);

				final SystemLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemLookupRequestDTO(null));
				assertEquals(expected, normalized);
			}
		);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRevokeSystem() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final String normalized = assertDoesNotThrow(() -> normalizer.normalizeRevokeSystemName("TemperatureManager"));
		assertEquals("TemperatureManager", normalized);
		verify(systemNameNormalizer, times(1)).normalize("TemperatureManager");
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
