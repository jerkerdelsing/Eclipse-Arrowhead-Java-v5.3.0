/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.solocalcloudconfigurator.Json;
import eu.arrowhead.solocalcloudconfigurator.TestDesigns;
import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.LcDesignRef;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Member;
import eu.arrowhead.solocalcloudconfigurator.jpa.repository.SolcDesignRepository;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine;

public class DesignServiceTest {

	private static final String LC_CATALOG = "{\"systems\":[{\"id\":\"serviceregistry\",\"requires\":[]},{\"id\":\"authentication\",\"requires\":[\"serviceregistry\"]},"
			+ "{\"id\":\"mqtt-broker\",\"requires\":[]},{\"id\":\"gatekeeper\",\"requires\":[\"authentication\",\"serviceregistry\",\"mqtt-broker\"]},"
			+ "{\"id\":\"gatetunnel\",\"requires\":[\"authentication\",\"serviceregistry\",\"gatekeeper\"]}]}";

	private static Member member(final List<String> lcComponents, final List<String> intercloud) {
		return new Member("Plant", new LcDesignRef(TestDesigns.LC_A, "Plant-design", 3, lcComponents), "plant", "arm64", 0, intercloud);
	}

	@Test
	public void addedIntercloudInfrastructureIsResolvedFromTheCatalogAndNeverTakenFromTheRequest() throws Exception {
		final PolicyCatalog catalog = TestDesigns.catalog();
		final LcDesignClient lcc = mock(LcDesignClient.class);
		final List<String> lc = List.of("serviceregistry", "authentication");
		when(lcc.snapshot(TestDesigns.LC_A)).thenReturn(new LcDesignRef(TestDesigns.LC_A, "Plant-design", 3, lc));
		when(lcc.catalog()).thenReturn(Json.LENIENT.readTree(LC_CATALOG));
		final DesignService service = new DesignService(mock(SolcDesignRepository.class), lcc, catalog, new PolicyEngine(catalog));

		// the request names the inter-cloud components (and something that is none): requirements the design lacks are added
		final SolcDesign first = service.resolve(TestDesigns.design(List.of(member(List.of("anything"), List.of("gatetunnel", "gatekeeper", "datamanager"))), List.of()), null);
		assertEquals(List.of("mqtt-broker", "gatekeeper", "gatetunnel"), first.members().get(0).intercloud());
		assertEquals(List.of("serviceregistry", "authentication", "mqtt-broker", "gatekeeper", "gatetunnel"), first.members().get(0).components());

		// saving it again keeps the resolved infrastructure without asking LocalCloudConfigurator
		final SolcDesign again = service.resolve(first, first);
		assertEquals(first.members().get(0), again.members().get(0));
		verify(lcc, times(1)).catalog();

		// a member that asks for nothing gets nothing
		assertNull(service.resolve(TestDesigns.design(List.of(member(lc, List.of("datamanager"))), List.of()), first).members().get(0).intercloud());
		assertNull(service.resolve(TestDesigns.design(List.of(member(lc, null)), List.of()), first).members().get(0).intercloud());
		verify(lcc, times(1)).catalog();
		verify(lcc, never()).revisions();
	}
}
