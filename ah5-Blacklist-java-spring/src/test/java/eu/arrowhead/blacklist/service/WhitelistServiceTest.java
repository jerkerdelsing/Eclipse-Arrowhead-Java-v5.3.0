/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.blacklist.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.blacklist.jpa.service.EntryDbService;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;

@ExtendWith(MockitoExtension.class)
public class WhitelistServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private WhitelistService service;

	@Mock
	private EntryDbService dbService;

	@Mock
	private Normalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckWhitelistOriginNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.checkWhitelist(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckWhitelistOriginEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.checkWhitelist(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckWhitelistNullInput() {
		assertDoesNotThrow(() -> service.checkWhitelist(null, "origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckWhitelistEmptyInput() {
		assertDoesNotThrow(() -> service.checkWhitelist(List.of(), "origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckWhitelistMatches() {
		final List<String> whitelist = List.of("TestSystem1", "TestSystem3");
		final List<String> input = List.of("TestSystem1", "TestSystem2", "TestSystem3");

		ReflectionTestUtils.setField(service, "normalizedWhitelist", whitelist);

		when(normalizer.normalizeSystemNames(input)).thenReturn(input);

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.checkWhitelist(input, "origin"));

		assertEquals("The following system names cannot be added, because they are on the whitelist: TestSystem1, TestSystem3", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(normalizer).normalizeSystemNames(input);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckWhitelistNoMatch() {
		final List<String> whitelist = List.of("TestSystem4", "TestSystem5");
		final List<String> input = List.of("TestSystem1", "TestSystem2", "TestSystem3");

		ReflectionTestUtils.setField(service, "normalizedWhitelist", whitelist);

		when(normalizer.normalizeSystemNames(input)).thenReturn(input);

		assertDoesNotThrow(() -> service.checkWhitelist(input, "origin"));

		verify(normalizer).normalizeSystemNames(input);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCleanDatabaseEmptyWhitelist() {
		assertDoesNotThrow(() -> service.cleanDatabase());

		verify(dbService, never()).inactivateNameList(anyList(), eq("Blacklist"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCleanDatabaseOk() {
		final List<String> whitelist = List.of("TestSystem4", "TestSystem5");

		ReflectionTestUtils.setField(service, "normalizedWhitelist", whitelist);
		doNothing().when(dbService).inactivateNameList(whitelist, "Blacklist");

		assertDoesNotThrow(() -> service.cleanDatabase());

		verify(dbService).inactivateNameList(whitelist, "Blacklist");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitNullWhitelist() {
		ReflectionTestUtils.setField(service, "whitelist", null);
		ReflectionTestUtils.setField(service, "normalizedWhitelist", List.of());

		ReflectionTestUtils.invokeMethod(service, "init");

		final List<String> list = (List<String>) ReflectionTestUtils.getField(service, "normalizedWhitelist");
		assertTrue(list.isEmpty());

		verify(normalizer, never()).normalizeSystemNames(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitEmptyWhitelist() {
		ReflectionTestUtils.setField(service, "whitelist", null);
		ReflectionTestUtils.setField(service, "normalizedWhitelist", List.of());

		ReflectionTestUtils.invokeMethod(service, "init");

		final List<String> list = (List<String>) ReflectionTestUtils.getField(service, "normalizedWhitelist");
		assertTrue(list.isEmpty());

		verify(normalizer, never()).normalizeSystemNames(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitOk() {
		ReflectionTestUtils.setField(service, "whitelist", List.of("TestSystem"));
		List<String> list = (List<String>) ReflectionTestUtils.getField(service, "normalizedWhitelist");
		assertTrue(list.isEmpty());

		when(normalizer.normalizeSystemNames(List.of("TestSystem"))).thenReturn(List.of("TestSystem"));

		ReflectionTestUtils.invokeMethod(service, "init");

		list = (List<String>) ReflectionTestUtils.getField(service, "normalizedWhitelist");
		assertEquals(List.of("TestSystem"), list);

		verify(normalizer).normalizeSystemNames(List.of("TestSystem"));
	}
}