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

import static eu.arrowhead.gatetunnel.api.http.ApiSupport.BASE;
import static eu.arrowhead.gatetunnel.api.http.ApiSupport.LEGACY;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_SESSIONS;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_SESSIONS_CLOSE;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_STATUS;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatetunnel.service.ControlSessionService;
import eu.arrowhead.gatetunnel.service.GateTunnelStatusService;
import eu.arrowhead.gatetunnel.service.Wire;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

// gateTunnelManagement (LocalCloudManager and Sysop).
@RestController
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GateTunnelManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private ControlSessionService controlSessions;

	@Autowired
	private GateTunnelStatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Lists the control sessions, newest first")
	@GetMapping(path = { BASE + HTTP_API_OP_SESSIONS, LEGACY + HTTP_API_OP_SESSIONS, HTTP_API_OP_SESSIONS })
	public ResponseEntity<Map<String, Object>> sessions(@RequestParam(required = false) final String limit) {
		return ApiSupport.json(HttpStatus.OK, controlSessions.list(limit));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Closes a control or inter-cloud session; 404 if none was open")
	@PostMapping(path = { BASE + HTTP_API_OP_SESSIONS_CLOSE, LEGACY + HTTP_API_OP_SESSIONS_CLOSE, HTTP_API_OP_SESSIONS_CLOSE }, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> close(@RequestBody(required = false) final byte[] body) {
		final boolean changed = controlSessions.close(ApiSupport.payload(body));
		return ApiSupport.json(changed ? HttpStatus.OK : HttpStatus.NOT_FOUND, Wire.m("closed", changed));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Relay link and inter-cloud session status")
	@GetMapping(path = { BASE + HTTP_API_OP_STATUS, LEGACY + HTTP_API_OP_STATUS, HTTP_API_OP_STATUS })
	public ResponseEntity<Map<String, Object>> status() {
		return ApiSupport.json(HttpStatus.OK, statusService.status());
	}
}
