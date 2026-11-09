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
package eu.arrowhead.serviceregistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@ExtendWith(MockitoExtension.class)
public class ServiceRegistrySystemInfoTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceRegistrySystemInfo sysInfo;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasClientDirectAccessSystemNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> sysInfo.hasClientDirectAccess(null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasClientDirectAccessSystemNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> sysInfo.hasClientDirectAccess(""));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasClientDirectAccessEmptyOriginalList() {
		final boolean result = sysInfo.hasClientDirectAccess("TestConsumer");

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasClientDirectAccessEmptyElementOnOriginalList() {
		ReflectionTestUtils.setField(sysInfo, "serviceDiscoveryDirectAccess", List.of(""));
		ReflectionTestUtils.setField(sysInfo, "normalizedServiceDiscoveryDirectAccess", new ArrayList<>(1));

		final boolean result = sysInfo.hasClientDirectAccess("TestConsumer");

		assertFalse(result);

		verify(systemNameNormalizer, never()).normalize(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasClientDirectAccessEmptyNormalizedListNoMatch() {
		ReflectionTestUtils.setField(sysInfo, "serviceDiscoveryDirectAccess", List.of("TestAdmin"));
		ReflectionTestUtils.setField(sysInfo, "normalizedServiceDiscoveryDirectAccess", new ArrayList<>(1));

		when(systemNameNormalizer.normalize("TestAdmin")).thenReturn("TestAdmin");

		final boolean result = sysInfo.hasClientDirectAccess("TestConsumer");

		assertFalse(result);

		verify(systemNameNormalizer).normalize("TestAdmin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasClientDirectAccessNotEmptyNormalizedListMatch() {
		ReflectionTestUtils.setField(sysInfo, "serviceDiscoveryDirectAccess", List.of("TestAdmin"));
		ReflectionTestUtils.setField(sysInfo, "normalizedServiceDiscoveryDirectAccess", List.of("TestAdmin"));

		final boolean result = sysInfo.hasClientDirectAccess("TestAdmin");

		assertTrue(result);

		verify(systemNameNormalizer, never()).normalize(anyString());
	}
}