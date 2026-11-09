/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import eu.arrowhead.common.init.SecurityProfileGuard;

// The deployment configuration (config/application.properties) meets the default secure profile; the packaged development
// sample is refused unless the isolated development exception is selected explicitly.
public class SecurityProfileTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void deploymentConfigurationIsSecure() throws Exception {
		final Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("config/application.properties"))) {
			properties.load(in);
		}

		assertDoesNotThrow(() -> guard(properties).enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void developmentSampleIsRefused() throws Exception {
		final Properties properties = new Properties();
		try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
			properties.load(in);
		}

		final IllegalStateException refused = assertThrows(IllegalStateException.class, () -> guard(properties).enforce());
		assertTrue(refused.getMessage().startsWith("Refusing to start"));
		assertTrue(refused.getMessage().contains("server.ssl.enabled=false"));
		assertTrue(refused.getMessage().contains("authentication.policy=declared"));

		properties.setProperty("arrowhead.security.profile", "development");
		assertDoesNotThrow(() -> guard(properties).enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void plainBrokerConnectionsAreRefused() throws Exception {
		final Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Path.of("config/application.properties"))) {
			properties.load(in);
		}

		properties.setProperty("sysmonitor.ssl.enabled", "false");
		assertThrows(IllegalStateException.class, () -> guard(properties).enforce());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static SecurityProfileGuard guard(final Properties properties) {
		final MockEnvironment environment = new MockEnvironment();
		properties.forEach((key, value) -> environment.setProperty(key.toString(), value.toString()));
		final SecurityProfileGuard guard = new SecurityProfileGuard();
		guard.setEnvironment(environment);
		return guard;
	}
}
