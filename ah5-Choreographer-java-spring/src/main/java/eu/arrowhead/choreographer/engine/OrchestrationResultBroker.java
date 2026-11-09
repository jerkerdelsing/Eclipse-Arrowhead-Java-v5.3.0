/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import eu.arrowhead.dto.OrchestrationResponseDTO;

// Correlates on-behalf push orchestrations with the result DynamicServiceOrchestration posts back.
// The correlation id is an unguessable UUID in the callback path.
@Component
public class OrchestrationResultBroker {

	private final Map<String, CompletableFuture<OrchestrationResponseDTO>> pending = new ConcurrentHashMap<>();

	public String open() {
		final String id = UUID.randomUUID().toString();
		pending.put(id, new CompletableFuture<>());
		return id;
	}

	public CompletableFuture<OrchestrationResponseDTO> future(final String id) {
		return pending.get(id);
	}

	// false when the id is unknown or already closed
	public boolean complete(final String id, final OrchestrationResponseDTO result) {
		final CompletableFuture<OrchestrationResponseDTO> future = pending.get(id);
		return future != null && future.complete(result);
	}

	public void close(final String id) {
		pending.remove(id);
	}

	public int pendingCount() {
		return pending.size();
	}
}
