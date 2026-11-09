/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

// The packaged development sample (no TLS, declared identities) does not meet the default secure profile: the start is
// refused before any bean (database, relay, web server) exists.
public class SecurityProfileStartupTest {

	//-------------------------------------------------------------------------------------------------
	@Test
	public void insecureConfigurationIsRefused() {
		final Throwable failure = assertThrows(Throwable.class, () -> new SpringApplicationBuilder(GateTunnelMain.class)
				.properties("spring.config.location=classpath:/application.properties", "server.port=0")
				.run());
		Throwable cause = failure;
		boolean refused = false;
		while (cause != null) {
			refused |= String.valueOf(cause.getMessage()).contains("Refusing to start");
			cause = cause.getCause();
		}
		assertTrue(refused, "expected the SecurityProfileGuard refusal, got " + failure);
	}
}
