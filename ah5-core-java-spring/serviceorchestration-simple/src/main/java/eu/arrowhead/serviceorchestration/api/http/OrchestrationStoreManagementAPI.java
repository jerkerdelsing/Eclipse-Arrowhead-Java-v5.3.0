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
package eu.arrowhead.serviceorchestration.api.http;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.serviceorchestration.service.OrchestrationStoreManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class OrchestrationStoreManagementAPI {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private OrchestrationStoreManagementService mgmtService;

	@Autowired
	private SystemNamePreprocessor preprocessor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the store records according to the filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationSimpleStoreListResponseDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@PostMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody OrchestrationSimpleStoreListResponseDTO query(@RequestBody final OrchestrationSimpleStoreQueryRequestDTO dto) {
		logger.debug("query started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH + SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH;
		return mgmtService.querySimpleStoreEntries(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the store records that were actually created")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationSimpleStoreListResponseDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_CREATE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody OrchestrationSimpleStoreListResponseDTO create(final HttpServletRequest httpServletRequest, @RequestBody final OrchestrationSimpleStoreListRequestDTO dto) {
		logger.debug("create started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH + SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_CREATE_PATH;
		final String requesterName = preprocessor.process(httpServletRequest, origin);
		return mgmtService.createSimpleStoreEntries(dto, requesterName, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the store records that were updated")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationSimpleStoreListResponseDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@PutMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_MODIFY_PRIORITIES_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody OrchestrationSimpleStoreListResponseDTO modifyPriorities(
			final HttpServletRequest httpServletRequest,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "Map of UUID strings and priority levels",
					required = true,
					content = @Content(
							schema = @Schema(
									implementation = PriorityRequestDTO.class,
									example = "{ \"123e4567-e89b-12d3-a456-426614174000\": 5 }"
							)
					)
			)
			@RequestBody final PriorityRequestDTO dto) {
		logger.debug("modifyPriorities started...");

		final String origin = HttpMethod.PUT.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH + SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_MODIFY_PRIORITIES_PATH;
		final String requesterName = preprocessor.process(httpServletRequest, origin);
		return mgmtService.modifyPriorities(dto, requesterName, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Deletes the simple store records by UUID.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@DeleteMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_REMOVE_PATH)
	public void remove(final @RequestParam List<String> uuids) {
		logger.debug("remove started...");

		final String origin = HttpMethod.DELETE.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_STORE_MANAGEMENT_PATH + SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_REMOVE_PATH;
		mgmtService.removeSimpleStoreEntries(uuids, origin);
	}
}
