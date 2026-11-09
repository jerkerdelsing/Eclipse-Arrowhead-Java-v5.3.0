/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PeerNamesTest {

	//-------------------------------------------------------------------------------------------------
	@Test
	public void explicitMappingIsUnambiguous() {
		assertEquals("Lab", PeerNames.parse("RustDev=Lab,GoCloud=Company").get("RustDev"));
		assertTrue(PeerNames.parse("").isEmpty());
		assertEquals("RustDev|Lab", PeerNames.cloudIdentity("GateTunnel.RustDev.Lab.arrowhead.eu"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void rejectsMalformedOrAmbiguousBindings() {
		for (final String value : new String[] { "RustDev=Lab,RustDev=Company", "RustDev=", "=Lab", "RustDev=Lab/other", "RustDev=Lab,", "../Rust=Lab" }) {
			assertThrows(IllegalArgumentException.class, () -> PeerNames.parse(value), value);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void unmappedCloudsBelongToTheOwnOrganisation() {
		final FederationSettings settings = new FederationSettings("TestCloud", "Company", "legacy-mtls", "RustDev=Lab");
		assertEquals("Lab", settings.organizationOf("RustDev"));
		assertEquals("Company", settings.organizationOf("GoCloud"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void startUpRefusesUnknownProfilesAndRemappingTheOwnCloud() {
		assertThrows(IllegalArgumentException.class, () -> new FederationSettings("TestCloud", "Company", "plain", ""));
		assertThrows(IllegalArgumentException.class, () -> new FederationSettings("TestCloud", "Company", "ah-gt-1", "TestCloud=Other"));
		assertThrows(IllegalArgumentException.class, () -> new FederationSettings("TestCloud", "Company", "ah-gt-1", "RustDev=Lab,RustDev=Lab"));
	}
}
