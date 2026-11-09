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

// A step service definition (and major-version window) an executor can execute.
@Entity
@Table(name = "executor_service_definition", uniqueConstraints = @UniqueConstraint(columnNames = { "executorId", "serviceDefinition" }))
public class ExecutorServiceDefinition extends ArrowheadEntity {

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "executorId", nullable = false)
	private Executor executor;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = false)
	private int minVersion;

	@Column(nullable = false)
	private int maxVersion;

	public ExecutorServiceDefinition() {
	}

	public ExecutorServiceDefinition(final Executor executor, final String serviceDefinition, final int minVersion, final int maxVersion) {
		this.executor = executor;
		this.serviceDefinition = serviceDefinition;
		this.minVersion = minVersion;
		this.maxVersion = maxVersion;
	}

	public Executor getExecutor() { return executor; }
	public String getServiceDefinition() { return serviceDefinition; }
	public int getMinVersion() { return minVersion; }
	public int getMaxVersion() { return maxVersion; }
	public void setMinVersion(final int minVersion) { this.minVersion = minVersion; }
	public void setMaxVersion(final int maxVersion) { this.maxVersion = maxVersion; }
}
