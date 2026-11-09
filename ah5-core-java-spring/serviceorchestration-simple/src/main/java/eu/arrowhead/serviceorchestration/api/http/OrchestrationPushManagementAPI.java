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
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.serviceorchestration.service.OrchestrationPushManagementService;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class OrchestrationPushManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationPushManagementService pushService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemNamePreprocessor preprocessor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the created subscription records. Existing ones will be overwritten.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationSubscriptionListResponseDTO.class))}),
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
	@PostMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody OrchestrationSubscriptionListResponseDTO pushSubscribe(final HttpServletRequest httpServletRequest, @RequestBody final OrchestrationSubscriptionListRequestDTO dto) {
		logger.debug("pushSubscribe started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH
				+ SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH;
		final String requesterSystem = preprocessor.process(httpServletRequest, origin);

		return pushService.pushSubscribe(requesterSystem, dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the created push orchestration job records")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationPushJobListResponseDTO.class))}),
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
	@PostMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_TRIGGER_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public OrchestrationPushJobListResponseDTO pushTrigger(final HttpServletRequest httpServletRequest, @RequestBody final OrchestrationPushTriggerDTO dto) {
		logger.debug("pushTrigger started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH
				+ SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_TRIGGER_PATH;
		final String requesterSystem = preprocessor.process(httpServletRequest, origin);

		return pushService.pushTrigger(requesterSystem, dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Removes the specified subscriptions")
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
	@DeleteMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_BULK_PATH)
	public void pushUnsubscribe(final HttpServletRequest httpServletRequest, final @RequestParam List<String> ids) {
		logger.debug("pushUnsubscribe started...");

		final String origin = HttpMethod.DELETE.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH
				+ SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_BULK_PATH;
		final String requesterSystem = preprocessor.process(httpServletRequest, origin);

		pushService.pushUnsubscribe(requesterSystem, ids, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the required subcription records.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationSubscriptionListResponseDTO.class))}),
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
	public OrchestrationSubscriptionListResponseDTO queryPushSubscriptions(@RequestBody(required = false) final OrchestrationSubscriptionQueryRequestDTO dto) {
		logger.debug("queryPushSubscriptions started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PUSH_MANAGEMENT_PATH
				+ SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_QUERY_PATH;

		return pushService.queryPushSubscriptions(dto, origin);
	}

}
