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
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_CLOSE_SESSIONS;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_CONNECT_CONSUMER;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_CONNECT_PROVIDER;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_INTERCLOUD_CONSUMER;
import static eu.arrowhead.gatetunnel.GateTunnelConstants.HTTP_API_OP_INTERCLOUD_PROVIDER;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatetunnel.service.ControlSessionService;
import eu.arrowhead.gatetunnel.service.InterCloudSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

// gateTunnelConnection. Each path is served under /gatetunnel, /gateway (4.x name) and without a base path.
@RestController
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class GateTunnelConnectionAPI {

	//=================================================================================================
	// members

	@Autowired
	private ControlSessionService controlSessions;

	@Autowired
	private InterCloudSessionService interCloud;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Reserves a control session (4.x connect-provider)")
	@PostMapping(path = { BASE + HTTP_API_OP_CONNECT_PROVIDER, LEGACY + HTTP_API_OP_CONNECT_PROVIDER, HTTP_API_OP_CONNECT_PROVIDER }, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> connectProvider(@RequestBody(required = false) final byte[] body) {
		return ApiSupport.json(HttpStatus.CREATED, controlSessions.connectProvider(ApiSupport.payload(body)));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Activates a control session by its queue id (4.x connect-consumer)")
	@PostMapping(path = { BASE + HTTP_API_OP_CONNECT_CONSUMER, LEGACY + HTTP_API_OP_CONNECT_CONSUMER, HTTP_API_OP_CONNECT_CONSUMER }, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> connectConsumer(@RequestBody(required = false) final byte[] body) {
		return ApiSupport.json(HttpStatus.CREATED, controlSessions.connectConsumer(ApiSupport.payload(body)));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Closes the control sessions of the given ports; returns the ports that had none")
	@PostMapping(path = { BASE + HTTP_API_OP_CLOSE_SESSIONS, LEGACY + HTTP_API_OP_CLOSE_SESSIONS, HTTP_API_OP_CLOSE_SESSIONS }, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<List<Object>> closeSessions(@RequestBody(required = false) final byte[] body) {
		return ApiSupport.json(HttpStatus.OK, controlSessions.closeSessions(ApiSupport.body(body)));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Opens the provider side of a negotiated inter-cloud relay session (local Gatekeeper only)")
	@PostMapping(path = { BASE + HTTP_API_OP_INTERCLOUD_PROVIDER, LEGACY + HTTP_API_OP_INTERCLOUD_PROVIDER, HTTP_API_OP_INTERCLOUD_PROVIDER }, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> openProvider(@RequestBody(required = false) final byte[] body) {
		return ApiSupport.json(HttpStatus.CREATED, interCloud.openProvider(ApiSupport.payload(body)));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Opens the consumer side (tunnel listener) of a negotiated inter-cloud relay session (local Gatekeeper only)")
	@PostMapping(path = { BASE + HTTP_API_OP_INTERCLOUD_CONSUMER, LEGACY + HTTP_API_OP_INTERCLOUD_CONSUMER, HTTP_API_OP_INTERCLOUD_CONSUMER }, consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, Object>> openConsumer(@RequestBody(required = false) final byte[] body) throws Exception {
		return ApiSupport.json(HttpStatus.CREATED, interCloud.openConsumer(ApiSupport.payload(body)));
	}
}
