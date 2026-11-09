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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.authorization.service.AuthorizationTokenService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import eu.arrowhead.dto.ErrorMessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class AuthorizationTokenAPI {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationTokenService authTokenService;

	@Autowired
	private SystemNamePreprocessor sysNamePreprocessor;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns a generated authorization token and its parameters.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationTokenGenerationResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_GENERATE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public AuthorizationTokenGenerationResponseDTO generate(final HttpServletRequest httpServletRequest, @RequestBody final AuthorizationTokenGenerationRequestDTO dto) {
		logger.debug("generate started...");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH + AuthorizationConstants.HTTP_API_OP_GENERATE_PATH;
		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);

		return authTokenService.generate(requesterSystem, dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Verifies the provided token and returns the belonged details.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationTokenVerifyResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@GetMapping(path = AuthorizationConstants.HTTP_API_OP_TOKEN_VERIFY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody AuthorizationTokenVerifyResponseDTO verify(final HttpServletRequest httpServletRequest, @PathVariable(required = true) final String token) {
		logger.debug("verify started...");

		final String origin = HttpMethod.GET.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH
				+ AuthorizationConstants.HTTP_API_OP_TOKEN_VERIFY_PATH.replace(AuthorizationConstants.HTTP_PATH_PARAM_TOKEN, token);
		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);

		return authTokenService.verify(requesterSystem, token, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the public key of the server if any.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_NOT_FOUND, description = Constants.SWAGGER_HTTP_404_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@GetMapping(path = AuthorizationConstants.HTTP_API_OP_PUBLIC_KEY_PATH, produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKey() {
		logger.debug("getPublicKey started...");

		final String origin = HttpMethod.GET.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH + AuthorizationConstants.HTTP_API_OP_PUBLIC_KEY_PATH;

		return authTokenService.getPublicKey(origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Registers the provided encryption key and returns the belonged auxiliary if any.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = String.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public String registerEncryptionKey(final HttpServletRequest httpServletRequest, @RequestBody final AuthorizationEncryptionKeyRegistrationRequestDTO dto) {
		logger.debug("registerEncryptionKey started...");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH + AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH;
		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);

		return authTokenService.registerEncryptionKey(requesterSystem, dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Unregisters the encryption key belonged to the requester system.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_NO_CONTENT, description = Constants.SWAGGER_HTTP_204_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@DeleteMapping(path = AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH)
	public ResponseEntity<Void> unregisterEncryptionKey(final HttpServletRequest httpServletRequest) {
		logger.debug("unregisterEncryptionKey started...");

		final String origin = HttpMethod.DELETE.name() + " " + AuthorizationConstants.HTTP_API_AUTHORIZATION_TOKEN_PATH + AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH;
		final String requesterSystem = sysNamePreprocessor.process(httpServletRequest, origin);
		final boolean result = authTokenService.unregisterEncryptionKey(requesterSystem, origin);

		return new ResponseEntity<Void>(result ? HttpStatus.OK : HttpStatus.NO_CONTENT);
	}
}