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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.service.ManagementService;
import eu.arrowhead.gatekeeper.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// gatekeeperManagement: neighbour clouds, relays and the relay/inter-cloud status.
@RestController
@RequestMapping(GatekeeperConstants.HTTP_API_BASE_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ManagementAPI {

	//=================================================================================================
	// members

	private static final String CLOUD_PREFIX = GatekeeperConstants.HTTP_API_BASE_PATH + GatekeeperConstants.HTTP_API_OP_MGMT_CLOUDS + "/";

	@Autowired
	private ManagementService managementService;

	@Autowired
	private StatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Relay link and inter-cloud status")
	@GetMapping(path = GatekeeperConstants.HTTP_API_OP_MGMT_STATUS, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> status() {
		return statusService.status();
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Configured neighbour clouds")
	@GetMapping(path = GatekeeperConstants.HTTP_API_OP_MGMT_CLOUDS, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> clouds() {
		return managementService.clouds();
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Adds or updates neighbour clouds (an array, {\"list\": [...]} or one object)")
	@PostMapping(path = GatekeeperConstants.HTTP_API_OP_MGMT_CLOUDS, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> upsertClouds(final HttpServletRequest request) throws Exception {
		return ResponseEntity.status(HttpStatus.CREATED).body(managementService.upsertClouds(RequestBodies.json(request)));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Removes a neighbour cloud: /mgmt/clouds/{operator}/{name}")
	@DeleteMapping(path = GatekeeperConstants.HTTP_API_OP_MGMT_CLOUDS + "/**", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> deleteCloud(final HttpServletRequest request) {
		final String path = SecurityUtilities.getDecodedUri(request.getRequestURI());
		final boolean removed = managementService.deleteCloud(path.substring(CLOUD_PREFIX.length()));
		return ResponseEntity.status(removed ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(m("removed", removed));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Configured relays")
	@GetMapping(path = GatekeeperConstants.HTTP_API_OP_MGMT_RELAYS, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> relays() {
		return managementService.relays();
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Adds or updates relays (an array, {\"list\": [...]} or one object)")
	@PostMapping(path = GatekeeperConstants.HTTP_API_OP_MGMT_RELAYS, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> upsertRelays(final HttpServletRequest request) throws Exception {
		return ResponseEntity.status(HttpStatus.CREATED).body(managementService.upsertRelays(RequestBodies.json(request)));
	}
}
