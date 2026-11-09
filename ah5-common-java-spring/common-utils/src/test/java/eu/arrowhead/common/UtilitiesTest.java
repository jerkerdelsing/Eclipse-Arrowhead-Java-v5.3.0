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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.enums.ExceptionType;

public class UtilitiesTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsEmptyString() {
		assertAll("isEmpty - string",
				() -> assertTrue(Utilities.isEmpty((String) null)),
				() -> assertTrue(Utilities.isEmpty("  ")),
				() -> assertFalse(Utilities.isEmpty("not empty")));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	public void testIsEmptyMap() {
		assertAll("isEmpty - map",
				() -> assertTrue(Utilities.isEmpty((Map) null)),
				() -> assertTrue(Utilities.isEmpty(Map.of())),
				() -> assertFalse(Utilities.isEmpty(Map.of("a", "b"))));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Test
	public void testIsEmptyCollection() {
		assertAll("isEmpty - collection",
				() -> assertTrue(Utilities.isEmpty((Collection) null)),
				() -> assertTrue(Utilities.isEmpty(List.of())),
				() -> assertFalse(Utilities.isEmpty(List.of("a"))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testContainsNullNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> Utilities.containsNull(null));

		assertEquals("iterable is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testContainsNull() {
		final List<String> wrongList = new ArrayList<>(1);
		wrongList.add(null);

		assertAll("containsNull - valid input",
				() -> assertTrue(Utilities.containsNull(wrongList)),
				() -> assertFalse(Utilities.containsNull(List.of("a"))));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testContainsNullOrEmptyNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> Utilities.containsNullOrEmpty(null));

		assertEquals("iterable is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testContainsNullOrEmpty() {
		assertAll("containsNullOrEmpty - valid input",
				() -> assertTrue(Utilities.containsNullOrEmpty(List.of(""))),
				() -> assertFalse(Utilities.containsNullOrEmpty(List.of("a"))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testToJsonValidInput() {
		assertAll("toJson - valid input",
				() -> assertNull(Utilities.toJson(null)),
				() -> assertEquals("{ \"a\" : \"b\" }", Utilities.toJson(Map.of("a", "b")).replaceAll("(?U)\\s+", " ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testToJsonInvalidInput() {
		final Throwable ex = assertThrows(ArrowheadException.class,
				() -> Utilities.toJson(new Object()));

		assertEquals("The specified object cannot be converted to text", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testFromJsonClassValidInput() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 400, ExceptionType.INVALID_PARAMETER, "testOrigin");

		assertAll("fromJson with Class parameter - valid input",
				() -> assertNull(Utilities.fromJson(null, ErrorMessageDTO.class)),
				() -> assertNull(Utilities.fromJson("text", (Class) null)),
				() -> assertEquals(
						dto,
						Utilities.fromJson("{ \"errorMessage\": \"test\", \"errorCode\": 400, \"exceptionType\": \"INVALID_PARAMETER\", \"origin\": \"testOrigin\" }", ErrorMessageDTO.class)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFromJsonClassInvalidInput() {
		final Throwable ex = assertThrows(ArrowheadException.class,
				() -> Utilities.fromJson("wrong", ErrorMessageDTO.class));

		assertEquals("The specified string cannot be converted to a(n) ErrorMessageDTO object", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testFromJsonTypeRefValidInput() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 400, ExceptionType.INVALID_PARAMETER, "testOrigin");
		final TypeReference<ErrorMessageDTO> typeRef = new TypeReference<ErrorMessageDTO>() {
		};

		assertAll("fromJson with type reference parameter - valid input",
				() -> assertNull(Utilities.fromJson(null, typeRef)),
				() -> assertNull(Utilities.fromJson("text", (TypeReference) null)),
				() -> assertEquals(
						dto,
						Utilities.fromJson("{ \"errorMessage\": \"test\", \"errorCode\": 400, \"exceptionType\": \"INVALID_PARAMETER\", \"origin\": \"testOrigin\" }", typeRef)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFromJsonTypeRefInvalidInput() {
		final TypeReference<ErrorMessageDTO> typeRef = new TypeReference<ErrorMessageDTO>() {
		};

		final Throwable ex = assertThrows(ArrowheadException.class,
				() -> Utilities.fromJson("wrong", typeRef));

		assertEquals("The specified string cannot be converted to a(n) class eu.arrowhead.dto.ErrorMessageDTO object", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testToPrettyJson() {
		assertAll("toPrettyJson",
				() -> assertNull(Utilities.toPrettyJson(null)),
				() -> assertEquals("string", Utilities.toPrettyJson("string")),
				() -> assertEquals("{ \"a\" : \"b\" }", Utilities.toPrettyJson("{\"a\":\"b\"}").replaceAll("(?U)\\s+", " ")),
				() -> assertEquals("[ \"a\", \"b\" ]", Utilities.toPrettyJson("[\"a\",\"b\"]").replaceAll("(?U)\\s+", " ")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStripEndSlath() {
		assertAll("stripEndSlash",
				() -> assertNull(Utilities.stripEndSlash(null)),
				() -> assertEquals("string", Utilities.stripEndSlash("string")),
				() -> assertEquals("string", Utilities.stripEndSlash("string/")),
				() -> assertEquals("/longer/path", Utilities.stripEndSlash("/longer/path/")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertZonedDateTimeToUTCString() {
		final ZonedDateTime time = ZonedDateTime.of(2025, 7, 31, 10, 12, 22, 0, ZoneId.of("UTC"));

		assertAll("convertZonedDateTimeToUTCString",
				() -> assertNull(Utilities.convertZonedDateTimeToUTCString(null)),
				() -> assertEquals("2025-07-31T10:12:22Z", Utilities.convertZonedDateTimeToUTCString(time)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseUTCStringToZonedDateTimeEmptyInput() {
		assertAll("parseUTCStringToZonedDateTime - empty input",
				() -> assertNull(Utilities.parseUTCStringToZonedDateTime(null)),
				() -> assertNull(Utilities.parseUTCStringToZonedDateTime("")));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testParseUTCStringToZonedDateTimeNormalInput() {
		final ZonedDateTime time = Utilities.parseUTCStringToZonedDateTime("2025-07-31T10:13:14Z");

		assertEquals(2025, time.getYear());
		assertEquals(7, time.getMonthValue());
		assertEquals(31, time.getDayOfMonth());
		assertEquals(10, time.getHour());
		assertEquals(13, time.getMinute());
		assertEquals(14, time.getSecond());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsEnumValue() {
		assertAll("isEnumValue",
				() -> assertFalse(Utilities.isEnumValue(null, AuthenticationPolicy.class)),
				() -> assertFalse(Utilities.isEnumValue("DECLARED", null)),
				() -> assertFalse(Utilities.isEnumValue("NOT_VALID", AuthenticationPolicy.class)),
				() -> assertTrue(Utilities.isEnumValue("DECLARED", AuthenticationPolicy.class)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsUUID() {
		assertAll("isEnumValue",
				() -> assertFalse(Utilities.isUUID(null)),
				() -> assertFalse(Utilities.isUUID(" ")),
				() -> assertFalse(Utilities.isUUID("not_a_uuid")),
				() -> assertTrue(Utilities.isUUID("ba6b3700-9485-433f-834a-1cf9a40b9f3e")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseToIntNullInput() {
		final Throwable ex = assertThrows(NumberFormatException.class,
				() -> Utilities.parseToInt(null));

		assertTrue(ex.getMessage().startsWith("Invalid input: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseToIntInvalidInput1() {
		final Throwable ex = assertThrows(NumberFormatException.class,
				() -> Utilities.parseToInt(new Object()));

		assertTrue(ex.getMessage().startsWith("Invalid input: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testParseToIntInvalidInput2() {
		final Throwable ex = assertThrows(NumberFormatException.class,
				() -> Utilities.parseToInt("not_number"));

		assertTrue(ex.getMessage().startsWith("For input string: \"not_number\""));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testParseToIntValidInput() {
		assertAll("parseToInt - valid input",
				() -> assertEquals(10, Utilities.parseToInt(Integer.valueOf(10))),
				() -> assertEquals(10, Utilities.parseToInt(Double.valueOf(10.2))),
				() -> assertEquals(10, Utilities.parseToInt("10")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBytesToHexNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> Utilities.bytesToHex(null));

		assertEquals("bytes array is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBytesToHexValidInput() {
		assertAll("bytesToHex - valid input",
				() -> assertEquals("", Utilities.bytesToHex(new byte[0])),
				() -> assertEquals("746573745f737472696e67", Utilities.bytesToHex("test_string".getBytes(StandardCharsets.UTF_8))));
	}
}