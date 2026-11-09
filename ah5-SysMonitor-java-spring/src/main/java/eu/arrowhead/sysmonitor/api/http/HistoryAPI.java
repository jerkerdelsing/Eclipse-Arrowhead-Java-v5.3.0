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

import static eu.arrowhead.sysmonitor.service.Json.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import eu.arrowhead.sysmonitor.service.Json;
import eu.arrowhead.sysmonitor.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// sysMonitorHistory (SysMonitor-History-HTTP-IDD-5.2.2). Authorization is checked by SysMonitorAccessFilter.
@RestController
@RequestMapping(SysMonitorConstants.HTTP_API_HISTORY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class HistoryAPI {

	//=================================================================================================
	// members

	private static final int MAX_COUNT = 1000;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private MonitoringService monitoring;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Systems with stored observations")
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_SYSTEMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> systems(final HttpServletRequest request) {
		Responses.caller(request);
		return Responses.json(HttpStatus.OK, map("systems", store.systems(), "retentionDays", monitoring.getRetentionDays()));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Newest observation of every type of a system")
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_LATEST_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> latest(final HttpServletRequest request, @PathVariable final String systemName) {
		Responses.caller(request);
		final Map<String, Object> latest = store.latest(systemName);
		if (latest.isEmpty()) {
			throw new DataNotFoundException("No stored observations for system", Responses.origin(request));
		}

		return Responses.json(HttpStatus.OK, map("systemName", systemName, "latest", latest));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Stored observations of a system, newest first")
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_OBSERVATIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> observations(
			final HttpServletRequest request,
			@PathVariable final String systemName,
			@RequestParam(required = false, defaultValue = "100") final String count,
			@RequestParam(required = false, name = "type") final List<String> type,
			@RequestParam(required = false) final String from,
			@RequestParam(required = false) final String to) {
		Responses.caller(request);
		final String origin = Responses.origin(request);
		int limit;
		try {
			limit = Integer.parseInt(count);
		} catch (final NumberFormatException ex) {
			limit = 0;
		}

		if (limit < 1 || limit > MAX_COUNT) {
			throw new InvalidParameterException("count must be 1.." + MAX_COUNT, origin);
		}

		final List<String> types = new ArrayList<>();
		if (type != null) {
			for (final String value : type) {
				for (final String t : value.split(",")) {
					if (!t.isEmpty()) {
						types.add(t);
					}
				}
			}
		}

		final List<Object> observations = store.history(systemName, types, time(from, origin), time(to, origin), limit);
		return Responses.json(HttpStatus.OK, map("systemName", systemName, "count", observations.size(), "observations", observations));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static Double time(final String value, final String origin) {
		if (value == null) {
			return null;
		}

		try {
			return Json.epoch(value);
		} catch (final RuntimeException ex) {
			throw new InvalidParameterException("from/to must be ISO-8601 UTC instants", origin);
		}
	}
}
