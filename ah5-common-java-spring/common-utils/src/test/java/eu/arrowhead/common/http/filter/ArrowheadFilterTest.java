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
package eu.arrowhead.common.http.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class ArrowheadFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TestArrowheadFilter filter;

	@Spy
	private ObjectMapper mapper;

	@Mock
	private FilterChain chain;

	@Mock
	private HttpServletResponse response;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternal() throws IOException, ServletException {
		assertDoesNotThrow(() -> filter.doFilterInternal(new MockHttpServletRequest(), null, chain));

		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testShouldNotFilterFalse() throws ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

		assertFalse(filter.shouldNotFilter(request));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testShouldNotFilterTrue() throws ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();

		request.setRequestURI("/");
		assertTrue(filter.shouldNotFilter(request));

		request.setRequestURI("/v3/api-docs/something");
		assertTrue(filter.shouldNotFilter(request));

		request.setRequestURI("/swagger-ui");
		assertTrue(filter.shouldNotFilter(request));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testHandleException() throws IOException {
		final ArrowheadException ex = new ForbiddenException("test", "testOrigin");
		final PrintWriter writerMock = Mockito.mock(PrintWriter.class);

		doNothing().when(response).setContentType("application/json");
		doNothing().when(response).setStatus(anyInt());
		when(response.getWriter()).thenReturn(writerMock);
		doNothing().when(writerMock).print(anyString());
		doNothing().when(writerMock).flush();

		assertDoesNotThrow(() -> filter.handleException(ex, response));

		verify(response).setContentType("application/json");
		verify(response).setStatus(403);
		verify(response, times(2)).getWriter();
		verify(writerMock).print(anyString());
		verify(writerMock).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testHandleExceptionNoOrigin() throws IOException {
		final ArrowheadException ex = new ForbiddenException("test", (String) null);
		final PrintWriter writerMock = Mockito.mock(PrintWriter.class);

		doNothing().when(response).setContentType("application/json");
		doNothing().when(response).setStatus(anyInt());
		when(response.getWriter()).thenReturn(writerMock);
		doNothing().when(writerMock).print(anyString());
		doNothing().when(writerMock).flush();

		assertDoesNotThrow(() -> filter.handleException(ex, response));

		verify(response).setContentType("application/json");
		verify(response).setStatus(403);
		verify(response, times(2)).getWriter();
		verify(writerMock).print(anyString());
		verify(writerMock).flush();
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static final class TestArrowheadFilter extends ArrowheadFilter {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@Override
		protected boolean isActive() {
			return false;
		}
	}
}