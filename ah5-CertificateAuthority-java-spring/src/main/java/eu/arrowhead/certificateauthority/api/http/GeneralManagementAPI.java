/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.api.http;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;
import eu.arrowhead.certificateauthority.security.Caller;
import eu.arrowhead.certificateauthority.service.CertificateAuthorityStatusService;
import eu.arrowhead.certificateauthority.service.CertificateManagementService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.service.ConfigService;
import eu.arrowhead.common.service.LogService;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.dto.LogEntryListResponseDTO;
import eu.arrowhead.dto.LogRequestDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// generalManagement: status (as in the native 5.2.2 implementation), get-log and get-config. Sysop and the CA only.
@RestController
@RequestMapping(CertificateAuthorityConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GeneralManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private LogService logService;

	@Autowired
	private ConfigService configService;

	@Autowired
	private CertificateManagementService managementService;

	@Autowired
	private CertificateAuthorityStatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = CertificateAuthorityConstants.HTTP_API_OP_STATUS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> status(final HttpServletRequest request) {
		final String origin = CertificateAuthorityAPI.origin(request);
		managementService.requireManager(Caller.of(request, origin), origin);
		return CertificateAuthorityAPI.json(HttpStatus.OK, statusService.status());
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = Constants.HTTP_API_OP_LOGS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody LogEntryListResponseDTO getLogEntries(final HttpServletRequest request, @RequestBody(required = false) final LogRequestDTO dto) {
		final String origin = CertificateAuthorityAPI.origin(request);
		managementService.requireManager(Caller.of(request, origin), origin);
		return logService.getLogEntries(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = Constants.HTTP_API_OP_GET_CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody KeyValuesDTO getConfig(final HttpServletRequest request, @RequestParam(required = false, name = Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM) final List<String> keys) {
		final String origin = CertificateAuthorityAPI.origin(request);
		managementService.requireManager(Caller.of(request, origin), origin);
		return configService.getConfig(keys, origin);
	}
}
