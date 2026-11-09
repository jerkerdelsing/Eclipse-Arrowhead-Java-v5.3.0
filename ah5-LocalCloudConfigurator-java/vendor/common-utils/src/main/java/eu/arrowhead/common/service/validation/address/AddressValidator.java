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

import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.AddressType;

@Component
public class AddressValidator {

	//=================================================================================================
	// members

	private static final String DELIMITER_DOT_REGEX = "\\.";
	private static final String ERROR_MSG_PREFIX = "Address verification failure: ";
	private static final String IPV4_REGEX_STRING = "\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
	private static final String IPV6_REGEX_STRING = "^([0-9a-fA-F]{4}:){7}[0-9a-fA-F]{4}$";
	private static final String HOSTNAME_REGEX_STRING = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)*((?!-)[A-Za-z0-9-]{1,63}(?<!-))$";
	private static final String MAC_REGEX_STRING = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$";

	private static final Pattern macPattern;
	private static final Pattern ipv4Pattern;
	private static final Pattern ipv6Pattern;
	private static final Pattern hostnamePattern;

	static {
		macPattern = Pattern.compile(MAC_REGEX_STRING);
		ipv4Pattern = Pattern.compile(IPV4_REGEX_STRING);
		ipv6Pattern = Pattern.compile(IPV6_REGEX_STRING);
		hostnamePattern = Pattern.compile(HOSTNAME_REGEX_STRING);
	}

	private static final String MAC_BROADCAST = "ff:ff:ff:ff:ff:ff";
	private static final String MAC_IPV4_MAPPED_MULTICAST_PREFIX = "01:00:5e:";
	private static final String MAC_IPV6_MAPPED_MULTICAST_PREFIX = "33:33:";

	private static final String IPV4_PLACEHOLDER = "0.0.0.0";
	private static final String IPV4_LOOPBACK_1ST_OCTET = "127";
	private static final String IPV4_APIPA_1ST_AND_2ND_OCTET = "169.254";
	private static final String IPV4_LOCAL_BROADCAST = "255.255.255.255";
	private static final int IPV4_MULTICAST_1ST_OCTET_START = 224;
	private static final int IPV4_MULTICAST_1ST_OCTET_END = 239;

	private static final String IPV6_UNSPECIFIED = "0000:0000:0000:0000:0000:0000:0000:0000";
	private static final String IPV6_LOOPBACK = "0000:0000:0000:0000:0000:0000:0000:0001";
	private static final String IPV6_LINK_LOCAL_PREFIX = "fe80";
	private static final String IPV6_MULTICAST_PREFIX = "ff";

	private static final int HOSTNAME_MAX_LENGTH = 253;
	private static final String HOSTNAME_LOCALHOST = "localhost";
	private static final String HOSTNAME_LOOPBACK = "loopback";

	@Value(Constants.$ALLOW_SELF_ADDRESSING_WD)
	private boolean allowSelfAddressing;

	@Value(Constants.$ALLOW_NON_ROUTABLE_ADDRESSING_WD)
	private boolean allowNonRoutableAddressing;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedAddress(final AddressType type, final String address) {
		logger.debug("AddressValidator.validateNormalizedAddress started...");
		Assert.notNull(type, "address type is null");
		Assert.isTrue(!Utilities.isEmpty(address), "address is empty");

		if (address.trim().length() > Constants.ADDRESS_MAX_LENGTH) {
			throw new InvalidParameterException("Address is too long");
		}

		switch (type) {
		case MAC:
			validateMAC(address);
			break;

		case IPV4:
			validateIPV4(address);
			break;

		case IPV6:
			validateIPV6(address);
			break;

		default:
			validateHostName(address);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public AddressType detectType(final String address) {
		logger.debug("AddressValidator.detectType started...");
		Assert.isTrue(!Utilities.isEmpty(address), "address is empty");

		if (macPattern.matcher(address).matches()) {
			return AddressType.MAC;
		}

		if (ipv4Pattern.matcher(address).matches()) {
			return AddressType.IPV4;
		}

		if (ipv6Pattern.matcher(address).matches()) {
			return AddressType.IPV6;
		}

		return AddressType.HOSTNAME;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void validateMAC(final String address) {
		logger.debug("AddressValidator.validateMAC started...");

		if (!macPattern.matcher(address).matches()) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " is not MAC address");
		}

		// Filter out local broadcast
		if (address.equalsIgnoreCase(MAC_BROADCAST)) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " MAC address is invalid: broadcast address is denied");
		}

		// Filter out multicast
		if (address.startsWith(MAC_IPV4_MAPPED_MULTICAST_PREFIX)
				|| address.startsWith(MAC_IPV6_MAPPED_MULTICAST_PREFIX)) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " MAC address is invalid: multicast address is denied");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateIPV4(final String address) {
		logger.debug("AddressValidator.validateIPV4 started...");

		if (!ipv4Pattern.matcher(address).matches()) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " is not IPv4 address");
		}

		if (!allowSelfAddressing) {
			// Filter out loopback (127.0.0.0 - 127.255.255.255)
			if (address.startsWith(IPV4_LOOPBACK_1ST_OCTET)) {
				throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv4 address is invalid: self-addressing is disabled");
			}
		}

		if (!allowNonRoutableAddressing) {
			// Filter out APIPA (Automatic Private IP Address: 169.254.?.?)
			if (address.startsWith(IPV4_APIPA_1ST_AND_2ND_OCTET)) {
				throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv4 address is invalid: non-routable-addressing is disabled");
			}
		}

		// Filter out IP placeholder (default route) (0.0.0.0)
		if (address.equalsIgnoreCase(IPV4_PLACEHOLDER)) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv4 address is invalid: placeholder address is denied");
		}

		// Filter out local broadcast (255.255.255.255)
		if (address.equalsIgnoreCase(IPV4_LOCAL_BROADCAST)) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv4 address is invalid: local broadcast address is denied");
		}

		// Could not filter out directed broadcast (cannot determine it without the subnet mask)

		// Filter out multicast (Class D: 224.0.0.0 - 239.255.255.255)
		final String[] octets = address.split(DELIMITER_DOT_REGEX);
		final Integer firstOctet = Integer.valueOf(octets[0]);
		if (firstOctet >= IPV4_MULTICAST_1ST_OCTET_START && firstOctet <= IPV4_MULTICAST_1ST_OCTET_END) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv4 address is invalid: multicast addresses are denied");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateIPV6(final String address) {
		logger.debug("AddressValidator.validateIPV6 started...");

		if (!ipv6Pattern.matcher(address).matches()) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " is not IPv6 address");
		}

		if (!allowSelfAddressing) {
			// Filter out loopback address (0000:0000:0000:0000:0000:0000:0000:0001)
			if (address.equalsIgnoreCase(IPV6_LOOPBACK)) {
				throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv6 address is invalid: self-addressing is disabled");
			}
		}

		if (!allowNonRoutableAddressing) {
			// Filter out link-local addresses (prefix fe80)
			if (address.startsWith(IPV6_LINK_LOCAL_PREFIX)) {
				throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv6 address is invalid: non-routable-addressing is disabled");
			}
		}

		// Filter out unspecified address (0000:0000:0000:0000:0000:0000:0000:0000)
		if (address.equalsIgnoreCase(IPV6_UNSPECIFIED)) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv6 address is invalid: unspecified address is denied");
		}

		// Filter out multicast (prefix ff)
		if (address.startsWith(IPV6_MULTICAST_PREFIX)) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " IPv6 address is invalid: multicast addresses are denied");
		}

		// Could not filter out anycast addresses (indistinguishable from other unicast addresses)
	}

	//-------------------------------------------------------------------------------------------------
	private void validateHostName(final String address) {
		logger.debug("AddressValidator.validateHostName started...");

		if (address.length() > HOSTNAME_MAX_LENGTH) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " hostname is invalid: max length is " + HOSTNAME_MAX_LENGTH);
		}

		if (!hostnamePattern.matcher(address).matches()) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " is not a hostname");
		}

		// Filter out host names that has highest-level component which only contains digits
		final String hlComponent = address.split(DELIMITER_DOT_REGEX)[0];
		boolean isHlComponentNumber = !hlComponent.chars().anyMatch(c -> Character.isAlphabetic(c));

		if (isHlComponentNumber) {
			throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " is not a hostname");
		}

		if (!allowSelfAddressing) {
			// Filter out 'localhost' and 'loopback'
			if (address.equalsIgnoreCase(HOSTNAME_LOCALHOST) || address.equalsIgnoreCase(HOSTNAME_LOOPBACK)) {
				throw new InvalidParameterException(ERROR_MSG_PREFIX + address + " hostname is invalid: self-addressing is disabled");
			}
		}
	}
}