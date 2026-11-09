/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

// JSON helpers of the relay wire format (AH-GT-1 and legacy-mTLS messages, relayed HTTP requests). A plain ObjectMapper is
// used on purpose, independent of the Spring MVC mapper, so the bytes on the relay stay exactly those of 5.2.2.
public final class Wire {

	//=================================================================================================
	// members

	public static final ObjectMapper MAPPER = new ObjectMapper();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> m(final Object... pairs) {
		final Map<String, Object> result = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			result.put((String) pairs[i], pairs[i + 1]);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static Map<String, Object> obj(final Object value) {
		return value instanceof Map<?, ?> ? (Map<String, Object>) value : new LinkedHashMap<>();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static List<Object> list(final Object value) {
		return value instanceof List<?> ? (List<Object>) value : List.of();
	}

	//-------------------------------------------------------------------------------------------------
	public static String str(final Object value) {
		return value == null ? "" : value.toString();
	}

	//-------------------------------------------------------------------------------------------------
	public static int integer(final Object value, final int fallback) {
		try {
			return Integer.parseInt(str(value));
		} catch (final Exception ex) {
			return fallback;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static Object parse(final String text) {
		try {
			return MAPPER.readValue(text, Object.class);
		} catch (final Exception ex) {
			throw new IllegalArgumentException("Invalid JSON");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static Object parse(final byte[] data) {
		return parse(new String(data, StandardCharsets.UTF_8));
	}

	//-------------------------------------------------------------------------------------------------
	public static String json(final Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (final Exception ex) {
			throw new IllegalArgumentException("JSON serialization failed", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static byte[] bytes(final Object value) {
		return json(value).getBytes(StandardCharsets.UTF_8);
	}

	//-------------------------------------------------------------------------------------------------
	public static Set<String> csv(final String value) {
		final Set<String> result = new TreeSet<>();
		if (value == null) {
			return result;
		}

		for (final String item : value.split(",")) {
			if (!item.isBlank()) {
				result.add(item.trim());
			}
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public static String id() {
		return UUID.randomUUID().toString();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Wire() {
		throw new UnsupportedOperationException();
	}
}
