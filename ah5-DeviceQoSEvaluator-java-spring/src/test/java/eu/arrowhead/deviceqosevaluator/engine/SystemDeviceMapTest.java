/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.deviceqosevaluator.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.deviceqosevaluator.engine.SystemDeviceMap.Address;
import eu.arrowhead.deviceqosevaluator.engine.SystemDeviceMap.Bool;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;

public class SystemDeviceMapTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetDeviceSize() {
		final SystemDeviceMap map = new SystemDeviceMap();

		assertEquals(0, map.getDeviceSize());

		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		final Bool trueBool = new Bool();
		trueBool.setValue(true);
		devices.put(0, Triple.of(
				Set.of(new Address("localhost", AddressType.HOSTNAME, false)),
				Set.of("TestSystem"),
				trueBool));
		ReflectionTestUtils.setField(map, "devices", devices);

		assertEquals(1, map.getDeviceSize());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetDeviceAddresses() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final Set<Address> addresses = Set.of(new Address("localhost", AddressType.HOSTNAME, false));
		final Bool trueBool = new Bool();
		trueBool.setValue(true);
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				addresses,
				Set.of("TestSystem"),
				trueBool));
		ReflectionTestUtils.setField(map, "devices", devices);

		assertEquals(addresses, map.getDeviceAddresses(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetDeviceSystems() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final Set<Address> addresses = Set.of(new Address("localhost", AddressType.HOSTNAME, false));
		final Bool trueBool = new Bool();
		trueBool.setValue(true);
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				addresses,
				Set.of("TestSystem"),
				trueBool));
		ReflectionTestUtils.setField(map, "devices", devices);

		assertEquals(Set.of("TestSystem"), map.getDeviceSystems(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasAugmented() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final Set<Address> addresses = Set.of(new Address("localhost", AddressType.HOSTNAME, false));
		final Bool trueBool = new Bool();
		trueBool.setValue(true);
		final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();
		devices.put(0, Triple.of(
				addresses,
				Set.of("TestSystem"),
				trueBool));
		ReflectionTestUtils.setField(map, "devices", devices);

		assertTrue(map.hasAugmented(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLoadOk() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final List<AddressDTO> deviceAddressList1 = List.of(
				new AddressDTO("HOSTNAME", "example.com"),
				new AddressDTO("INVALID", "doesn't even matter"),
				new AddressDTO("MAC", "00:1A:2B:3C:4D:5E"));
		final DeviceResponseDTO device1 = new DeviceResponseDTO("DEVICE_01", null, deviceAddressList1, null, null);
		final List<AddressDTO> sysAddressList1 = List.of(
				new AddressDTO("INVALID", "doesn't even matter"));
		final SystemResponseDTO sys1 = new SystemResponseDTO("TestSystem", Map.of("qos", Map.of("deviceAugmented", List.of("1.4"))), "1.0.0", sysAddressList1, device1, null, null);
		final List<AddressDTO> sysAddressList2 = List.of(
				new AddressDTO("HOSTNAME", "other.com"),
				new AddressDTO("IPV4", "10.0.0.15"));
		final SystemResponseDTO sys2 = new SystemResponseDTO("TestSystem2", null, "1.0.0", sysAddressList2, null, null, null);
		final List<AddressDTO> sysAddressList3 = List.of(
				new AddressDTO("HOSTNAME", "example.com"),
				new AddressDTO("IPV4", "10.0.0.16"));
		final SystemResponseDTO sys3 = new SystemResponseDTO("TestSystem3", null, "1.0.0", sysAddressList3, null, null, null);

		assertDoesNotThrow(() -> map.load(List.of(sys1, sys2, sys3)));

		assertEquals(2, map.getDeviceSize());
		assertEquals(Set.of(new Address("example.com", AddressType.HOSTNAME, true)), map.getDeviceAddresses(0));
		assertEquals(Set.of("TestSystem", "TestSystem3"), map.getDeviceSystems(0));
		assertTrue(map.hasAugmented(0));
		assertEquals(Set.of(new Address("other.com", AddressType.HOSTNAME, false), new Address("10.0.0.15", AddressType.IPV4, false)), map.getDeviceAddresses(1));
		assertEquals(Set.of("TestSystem2"), map.getDeviceSystems(1));
		assertFalse(map.hasAugmented(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedNullMetadata() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", null, "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedEmptyMetadata() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", Map.of(), "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedNoQoSMetadata() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", Map.of("otherKey", "otherValue"), "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedNoAugmentedMetadata() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", Map.of("qos", Map.of("other", 1)), "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedInvalidAugmentedMetadata() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", Map.of("qos", Map.of("deviceAugmented", 1)), "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedAugmentedMetadataInvalidValue() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", Map.of("qos", Map.of("deviceAugmented", List.of("not_oid"))), "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertFalse((Boolean) resultObj);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSupportsAugmentedOk() {
		final SystemDeviceMap map = new SystemDeviceMap();
		final SystemResponseDTO sys = new SystemResponseDTO("TestSystem", Map.of("qos", Map.of("deviceAugmented", List.of("0.0", "1.4"))), "1.0.0", null, null, null, null);

		final Object resultObj = ReflectionTestUtils.invokeMethod(map, "supportsAugmented", sys);

		assertTrue((Boolean) resultObj);
	}
}