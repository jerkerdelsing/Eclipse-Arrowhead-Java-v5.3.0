/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.graphdb;

import java.util.List;

// Persistence of historian SenML packs (GraphDB in deployments; replaced by a mock in unit tests).
public interface HistorianStore {

	void save(HistorianPack pack);

	List<String> findDistinctSystemNames();

	List<String> findDistinctServiceNames(String systemName);

	// Packs of the endpoint overlapping [from, to], newest base time first
	List<HistorianPack> findInTimeRange(String systemName, String serviceName, double from, double to, int limit);
}
