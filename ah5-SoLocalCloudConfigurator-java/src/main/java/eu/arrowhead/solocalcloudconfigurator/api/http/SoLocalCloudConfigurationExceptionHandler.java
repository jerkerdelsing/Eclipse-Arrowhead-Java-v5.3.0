/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.api.http;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.arrowhead.solocalcloudconfigurator.Refused;

// {"error": "..."}; 400 for invalid input, the Refused status for refusals, 500 otherwise (details only in the log).
@RestControllerAdvice(assignableTypes = SoLocalCloudConfigurationAPI.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SoLocalCloudConfigurationExceptionHandler {

	private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger(SoLocalCloudConfigurationExceptionHandler.class);

	@ExceptionHandler(Refused.class)
	public ResponseEntity<Map<String, String>> refused(final Refused ex) {
		return error(ex.status(), ex.getMessage());
	}

	@ExceptionHandler({ IllegalArgumentException.class, JsonProcessingException.class })
	public ResponseEntity<Map<String, String>> invalid(final Exception ex) {
		final String message = ex instanceof JsonProcessingException j ? j.getOriginalMessage() : ex.getMessage();
		return error(400, String.valueOf(message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> failed(final Exception ex) {
		LOG.error("soLocalCloudConfiguration request failed", ex);
		return error(500, "Internal error; see the SoLocalCloudConfigurator log");
	}

	private static ResponseEntity<Map<String, String>> error(final int status, final String message) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).body(Map.of("error", message));
	}
}
