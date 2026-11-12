/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.solocalcloudconfigurator.TestDesigns;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Exchange;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine.CloudConfig;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine.Evaluation;

public class PolicyEngineTest {

	private static Evaluation evaluate(final SolcDesign d) throws Exception {
		return new PolicyEngine(TestDesigns.catalog()).evaluate(d);
	}

	private static List<String> rules(final Evaluation e) {
		return e.violations().stream().map(PolicyEngine.Violation::rule).toList();
	}

	private static SolcDesign withExchange(final Exchange x) {
		final SolcDesign d = TestDesigns.twoClouds();
		return new SolcDesign(d.name(), d.organization(), d.relay(), d.members(), List.of(x), d.verifyDiscovery(), d.packaging(), d.layout());
	}

	@Test
	public void allowedExchangeGrantsExactlyWhatItNeeds() throws Exception {
		final Evaluation e = evaluate(TestDesigns.twoClouds());
		assertTrue(e.allowed(), e.violations().toString());
		assertEquals(List.of("Plant", "Office"), e.relay().clouds());
		assertEquals(4, e.relay().subjects().size());

		final CloudConfig office = e.clouds().get("Office");
		assertEquals(1, office.grants().size());
		assertEquals("Plant|Company", office.grants().get(0).cloud());
		assertEquals(List.of("RandomDataProducer"), office.grants().get(0).consumers());
		assertEquals("dataHistorian", office.grants().get(0).service());
		assertEquals(List.of("Plant"), office.neighbours().stream().map(PolicyEngine.Neighbour::name).toList());
		assertTrue(office.revokes().isEmpty());
		assertTrue(office.verify().stream().anyMatch(c -> c.expected() && "put-data".equals(c.scope())));
		assertTrue(office.preconditions().stream().anyMatch(c -> "GateTunnel".equals(c.consumer()) && "put-data".equals(c.scope())));
		assertEquals(List.of("datamanager"), office.exportServices());

		final CloudConfig plant = e.clouds().get("Plant");
		assertTrue(plant.grants().isEmpty());
		assertEquals(1, plant.discovery().size());
		assertEquals("Office|Company", plant.discovery().get(0).targetCloud());
	}

	@Test
	public void unusedExportIsRevokedAndVerifiedDenied() throws Exception {
		final SolcDesign d = TestDesigns.twoClouds();
		final Evaluation e = evaluate(new SolcDesign(d.name(), d.organization(), d.relay(), d.members(), List.of(), false, d.packaging(), d.layout()));
		assertTrue(e.allowed());
		final CloudConfig office = e.clouds().get("Office");
		assertEquals(List.of("MGMT|Plant|Company|DataManager|SERVICE_DEF|dataHistorian"), office.revokes());
		assertTrue(office.verify().stream().anyMatch(c -> !c.expected() && "RandomDataProducer".equals(c.consumer())));
		assertEquals(List.of("Plant"), office.removeNeighbours().stream().map(PolicyEngine.Neighbour::name).toList());
	}

	@Test
	public void refusedExchangesNameTheirRule() throws Exception {
		assertEquals(List.of("A2"), rules(evaluate(withExchange(new Exchange("Plant", "Sysop", "Office", "DataManager", "dataHistorian", List.of("put-data"))))));
		assertEquals(List.of("A2"), rules(evaluate(withExchange(new Exchange("Office", "RandomDataProducer", "Plant", "DataManager", "dataHistorian", List.of("put-data"))))));
		assertEquals(List.of("A3"), rules(evaluate(withExchange(new Exchange("Plant", "RandomDataProducer", "Office", "DataManager", "dataManagement", List.of("put-data"))))));
		assertEquals(List.of("A4"), rules(evaluate(withExchange(new Exchange("Plant", "RandomDataProducer", "Office", "DataManager", "dataHistorian", List.of("delete-data"))))));
		assertEquals(List.of("A1"), rules(evaluate(withExchange(new Exchange("Plant", "RandomDataProducer", "Plant", "DataManager", "dataHistorian", List.of("put-data"))))));
		assertEquals(List.of("A1"), rules(evaluate(withExchange(new Exchange("Plant", "RandomDataProducer", "Mars", "DataManager", "dataHistorian", List.of("put-data"))))));
	}

	@Test
	public void federationNeedsIntercloudComponents() throws Exception {
		final SolcDesign d = TestDesigns.design(List.of(
				TestDesigns.member("Plant", TestDesigns.LC_A, "plant", "arm64", 0, TestDesigns.components("random-data-producer")),
				TestDesigns.member("Office", TestDesigns.LC_B, "office", "amd64", 0, TestDesigns.components("gatekeeper", "gatetunnel", "datamanager"))), List.of());
		final Evaluation e = evaluate(d);
		assertFalse(e.allowed());
		assertEquals(List.of("S4"), rules(e));
		assertTrue(e.violations().get(0).message().contains("gatekeeper, gatetunnel"));
	}

	@Test
	public void intercloudInfrastructureAddedByTheSolcMakesAMemberFederationReady() throws Exception {
		final SolcDesign.Member plant = new SolcDesign.Member("Plant", new SolcDesign.LcDesignRef(TestDesigns.LC_A, "Plant-design", 3,
				TestDesigns.components("random-data-producer")), "plant", "arm64", 0, List.of("gatekeeper", "gatetunnel"));
		assertEquals(TestDesigns.components("random-data-producer", "gatekeeper", "gatetunnel"), plant.components());
		final SolcDesign d = TestDesigns.design(List.of(plant,
				TestDesigns.member("Office", TestDesigns.LC_B, "office", "amd64", 0, TestDesigns.components("gatekeeper", "gatetunnel", "datamanager"))),
				List.of(new Exchange("Plant", "RandomDataProducer", "Office", "DataManager", "dataHistorian", List.of("put-data"))));
		final Evaluation e = evaluate(d);
		assertTrue(e.allowed(), e.violations().toString());
		assertEquals(List.of("Plant", "Office"), e.relay().clouds());
	}

	@Test
	public void singleCloudNeedsNoFederation() throws Exception {
		final SolcDesign d = TestDesigns.design(List.of(
				TestDesigns.member("Plant", TestDesigns.LC_A, "plant", "arm64", 0, TestDesigns.components("random-data-producer"))), List.of());
		final Evaluation e = evaluate(d);
		assertTrue(e.allowed());
		assertTrue(e.clouds().isEmpty());
		assertTrue(e.relay().clouds().isEmpty());
	}
}
