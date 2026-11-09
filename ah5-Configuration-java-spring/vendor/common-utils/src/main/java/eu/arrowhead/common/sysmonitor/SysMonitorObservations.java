/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.sysmonitor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// Builds SysMonitor-AMQP-IDD-5.2.2 routing keys and observation envelopes for one provider system.
public class SysMonitorObservations {

	//=================================================================================================
	// members

	public static final String TYPE_ECHO = "echo";
	public static final String TYPE_POWER = "power";
	public static final String TYPE_STATUS = "status";
	public static final List<String> TYPES = List.of(TYPE_ECHO, TYPE_POWER, TYPE_STATUS);

	public static final String STATUS_ALIVE = "alive";
	public static final String STATUS_DEGRADED = "degraded";
	public static final String SCHEMA_VERSION = "1.0";
	public static final String PUBLISHER_MODE = "native";

	private final String systemName;
	private final String localCloud;
	private final String serviceInstance;
	private final AtomicLong sequence = new AtomicLong();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SysMonitorObservations(final String systemName, final String localCloud, final String serviceInstance) {
		this.systemName = systemName;
		this.localCloud = localCloud;
		this.serviceInstance = serviceInstance;
	}

	//-------------------------------------------------------------------------------------------------
	public String baseRoutingKey() {
		return localCloud + ".sysmonitor." + systemName + "." + serviceInstance;
	}

	//-------------------------------------------------------------------------------------------------
	public String routingKey(final String observationType) {
		return baseRoutingKey() + "." + observationType;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> echo(final Instant now, final long uptimeSeconds, final SysMonitorHealth health) {
		final Map<String, Object> value = new LinkedHashMap<>();
		value.put("status", health.healthy() ? STATUS_ALIVE : STATUS_DEGRADED);
		value.put("uptimeSeconds", uptimeSeconds);
		value.put("detail", health.detail());

		return envelope(TYPE_ECHO, now, value, null, health.healthy() ? "good" : "uncertain");
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> power(final Instant now) {
		final Map<String, Object> value = new LinkedHashMap<>();
		value.put("supplyType", "line");
		value.put("state", "available");

		return envelope(TYPE_POWER, now, value, "1", "good");
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status(final Instant now, final SysMonitorHealth health, final int serviceCount) {
		final Map<String, Object> value = new LinkedHashMap<>();
		value.put("state", "running");
		value.put("health", health.healthy() ? "healthy" : "degraded");
		value.put("serviceCount", serviceCount);

		return envelope(TYPE_STATUS, now, value, "1", health.healthy() ? "good" : "uncertain");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> envelope(final String observationType, final Instant now, final Map<String, Object> value, final String unit, final String quality) {
		final long seq = sequence.incrementAndGet();
		final Map<String, Object> observation = new LinkedHashMap<>();
		observation.put("schemaVersion", SCHEMA_VERSION);
		observation.put("observationId", systemName + "-" + observationType + "-" + now.toEpochMilli() + "-" + seq);
		observation.put("providerSystem", systemName);
		observation.put("serviceInstance", serviceInstance);
		observation.put("observationType", observationType);
		observation.put("sourceTime", now.truncatedTo(ChronoUnit.MILLIS).toString());
		observation.put("sequence", seq);
		observation.put("value", value);
		if (unit != null) {
			observation.put("unit", unit);
		}
		observation.put("quality", quality);
		observation.put("metadata", Map.of("publisherMode", PUBLISHER_MODE));

		return observation;
	}
}
