/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.api.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.service.GateTunnelStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

// generalManagement at the 5.2.2 path /gatetunnel/mgmt/general: status (as before), plus the common log and config operations.
@RestController
@RequestMapping(GateTunnelConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GeneralManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private GateTunnelStatusService statusService;

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Status of the system")
	@GetMapping(path = GateTunnelConstants.HTTP_API_OP_GENERAL_STATUS)
	public ResponseEntity<Map<String, Object>> status() {
		return ApiSupport.json(HttpStatus.OK, statusService.status());
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the log entries")
	@PostMapping(path = Constants.HTTP_API_OP_LOGS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody LogEntryListResponseDTO getLogEntries(@RequestBody(required = false) final LogRequestDTO dto) {
		return logService.getLogEntries(dto, HttpMethod.POST.name() + " " + GateTunnelConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_LOGS_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the public configuration")
	@GetMapping(path = Constants.HTTP_API_OP_GET_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody KeyValuesDTO getConfig(@RequestParam(required = false) final List<String> keys) {
		return configService.getConfig(keys, HttpMethod.GET.name() + " " + GateTunnelConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_GET_CONFIG_PATH);
	}
}
