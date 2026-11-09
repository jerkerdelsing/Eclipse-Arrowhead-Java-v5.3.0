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
package eu.arrowhead.common.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.enums.ExceptionType;

@ExtendWith(MockitoExtension.class)
public class ArrowheadResponseEntityExceptionHandlerTest {

	//=================================================================================================
	// members

	private ArrowheadResponseEntityExceptionHandler handler = new ArrowheadResponseEntityExceptionHandler();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testHandleArrowheadException() {
		final WebRequest requestMock = Mockito.mock(WebRequest.class);
		final ArrowheadException ex = new ForbiddenException("test", "testOrigin");

		final ResponseEntity<Object> response = handler.handleArrowheadException(ex, requestMock);

		assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
		assertTrue(response.getBody() instanceof ErrorMessageDTO);
		final ErrorMessageDTO dto = (ErrorMessageDTO) response.getBody();
		assertEquals(403, dto.errorCode());
		assertEquals(ExceptionType.FORBIDDEN, dto.exceptionType());
		assertEquals("test", dto.errorMessage());
		assertEquals("testOrigin", dto.origin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testHandleArrowheadExceptionNoOrigin() {
		final WebRequest requestMock = Mockito.mock(WebRequest.class);
		final ArrowheadException ex = new InvalidParameterException("test");

		when(requestMock.getContextPath()).thenReturn("context path");

		final ResponseEntity<Object> response = handler.handleArrowheadException(ex, requestMock);

		verify(requestMock).getContextPath();

		assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
		assertTrue(response.getBody() instanceof ErrorMessageDTO);
		final ErrorMessageDTO dto = (ErrorMessageDTO) response.getBody();
		assertEquals(400, dto.errorCode());
		assertEquals(ExceptionType.INVALID_PARAMETER, dto.exceptionType());
		assertEquals("test", dto.errorMessage());
		assertEquals("context path", dto.origin());
	}
}
