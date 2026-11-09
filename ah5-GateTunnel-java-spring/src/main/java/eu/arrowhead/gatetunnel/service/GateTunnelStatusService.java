/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import static eu.arrowhead.gatetunnel.service.Wire.m;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;
import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.jpa.service.SessionDbService;
import eu.arrowhead.gatetunnel.security.SystemTls;

// Status documents (mgmt/status, mgmt/general/status, health, echo, publickey) and the sysMonitor health: degraded while
// the relay link is down.
@Service
public class GateTunnelStatusService implements SysMonitorHealthIndicator {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private RelayClient relay;

	@Autowired
	private InterCloudSessionService interCloud;

	@Autowired
	private SessionDbService dbService;

	@Autowired
	private SystemTls tls;

	private volatile Supplier<List<String>> registeredInstances = List::of;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void setRegisteredInstances(final Supplier<List<String>> supplier) {
		this.registeredInstances = supplier;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status() {
		final List<String> instances = new ArrayList<>(registeredInstances.get());
		final Map<String, Object> result = m("status", "ok", "system", GateTunnelConstants.SYSTEM_NAME, "version", GateTunnelConstants.SYSTEM_VERSION, "implementation", "java-spring",
				"x509", true, "registration", m("registered", !instances.isEmpty(), "instances", instances, "error", null));
		result.put("relay", relay.status());
		result.put("interCloud", m("providerSessions", interCloud.providerSessionCount(), "consumerSessions", interCloud.consumerSessionCount(), "sessions", interCloud.summaries()));
		try {
			result.put("activeSessions", dbService.countOpen());
		} catch (final RuntimeException ex) {
			logger.debug("Session count unavailable", ex);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> liveness() {
		return m("status", "ok", "system", GateTunnelConstants.SYSTEM_NAME);
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> echo() {
		return m("status", "ok", "system", GateTunnelConstants.SYSTEM_NAME, "version", GateTunnelConstants.SYSTEM_VERSION);
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> publicKey() {
		try {
			final java.security.PublicKey key = tls.certificate().getPublicKey();
			return m("algorithm", key.getAlgorithm(), "format", "X.509", "publicKey", Base64.getEncoder().encodeToString(key.getEncoded()));
		} catch (final Exception ex) {
			throw new GateTunnelApiException(500, "Internal service failure");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SysMonitorHealth health() {
		return relay.isConnected()
				? SysMonitorHealth.alive("relay connected; " + interCloud.providerSessionCount() + " provider and " + interCloud.consumerSessionCount() + " consumer sessions")
				: SysMonitorHealth.degraded("relay not connected");
	}
}
