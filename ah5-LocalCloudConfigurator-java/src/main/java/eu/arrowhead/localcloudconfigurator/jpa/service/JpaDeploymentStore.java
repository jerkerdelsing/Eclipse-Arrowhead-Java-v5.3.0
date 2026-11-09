package eu.arrowhead.localcloudconfigurator.jpa.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.jpa.entity.ActiveDeployment;
import eu.arrowhead.localcloudconfigurator.jpa.entity.Deployment;
import eu.arrowhead.localcloudconfigurator.jpa.repository.ActiveDeploymentRepository;
import eu.arrowhead.localcloudconfigurator.jpa.repository.DeploymentRepository;
import eu.arrowhead.localcloudconfigurator.service.DeploymentStore;

/** Deployment records and the active-deployment guard in the private schema ah_localcloudconfigurator. */
@Service
public class JpaDeploymentStore implements DeploymentStore {

	private final DeploymentRepository deployments;
	private final ActiveDeploymentRepository active;

	public JpaDeploymentStore(final DeploymentRepository deployments, final ActiveDeploymentRepository active) {
		this.deployments = deployments;
		this.active = active;
	}

	@Override
	@Transactional(readOnly = true)
	public String activeDeployment() {
		return active.findById(ActiveDeployment.SLOT).map(ActiveDeployment::getDeploymentId).orElse(null);
	}

	@Override
	@Transactional
	public void setActiveDeployment(final String id) {
		if (id == null) {
			active.deleteById(ActiveDeployment.SLOT);
		} else {
			active.save(new ActiveDeployment(id));
		}
		active.flush();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean exists(final String id) {
		return deployments.existsById(id);
	}

	@Override
	@Transactional
	public void submitted(final String id, final Planner.Plan plan, final Planner.Request request) {
		deployments.saveAndFlush(new Deployment(id, Json.write(plan), Json.write(request), "submitted", Instant.now().toString()));
	}

	// the latest step results are written each time they are read, so the record follows the backend step by step
	@Override
	@Transactional
	public void observed(final String id, final Object result) {
		deployments.findById(id).ifPresent(d -> {
			final String state = Json.LENIENT.valueToTree(result).path("state").asText(d.getState());
			d.observe(state.isEmpty() ? d.getState() : state, Json.write(result), Instant.now().toString());
			deployments.saveAndFlush(d);
		});
	}
}
