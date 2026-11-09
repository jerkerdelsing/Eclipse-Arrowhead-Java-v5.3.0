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
package eu.arrowhead.serviceregistry.api.http.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;

public class SystemNamePreprocessorTest {

	//=================================================================================================
	// members

	private final SystemNamePreprocessor preprocessor = new SystemNamePreprocessor();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void process1DTONull() {
		final SystemRequestDTO result = preprocessor.process((SystemRegisterRequestDTO) null, null, null);

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void process1Ok() {
		final SystemRegisterRequestDTO dto = new SystemRegisterRequestDTO(
				Map.of("a", "b"),
				"1.0.0",
				List.of("localhost"),
				"DEVICE");
		final MockHttpServletRequest request = new MockHttpServletRequest();

		try (MockedStatic<HttpUtilities> staticMock = Mockito.mockStatic(HttpUtilities.class)) {
			staticMock.when(() -> HttpUtilities.acquireName(request, "origin")).thenReturn("TestConsumer");

			final SystemRequestDTO result = preprocessor.process(dto, request, "origin");

			assertEquals("TestConsumer", result.name());
			assertEquals(Map.of("a", "b"), result.metadata());
			assertEquals("1.0.0", result.version());
			assertEquals(List.of("localhost"), result.addresses());
			assertEquals("DEVICE", result.deviceName());

			staticMock.verify(() -> HttpUtilities.acquireName(request, "origin"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void process2DTONull() {
		final ServiceInstanceRequestDTO result = preprocessor.process((ServiceInstanceCreateRequestDTO) null, null, null);

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void process2Ok() {
		final ServiceInstanceCreateRequestDTO dto = new ServiceInstanceCreateRequestDTO(
				"testService",
				"1.0.0",
				null,
				Map.of("a", "b"),
				List.of());
		final MockHttpServletRequest request = new MockHttpServletRequest();

		try (MockedStatic<HttpUtilities> staticMock = Mockito.mockStatic(HttpUtilities.class)) {
			staticMock.when(() -> HttpUtilities.acquireName(request, "origin")).thenReturn("TestConsumer");

			final ServiceInstanceRequestDTO result = preprocessor.process(dto, request, "origin");

			assertEquals("TestConsumer", result.systemName());
			assertEquals("testService", result.serviceDefinitionName());
			assertEquals(Map.of("a", "b"), result.metadata());
			assertEquals("1.0.0", result.version());
			assertNull(result.expiresAt());
			assertEquals(List.of(), result.interfaces());

			staticMock.verify(() -> HttpUtilities.acquireName(request, "origin"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void process3Ok() {
		final MockHttpServletRequest request = new MockHttpServletRequest();

		try (MockedStatic<HttpUtilities> staticMock = Mockito.mockStatic(HttpUtilities.class)) {
			staticMock.when(() -> HttpUtilities.acquireName(request, "origin")).thenReturn("TestConsumer");

			final String result = preprocessor.process(request, "origin");

			assertEquals("TestConsumer", result);

			staticMock.verify(() -> HttpUtilities.acquireName(request, "origin"));
		}
	}
}