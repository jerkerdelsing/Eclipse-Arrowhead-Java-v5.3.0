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
package eu.arrowhead.authorization.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.authorization.service.AuthorizationService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.AuthorizationGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationLookupRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.ErrorMessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class AuthorizationAPI {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationService authService;

	@Autowired
	private SystemNamePreprocessor sysNamePreprocessor;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the created authorization policy")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationPolicyResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationPolicyResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_GRANT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ResponseEntity<AuthorizationPolicyResponseDTO> grant(final HttpServletRequest httpServletRequest, @RequestBody final AuthorizationGrantRequestDTO dto) {
		logger.debug("grant started");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH + AuthorizationConstants.HTTP_API_OP_GRANT_PATH;
		final String provider = sysNamePreprocessor.process(httpServletRequest, origin);
		final Pair<AuthorizationPolicyResponseDTO, Boolean> result = authService.grantOperation(provider, dto, origin);

		return new ResponseEntity<>(result.getFirst(), result.getSecond() ? HttpStatus.CREATED : HttpStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Deletes policy instance entry if exists")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_NO_CONTENT, description = Constants.SWAGGER_HTTP_204_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@DeleteMapping(path = AuthorizationConstants.HTTP_API_OP_SERVICE_REVOKE_PATH)
	public ResponseEntity<Void> revoke(final HttpServletRequest httpServletRequest, @PathVariable(required = true) final String instanceId) {
		logger.debug("revoke started");

		final String origin = HttpMethod.DELETE.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH
				+ AuthorizationConstants.HTTP_API_OP_SERVICE_REVOKE_PATH.replace(AuthorizationConstants.HTTP_PARAM_INSTANCE_ID, instanceId);
		final String identifiedSystemName = sysNamePreprocessor.process(httpServletRequest, origin);
		final boolean result = authService.revokeOperation(identifiedSystemName, instanceId, origin);

		return new ResponseEntity<Void>(result ? HttpStatus.OK : HttpStatus.NO_CONTENT);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the requester created authorization policies according to the given filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationPolicyListResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_LOOKUP_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody AuthorizationPolicyListResponseDTO lookup(
			final HttpServletRequest httpServletRequest,
			@RequestBody final AuthorizationLookupRequestDTO dto) {
		logger.debug("lookup started");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH + AuthorizationConstants.HTTP_API_OP_LOOKUP_PATH;
		final String provider = sysNamePreprocessor.process(httpServletRequest, origin);

		return authService.lookupOperation(provider, dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns whether a consumer has access to a specified service/service operation/event type.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Boolean.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_VERIFY_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody boolean verify(
			final HttpServletRequest httpServletRequest,
			@RequestBody final AuthorizationVerifyRequestDTO dto) {
		logger.debug("verify started");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_PATH + AuthorizationConstants.HTTP_API_OP_VERIFY_PATH;
		final String requester = sysNamePreprocessor.process(httpServletRequest, origin);

		return authService.verifyOperation(requester, dto, origin);
	}
}