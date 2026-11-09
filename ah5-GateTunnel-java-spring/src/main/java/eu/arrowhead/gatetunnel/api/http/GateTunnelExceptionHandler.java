/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.enums.ExceptionType;
import eu.arrowhead.gatetunnel.service.GateTunnelApiException;
import jakarta.servlet.http.HttpServletRequest;

// Errors of the GateTunnel API keep the 5.2.2 status codes (400 for invalid input, 413, 502, 503, ...) in the Arrowhead
// error body. Arrowhead exceptions are handled by the common ArrowheadResponseEntityExceptionHandler.
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GateTunnelExceptionHandler {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(GateTunnelApiException.class)
	public ResponseEntity<ErrorMessageDTO> handle(final GateTunnelApiException ex, final HttpServletRequest request) {
		return error(ex.getStatus(), ex.getMessage(), request);
	}

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorMessageDTO> handle(final IllegalArgumentException ex, final HttpServletRequest request) {
		return error(400, ex.getMessage(), request);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	static ResponseEntity<ErrorMessageDTO> error(final int status, final String message, final HttpServletRequest request) {
		return ResponseEntity.status(status)
				.cacheControl(CacheControl.noStore())
				.contentType(MediaType.APPLICATION_JSON)
				.body(new ErrorMessageDTO(message, status, type(status), request.getMethod() + " " + request.getRequestURI()));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	private static ExceptionType type(final int status) {
		return switch (status) {
		case 400, 413 -> ExceptionType.INVALID_PARAMETER;
		case 401 -> ExceptionType.AUTH;
		case 403 -> ExceptionType.FORBIDDEN;
		case 404 -> ExceptionType.DATA_NOT_FOUND;
		case 502, 503 -> ExceptionType.EXTERNAL_SERVER_ERROR;
		default -> ExceptionType.INTERNAL_SERVER_ERROR;
		};
	}
}
