/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static eu.arrowhead.sysmonitor.service.Json.json;
import static eu.arrowhead.sysmonitor.service.Json.list;
import static eu.arrowhead.sysmonitor.service.Json.map;
import static eu.arrowhead.sysmonitor.service.Json.now;
import static eu.arrowhead.sysmonitor.service.Json.obj;
import static eu.arrowhead.sysmonitor.service.Json.str;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import eu.arrowhead.sysmonitor.SysMonitorConstants;

// SysMonitor-AMQP-IDD-5.2.2 intake: validates an observation against the routing key it was delivered with and builds the
// normalised envelope that is stored and published on sysMonitorData (same fields and order as the Go and Rust systems).
public final class ObservationFormat {

	//=================================================================================================
	// members

	private static final Set<String> ECHO_STATUSES = Set.of("alive", "degraded", "failed");
	private static final Set<String> SUPPLY_TYPES = Set.of("line", "battery", "harvesting");
	private static final int ROUTING_KEY_PARTS = 5;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// routing key: <cloud>.sysmonitor.<providerSystem>.<serviceInstance>.<observationType>; IllegalArgumentException rejects
	public static Map<String, Object> normalize(final Object raw, final String routing, final String cloud) {
		if (!(raw instanceof Map<?, ?>)) {
			throw new IllegalArgumentException("Observation must be an object");
		}

		final Map<String, Object> p = obj(raw);
		final String[] parts = routing.split("\\.", -1);
		if (parts.length != ROUTING_KEY_PARTS || !parts[0].equals(cloud) || !parts[1].equals("sysmonitor")) {
			throw new IllegalArgumentException("Routing key does not match sysMonitor contract");
		}

		final String provider = str(p.getOrDefault("providerSystem", parts[2]));
		final String instance = str(p.getOrDefault("serviceInstance", parts[3]));
		final String type = str(p.getOrDefault("observationType", parts[4]));
		if (!provider.equals(parts[2]) || !instance.equals(parts[3]) || !type.equals(parts[4])) {
			throw new IllegalArgumentException("Observation identity does not match authorized routing key");
		}

		if (!str(p.getOrDefault("schemaVersion", "1.0")).split("\\.")[0].equals("1") || !p.containsKey("value")) {
			throw new IllegalArgumentException("Unsupported observation schema/value");
		}

		final Map<String, Object> value = obj(p.get("value"));
		if (type.equals("echo") && !ECHO_STATUSES.contains(str(value.get("status")))) {
			throw new IllegalArgumentException("Invalid echo status");
		}

		if (type.equals("power") && !SUPPLY_TYPES.contains(str(value.get("supplyType")))) {
			throw new IllegalArgumentException("Invalid power supply");
		}

		final String source = str(p.get("sourceTime"));
		if (!source.endsWith("Z")) {
			throw new IllegalArgumentException("UTC sourceTime required");
		}

		try {
			Instant.parse(source);
		} catch (final RuntimeException ex) {
			throw new IllegalArgumentException("UTC sourceTime required");
		}

		String observationId = str(p.get("observationId"));
		if (observationId.isEmpty()) {
			observationId = sha256(routing + "\0" + canonical(raw));
		}

		if (observationId.length() > SysMonitorConstants.MAX_ID_LENGTH) {
			throw new IllegalArgumentException("observationId too long");
		}

		return map("schemaVersion", "1.0",
				"observationId", observationId,
				"providerSystem", provider,
				"serviceInstance", instance,
				"observationType", type,
				"sourceTime", source,
				"receivedTime", now(),
				"publishedTime", null,
				"value", p.get("value"),
				"unit", p.get("unit"),
				"quality", p.getOrDefault("quality", "good"),
				"pipeline", map("sysMonitor", SysMonitorConstants.SYSTEM_NAME, "revision", 1),
				"metadata", p.getOrDefault("metadata", map()));
	}

	//-------------------------------------------------------------------------------------------------
	// JSON with recursively sorted object keys: the basis of the derived observationId
	public static String canonical(final Object value) {
		return json(sort(value));
	}

	//-------------------------------------------------------------------------------------------------
	// sysMonitorData topic of a normalised observation
	public static String topic(final String outputBase, final Map<String, Object> observation) {
		return outputBase + "/" + observation.get("observationType") + "/" + observation.get("providerSystem");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static Object sort(final Object value) {
		if (value instanceof Map<?, ?>) {
			final Map<String, Object> sorted = new TreeMap<>();
			obj(value).forEach((k, v) -> sorted.put(k, sort(v)));
			return sorted;
		}

		if (value instanceof java.util.List<?>) {
			return list(value).stream().map(ObservationFormat::sort).toList();
		}

		return value;
	}

	//-------------------------------------------------------------------------------------------------
	private static String sha256(final String text) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (final NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private ObservationFormat() {
		throw new UnsupportedOperationException();
	}
}
