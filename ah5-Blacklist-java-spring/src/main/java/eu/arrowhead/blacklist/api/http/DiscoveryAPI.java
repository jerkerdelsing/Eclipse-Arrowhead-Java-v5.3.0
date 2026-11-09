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
package eu.arrowhead.blacklist.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.blacklist.service.DiscoveryService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.ErrorMessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(BlacklistConstants.HTTP_API_BASE_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class DiscoveryAPI {

	//=================================================================================================
	// members

	@Autowired
	private DiscoveryService discoveryService;

	@Autowired
	private SystemNamePreprocessor preprocessor;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// check
	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns if the specified system name is on the blacklist (boolean value)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "boolean")) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@GetMapping(path = BlacklistConstants.HTTP_API_OP_CHECK_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean check(@PathVariable(required = true) final String systemName) {
		logger.debug("Check started for {}", systemName);

		final String origin = HttpMethod.GET.name() + " " + BlacklistConstants.HTTP_API_BASE_PATH
				+ BlacklistConstants.HTTP_API_OP_CHECK_PATH.replace(BlacklistConstants.HTTP_API_PARAM_NAME, systemName);

		return discoveryService.check(systemName, origin);
	}

	// lookup
	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the active blacklist entries of the requester")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BlacklistEntryListResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@GetMapping(path = BlacklistConstants.HTTP_API_OP_LOOKUP, produces = MediaType.APPLICATION_JSON_VALUE)
	public BlacklistEntryListResponseDTO lookup(final HttpServletRequest httpServletRequest) {
		logger.debug("Lookup started");

		final String origin = HttpMethod.GET.name() + " " + BlacklistConstants.HTTP_API_BASE_PATH + BlacklistConstants.HTTP_API_OP_LOOKUP;
		final String identifiedName = preprocessor.process(httpServletRequest, origin);

		return discoveryService.lookup(identifiedName, origin);
	}
}