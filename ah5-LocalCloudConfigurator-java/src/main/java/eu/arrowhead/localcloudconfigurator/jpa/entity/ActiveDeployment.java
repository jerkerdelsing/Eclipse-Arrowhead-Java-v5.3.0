package eu.arrowhead.localcloudconfigurator.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// The single active-deployment record (row id 1 while a deployment blocks new ones).
@Entity
@Table(name = "active_deployment")
public class ActiveDeployment {

	public static final int SLOT = 1;

	@Id
	private int id;

	@Column(nullable = false, length = 128)
	private String deploymentId;

	public ActiveDeployment() {
	}

	public ActiveDeployment(final String deploymentId) {
		this.id = SLOT;
		this.deploymentId = deploymentId;
	}

	public int getId() { return id; }
	public String getDeploymentId() { return deploymentId; }
}
