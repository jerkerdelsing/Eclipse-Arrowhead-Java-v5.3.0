/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.api.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(SoLocalCloudConfiguratorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GeneralManagementAPI {

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	@PostMapping(path = Constants.HTTP_API_OP_LOGS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody LogEntryListResponseDTO getLogEntries(@RequestBody(required = false) final LogRequestDTO dto) {
		final String origin = HttpMethod.POST.name() + " " + SoLocalCloudConfiguratorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_LOGS_PATH;
		return logService.getLogEntries(dto, origin);
	}

	@GetMapping(path = Constants.HTTP_API_OP_GET_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody KeyValuesDTO getConfig(@RequestParam(required = false, name = Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM) final List<String> keys) {
		final String origin = HttpMethod.GET.name() + " " + SoLocalCloudConfiguratorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_GET_CONFIG_PATH;
		return configService.getConfig(keys, origin);
	}
}
