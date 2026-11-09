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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AddressNormalizerTest {

	//=================================================================================================
	// members

	private final AddressNormalizer addressNormalizer = new AddressNormalizer();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeEmptyInputTest() {
		assertAll("Empty address",
				() -> assertEquals("", addressNormalizer.normalize(null)),
				() -> assertEquals("", addressNormalizer.normalize(" ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeInvalidAddressOrDomainNameTest() {
		// If the address is not MAC, IPv4, IPv6 or hybrid, the normalizer shouldn't change it (besides trim and toLowerCase).
		assertAll("Domain name or invalid address",
				// domain name
				() -> assertEquals("localhost", addressNormalizer.normalize("localhost")),
				() -> assertEquals("example.com", addressNormalizer.normalize("example.com")),
				// incorrect number of separators
				() -> assertEquals("a1:a1:a1:a1:a1a1", addressNormalizer.normalize("a1:a1:a1:a1:a1a1")),
				// mixed separators
				() -> assertEquals("a1-a1:a1.a1-a1:a1", addressNormalizer.normalize("a1-a1:a1.a1-a1:a1")),
				() -> assertEquals("128.0.0.11:a:a:a:a:a", addressNormalizer.normalize("128.0.0.11:a:a:a:a:a")),
				// no separator character
				() -> assertEquals("a1a1a1a1a1a1", addressNormalizer.normalize("\tA1A1A1A1A1A1\n")),
				// invalid characters
				() -> assertEquals("ä1:a1:a1:a1:a1:a1", addressNormalizer.normalize("ä1:a1:a1:a1:a1:a1")),
				// looks like IPv6, but contains more than one duplicated colons
				() -> assertEquals("2001::96b3::8a2e:0370:7cf4", addressNormalizer.normalize("2001::96b3::8a2e:0370:7cf4")),
				// looks like IPv6, but too much parts
				() -> assertEquals("2001::96b3:0000:0000:8a2e:0370:7cf4:1111", addressNormalizer.normalize("2001::96b3:0000:0000:8a2e:0370:7cf4:1111")),
				// looks like MAC address, but not
				() -> assertEquals("1:a1:a1:a1:a1:a1", addressNormalizer.normalize("1:a1:a1:a1:a1:a1")),
				// looks like IPv4-IPv6 hybrid, but contains test
				() -> assertEquals("::ffff:192.0.two.128", addressNormalizer.normalize("::FFFF:192.0.two.128")),
				// looks like IPv4-IPv6 hybrid, but invalid octet
				() -> assertEquals("::ffff:192.0.-1.128", addressNormalizer.normalize("::FFFF:192.0.-1.128")),
				() -> assertEquals("::ffff:192.0.300.128", addressNormalizer.normalize("::FFFF:192.0.300.128")));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeMACTest() {
		assertAll("MAC address",
				// raw address was uppercase and contained whitespace characters
				() -> assertEquals("ab:cd:ef:11:22:33", addressNormalizer.normalize(" \n\tAB:CD:EF:11:22:33 \r")),
				// change dashes to colons
				() -> assertEquals("ab:cd:ef:11:22:33", addressNormalizer.normalize(" \n\tAB-CD-EF-11-22-33 \r")),
				// change dot-separated representation to colon-separated representation
				() -> assertEquals("00:1b:44:11:3a:b7", addressNormalizer.normalize("\n   001B.4411.3AB7   ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeIPv4Test() {
		// remove whitespaces
		assertEquals("192.168.0.1", addressNormalizer.normalize(" \n\t192.168.0.1 \r"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeIPv6Test() {
		assertAll("IPv6 address",
				// remove whitespaces, to lowercase
				() -> assertEquals("2001:11b8:96b3:0000:0000:8a2e:0370:7cf4", addressNormalizer.normalize("\n \t2001:11B8:96B3:0000:0000:8A2E:0370:7Cf4\r ")),
				// add leading zeros
				() -> assertEquals("2001:00b8:85a3:0000:0000:9b3f:0000:0004", addressNormalizer.normalize("2001:b8:85a3:0:0:9b3f:0:4")),
				// convert double colons
				() -> assertEquals("2001:00b8:85a3:0000:0000:9b3f:0000:0004", addressNormalizer.normalize("2001:00b8:85a3::9b3f:0000:0004")),
				// normalize loopback address
				() -> assertEquals("0000:0000:0000:0000:0000:0000:0000:0001", addressNormalizer.normalize("::1")),
				// normalize unspecified address
				() -> assertEquals("0000:0000:0000:0000:0000:0000:0000:0000", addressNormalizer.normalize("::")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void normalizeIPv6IPv4HybridTest() {
		assertAll("IPv6-IPv4 hybrid address",
				// remove whitespaces, to lowercase, convert to IPv6
				() -> assertEquals("0000:0000:0000:0000:0000:ffff:c000:0280", addressNormalizer.normalize("\r \n::FFFF:192.0.2.128  ")));
	}
}
