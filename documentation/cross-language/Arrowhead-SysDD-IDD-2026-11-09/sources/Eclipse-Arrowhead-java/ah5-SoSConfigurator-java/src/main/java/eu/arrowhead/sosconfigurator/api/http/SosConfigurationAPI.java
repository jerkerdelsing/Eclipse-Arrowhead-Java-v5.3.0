/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.api.http;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.arrowhead.common.Constants;
import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;
import eu.arrowhead.sosconfigurator.service.SosConfigurationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// sosConfiguration. Paths, JSON documents and status codes are those of the native 5.2.2 API, which the operator UI and the apply
// tool use: 200, 202 (submitted), 400 (invalid request), 404, 413 (too large), 422 (refused by the policies), 502 (ColonyOS).
@RestController
@RequestMapping(SoSConfiguratorConstants.HTTP_API_BASE_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class SosConfigurationAPI {

	//=================================================================================================
	// members

	private static final Pattern ID = Pattern.compile("[0-9a-f]{64}");

	@Autowired
	private SosConfigurationService service;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = SoSConfiguratorConstants.HTTP_API_OP_CATALOG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> catalog() {
		return json(HttpStatus.OK, service.catalog());
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = SoSConfiguratorConstants.HTTP_API_OP_STATUS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> status() {
		return json(HttpStatus.OK, service.status());
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = SoSConfiguratorConstants.HTTP_API_OP_PLAN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> plan(@RequestBody(required = false) final byte[] body) {
		final Parsed parsed = parse(body);
		return parsed.error != null ? parsed.error : json(HttpStatus.OK, service.plan(parsed.request));
	}

	//-------------------------------------------------------------------------------------------------
	@PostMapping(path = SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> submit(final HttpServletRequest request, @RequestBody(required = false) final byte[] body) {
		final Parsed parsed = parse(body);
		if (parsed.error != null) {
			return parsed.error;
		}

		final Planner.Plan plan = service.plan(parsed.request);
		if (!plan.allowed()) {
			final Map<String, Object> refused = new LinkedHashMap<>();
			refused.put("error", "the SoS violates the security or authorization policies");
			refused.put("plan", plan);
			return json(HttpStatus.UNPROCESSABLE_ENTITY, refused);
		}

		try {
			final String id = service.submit(plan, parsed.request, submitter(request));
			final Map<String, Object> accepted = new LinkedHashMap<>();
			accepted.put("plan", plan);
			accepted.put("id", id);
			return json(HttpStatus.ACCEPTED, accepted);
		} catch (final IOException ex) {
			return json(HttpStatus.BAD_GATEWAY, Map.of("error", String.valueOf(ex.getMessage())));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> submissions() {
		return json(HttpStatus.OK, service.submissions());
	}

	//-------------------------------------------------------------------------------------------------
	@GetMapping(path = SoSConfiguratorConstants.HTTP_API_OP_DEPLOYMENT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> deployment(@PathVariable final String id) {
		if (!ID.matcher(id).matches()) {
			return json(HttpStatus.NOT_FOUND, Map.of("error", "deployment not found"));
		}

		try {
			return json(HttpStatus.OK, service.deployment(id));
		} catch (final IOException ex) {
			return json(HttpStatus.BAD_GATEWAY, Map.of("error", String.valueOf(ex.getMessage())));
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private record Parsed(SosRequest request, ResponseEntity<String> error) {
	}

	//-------------------------------------------------------------------------------------------------
	private static Parsed parse(final byte[] body) {
		if (body != null && body.length > SoSConfiguratorConstants.MAX_BODY_BYTES) {
			return new Parsed(null, json(HttpStatus.PAYLOAD_TOO_LARGE, Map.of("error", "request too large")));
		}

		try {
			final SosRequest request = Json.MAPPER.treeToValue(Json.MAPPER.readTree(body == null ? new byte[0] : body), SosRequest.class);
			if (request == null) {
				return new Parsed(null, json(HttpStatus.BAD_REQUEST, Map.of("error", "invalid request: empty body")));
			}

			return new Parsed(request, null);
		} catch (final IOException | IllegalArgumentException ex) {
			final String message = ex instanceof JsonProcessingException processing ? processing.getOriginalMessage() : ex.getMessage();
			return new Parsed(null, json(HttpStatus.BAD_REQUEST, Map.of("error", "invalid request: " + message)));
		}
	}

	//-------------------------------------------------------------------------------------------------
	static String submitter(final HttpServletRequest request) {
		if (Boolean.TRUE.equals(request.getAttribute(SoSConfiguratorConstants.HTTP_ATTR_CONSOLE_OPERATOR))) {
			return "console";
		}

		final Object system = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		return system == null ? Constants.UNKNOWN : system.toString();
	}

	//-------------------------------------------------------------------------------------------------
	// written with the configurator's own mapper (no null fields), as the native API did
	static ResponseEntity<String> json(final HttpStatus status, final Object body) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).body(Json.write(body));
	}
}
