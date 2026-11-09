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
package eu.arrowhead.serviceregistry.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.matching.AddressMatching;
import eu.arrowhead.serviceregistry.service.validation.DeviceDiscoveryValidation;

@ExtendWith(MockitoExtension.class)
public class DeviceDiscoveryServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceDiscoveryService service;

	@Mock
	private DeviceDbService dbService;

	@Mock
	private DeviceDiscoveryValidation validator;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private AddressMatching addressMatcher;

	// expected error messages
	private static final String METADATA_DOES_NOT_MATCH = "Device with name '{}' already exists, but provided metadata is not matching";
	private static final String ADDRESSES_DO_NOT_MATCH = "Device with name '{}' already exists, but provided addresses are not matching";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerDevice(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerDevice(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceDifferentMetadata() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		final Device device = new Device("ALARM", Utilities.toJson(Map.of("indoor", false)));
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(dbService.getByName("ALARM")).thenReturn(Optional.of(Map.entry(device, List.of(deviceAddress))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerDevice(dto, "test origin"));
		assertEquals(METADATA_DOES_NOT_MATCH.replace("{}", "ALARM"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceOnlyNewDeviceHasMetadata() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		final Device device = new Device("ALARM", null);
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(dbService.getByName("ALARM")).thenReturn(Optional.of(Map.entry(device, List.of(deviceAddress))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerDevice(dto, "test origin"));
		assertEquals(METADATA_DOES_NOT_MATCH.replace("{}", "ALARM"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceOnlyExistingHasMetadata() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of(), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of(), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		final Device device = new Device("ALARM", Utilities.toJson(Map.of("indoor", true)));
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(dbService.getByName("ALARM")).thenReturn(Optional.of(Map.entry(device, List.of(deviceAddress))));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerDevice(dto, "test origin"));
		assertEquals(METADATA_DOES_NOT_MATCH.replace("{}", "ALARM"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceDifferentAddresses() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		final Device device = new Device("ALARM", Utilities.toJson(Map.of("indoor", true)));
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:45");
		when(dbService.getByName("ALARM")).thenReturn(Optional.of(Map.entry(device, List.of(deviceAddress))));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(false);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.registerDevice(dto, "test origin"));
		assertEquals(ADDRESSES_DO_NOT_MATCH.replace("{}", "ALARM"), ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceExistingDeviceOk() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of(), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of(), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		final Device device = new Device("ALARM", null);
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(dbService.getByName("ALARM")).thenReturn(Optional.of(Map.entry(device, List.of(deviceAddress))));

		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of(), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		when(dtoConverter.convertDeviceEntityToDeviceResponseDTO(any(), any())).thenReturn(response);

		final Entry<DeviceResponseDTO, Boolean> actual = assertDoesNotThrow(() -> service.registerDevice(dto, "test origin"));
		assertEquals(Map.entry(response, false), actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceExistingMetadataWrittenWithWindowsLineEndings() {
		assertExistingDeviceAccepted("{\r\n  \"indoor\" : true\r\n}", Map.of("indoor", true));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceExistingMetadataWrittenWithUnixLineEndings() {
		assertExistingDeviceAccepted("{\n  \"indoor\" : true\n}", Map.of("indoor", true));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceExistingMetadataInOtherKeyOrder() {
		assertExistingDeviceAccepted("{\"zone\":\"a\",\"indoor\":true}", new java.util.LinkedHashMap<>(Map.of("indoor", true, "zone", "a")));
	}

	//-------------------------------------------------------------------------------------------------
	private void assertExistingDeviceAccepted(final String storedMetadata, final Map<String, Object> metadata) {
		final List<String> addresses = List.of("7c:5a:2e:d1:9b:44");
		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", metadata, addresses);
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", metadata, List.of(new AddressDTO("MAC", addresses.getFirst())));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		final Device device = new Device("ALARM", storedMetadata);
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, addresses.getFirst());
		when(dbService.getByName("ALARM")).thenReturn(Optional.of(Map.entry(device, List.of(deviceAddress))));
		when(addressMatcher.isAddressListMatching(any(), any())).thenReturn(true);

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", metadata, List.of(new AddressDTO("MAC", addresses.getFirst())), null, null);
		when(dtoConverter.convertDeviceEntityToDeviceResponseDTO(any(), any())).thenReturn(response);

		final Entry<DeviceResponseDTO, Boolean> actual = assertDoesNotThrow(() -> service.registerDevice(dto, "test origin"));
		assertEquals(Map.entry(response, false), actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceNewDeviceOk() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		when(dbService.getByName("ALARM")).thenReturn(Optional.empty());

		final Device device = new Device("ALARM", Utilities.toJson(Map.of("indoor", true)));
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");
		when(dbService.create(any())).thenReturn(Map.entry(device, List.of(deviceAddress)));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		when(dtoConverter.convertDeviceEntityToDeviceResponseDTO(any(), any())).thenReturn(response);

		final Entry<DeviceResponseDTO, Boolean> actual = assertDoesNotThrow(() -> service.registerDevice(dto, "test origin"));
		assertEquals(Map.entry(response, true), actual);
		verify(dbService).create(normalizedDto);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterDeviceInternalServerError() {

		final DeviceRequestDTO dto = new DeviceRequestDTO("ALARM", Map.of("indoor", true), List.of("7c:5a:2e:d1:9b:44"));
		final NormalizedDeviceRequestDTO normalizedDto = new NormalizedDeviceRequestDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")));
		when(validator.validateAndNormalizeRegisterDevice(dto, "test origin")).thenReturn(normalizedDto);

		when(dbService.getByName("ALARM")).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.registerDevice(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookupDevice(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.lookupDevice(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceAddressTypeNull() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), null, List.of(requirement));
		when(validator.validateAndNormalizeLookupDevice(dto, "test origin")).thenReturn(dto);

		final Device device = new Device("ALARM", Utilities.toJson(Map.of("indoor", true)));
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");

		when(dbService.getByFilters(any(), any(), any(), any())).thenReturn(List.of(Map.entry(device, List.of(deviceAddress))));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(deviceAddress))), 1)).thenReturn(new DeviceListResponseDTO(List.of(response), 1));

		final DeviceListResponseDTO actual = assertDoesNotThrow(() -> service.lookupDevice(dto, "test origin"));
		assertEquals(new DeviceListResponseDTO(List.of(response), 1), actual);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceAddressTypeNotNull() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "MAC", List.of(requirement));
		when(validator.validateAndNormalizeLookupDevice(dto, "test origin")).thenReturn(dto);

		final Device device = new Device("ALARM", Utilities.toJson(Map.of("indoor", true)));
		final DeviceAddress deviceAddress = new DeviceAddress(device, AddressType.MAC, "7c:5a:2e:d1:9b:44");

		when(dbService.getByFilters(any(), any(), any(), any())).thenReturn(List.of(Map.entry(device, List.of(deviceAddress))));

		final DeviceResponseDTO response = new DeviceResponseDTO("ALARM", Map.of("indoor", true), List.of(new AddressDTO("MAC", "7c:5a:2e:d1:9b:44")), null, null);
		when(dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(List.of(Map.entry(device, List.of(deviceAddress))), 1)).thenReturn(new DeviceListResponseDTO(List.of(response), 1));

		final DeviceListResponseDTO actual = assertDoesNotThrow(() -> service.lookupDevice(dto, "test origin"));
		assertEquals(new DeviceListResponseDTO(List.of(response), 1), actual);
		verify(dbService).getByFilters(List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), AddressType.MAC, List.of(requirement));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupDeviceInternalServerError() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("indoor", Map.of("op", "EQUALS", "value", true));

		final DeviceLookupRequestDTO dto = new DeviceLookupRequestDTO(List.of("ALARM"), List.of("7c:5a:2e:d1:9b:44"), "MAC", List.of(requirement));
		when(validator.validateAndNormalizeLookupDevice(dto, "test origin")).thenReturn(dto);

		when(dbService.getByFilters(any(), any(), any(), any())).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.lookupDevice(dto, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeDeviceOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeDevice(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeDeviceOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeDevice(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeDeviceOk() {

		final String name = "ALARM";
		when(validator.validateAndNormalizeRevokeDevice(name, "test origin")).thenReturn(name);
		when(dbService.deleteByName(name)).thenReturn(true);

		assertTrue(() -> service.revokeDevice(name, "test origin"));
		verify(dbService).deleteByName(name);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeDeviceInvernalServerError() {

		final String name = "ALARM";
		when(validator.validateAndNormalizeRevokeDevice(name, "test origin")).thenReturn(name);
		when(dbService.deleteByName(name)).thenThrow(new InternalServerError("Database error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.revokeDevice(name, "test origin"));
		assertEquals("Database error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeDeviceLockedException() {

		final String name = "ALARM";
		when(validator.validateAndNormalizeRevokeDevice(name, "test origin")).thenReturn(name);
		when(dbService.deleteByName(name)).thenThrow(new LockedException("Lock error"));

		final LockedException ex = assertThrows(LockedException.class, () -> service.revokeDevice(name, "test origin"));
		assertEquals("Lock error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}
}
