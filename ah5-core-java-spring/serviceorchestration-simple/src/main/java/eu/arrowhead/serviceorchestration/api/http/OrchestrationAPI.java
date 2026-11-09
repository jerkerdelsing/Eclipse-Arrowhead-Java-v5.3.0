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

import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.serviceorchestration.service.OrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class OrchestrationAPI {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private OrchestrationService orchService;

	@Autowired
	private SystemNamePreprocessor preprocessor;

	//=================================================================================================
	// methods

	// pull
	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the orchestration results from the simple store")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrchestrationResponseDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@PostMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody OrchestrationResponseDTO pull(final HttpServletRequest httpServletRequest, @RequestBody(required = false) final OrchestrationRequestDTO dto) {
		logger.debug("pull started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH + SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PULL_PATH;

		final String requesterSystem = preprocessor.process(httpServletRequest, origin);
		return orchService.pull(requesterSystem, dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns a subscription id. Existing subscriptions will be overwritten.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@PostMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> pushSubscribe(
			final HttpServletRequest httpServletRequest,
			@RequestBody final OrchestrationSubscriptionRequestDTO dto,
			@Parameter(name = SimpleStoreServiceOrchestrationConstants.PARAM_NAME_TRIGGER, description = "Set to true in order to initiate a push orchestration after the successful subscription")
			@RequestParam(required = false, defaultValue = "false") final Boolean trigger) {
		logger.debug("pushSubscribe started...");

		final String origin = HttpMethod.POST.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH + SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_SUBSCRIBE_PATH;
		final String requesterSystem = preprocessor.process(httpServletRequest, origin);
		final Pair<Boolean, String> result = orchService.pushSubscribe(requesterSystem, dto, trigger, origin);

		return new ResponseEntity<String>(result.getRight(), result.getLeft() ? HttpStatus.CREATED : HttpStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Removes the subscription by the given id")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_NO_CONTENT, description = Constants.SWAGGER_HTTP_204_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))}),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class))})
	})
	@DeleteMapping(path = SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH)
	public ResponseEntity<Void> pushUnsubscribe(final HttpServletRequest httpServletRequest, @PathVariable final String id) {
		logger.debug("pushUnsubscribe started...");

		final String origin = HttpMethod.DELETE.name() + " " + SimpleStoreServiceOrchestrationConstants.HTTP_API_ORCHESTRATION_PATH
				+ SimpleStoreServiceOrchestrationConstants.HTTP_API_OP_PUSH_UNSUBSCRIBE_PATH.replace(SimpleStoreServiceOrchestrationConstants.HTTP_PATH_PARAM_ID, id);
		final String requesterSystem = preprocessor.process(httpServletRequest, origin);
		final boolean result = orchService.pushUnsubscribe(requesterSystem, id, origin);

		return new ResponseEntity<Void>(result ? HttpStatus.OK : HttpStatus.NO_CONTENT);
	}

}
