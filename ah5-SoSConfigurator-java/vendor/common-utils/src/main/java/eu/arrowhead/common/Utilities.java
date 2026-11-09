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
package eu.arrowhead.common;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.arrowhead.common.exception.ArrowheadException;

public final class Utilities {

	//=================================================================================================
	// members

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;

	static {
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static boolean isEmpty(final String str) {
		return str == null || str.isBlank();
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isEmpty(final Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isEmpty(final Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	//-------------------------------------------------------------------------------------------------
	public static <T> boolean containsNull(final Iterable<T> iterable) {
		Assert.notNull(iterable, "iterable is null");

		for (final T element : iterable) {
			if (element == null) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean containsNullOrEmpty(final Iterable<String> iterable) {
		Assert.notNull(iterable, "iterable is null");

		for (final String element : iterable) {
			if (isEmpty(element)) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String toJson(final Object object) {
		if (object == null) {
			return null;
		}

		try {
			return mapper.writeValueAsString(object);
		} catch (final JsonProcessingException ex) {
			throw new ArrowheadException("The specified object cannot be converted to text", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static <T> T fromJson(final String json, final Class<T> parsedClass) {
		if (json == null || parsedClass == null) {
			return null;
		}

		try {
			return mapper.readValue(json, parsedClass);
		} catch (final IOException ex) {
			throw new ArrowheadException("The specified string cannot be converted to a(n) " + parsedClass.getSimpleName() + " object", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static <T> T fromJson(final String json, final com.fasterxml.jackson.core.type.TypeReference<T> reference) {
		if (json == null || reference == null) {
			return null;
		}

		try {
			return mapper.readValue(json, reference);
		} catch (final IOException ex) {
			throw new ArrowheadException("The specified string cannot be converted to a(n) " + reference.getType() + " object", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String toPrettyJson(final String jsonString) {
		try {
			if (jsonString != null) {
				final String jsonString_ = jsonString.trim();
				if (jsonString_.startsWith("{")) {
					final Object tempObj = mapper.readValue(jsonString_, Object.class);
					return mapper.writeValueAsString(tempObj);
				} else {
					final Object[] tempObj = mapper.readValue(jsonString_, Object[].class);
					return mapper.writeValueAsString(tempObj);
				}
			}
		} catch (final IOException __) {
			// it seems it is not a JSON string, so we just return untouched
		}

		return jsonString;
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String stripEndSlash(final String uri) {
		if (uri != null && uri.endsWith("/")) {
			return uri.substring(0, uri.length() - 1);
		}

		return uri;
	}

	//-------------------------------------------------------------------------------------------------
	public static String convertZonedDateTimeToUTCString(final ZonedDateTime time) {
		if (time == null) {
			return null;
		}

		return dateTimeFormatter.format(time.toInstant());
	}

	//-------------------------------------------------------------------------------------------------
	public static ZonedDateTime parseUTCStringToZonedDateTime(final String timeStr) throws DateTimeParseException {
		if (isEmpty(timeStr)) {
			return null;
		}

		final TemporalAccessor tempAcc = dateTimeFormatter.parse(timeStr);

		return ZonedDateTime.ofInstant(Instant.from(tempAcc), ZoneId.of(Constants.UTC));
	}

	//-------------------------------------------------------------------------------------------------
	public static ZonedDateTime utcNow() {
		return ZonedDateTime.now(ZoneId.of(Constants.UTC));
	}

	//-------------------------------------------------------------------------------------------------
	public static <E extends Enum<E>> boolean isEnumValue(final String value, final Class<E> enumClass) {
		if (value == null || enumClass == null) {
			return false;
		}

		try {
			Enum.valueOf(enumClass, value);
			return true;
		} catch (final IllegalArgumentException __) {
			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isUUID(final String value) {
		if (isEmpty(value)) {
			return false;
		}

		try {
			UUID.fromString(value);
			return true;
		} catch (final IllegalArgumentException __) {
			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static int parseToInt(final Object obj) {
		return switch (obj) {
		case final Integer i -> i;
		case final Number n -> n.intValue();
		case final String s -> Integer.parseInt(s);
		case null, default -> throw new NumberFormatException("Invalid input: " + obj);
		};
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	public static String bytesToHex(final byte[] bytes) {
		Assert.notNull(bytes, "bytes array is null");

		final StringBuilder hexString = new StringBuilder(2 * bytes.length);
		for (final byte b : bytes) {
			final String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}

		return hexString.toString();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Utilities() {
		throw new UnsupportedOperationException();
	}
}