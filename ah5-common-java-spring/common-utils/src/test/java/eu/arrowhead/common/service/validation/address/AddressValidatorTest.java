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
package eu.arrowhead.common.service.validation.address;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.AddressType;

public class AddressValidatorTest {

	//=================================================================================================
	// members

	private final AddressValidator addressValidator = new AddressValidator();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateNormalizedAddressNullAddressTypeTest() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> addressValidator.validateNormalizedAddress(null, "abcd"));

		assertEquals("address type is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateNormalizedAddressNullAddressTest() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, null));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateNormalizedAddressEmptyAddressTest() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, ""));

		assertEquals("address is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateNormalizedAddressTooLongTest() {
		final String address = "veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery"
				+ "veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery"
				+ "veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery"
				+ "veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery"
				+ "veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery"
				+ "veryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryvery"
				+ "veryveryveryveryveryverylongaddress";

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, address));

		assertEquals("Address is too long", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateIPv4Test() {
		assertAll("IPv4 address",
				// valid IPv4
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "192.168.2.1");
				}),
				// invalid IPv4
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "192.168.2.a");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "777.666.555.444");
				}),
				// IP placeholder
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "0.0.0.0");
				}),
				// broadcast address
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "255.255.255.255");
				}),
				// multicast address
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "225.0.0.0");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "240.255.255.255");
				}));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateIPv6Test() {
		assertAll("IPv6 address",
				// valid IPv6
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "2001:11b8:96b3:0000:0000:8a2e:0370:7cf4");
				}),
				// invalid IPv6
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "192.168.2.a");
				}),
				// unspecified address
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "0000:0000:0000:0000:0000:0000:0000:0000");
				}),
				// multicast address
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "ff01:11b8:96b3:0000:0000:8a2e:0370:7cf4");
				})
		// anycast -> indistinguishable from other unicast addresses
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateMACTest() {
		assertAll("MAC address",
				// valid MAC
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.MAC, "00:1a:2b:3c:4d:5e");
				}),
				// invalid MAC
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.MAC, "00-1A-2B-3C-4D-5E");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.MAC, "192.168.2.1");
				}),
				// local broadcast address
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.MAC, "ff:ff:ff:ff:ff:ff");
				}),
				// IPv4 mapped multicast address
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.MAC, "01:00:5e:3c:4d:5e");
				}),
				// IPv6 mapped
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.MAC, "33:33:5e:3c:4d:5e");
				}));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateHostNameTest() {
		// valid hostname, invalid hostname, invalid length
		assertAll("hostname",
				// valid hostname
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "example.com");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "subdomain.example.hu");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "EXAMPLE.ORG");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "a.b.c");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "123x.10.20.30");
				}),
				// invalid hostname
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "example..com");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, ".example.com");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "example.com.");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "-example.com");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "example-.com");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "example.com-");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "example.invalid-segment-length-because-it-is-more-than-sixty-three-characters-long.com");
				}),
				// invalid length (max: 253)
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME,
							"too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long."
									+ "too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.too-long.com");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "300.1.10.12");
				}));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void detectTypeTest() {
		assertAll("detect type",
				// empty address
				() -> assertThrows(IllegalArgumentException.class, () -> {
					addressValidator.detectType(null);
				}),
				() -> assertThrows(IllegalArgumentException.class, () -> {
					addressValidator.detectType("");
				}),
				// IPv4
				() -> assertEquals(AddressType.IPV4, addressValidator.detectType("192.168.2.1")),
				() -> assertEquals(AddressType.IPV4, addressValidator.detectType("0.0.0.0")),
				() -> assertEquals(AddressType.IPV4, addressValidator.detectType("255.255.255.255")),
				// IPv6
				() -> assertEquals(AddressType.IPV6, addressValidator.detectType("2001:11b8:96b3:0000:0000:8a2e:0370:7cf4")),
				() -> assertEquals(AddressType.IPV6, addressValidator.detectType("ff01:11b8:96b3:0000:0000:8a2e:0370:7cf4")),
				() -> assertEquals(AddressType.IPV6, addressValidator.detectType("0000:0000:0000:0000:0000:0000:0000:0000")),
				// MAC
				() -> assertEquals(AddressType.MAC, addressValidator.detectType("00:1a:2b:3c:4d:5e")),
				() -> assertEquals(AddressType.MAC, addressValidator.detectType("ff:ff:ff:ff:ff:ff")),
				() -> assertEquals(AddressType.MAC, addressValidator.detectType("00:00:00:00:00:00")),
				// hostname
				() -> assertEquals(AddressType.HOSTNAME, addressValidator.detectType("example.com")),
				() -> assertEquals(AddressType.HOSTNAME, addressValidator.detectType("1x5ä!?")),
				() -> assertEquals(AddressType.HOSTNAME, addressValidator.detectType("000")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateAddressSelfAddressingNotAllowedTest() {

		ReflectionTestUtils.setField(addressValidator, "allowSelfAddressing", false);

		assertAll("don't allow self addressing",
				// hostname
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "my-hostname.com");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "loopback");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "localhost");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "LOOPBACK");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "LOCALHOST");
				}),
				// IPv4
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "192.168.2.1");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "127.0.0.1");
				}),
				// IPv6
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "2c2c:0000:0000:0000:0000:0000:0000:0001");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "0000:0000:0000:0000:0000:0000:0000:0001");
				}));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateAddressSelfAddressingAllowedTest() {

		ReflectionTestUtils.setField(addressValidator, "allowSelfAddressing", true);

		assertAll("allow self addressing",
				// hostname
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "loopback");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.HOSTNAME, "localhost");
				}),
				// IPv4
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "192.168.2.1");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "127.0.0.1");
				}),
				// IPv6
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "2c2c:0000:0000:0000:0000:0000:0000:0001");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "0000:0000:0000:0000:0000:0000:0000:0001");
				}));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateAddressNonRoutableAddressingNotAllowedTest() {

		ReflectionTestUtils.setField(addressValidator, "allowNonRoutableAddressing", false);

		assertAll("don't allow non routable addressing",
				// IPv4
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "192.168.2.1");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "169.254.0.1");
				}),
				// IPv6
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "2c2c:0000:0000:0000:0000:0000:0000:0001");
				}),
				() -> assertThrows(InvalidParameterException.class, () -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "fe80:0000:0000:0000:0000:0000:0000:0001");
				}));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validateAddressNonRoutableAddressingAllowedTest() {

		ReflectionTestUtils.setField(addressValidator, "allowNonRoutableAddressing", true);
		assertAll("allow non routable addressing",
				// IPv4
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "192.168.2.1");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV4, "169.254.0.1");
				}),
				// IPv6
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "2c2c:0000:0000:0000:0000:0000:0000:0001");
				}),
				() -> assertDoesNotThrow(() -> {
					addressValidator.validateNormalizedAddress(AddressType.IPV6, "fe80:0000:0000:0000:0000:0000:0000:0001");
				}));
	}
}