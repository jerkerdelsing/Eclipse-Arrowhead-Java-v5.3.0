package eu.arrowhead.localcloudconfigurator.service;

import java.util.List;

import eu.arrowhead.localcloudconfigurator.CloudColony;
import eu.arrowhead.localcloudconfigurator.Planner;

/** The retained ColonyOS backend (lcc.backend=colony): unchanged CloudColony workflows. */
public class ColonyBackend implements ConfiguratorBackend {

	private final CloudColony colony;

	public ColonyBackend(final CloudColony colony) {
		this.colony = colony;
	}

	@Override
	public List<String> status() throws Exception {
		return colony.status();
	}

	@Override
	public String submit(final Planner.Plan plan) throws Exception {
		return colony.submit(plan);
	}

	@Override
	public Object deployment(final String id) throws Exception {
		return colony.deployment(id);
	}

	@Override
	public String kind() {
		return "colony";
	}
}
