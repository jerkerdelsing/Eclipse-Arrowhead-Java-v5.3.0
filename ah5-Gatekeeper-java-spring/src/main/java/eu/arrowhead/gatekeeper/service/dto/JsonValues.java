/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service.dto;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

// Loosely typed JSON access, as the native 5.2.2 Gatekeeper used it: the relay protocol and the inter-cloud payloads are
// free-form maps whose shape must stay byte-for-byte compatible with the Go and Rust implementations.
public final class JsonValues {

	//=================================================================================================
	// members

	public static final ObjectMapper MAPPER = new ObjectMapper();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// insertion-ordered map of key/value pairs
	public static Map<String, Object> m(final Object... pairs) {
		final Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put((String) pairs[i], pairs[i + 1]);
		}

		return map;
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
		} catch (final NumberFormatException ex) {
			return fallback;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static List<String> strings(final Object value) {
		return list(value).stream().map(JsonValues::str).toList();
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

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private JsonValues() {
		throw new UnsupportedOperationException();
	}
}
