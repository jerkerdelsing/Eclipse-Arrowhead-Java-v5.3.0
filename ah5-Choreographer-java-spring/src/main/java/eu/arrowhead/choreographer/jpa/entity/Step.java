/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "plan_step", uniqueConstraints = @UniqueConstraint(columnNames = { "actionId", "name" }))
public class Step extends ArrowheadEntity {

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "actionId", nullable = false)
	private Action action;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = false)
	private boolean firstStep;

	// service definition the step's provider must offer; also selects the executor
	@Column(nullable = false, length = VARCHAR_SMALL)
	private String serviceDefinition;

	// major-version window derived from the requirement's versions (null = unbounded)
	@Column(nullable = true)
	private Integer minVersion;

	@Column(nullable = true)
	private Integer maxVersion;

	// v5 OrchestrationServiceRequirementDTO as JSON
	@Column(nullable = false, columnDefinition = "TEXT")
	private String serviceRequirement;

	@Column(nullable = false)
	private boolean localCloudOnly;

	// Map<String, String> as JSON
	@Column(nullable = false, columnDefinition = "TEXT")
	private String staticParameters;

	@Column(nullable = false)
	private int quantity;

	public Step() {
	}

	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public Step(final Action action, final String name, final boolean firstStep, final String serviceDefinition, final Integer minVersion, final Integer maxVersion,
			final String serviceRequirement, final boolean localCloudOnly, final String staticParameters, final int quantity) {
		this.action = action;
		this.name = name;
		this.firstStep = firstStep;
		this.serviceDefinition = serviceDefinition;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
		this.serviceRequirement = serviceRequirement;
		this.localCloudOnly = localCloudOnly;
		this.staticParameters = staticParameters;
		this.quantity = quantity;
	}

	public Action getAction() { return action; }
	public String getName() { return name; }
	public boolean isFirstStep() { return firstStep; }
	public String getServiceDefinition() { return serviceDefinition; }
	public Integer getMinVersion() { return minVersion; }
	public Integer getMaxVersion() { return maxVersion; }
	public String getServiceRequirement() { return serviceRequirement; }
	public boolean isLocalCloudOnly() { return localCloudOnly; }
	public String getStaticParameters() { return staticParameters; }
	public int getQuantity() { return quantity; }
	public void setAction(final Action action) { this.action = action; }
	public void setFirstStep(final boolean firstStep) { this.firstStep = firstStep; }
}
