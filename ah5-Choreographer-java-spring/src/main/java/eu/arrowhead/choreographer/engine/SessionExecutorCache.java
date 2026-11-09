/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.choreographer.service.Versions;

// Per-session state: the executor chosen for each (service definition, version window) and the executors excluded after failures.
public class SessionExecutorCache {

	private final Map<String, ExecutorData> executors = new ConcurrentHashMap<>();
	private final Set<Long> exclusions = ConcurrentHashMap.newKeySet();
	private final boolean allowInterCloud;
	private final boolean chooseOptimalExecutor;
	private volatile boolean finished;

	public SessionExecutorCache(final boolean allowInterCloud, final boolean chooseOptimalExecutor) {
		this.allowInterCloud = allowInterCloud;
		this.chooseOptimalExecutor = chooseOptimalExecutor;
	}

	public ExecutorData get(final String serviceDefinition, final Integer minVersion, final Integer maxVersion) {
		return executors.get(key(serviceDefinition, minVersion, maxVersion));
	}

	public void put(final String serviceDefinition, final Integer minVersion, final Integer maxVersion, final ExecutorData data) {
		executors.put(key(serviceDefinition, minVersion, maxVersion), data);
	}

	public void remove(final String serviceDefinition, final Integer minVersion, final Integer maxVersion) {
		executors.remove(key(serviceDefinition, minVersion, maxVersion));
	}

	public Set<Long> getExclusions() { return exclusions; }
	public boolean isAllowInterCloud() { return allowInterCloud; }
	public boolean isChooseOptimalExecutor() { return chooseOptimalExecutor; }
	public boolean isFinished() { return finished; }
	public void finish() { finished = true; }

	private static String key(final String serviceDefinition, final Integer minVersion, final Integer maxVersion) {
		return serviceDefinition + "/" + (minVersion == null ? Versions.DEFAULT_MIN : minVersion) + "/" + (maxVersion == null ? Versions.UNBOUNDED_MAX : maxVersion);
	}
}
