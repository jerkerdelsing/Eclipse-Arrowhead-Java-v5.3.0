/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

// Plain Jackson (not the Spring-configured mapper), as the native 5.2.2 implementation, so that stored and published
// observations keep exactly the same JSON serialisation.
public final class Json {

	//=================================================================================================
	// members

	public static final ObjectMapper MAPPER = new ObjectMapper();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// ordered map from key/value pairs; values may be null
	public static Map<String, Object> map(final Object... pairs) {
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
	public static List<String> strings(final Object value) {
		return list(value).stream().map(Json::str).toList();
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
	public static String now() {
		return Instant.now().toString();
	}

	//-------------------------------------------------------------------------------------------------
	public static double epoch(final String time) {
		return Instant.parse(time).toEpochMilli() / 1000.0;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Json() {
		throw new UnsupportedOperationException();
	}
}
