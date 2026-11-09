package eu.arrowhead.localcloudconfigurator.service;

import java.util.List;

import eu.arrowhead.localcloudconfigurator.Planner;

/** Where reviewed plans are executed: the host worker (direct, default) or the retained ColonyOS backend. */
public interface ConfiguratorBackend {

	List<String> status() throws Exception;

	String submit(Planner.Plan plan) throws Exception;

	default String submit(final Planner.Plan plan, final Planner.Request request) throws Exception {
		return submit(plan);
	}

	Object deployment(String id) throws Exception;

	/** "direct" or "colony", reported by /healthz. */
	default String kind() {
		return "direct";
	}
}
