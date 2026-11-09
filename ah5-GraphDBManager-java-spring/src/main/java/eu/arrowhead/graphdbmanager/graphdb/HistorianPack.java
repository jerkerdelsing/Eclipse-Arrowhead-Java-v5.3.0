/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.graphdb;

import java.util.List;
import java.util.UUID;

// One stored SenML pack: the complete normalized pack as JSON (returned by get-data) plus its measurements
// as individual RDF resources, so that the data can also be queried with SPARQL in GraphDB.
public record HistorianPack(
		UUID id,
		String systemName,
		String serviceName,
		double baseTime,
		double minTime,
		double maxTime,
		String payload,
		List<Measurement> measurements) {

	public HistorianPack {
		measurements = measurements == null ? List.of() : List.copyOf(measurements);
	}

	public record Measurement(String name, String unit, Double value, String stringValue, Boolean booleanValue, double time) {
	}
}
