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
package eu.arrowhead.common.service.validation.meta;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class MetadataKeyEvaluatorTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getMetadataValueForEmptyInputs() {
		assertAll("Empty inputs",
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(null, "key")),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(Map.of(), "key")),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(Map.of("key", "text"), null)),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(Map.of("key", "text"), "")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getMetadataValueForWrongInputs() {
		assertAll("Wrong inputs",
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(Map.of("key", List.of()), "key.subkey")),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(Map.of("key", List.of()), "otherkey[0]")),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(Map.of("key", Map.of()), "key[1]"))

		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getMetadataValueForCompositeKeySimpleKeyText() {
		final String key = "key";
		final Map<String, Object> map = Map.of("key", "text");
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Simple key - text",
				() -> assertNotNull(value),
				() -> assertEquals(String.class, value.getClass()),
				() -> assertEquals("text", value));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeySimpleKeyNumber() {
		final String key = "key";
		final Map<String, Object> map = Map.of("key", 10);
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Simple key - number",
				() -> assertNotNull(value),
				() -> assertEquals(Integer.class, value.getClass()),
				() -> assertEquals(10, value));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeySimpleKeyList() {
		final String key = "key";
		final Map<String, Object> map = Map.of("key", List.of(10));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Simple key - list",
				() -> assertNotNull(value),
				() -> assertInstanceOf(List.class, value),
				() -> assertEquals(1, ((List<?>) value).size()),
				() -> assertEquals(10, ((List<?>) value).get(0)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyNotExistingSimpleKey() {
		final Map<String, Object> map = Map.of("key", List.of(10));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, "not_key");

		assertNull(value);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyNotExistingCompositeKey1() {
		final Map<String, Object> map = Map.of("key", Map.of("subkey", 10));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, "not_key");

		assertNull(value);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyNotExistingCompositeKey2() {
		final String key = "key.not_subkey";
		final Map<String, Object> map = Map.of("key", Map.of("subkey", 10));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertNull(value);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyExistingCompositeKeyNumber() {
		final String key = "key.subkey";
		final Map<String, Object> map = Map.of("key", Map.of("subkey", 10));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Composite key - number",
				() -> assertNotNull(value),
				() -> assertEquals(Integer.class, value.getClass()),
				() -> assertEquals(10, value));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyNoArrayAccessor() {
		final Map<String, Object> map = Map.of("key", List.of(1, 2));

		assertAll("Composite key (not array accessor)",
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, "otherkey")),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, "other[]key")),
				() -> assertNull(MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, "otherkey[]")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyExistingCompositeKeyArrayAccessor() {
		final String key = "key[1]";
		final Map<String, Object> map = Map.of("key", List.of(1, 2));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Composite key (array accessor) - number",
				() -> assertNotNull(value),
				() -> assertEquals(Integer.class, value.getClass()),
				() -> assertEquals(2, value));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyExistingCompositeKeyArrayAccessorNotNumber() {
		final String key = "key[text]";
		final Map<String, Object> map = Map.of("key", List.of(1, 2));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertNull(value);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyExistingCompositeKeyArrayAccessorNegativeNumber() {
		final String key = "key[-2]";
		final Map<String, Object> map = Map.of("key", List.of(1, 2));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertNull(value);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyExistingCompositeKeyArrayAccessorTooBigNumber() {
		final String key = "key[10]";
		final Map<String, Object> map = Map.of("key", List.of(1, 2));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertNull(value);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeySimpleKeyWithBrackets() {
		final String key = "key[text]";
		final Map<String, Object> map = Map.of("key[text]", 10);
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Simple key with brackets - number ",
				() -> assertNotNull(value),
				() -> assertEquals(Integer.class, value.getClass()),
				() -> assertEquals(10, value));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:magicnumber")
	public void getMetadataValueForCompositeKeyExistingCompositeKeyArrayAccessorAndSubkey() {
		final String key = "key[0].subkey";
		final Map<String, Object> map = Map.of("key", List.of(Map.of("subkey", 1.2)));
		final Object value = MetadataKeyEvaluator.getMetadataValueForCompositeKey(map, key);

		assertAll("Composite key (array accessor and subkey) - number",
				() -> assertNotNull(value),
				() -> assertEquals(Double.class, value.getClass()),
				() -> assertEquals(1.2, value));
	}
}