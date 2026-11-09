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
package eu.arrowhead.common.service.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor.AddressData;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.dto.enums.AddressType;

@ExtendWith(MockitoExtension.class)
public class ServiceInterfaceAddressPropertyProcessorTest {

	//=================================================================================================
	// members

	private static final List<String> testAliases = List.of("address", "addresses", "accessAddress", "accessAddresses", "host", "hosts");

	@InjectMocks
	private ServiceInterfaceAddressPropertyProcessor processor;

	@Mock
	private AddressValidator addressValidator;

	//=================================================================================================
	// methods

	@BeforeEach
	public void init() {
		processor.setAddressAliasNames(testAliases);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetAddressAliasNames() {
		ReflectionTestUtils.setField(processor, "addressAliasNames", List.of());

		processor.setAddressAliasNames(testAliases);

		final Object actualList = ReflectionTestUtils.getField(processor, "addressAliasNames");
		assertEquals(testAliases, actualList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindAddressess() {
		assertAll("interesting cases - find",
				() -> {
					final AddressData expected = new AddressData(List.of(), null, false);
					final AddressData data = processor.findAddresses(Map.of());
					assertEquals(expected, data);
				},
				() -> {
					final AddressData expected = new AddressData(List.of(), null, false);
					final AddressData data = processor.findAddresses(Map.of("address", ""));
					assertEquals(expected, data);
				},
				() -> {
					final AddressData expected = new AddressData(List.of(), null, false);
					final AddressData data = processor.findAddresses(Map.of("accessAddresses", List.of()));
					assertEquals(expected, data);
				},
				() -> {
					final AddressData expected = new AddressData(List.of("192.168.0.10"), "host", false);
					final AddressData data = processor.findAddresses(Map.of("port", 1234, "host", "192.168.0.10"));
					assertEquals(expected, data);
				},
				() -> {
					final AddressData expected = new AddressData(List.of("192.168.0.10", "example.com"), "addresses", true);
					final AddressData data = processor.findAddresses(Map.of("port", 1234, "addresses", List.of("192.168.0.10", "example.com", 1234), "other", "something"));
					assertEquals(expected, data);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:MagicNumber", "rawtypes" })
	@Test
	public void testFilterOnAddressTypes() {
		assertAll("interesting cases - filter",
				() -> {
					final boolean result = processor.filterOnAddressTypes(Map.of("something", "irrelevant"), List.of("IPV4"));
					assertFalse(result);
				},
				() -> {
					when(addressValidator.detectType("example.com")).thenReturn(AddressType.HOSTNAME);

					final boolean result = processor.filterOnAddressTypes(Map.of("host", "example.com"), List.of("IPV4"));
					assertFalse(result);

					verify(addressValidator).detectType(anyString());
				},
				() -> {
					when(addressValidator.detectType("192.168.0.10")).thenReturn(AddressType.IPV4);
					when(addressValidator.detectType("example.com")).thenReturn(AddressType.HOSTNAME);

					final Map<String, Object> props = new HashMap<>();
					props.put("hosts", List.of("192.168.0.10", "example.com"));

					final boolean result = processor.filterOnAddressTypes(props, List.of("IPV4"));
					assertTrue(result);
					assertEquals(1, ((List) props.get("hosts")).size());
					assertEquals("192.168.0.10", ((List) props.get("hosts")).get(0));

					verify(addressValidator, times(3)).detectType(anyString());
				},
				() -> {
					when(addressValidator.detectType("192.168.0.10")).thenReturn(AddressType.IPV4);

					final Map<String, Object> props = new HashMap<>();
					props.put("address", "192.168.0.10");

					final boolean result = processor.filterOnAddressTypes(props, List.of("IPV4"));
					assertTrue(result);
					assertEquals("192.168.0.10", props.get("address"));

					verify(addressValidator, times(4)).detectType(anyString());
				});
	}
}