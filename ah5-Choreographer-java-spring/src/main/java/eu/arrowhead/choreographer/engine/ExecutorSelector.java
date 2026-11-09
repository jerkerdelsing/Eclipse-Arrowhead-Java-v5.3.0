/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.choreographer.api.dto.ExecutorDependencyDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorServiceInfoResponseDTO;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.service.ExecutorDbService;
import eu.arrowhead.choreographer.service.ChoreographerDriver;
import eu.arrowhead.choreographer.service.Versions;

// Selects an executor for a step as the v4.6 ExecutorSelector: candidates that can execute the service definition and
// version window, minus exclusions, that answer service-info, ordered by the number of dependencies
// (MinimalDependencyExecutorPrioritizationStrategy), whose dependencies have providers.
@Component
public class ExecutorSelector {

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ExecutorDbService executorDbService;

	@Autowired
	private ChoreographerDriver driver;

	public ExecutorData select(final String serviceDefinition, final Integer minVersion, final Integer maxVersion, final Set<Long> exclusions,
			final boolean allowInterCloud, final boolean chooseOptimal) {
		final List<Executor> candidates = new ArrayList<>();
		for (final Executor executor : executorDbService.getExecutorsFor(serviceDefinition, minVersion, maxVersion)) {
			if (exclusions == null || !exclusions.contains(executor.getId())) {
				candidates.add(executor);
			}
		}

		final int min = minVersion == null ? Versions.DEFAULT_MIN : minVersion;
		final int max = maxVersion == null ? Versions.UNBOUNDED_MAX : maxVersion;
		final Map<Long, ExecutorServiceInfoResponseDTO> infos = new HashMap<>();
		for (final Executor executor : candidates) {
			try {
				infos.put(executor.getId(), driver.queryExecutorServiceInfo(executor, serviceDefinition, min, max));
			} catch (final Exception ex) {
				logger.warn("Executor {} did not answer service-info: {}", executor.getName(), ex.getMessage());
			}
		}
		final List<Executor> answering = new ArrayList<>(candidates.stream().filter(e -> infos.containsKey(e.getId())).toList());
		answering.sort(Comparator.comparingInt(e -> dependencies(infos.get(e.getId())).size()));

		ExecutorData best = null;
		for (final Executor executor : answering) {
			final List<ExecutorDependencyDTO> dependencies = dependencies(infos.get(executor.getId()));
			final Boolean usesOtherClouds = verifyDependencies(executor, dependencies, allowInterCloud);
			if (usesOtherClouds == null) {
				continue; // a dependency has no provider
			}
			final ExecutorData data = new ExecutorData(executor, dependencies, usesOtherClouds);
			if (!chooseOptimal || !usesOtherClouds) {
				return data; // a local executor is always the most optimal one
			}
			if (best == null) {
				best = data;
			}
		}

		return best;
	}

	//-------------------------------------------------------------------------------------------------
	private static List<ExecutorDependencyDTO> dependencies(final ExecutorServiceInfoResponseDTO info) {
		return info == null || info.dependencies() == null ? List.of() : info.dependencies();
	}

	// null: a dependency cannot be satisfied; true: at least one dependency is only possible through other clouds
	private Boolean verifyDependencies(final Executor executor, final List<ExecutorDependencyDTO> dependencies, final boolean allowInterCloud) {
		boolean otherClouds = false;
		for (final ExecutorDependencyDTO dependency : dependencies) {
			if (dependency == null || dependency.serviceRequirement() == null) {
				return null;
			}
			try {
				if (!driver.isServiceAvailable(dependency.serviceRequirement())) {
					if (allowInterCloud && !Boolean.TRUE.equals(dependency.localCloudOnly())) {
						otherClouds = true; // may be found by inter-cloud orchestration
					} else {
						return null;
					}
				}
			} catch (final Exception ex) {
				logger.warn("Dependency check of executor {} failed: {}", executor.getName(), ex.getMessage());
				return null;
			}
		}
		return otherClouds;
	}
}
