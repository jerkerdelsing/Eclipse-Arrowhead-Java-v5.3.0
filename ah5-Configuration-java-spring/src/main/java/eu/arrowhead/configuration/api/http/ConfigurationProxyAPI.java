/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.configuration.ConfigurationConstants;
import eu.arrowhead.configuration.api.dto.ConfigurationRequestDTO;
import eu.arrowhead.configuration.api.dto.ConfigurationResponseDTO;
import eu.arrowhead.configuration.security.ProxyAccess;
import eu.arrowhead.configuration.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// configurationProxy service: an authorized system reads or stores the configuration of another system
// (v4.6 conf-by-proxy and save-conf-by-proxy), for example a gateway acting for devices without Arrowhead identity.
@RestController
@RequestMapping(ConfigurationConstants.HTTP_API_PROXY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ConfigurationProxyAPI {

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private ProxyAccess proxyAccess;

	@Operation(summary = "Returns the configuration of the named system (configurationProxy.get-configuration)")
	@GetMapping(path = ConfigurationConstants.HTTP_API_OP_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ConfigurationResponseDTO getConfiguration(@PathVariable(ConfigurationConstants.PATH_VARIABLE_SYSTEM_NAME) final String systemName,
			final HttpServletRequest request) {
		final String origin = "GET " + ConfigurationConstants.HTTP_API_PROXY_PATH + ConfigurationConstants.HTTP_API_OP_CONFIG_PATH;
		proxyAccess.check(request, ConfigurationConstants.SERVICE_OP_GET_CONFIGURATION, origin);
		return configurationService.getConfiguration(systemName, origin);
	}

	@Operation(summary = "Stores the configuration of the system named in the body (configurationProxy.store-configuration)")
	@PutMapping(path = ConfigurationConstants.HTTP_API_OP_PROXY_SAVE_CONFIG_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ConfigurationResponseDTO storeConfiguration(@RequestBody final ConfigurationRequestDTO dto, final HttpServletRequest request) {
		final String origin = "PUT " + ConfigurationConstants.HTTP_API_PROXY_PATH + ConfigurationConstants.HTTP_API_OP_PROXY_SAVE_CONFIG_PATH;
		proxyAccess.check(request, ConfigurationConstants.SERVICE_OP_STORE_CONFIGURATION, origin);
		return configurationService.storeConfiguration(null, dto, origin);
	}
}
