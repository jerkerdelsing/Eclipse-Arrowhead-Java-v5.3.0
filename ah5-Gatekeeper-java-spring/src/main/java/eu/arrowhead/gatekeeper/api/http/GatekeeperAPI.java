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

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.str;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.service.FederationService;
import eu.arrowhead.gatekeeper.service.validation.GatekeeperValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// globalServiceDiscovery (query, multi-query), interCloudNegotiation (negotiate) and the echo operations.
@RestController
@RequestMapping(GatekeeperConstants.HTTP_API_BASE_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GatekeeperAPI {

	//=================================================================================================
	// members

	@Autowired
	private FederationService federationService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Global service discovery in the configured or named neighbour clouds")
	@PostMapping(path = { GatekeeperConstants.HTTP_API_OP_INIT_GSD, GatekeeperConstants.HTTP_API_OP_INIT_MULTI_GSD }, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> globalServiceDiscovery(final HttpServletRequest request) throws Exception {
		final Map<String, Object> payload = obj(RequestBodies.json(request));
		return federationService.gsd(payload, consumer(request, payload));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Inter-cloud negotiation with a provider of a neighbour cloud")
	@PostMapping(path = GatekeeperConstants.HTTP_API_OP_INIT_ICN, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> interCloudNegotiation(final HttpServletRequest request) throws Exception {
		final Map<String, Object> payload = obj(RequestBodies.json(request));
		return federationService.icn(payload, consumer(request, payload));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Echo")
	@GetMapping(path = { GatekeeperConstants.HTTP_API_OP_ECHO, GatekeeperConstants.HTTP_API_OP_MONITOR_ECHO }, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> echo() {
		return m("status", "ok", "system", GatekeeperConstants.SYSTEM_NAME, "version", GatekeeperConstants.SYSTEM_VERSION);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// consumerSystem, defaulting to the caller; naming another consumer is reserved
	private static String consumer(final HttpServletRequest request, final Map<String, Object> payload) {
		final String actor = RequestBodies.caller(request);
		final String consumer = str(payload.getOrDefault("consumerSystem", actor));
		GatekeeperValidation.actingConsumer(actor, RequestBodies.sysop(request), consumer);
		return consumer;
	}
}
