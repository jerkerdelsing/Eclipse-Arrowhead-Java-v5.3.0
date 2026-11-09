/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.dto.OrchestrationResponseDTO;

public class OrchestrationResultBrokerTest {

	@Test
	public void resultCompletesOnlyPendingIds() throws Exception {
		final OrchestrationResultBroker broker = new OrchestrationResultBroker();
		final String id = broker.open();
		assertNotEquals(id, broker.open());
		assertEquals(2, broker.pendingCount());

		final OrchestrationResponseDTO result = new OrchestrationResponseDTO(List.of(), List.of());
		assertFalse(broker.complete("unknown", result));
		assertTrue(broker.complete(id, result));
		assertSame(result, broker.future(id).get());

		broker.close(id);
		assertEquals(1, broker.pendingCount());
		assertFalse(broker.complete(id, result));
	}
}
