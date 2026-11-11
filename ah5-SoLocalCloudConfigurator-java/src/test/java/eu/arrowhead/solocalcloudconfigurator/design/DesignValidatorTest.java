/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.solocalcloudconfigurator.TestDesigns;

public class DesignValidatorTest {

	@Test
	public void wellFormedDesignHasNoProblems() throws Exception {
		assertEquals(List.of(), DesignValidator.problems(TestDesigns.twoClouds(), TestDesigns.catalog()));
	}

	@Test
	public void clashingPlacementAndUnsafeValuesAreProblems() throws Exception {
		final SolcDesign d = TestDesigns.design(List.of(
				TestDesigns.member("Plant", TestDesigns.LC_A, "mac.local", "arm64", 0, TestDesigns.components("gatekeeper")),
				TestDesigns.member("plant", TestDesigns.LC_B, "mac.local", "arm64", 0, TestDesigns.components("gatekeeper")),
				TestDesigns.member("Lab", TestDesigns.LC_B, "host;rm -rf", "riscv", 1500, TestDesigns.components("gatekeeper"))), List.of());
		final String problems = String.join("\n", DesignValidator.problems(d, TestDesigns.catalog()));
		assertTrue(problems.contains("cloud names must be unique"), problems);
		assertTrue(problems.contains("uses port offset 0"), problems);
		assertTrue(problems.contains("Lab: host must be"), problems);
		assertTrue(problems.contains("Lab: architecture must be"), problems);
		assertTrue(problems.contains("Lab: port offset must be"), problems);
	}

	@Test
	public void relayPortMustNotCollideWithAMemberBroker() throws Exception {
		final SolcDesign d = new SolcDesign("Factory", "Company", new SolcDesign.Relay("mac.local", 9883), List.of(
				TestDesigns.member("Plant", TestDesigns.LC_A, "mac.local", "arm64", 1000, TestDesigns.components("gatekeeper"))), List.of(), false,
				new SolcDesign.Packaging("build", List.of("arm64")), null);
		assertTrue(DesignValidator.problems(d, TestDesigns.catalog()).stream().anyMatch(p -> p.contains("collides")));
	}

	@Test
	public void memberArchitectureMustBePackaged() throws Exception {
		final SolcDesign t = TestDesigns.twoClouds();
		final SolcDesign d = new SolcDesign(t.name(), t.organization(), t.relay(), t.members(), t.exchanges(), false,
				new SolcDesign.Packaging("prebuilt", List.of("arm64")), null);
		assertTrue(DesignValidator.problems(d, TestDesigns.catalog()).stream().anyMatch(p -> p.contains("Office: its architecture amd64")));
	}
}
