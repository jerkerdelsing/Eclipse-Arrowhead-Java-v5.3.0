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
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_PUBLIC_KEY;

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

// gateTunnelKey: the public key of the GateTunnel's system certificate.
@RestController
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GateTunnelKeyAPI {

	//=================================================================================================
	// members

	@Autowired
	private GateTunnelStatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Returns the public key of the GateTunnel certificate")
	@GetMapping(path = { BASE + HTTP_API_OP_PUBLIC_KEY, LEGACY + HTTP_API_OP_PUBLIC_KEY, HTTP_API_OP_PUBLIC_KEY })
	public ResponseEntity<Map<String, Object>> publicKey() {
		return ApiSupport.json(HttpStatus.OK, statusService.publicKey());
	}
}
