/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.api.http;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.gatekeeper.service.ApiError;

// Errors of the Gatekeeper controllers with the native status codes and {"error": message} bodies; Arrowhead exceptions
// keep the common ErrorMessageDTO (the native "error" field is added by NativeResponseFilter).
@RestControllerAdvice(basePackageClasses = GatekeeperExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatekeeperExceptionHandler {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(ApiError.class)
	public ResponseEntity<Map<String, Object>> apiError(final ApiError ex) {
		return ResponseEntity.status(ex.getStatus()).body(m("error", ex.getMessage()));
	}

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> invalid(final IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(m("error", ex.getMessage()));
	}

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(ArrowheadException.class)
	public ResponseEntity<ErrorMessageDTO> arrowhead(final ArrowheadException ex) {
		return ResponseEntity.status(HttpUtilities.calculateHttpStatusFromArrowheadException(ex)).body(HttpUtilities.createErrorMessageDTO(ex));
	}

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> failure(final Exception ex) {
		logger.error("Gatekeeper request failed: {}", ex.getClass().getSimpleName());
		logger.debug(ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(m("error", "Internal service failure"));
	}
}
