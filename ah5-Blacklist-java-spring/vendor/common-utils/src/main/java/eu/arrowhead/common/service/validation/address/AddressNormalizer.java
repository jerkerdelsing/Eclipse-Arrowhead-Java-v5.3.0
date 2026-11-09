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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;

@Component
public class AddressNormalizer {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	private static final String DASH = "-";
	private static final String COLON = ":";
	private static final String DOUBLE_COLON = "::";
	private static final int MAC_DOT_PARTS_LENGTH = 3;
	private static final int MAC_DASH_OR_COLON_PARTS_LENGTH = 6;
	private static final int MAC_DASH_OR_COLON_PART_CHAR_LENGTH = 2;
	private static final int OCTET_MIN_LENGTH = 0;
	private static final int OCTET_MAX_LENGTH = 255;
	private static final int IPV4_PARTS_LENGTH = 4;
	private static final int IPV6_GROUP_LENGTH = 4;
	private static final int IPV6_SIZE = 8;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String normalize(final String address) {
		logger.debug("normalize started...");

		if (Utilities.isEmpty(address)) {
			return "";
		}

		final String candidate = address.toLowerCase().trim();
		if (!candidate.contains(Constants.DOT) && !candidate.contains(COLON) && !candidate.contains(DASH)) {
			// Simple string
			return candidate;
		} else if (candidate.split("\\" + Constants.DOT).length == MAC_DOT_PARTS_LENGTH
				|| (candidate.contains(DASH) && candidate.split(DASH).length == MAC_DASH_OR_COLON_PARTS_LENGTH)
				|| (!candidate.contains(DOUBLE_COLON) && candidate.split(COLON).length == MAC_DASH_OR_COLON_PARTS_LENGTH)) {
			// Possible MAC address
			return normalizeMAC(candidate);
		} else if (candidate.contains(Constants.DOT) && !candidate.contains(COLON)) {
			// Possible IPv4 or domain name
			return candidate;
		} else if (!candidate.contains(Constants.DOT) && candidate.contains(COLON)) {
			// Possible IPv6
			return normalizeIPv6(candidate);
		} else {
			// Possible IPv6-IPv4 hybrid
			return normalizeIPv6IPv4Hybrid(candidate);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private String normalizeMAC(final String candidate) {
		logger.debug("normalizeMAC started...");

		String[] groups = new String[MAC_DASH_OR_COLON_PARTS_LENGTH];

		if (candidate.contains(DASH)) {
			groups = candidate.split(DASH);
		}

		if (candidate.contains(COLON)) {
			groups = candidate.split(COLON);
		}

		if (candidate.contains(Constants.DOT)) {
			final String flat = candidate.replace(Constants.DOT, "");
			if (flat.length() != MAC_DASH_OR_COLON_PARTS_LENGTH * MAC_DASH_OR_COLON_PART_CHAR_LENGTH) {
				return candidate; // not MAC
			}

			int startIdx = 0;
			for (int i = 0; i < groups.length; ++i) {
				groups[i] = flat.substring(startIdx, startIdx + MAC_DASH_OR_COLON_PART_CHAR_LENGTH);
				startIdx = startIdx + MAC_DASH_OR_COLON_PART_CHAR_LENGTH;
			}
		}

		final List<String> parts = Arrays.asList(groups);
		if (!Utilities.isEmpty(parts.stream().filter(p -> p.length() != 2).collect(Collectors.toList()))) {
			return candidate; // not MAC
		}

		return parts.stream().collect(Collectors.joining(COLON));
	}

	//-------------------------------------------------------------------------------------------------
	private String normalizeIPv6(final String candidate) {
		logger.debug("normalizeIPv6 started...");

		if (candidate.split(DOUBLE_COLON, -1).length > 2) { // More than one double colon is present
			return candidate; // not IPv6
		}

		final List<String> groups = new ArrayList<>(IPV6_SIZE);
		int lastAbbreviatedGroupIdx = -1;

		final String[] split = candidate.split(COLON, -1); // -1 is present in order to not trim trailing empty strings
		for (int i = 0; i < split.length; ++i) {
			String group = split[i];

			// Handle double colons
			if (Utilities.isEmpty(group)) {
				lastAbbreviatedGroupIdx = i;
				group = "0000";

				// Add leading zeroes
			} else if (group.length() < IPV6_GROUP_LENGTH) {
				final int candidateGroupLength = group.length();
				for (int j = 0; j < (IPV6_GROUP_LENGTH - candidateGroupLength); ++j) {
					group = "0" + group;
				}
			}

			groups.add(group);
		}

		final int candidateSize = groups.size();
		if (lastAbbreviatedGroupIdx == -1) {
			// Handle invalid size
			if (candidateSize != IPV6_SIZE) {
				return candidate; // not IPv6
			}
		} else {
			// Handle invalid size
			if (candidateSize > IPV6_SIZE) {
				return candidate; // not IPv6
			}

			for (int i = 0; i < (IPV6_SIZE - candidateSize); ++i) {
				groups.add(lastAbbreviatedGroupIdx, "0000");
			}
		}

		// Assemble final string address
		final StringJoiner normalized = new StringJoiner(COLON);
		for (final String group : groups) {
			normalized.add(group);
		}

		return normalized.toString();
	}

	//-------------------------------------------------------------------------------------------------
	private String normalizeIPv6IPv4Hybrid(final String candidate) {
		logger.debug("normalizeIPv6IPv4Hybrid started...");

		final String[] split = candidate.split(COLON);
		final String ip4str = split[split.length - 1];
		final String[] ip4parts = ip4str.split("\\.");

		// handle invalid IPv4 size
		if (ip4parts.length != IPV4_PARTS_LENGTH) {
			logger.debug("unprocessable IPv6-IPv4 hybrid. Invalid IPv4 part: " + ip4str);
			return candidate; // AddressValidator will filter it out
		}

		// transform IPv4 to Hexadecimal
		final StringBuilder ip4HexBuilder = new StringBuilder();

		for (int i = 0; i < IPV4_PARTS_LENGTH; ++i) {
			try {
				final int octet = Integer.parseInt(ip4parts[i]);
				if (octet > OCTET_MAX_LENGTH || octet < OCTET_MIN_LENGTH) {
					logger.debug("unprocessable IPv6-IPv4 hybrid. Invalid IPv4 part: " + ip4str);
					return candidate; // AddressValidator will filter it out
				}

				final String hex = Integer.toHexString(octet);
				if (hex.length() == 1) {
					ip4HexBuilder.append("0");
				}
				ip4HexBuilder.append(hex);
				if (i == 1) {
					ip4HexBuilder.append(COLON);
				}
			} catch (final NumberFormatException ex) {
				logger.debug("unprocessable IPv6-IPv4 hybrid. Not number octet: " + ip4str);
				return candidate; // AddressValidator will filter it out
			}
		}

		final String converted = candidate.replace(ip4str, ip4HexBuilder.toString());

		return normalizeIPv6(converted);
	}
}