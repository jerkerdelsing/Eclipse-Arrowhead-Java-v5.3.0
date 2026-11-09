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

import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.gatetunnel.GateTunnelConstants;

// The AH-GT-1 key-exchange identity (relay key and chain, organization CA) exists only in the ah-gt-1 profile. A missing
// or unreadable file refuses the start; there is no fallback to legacy-mTLS.
@Configuration
public class SessionSecurityConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	@ConditionalOnProperty(name = GateTunnelConstants.FEDERATION_PROFILE, havingValue = GateTunnelConstants.PROFILE_AH_GT_1)
	SessionSecurity sessionSecurity(final FederationSettings settings) throws Exception {
		return new SessionSecurity(Path.of(settings.getRelayKey()), Path.of(settings.getRelayChainCert()), Path.of(settings.getRelayCaCert()));
	}
}
