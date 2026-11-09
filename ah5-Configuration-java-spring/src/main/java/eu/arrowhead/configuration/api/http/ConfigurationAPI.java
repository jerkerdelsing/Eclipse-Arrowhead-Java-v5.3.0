/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.configuration.ConfigurationConstants;
import eu.arrowhead.configuration.api.dto.ConfigurationResponseDTO;
import eu.arrowhead.configuration.jpa.entity.ConfigurationData;
import eu.arrowhead.configuration.security.RequestIdentity;
import eu.arrowhead.configuration.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// configuration service: a system reads its own configuration (v4.6 conf and confraw).
@RestController
@RequestMapping(ConfigurationConstants.HTTP_API_BASE_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ConfigurationAPI {

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private RequestIdentity identity;

	@Operation(summary = "Returns the requester's own configuration with Base64 content (configuration.get-configuration)")
	@GetMapping(path = ConfigurationConstants.HTTP_API_OP_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ConfigurationResponseDTO getConfiguration(@PathVariable(ConfigurationConstants.PATH_VARIABLE_SYSTEM_NAME) final String systemName,
			final HttpServletRequest request) {
		final String origin = "GET " + ConfigurationConstants.HTTP_API_BASE_PATH + ConfigurationConstants.HTTP_API_OP_CONFIG_PATH;
		return configurationService.getConfiguration(ownSystem(systemName, request, origin), origin);
	}

	@Operation(summary = "Returns the requester's own configuration file as raw bytes (configuration.get-raw-configuration)")
	@GetMapping(path = ConfigurationConstants.HTTP_API_OP_RAW_CONFIG_PATH)
	public ResponseEntity<byte[]> getRawConfiguration(@PathVariable(ConfigurationConstants.PATH_VARIABLE_SYSTEM_NAME) final String systemName,
			final HttpServletRequest request) {
		final String origin = "GET " + ConfigurationConstants.HTTP_API_BASE_PATH + ConfigurationConstants.HTTP_API_OP_RAW_CONFIG_PATH;
		final ConfigurationData data = configurationService.getConfigurationData(ownSystem(systemName, request, origin), origin);
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType(data.getContentType()));
		headers.setContentDisposition(ContentDisposition.attachment().filename(data.getFileName()).build());
		headers.setCacheControl("no-store");
		return ResponseEntity.ok().headers(headers).body(data.getData());
	}

	// A system may read only its own configuration (v4.6 compared the path with the certificate common name).
	private String ownSystem(final String systemName, final HttpServletRequest request, final String origin) {
		final String requester = identity.requester(request, origin);
		final String normalized = configurationService.normalizeSystemName(systemName, origin);
		if (!normalized.equals(requester)) {
			throw new ForbiddenException(requester + " may only read its own configuration", origin);
		}
		return normalized;
	}
}
