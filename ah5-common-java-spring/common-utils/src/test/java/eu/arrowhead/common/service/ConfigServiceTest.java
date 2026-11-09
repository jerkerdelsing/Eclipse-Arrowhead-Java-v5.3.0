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
package eu.arrowhead.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.ConfigValidation;
import eu.arrowhead.dto.KeyValuesDTO;

@ExtendWith(MockitoExtension.class)
public class ConfigServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ConfigService service;

	@Mock
	private ConfigValidation validator;

	@Mock
	private Environment environment;

	@Mock
	private SystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetConfigNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getConfig(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetConfigEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getConfig(null, " "));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetConfigExceptionForwarding() {
		when(validator.validateAndNormalizeConfigKeyList(anyList())).thenThrow(new InvalidParameterException("The list of the requested configuration keys is null or empty"));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> service.getConfig(List.of(), "test"));

		assertEquals("test", ((InvalidParameterException) ex).getOrigin());
		assertEquals("The list of the requested configuration keys is null or empty", ex.getMessage());

		verify(validator).validateAndNormalizeConfigKeyList(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGetConfig() {
		when(validator.validateAndNormalizeConfigKeyList(anyList())).thenReturn(List.of("a", "b", "c"));
		when(sysInfo.getConfigDefaultsMap()).thenReturn(Map.of("a", "aDefaultValue", "b", "bDefaultValue"));
		when(environment.getProperty("a")).thenReturn("aValue");
		when(environment.getProperty("b")).thenReturn(null);

		final KeyValuesDTO result = service.getConfig(List.of("a", "b", "c"), "test");

		assertEquals(2, result.map().size());
		assertEquals("aValue", result.map().get("a"));
		assertEquals("bDefaultValue", result.map().get("b"));
		assertNull(result.map().get("c"));

		verify(validator).validateAndNormalizeConfigKeyList(anyList());
		verify(sysInfo).getConfigDefaultsMap();
		verify(environment, times(3)).getProperty(anyString());
	}
}