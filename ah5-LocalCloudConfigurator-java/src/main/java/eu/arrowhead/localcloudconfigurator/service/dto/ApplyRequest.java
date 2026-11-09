package eu.arrowhead.localcloudconfigurator.service.dto;

import java.util.List;

import eu.arrowhead.localcloudconfigurator.Planner;

// request body of POST /api/deployments: the reviewed selection and the hash of the reviewed plan
public record ApplyRequest(List<String> systems, List<String> remove, boolean reapply, boolean smoke, String expectedPlanHash) {

	public Planner.Request request() {
		return new Planner.Request(systems, remove, reapply, smoke);
	}
}
