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
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_LEGACY_HEALTH;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_ECHO;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_HEALTH;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_MONITOR_ECHO;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatetunnel.service.GateTunnelStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class MonitorAPI {

	//=================================================================================================
	// members

	@Autowired
	private GateTunnelStatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Echo")
	@GetMapping(path = { BASE + HTTP_API_OP_ECHO, BASE + HTTP_API_OP_MONITOR_ECHO, LEGACY + HTTP_API_OP_ECHO, LEGACY + HTTP_API_OP_MONITOR_ECHO, HTTP_API_OP_ECHO,
			HTTP_API_OP_MONITOR_ECHO })
	public ResponseEntity<Map<String, Object>> echo() {
		return ApiSupport.json(HttpStatus.OK, statusService.echo());
	}

	//-------------------------------------------------------------------------------------------------
	// Liveness only (status, system). Since the Spring port it needs an identity token like every other path.
	@Operation(summary = "Liveness")
	@GetMapping(path = { HTTP_API_OP_HEALTH, HTTP_API_LEGACY_HEALTH })
	public ResponseEntity<Map<String, Object>> health() {
		return ApiSupport.json(HttpStatus.OK, statusService.liveness());
	}
}
