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
package eu.arrowhead.serviceregistry.api.http;

import java.util.Map.Entry;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.service.DeviceDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class DeviceDiscoveryAPI {

	//=================================================================================================
	// members

	@Autowired
	private DeviceDiscoveryService ddService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the newly created or matching device entry")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_CREATED, description = Constants.SWAGGER_HTTP_201_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DeviceResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DeviceResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = ServiceRegistryConstants.HTTP_API_OP_REGISTER_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DeviceResponseDTO> register(@RequestBody final DeviceRequestDTO dto) {
		logger.debug("register started");

		final String origin = HttpMethod.POST.name() + " " + ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH + ServiceRegistryConstants.HTTP_API_OP_REGISTER_PATH;
		final Entry<DeviceResponseDTO, Boolean> result = ddService.registerDevice(dto, origin);

		return new ResponseEntity<DeviceResponseDTO>(result.getKey(), result.getValue() ? HttpStatus.CREATED : HttpStatus.OK);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns device entries according to the given filters")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DeviceListResponseDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@PostMapping(path = ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody DeviceListResponseDTO lookup(@RequestBody(required = false) final DeviceLookupRequestDTO dto) {
		logger.debug("lookup started");

		final String origin = HttpMethod.POST.name() + " " + ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH + ServiceRegistryConstants.HTTP_API_OP_LOOKUP_PATH;

		return ddService.lookupDevice(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Deletes device entry by name if exists and no systems are assigned to it")
	@ApiResponses(value = {
			@ApiResponse(responseCode = Constants.HTTP_STATUS_OK, description = Constants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_NO_CONTENT, description = Constants.SWAGGER_HTTP_204_MESSAGE),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_BAD_REQUEST, description = Constants.SWAGGER_HTTP_400_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_UNAUTHORIZED, description = Constants.SWAGGER_HTTP_401_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_FORBIDDEN, description = Constants.SWAGGER_HTTP_403_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_LOCKED, description = Constants.SWAGGER_HTTP_423_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) }),
			@ApiResponse(responseCode = Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR, description = Constants.SWAGGER_HTTP_500_MESSAGE, content = {
					@Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorMessageDTO.class)) })
	})
	@DeleteMapping(path = ServiceRegistryConstants.HTTP_API_OP_DEVICE_REVOKE_PATH)
	public ResponseEntity<Void> revoke(final @PathVariable String name) {
		logger.debug("revoke started");

		final String origin = HttpMethod.DELETE.name() + " " + ServiceRegistryConstants.HTTP_API_DEVICE_DISCOVERY_PATH
				+ ServiceRegistryConstants.HTTP_API_OP_DEVICE_REVOKE_PATH.replace(ServiceRegistryConstants.HTTP_PARAM_NAME, name);
		final boolean result = ddService.revokeDevice(name, origin);

		return new ResponseEntity<Void>(result ? HttpStatus.OK : HttpStatus.NO_CONTENT);
	}
}