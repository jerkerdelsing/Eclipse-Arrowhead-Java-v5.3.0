package eu.arrowhead.localcloudconfigurator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.arrowhead.localcloudconfigurator.service.DeploymentStore;

// An in-memory deployment store that survives a coordinator "restart" in tests.
public class MemoryDeploymentStore implements DeploymentStore {

	public volatile String active;
	public final Map<String, Object> records = new ConcurrentHashMap<>();
	public final Map<String, Object> observed = new ConcurrentHashMap<>();

	@Override
	public String activeDeployment() {
		return active;
	}

	@Override
	public void setActiveDeployment(final String id) {
		active = id;
	}

	@Override
	public boolean exists(final String id) {
		return records.containsKey(id);
	}

	@Override
	public void submitted(final String id, final Planner.Plan plan, final Planner.Request request) {
		records.put(id, plan);
	}

	@Override
	public void observed(final String id, final Object result) {
		observed.put(id, result);
	}
}
