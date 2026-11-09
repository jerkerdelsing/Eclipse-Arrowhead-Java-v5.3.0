package eu.arrowhead.localcloudconfigurator.service;

import eu.arrowhead.localcloudconfigurator.Planner;

/** Durable deployment records: the private MySQL schema of the API, the file state of the host worker. */
public interface DeploymentStore {

	/** The deployment that blocks new ones until it has finished, or null. */
	String activeDeployment();

	/** Records (id) or clears (null) the active deployment. */
	void setActiveDeployment(String id);

	boolean exists(String id);

	void submitted(String id, Planner.Plan plan, Planner.Request request);

	/** The latest state reported by the backend for a deployment. */
	default void observed(final String id, final Object result) {
	}
}
