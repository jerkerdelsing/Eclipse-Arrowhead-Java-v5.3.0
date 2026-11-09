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
package eu.arrowhead.authentication.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.service.IdentityService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.security.TokenCertificateBinding;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.IdentityChangeRequestDTO;
import eu.arrowhead.dto.IdentityLoginResponseDTO;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(AuthenticationConstants.HTTP_API_IDENTITY_PATH)
public class IdentityAPI {

	//=================================================================================================
	// members

	@Autowired
	private IdentityService identityService;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns an identity token for the identified system")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = IdentityLoginResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_SERVICE_UNAVAILABLE, description = Constants.SWAGGER_HTTP_503_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthenticationConstants.HTTP_API_OP_LOGIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody IdentityLoginResponseDTO login(final HttpServletRequest request, @RequestBody final IdentityRequestDTO dto) {
		logger.debug("login started...");
		checkCertificateMatches(request, dto == null ? null : dto.systemName());

		final String origin = HttpMethod.POST.name() + " " + AuthenticationConstants.HTTP_API_IDENTITY_PATH + AuthenticationConstants.HTTP_API_OP_LOGIN_PATH;

		return identityService.loginOperation(dto, false, origin).response();
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Logs out from the Local Cloud")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Void.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_SERVICE_UNAVAILABLE, description = Constants.SWAGGER_HTTP_503_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthenticationConstants.HTTP_API_OP_LOGOUT_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public void logout(final HttpServletRequest request, @RequestBody final IdentityRequestDTO dto) {
		logger.debug("logout started...");
		checkCertificateMatches(request, dto == null ? null : dto.systemName());

		final String origin = HttpMethod.POST.name() + " " + AuthenticationConstants.HTTP_API_IDENTITY_PATH + AuthenticationConstants.HTTP_API_OP_LOGOUT_PATH;
		identityService.logoutOperation(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Changes the credentials (where it is possible) of the identified system")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Void.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_SERVICE_UNAVAILABLE, description = Constants.SWAGGER_HTTP_503_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = AuthenticationConstants.HTTP_API_OP_CHANGE_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public void change(final HttpServletRequest request, @RequestBody final IdentityChangeRequestDTO dto) {
		logger.debug("change started...");
		checkCertificateMatches(request, dto == null ? null : dto.systemName());

		final String origin = HttpMethod.POST.name() + " " + AuthenticationConstants.HTTP_API_IDENTITY_PATH + AuthenticationConstants.HTTP_API_OP_CHANGE_PATH;
		identityService.changeOperation(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns information about a system (if verified) based on token")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = IdentityVerifyResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
	@GetMapping(path = AuthenticationConstants.HTTP_API_OP_VERIFY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody IdentityVerifyResponseDTO verify(@PathVariable final String token) {
		logger.debug("verify started...");

		final String origin = HttpMethod.GET.name() + " " + AuthenticationConstants.HTTP_API_IDENTITY_PATH
				+ AuthenticationConstants.HTTP_API_OP_VERIFY_PATH.replace(AuthenticationConstants.HTTP_PARAM_TOKEN, token);

		return identityService.verifyOperation(token, origin);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// Over mutual TLS a system can only log in, log out or change credentials as the system named by its client certificate.
	private void checkCertificateMatches(final HttpServletRequest request, final String systemName) {
		if (systemName != null) {
			TokenCertificateBinding.check(request, systemName, systemNameNormalizer);
		}
	}
}