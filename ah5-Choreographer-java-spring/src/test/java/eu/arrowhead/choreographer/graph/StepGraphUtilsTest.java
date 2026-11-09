/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Uses the step-graph examples of the v4.6 Choreographer test suite (StepGraphExamples), exported as edge lists.
public class StepGraphUtilsTest {

	private static JsonNode examples() throws Exception {
		try (InputStream in = StepGraphUtilsTest.class.getResourceAsStream("/step-graph-examples-v46.json")) {
			return new ObjectMapper().readTree(in);
		}
	}

	private static StepGraph graph(final JsonNode spec) {
		final StepGraph graph = new StepGraph();
		spec.get("nodes").forEach(n -> graph.addNode(n.asText()));
		spec.get("edges").forEach(e -> {
			final String[] parts = e.asText().split(">");
			graph.addEdge(parts[0], parts[1]);
		});
		return graph;
	}

	private static Set<String> edges(final JsonNode spec) {
		final Set<String> result = new HashSet<>();
		spec.get("edges").forEach(e -> result.add(e.asText()));
		return result;
	}

	@Test
	public void detectsNoCycleInTheV46NonCircularExamples() throws Exception {
		for (final JsonNode spec : examples().get("nonCirculars")) {
			assertFalse(StepGraphUtils.hasCycle(graph(spec)), spec.toString());
		}
	}

	@Test
	public void detectsTheCycleInTheV46CircularExamples() throws Exception {
		for (final JsonNode spec : examples().get("circulars")) {
			assertTrue(StepGraphUtils.hasCycle(graph(spec)), spec.toString());
		}
	}

	@Test
	public void normalizesLikeTheV46EdgeDestroyer() throws Exception {
		final JsonNode normalizables = examples().get("normalizables");
		assertEquals(5, normalizables.size());
		for (final JsonNode example : normalizables) {
			assertEquals(edges(example.get("expected")), StepGraphUtils.normalize(graph(example.get("input"))).edges(), example.toString());
		}
	}

	@Test
	public void removesOnlyImpliedEdges() {
		final StepGraph graph = new StepGraph().addEdge("a", "b").addEdge("b", "c").addEdge("a", "c").addEdge("a", "d");
		assertEquals(Set.of("a>b", "b>c", "a>d"), StepGraphUtils.normalize(graph).edges());
	}

	@Test
	public void refusesToNormalizeACyclicGraph() {
		assertThrows(IllegalArgumentException.class, () -> StepGraphUtils.normalize(new StepGraph().addEdge("a", "b").addEdge("b", "a")));
	}
}
