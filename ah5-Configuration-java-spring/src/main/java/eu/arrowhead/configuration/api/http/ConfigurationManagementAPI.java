/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.configuration.ConfigurationConstants;
import eu.arrowhead.configuration.api.dto.ConfigurationListResponseDTO;
import eu.arrowhead.configuration.api.dto.ConfigurationRequestDTO;
import eu.arrowhead.configuration.api.dto.ConfigurationResponseDTO;
import eu.arrowhead.configuration.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

// configurationManagement service; access is enforced by the common management filter (management.policy).
@RestController
@RequestMapping(ConfigurationConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ConfigurationManagementAPI {

	@Autowired
	private ConfigurationService configurationService;

	@Operation(summary = "Lists all stored configurations (configurationManagement.list-configurations)")
	@GetMapping(path = ConfigurationConstants.HTTP_API_OP_MGMT_CONFIGS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ConfigurationListResponseDTO listConfigurations() {
		return configurationService.listConfigurations();
	}

	@Operation(summary = "Creates or replaces the configuration of a system (configurationManagement.store-configuration)")
	@PutMapping(path = ConfigurationConstants.HTTP_API_OP_CONFIG_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ConfigurationResponseDTO storeConfiguration(@PathVariable(ConfigurationConstants.PATH_VARIABLE_SYSTEM_NAME) final String systemName,
			@RequestBody final ConfigurationRequestDTO dto) {
		final String origin = "PUT " + ConfigurationConstants.HTTP_API_MANAGEMENT_PATH + ConfigurationConstants.HTTP_API_OP_CONFIG_PATH;
		return configurationService.storeConfiguration(systemName, dto, origin);
	}

	@Operation(summary = "Removes the configuration of a system and returns it (configurationManagement.remove-configuration)")
	@DeleteMapping(path = ConfigurationConstants.HTTP_API_OP_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ConfigurationResponseDTO removeConfiguration(@PathVariable(ConfigurationConstants.PATH_VARIABLE_SYSTEM_NAME) final String systemName) {
		final String origin = "DELETE " + ConfigurationConstants.HTTP_API_MANAGEMENT_PATH + ConfigurationConstants.HTTP_API_OP_CONFIG_PATH;
		return configurationService.removeConfiguration(systemName, origin);
	}
}
