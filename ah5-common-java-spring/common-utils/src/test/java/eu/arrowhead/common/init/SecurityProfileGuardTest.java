/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.common.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class SecurityProfileGuardTest {

	//=================================================================================================
	// members

	private SecurityProfileGuard guard;
	private MockEnvironment environment;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		environment = new MockEnvironment();
		environment.setProperty("server.ssl.enabled", "true");
		environment.setProperty("authentication.policy", "outsourced");

		guard = new SecurityProfileGuard();
		guard.setEnvironment(environment);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSecureConfigurationStarts() {
		assertDoesNotThrow(() -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCertificatePolicyStarts() {
		environment.setProperty("authentication.policy", "certificate");

		assertDoesNotThrow(() -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDisabledTlsIsRefused() {
		environment.setProperty("server.ssl.enabled", "false");

		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.enforce());

		assertTrue(ex.getMessage().contains("server.ssl.enabled=false"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeclaredPolicyIsRefused() {
		environment.setProperty("authentication.policy", "declared");

		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.enforce());

		assertTrue(ex.getMessage().contains("authentication.policy=declared"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDisabledServiceAuthorizationIsRefused() {
		environment.setProperty("service.authorization.enabled", "false");

		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.enforce());

		assertTrue(ex.getMessage().contains("service.authorization.enabled=false"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEnabledServiceAuthorizationStarts() {
		environment.setProperty("service.authorization.enabled", "true");

		assertDoesNotThrow(() -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPlainMonitoringIsRefused() {
		environment.setProperty("sysmonitor.enabled", "true");
		environment.setProperty("sysmonitor.ssl.enabled", "false");

		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.enforce());

		assertTrue(ex.getMessage().contains("sysmonitor.ssl.enabled=false"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPlainMonitoringSwitchIgnoredWhileMonitoringIsOff() {
		environment.setProperty("sysmonitor.enabled", "false");
		environment.setProperty("sysmonitor.ssl.enabled", "false");

		assertDoesNotThrow(() -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAllDeviationsAreReported() {
		environment.setProperty("server.ssl.enabled", "false");
		environment.setProperty("authentication.policy", "declared");
		environment.setProperty("service.authorization.enabled", "false");

		final String message = assertThrows(IllegalStateException.class, () -> guard.enforce()).getMessage();

		assertTrue(message.contains("server.ssl.enabled=false"));
		assertTrue(message.contains("authentication.policy=declared"));
		assertTrue(message.contains("service.authorization.enabled=false"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExplicitDevelopmentProfileStarts() {
		environment.setProperty("arrowhead.security.profile", "development");
		environment.setProperty("server.ssl.enabled", "false");
		environment.setProperty("authentication.policy", "declared");

		assertDoesNotThrow(() -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProfileValueIsCaseInsensitive() {
		environment.setProperty("arrowhead.security.profile", " DEVELOPMENT ");
		environment.setProperty("server.ssl.enabled", "false");

		assertDoesNotThrow(() -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnknownProfileFailsClosed() {
		environment.setProperty("arrowhead.security.profile", "dev");

		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> guard.enforce());

		assertFalse(ex.getMessage().contains("Refusing"));
		assertTrue(ex.getMessage().contains("arrowhead.security.profile"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMissingProfileMeansSecure() {
		environment.setProperty("server.ssl.enabled", "false");

		assertThrows(IllegalStateException.class, () -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMissingSslPropertyFollowsTheLibraryDefault() {
		final MockEnvironment empty = new MockEnvironment();
		guard.setEnvironment(empty);

		assertThrows(IllegalStateException.class, () -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUpperCaseDeclaredPolicyIsRefused() {
		environment.setProperty("authentication.policy", "DECLARED");

		assertThrows(IllegalStateException.class, () -> guard.enforce());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGuardRunsAtBeanFactoryPostProcessing() {
		environment.setProperty("server.ssl.enabled", "false");

		assertThrows(IllegalStateException.class, () -> guard.postProcessBeanFactory(null));
	}
}
