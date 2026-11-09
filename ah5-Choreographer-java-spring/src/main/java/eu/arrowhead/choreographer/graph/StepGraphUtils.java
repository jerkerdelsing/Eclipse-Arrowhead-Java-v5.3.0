/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Cycle detection (Kahn's algorithm) and normalization of step graphs, as in the v4.6 Choreographer.
public final class StepGraphUtils {

	private StepGraphUtils() {
		throw new UnsupportedOperationException();
	}

	// A graph is acyclic exactly when Kahn's topological sort consumes every node.
	public static boolean hasCycle(final StepGraph graph) {
		final Map<String, Integer> inDegree = graph.inDegrees();
		final Deque<String> ready = new ArrayDeque<>();
		inDegree.forEach((node, degree) -> {
			if (degree == 0) {
				ready.add(node);
			}
		});
		int visited = 0;
		while (!ready.isEmpty()) {
			final String node = ready.poll();
			visited++;
			for (final String next : graph.nextOf(node)) {
				if (inDegree.merge(next, -1, Integer::sum) == 0) {
					ready.add(next);
				}
			}
		}

		return visited != inDegree.size();
	}

	// Removes every edge a -> b that is implied by a longer path from a to b (transitive reduction of the DAG).
	// The resulting graph has the same precedence constraints and matches the v4.6 EdgeDestroyerStepGraphNormalizer.
	public static StepGraph normalize(final StepGraph graph) {
		if (hasCycle(graph)) {
			throw new IllegalArgumentException("Step graph has a cycle");
		}
		final StepGraph result = graph.copy();
		for (final String from : graph.nodes()) {
			for (final String to : List.copyOf(graph.nextOf(from))) {
				if (reachableAvoidingEdge(graph, from, to)) {
					result.removeEdge(from, to);
				}
			}
		}

		return result;
	}

	private static boolean reachableAvoidingEdge(final StepGraph graph, final String from, final String to) {
		final Deque<String> stack = new ArrayDeque<>();
		final Set<String> seen = new HashSet<>();
		for (final String next : graph.nextOf(from)) {
			if (!next.equals(to)) {
				stack.push(next);
			}
		}
		while (!stack.isEmpty()) {
			final String node = stack.pop();
			if (node.equals(to)) {
				return true;
			}
			if (seen.add(node)) {
				graph.nextOf(node).forEach(stack::push);
			}
		}

		return false;
	}
}
