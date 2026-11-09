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

import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.AuthorizationDefaults;
import eu.arrowhead.authorization.api.http.utils.SystemNamePreprocessor;
import eu.arrowhead.authorization.service.AuthorizationTokenManagementService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyListResponseDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.ErrorMessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class AuthorizationTokenManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private SystemNamePreprocessor sysNamePreprocessor;

	@Autowired
	private AuthorizationTokenManagementService mgmtService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:linelength")
	@Operation(summary = "Returns the generated authorization tokens and their parameters.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationTokenMgmtListResponseDTO.class)) }),
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
	public @ResponseBody AuthorizationTokenMgmtListResponseDTO generateTokens(
			final HttpServletRequest httpServletRequest,
			@RequestBody final AuthorizationTokenGenerationMgmtListRequestDTO dto,
			@Parameter(name = Constants.UNBOUND, description = "Set true if you want to skip the authorization check. (It should be configured in the application.properties as well)", example = "true") @RequestParam(required = false, defaultValue = AuthorizationDefaults.DEFAULT_UNBOUND_VALUE) final boolean unbound) {
		logger.debug("generateTokens started");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH
				+ AuthorizationConstants.HTTP_API_OP_GENERATE_PATH;
		final String requester = sysNamePreprocessor.process(httpServletRequest, origin);

		return mgmtService.generateTokensOperation(requester, dto, unbound, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the matching token entries according to the given filters.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationTokenMgmtListResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_QUERY_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody AuthorizationTokenMgmtListResponseDTO queryTokens(@RequestBody final AuthorizationTokenQueryRequestDTO dto) {
		logger.debug("queryTokens started");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH
				+ AuthorizationConstants.HTTP_API_OP_QUERY_PATH;

		return mgmtService.queryTokensOperation(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Deletes token entries by tokenReference if exists")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@DeleteMapping(path = AuthorizationConstants.HTTP_API_OP_REVOKE_PATH)
	public void revokeTokens(@RequestParam final List<String> tokenReferences) {
		logger.debug("revokeTokens started");

		final String origin = HttpMethod.DELETE.name() + " " + AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH
				+ AuthorizationConstants.HTTP_API_OP_REVOKE_PATH;

		mgmtService.revokeTokensOperation(tokenReferences, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the created encryption key entries.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthorizationMgmtEncryptionKeyListResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody AuthorizationMgmtEncryptionKeyListResponseDTO addEncryptionKeys(@RequestBody final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto) {
		logger.debug("addEncryptionKeys started");

		final String origin = HttpMethod.POST.name() + " " + AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH
				+ AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH;

		return mgmtService.addEncryptionKeysOperation(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Deletes encryption key entries if exists")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@DeleteMapping(path = AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH)
	public void removeEncryptionKeys(@RequestParam final List<String> systemNames) {
		logger.debug("removeEncryptionKeys started");

		final String origin = HttpMethod.DELETE.name() + " " + AuthorizationConstants.HTTP_API_MANAGEMENT_PATH + AuthorizationConstants.HTTP_API_TOKEN_SUB_PATH
				+ AuthorizationConstants.HTTP_API_OP_ENCRYPTION_KEY_PATH;

		mgmtService.removeEncryptionKeysOperation(systemNames, origin);
	}
}