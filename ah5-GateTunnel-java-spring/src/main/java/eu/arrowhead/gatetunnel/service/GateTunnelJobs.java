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

import java.time.ZonedDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.gatetunnel.jpa.service.SessionDbService;

// Periodic work (formerly the native scheduler): relay (re)connection every 10 s, session expiry every 60 s.
@Component
public class GateTunnelJobs {

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
	private FederationSettings settings;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Scheduled(initialDelay = 0, fixedDelay = 10_000)
	public void connectRelay() {
		try {
			relay.connect();
		} catch (final Exception ex) {
			logger.warn("Relay connection to {}:{} failed: {}", settings.getRelayHost(), settings.getRelayPort(), ex.getClass().getSimpleName());
			logger.debug(ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Inter-cloud sessions end after SESSION_MAX_AGE_SECONDS or SESSION_IDLE_SECONDS without use. Control sessions closed
	// longer than SESSION_MAX_AGE_SECONDS ago are removed, which returns their ports to the control pool.
	@Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
	public void expireSessions() {
		try {
			interCloud.expireSessions();
			final ZonedDateTime threshold = Utilities.utcNow().minusSeconds(settings.getSessionMaxAgeSeconds());
			final int purged = dbService.purgeClosed(threshold);
			if (purged > 0) {
				logger.info("Removed {} closed control session(s)", purged);
			}
		} catch (final RuntimeException ex) {
			logger.warn("Session expiry failed: {}", ex.getMessage());
			logger.debug(ex);
		}
	}
}
