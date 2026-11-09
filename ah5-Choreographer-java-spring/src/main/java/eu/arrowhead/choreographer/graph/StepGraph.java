/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Directed graph of the steps of one action: an edge a -> b means b is a next step of a.
public final class StepGraph {

	private final Map<String, Set<String>> next = new LinkedHashMap<>();

	public StepGraph addNode(final String node) {
		next.computeIfAbsent(node, n -> new LinkedHashSet<>());
		return this;
	}

	public StepGraph addEdge(final String from, final String to) {
		addNode(from);
		addNode(to);
		next.get(from).add(to);
		return this;
	}

	public void removeEdge(final String from, final String to) {
		final Set<String> targets = next.get(from);
		if (targets != null) {
			targets.remove(to);
		}
	}

	public Set<String> nodes() {
		return Collections.unmodifiableSet(next.keySet());
	}

	public Set<String> nextOf(final String node) {
		return Collections.unmodifiableSet(next.getOrDefault(node, Set.of()));
	}

	public Map<String, Integer> inDegrees() {
		final Map<String, Integer> result = new LinkedHashMap<>();
		next.keySet().forEach(n -> result.put(n, 0));
		next.values().forEach(targets -> targets.forEach(t -> result.merge(t, 1, Integer::sum)));
		return result;
	}

	public StepGraph copy() {
		final StepGraph copy = new StepGraph();
		next.forEach((from, targets) -> {
			copy.addNode(from);
			targets.forEach(to -> copy.addEdge(from, to));
		});
		return copy;
	}

	public Set<String> edges() {
		final Set<String> result = new LinkedHashSet<>();
		next.forEach((from, targets) -> targets.forEach(to -> result.add(from + ">" + to)));
		return result;
	}
}
