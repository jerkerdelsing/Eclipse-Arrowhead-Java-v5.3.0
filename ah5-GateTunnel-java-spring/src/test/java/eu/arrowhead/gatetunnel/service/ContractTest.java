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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.gatetunnel.service.normalization.GateTunnelNormalization;
import eu.arrowhead.gatetunnel.service.validation.GateTunnelValidation;
import eu.arrowhead.gatetunnel.service.validation.OperationScope;

// Ported from the 5.2.2 ContractTest. The persistence part (create, activate, close, reopen) runs against MySQL in
// it.MySqlEndToEndTest.
public class ContractTest {

	//-------------------------------------------------------------------------------------------------
	@Test
	public void controlRejectsMissingFields() {
		assertThrows(IllegalArgumentException.class, () -> new GateTunnelValidation().validateControlSessionCreate(m("serviceDefinition", "temperature")));
		assertThrows(IllegalArgumentException.class, () -> new GateTunnelValidation().validateQueueId(" "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void parameterizedOperationsCannotEscapeScope() {
		final Map<String, Object> operations = m("put-data", m("method", "PUT", "path", "/systems/{systemName}/services/{serviceName}/data"));
		final String base = "/datamanager/data-historian";
		final String valid = base + "/systems/Producer/services/reading/data";
		assertTrue(OperationScope.operationAllowed("PUT", valid, base, operations));
		assertTrue(OperationScope.operationAllowed("PUT", valid + "?count=1", base, operations));
		for (final String wrong : List.of(valid + "/../mgmt", base + "/systems/%2e%2e/services/reading/data", base + "/systems/Producer%2fother/services/reading/data",
				base + "/systems/%252e/services/reading/data", "https://attacker.invalid" + valid, "//attacker.invalid" + valid, base + "/systems/Producer/services/reading/mgmt",
				base + "/systems/Producer/services//data")) {
			assertFalse(OperationScope.operationAllowed("PUT", wrong, base, operations), wrong);
		}
		assertFalse(OperationScope.operationAllowed("DELETE", valid, base, operations));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void closeSessionsAcceptsAnArrayOrPortsObjectAndLimitsAreClamped() {
		final GateTunnelNormalization normalization = new GateTunnelNormalization();
		assertEquals(List.of(8000, 8001), normalization.normalizePorts(List.of(8000, 8001, 8000)));
		assertEquals(List.of(8002, 0), normalization.normalizePorts(m("ports", List.of("8002", "x"))));
		assertEquals(List.of(), normalization.normalizePorts(m()));
		assertEquals(200, normalization.normalizeLimit(null));
		assertEquals(200, normalization.normalizeLimit("abc"));
		assertEquals(1000, normalization.normalizeLimit("5000"));
		assertEquals(1, normalization.normalizeLimit("-3"));
	}
}
