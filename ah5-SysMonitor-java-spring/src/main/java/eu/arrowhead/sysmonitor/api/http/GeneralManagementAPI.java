/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.api.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.service.StatusService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// generalManagement: status (as in the native 5.2.2 implementation; 200 running, 503 degraded), get-log and get-config.
// The common management filter (/mgmt/ paths) lets only Sysop and the management whitelist (LocalCloudManager) in.
@RestController
@RequestMapping(SysMonitorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GeneralManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	@Autowired
	private StatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_STATUS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> status(final HttpServletRequest request) {
		Responses.caller(request);
		return Responses.json(statusService.healthCode(), statusService.status());
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = Constants.HTTP_API_OP_LOGS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody LogEntryListResponseDTO getLogEntries(final HttpServletRequest request, @RequestBody(required = false) final LogRequestDTO dto) {
		Responses.caller(request);
		return logService.getLogEntries(dto, Responses.origin(request));
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = Constants.HTTP_API_OP_GET_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody KeyValuesDTO getConfig(final HttpServletRequest request, @RequestParam(required = false, name = Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM) final List<String> keys) {
		Responses.caller(request);
		return configService.getConfig(keys, Responses.origin(request));
	}
}
