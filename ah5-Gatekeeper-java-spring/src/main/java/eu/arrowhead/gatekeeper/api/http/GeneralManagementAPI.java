/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.api.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

// generalManagement 1.0.0 under /gatekeeper/mgmt/general: the native status operation plus the common get-log and get-config.
@RestController
@RequestMapping(GatekeeperConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GeneralManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private StatusService statusService;

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Status of the system")
	@GetMapping(path = GatekeeperConstants.HTTP_API_OP_GENERAL_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> status() {
		return statusService.status();
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the log entries")
	@PostMapping(path = Constants.HTTP_API_OP_LOGS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public LogEntryListResponseDTO getLogEntries(@RequestBody(required = false) final LogRequestDTO dto) {
		return logService.getLogEntries(dto, HttpMethod.POST.name() + " " + GatekeeperConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_LOGS_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the public configuration")
	@GetMapping(path = Constants.HTTP_API_OP_GET_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public KeyValuesDTO getConfig(@RequestParam(required = false) final List<String> keys) {
		return configService.getConfig(keys, HttpMethod.GET.name() + " " + GatekeeperConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + Constants.HTTP_API_OP_GET_CONFIG_PATH);
	}
}
