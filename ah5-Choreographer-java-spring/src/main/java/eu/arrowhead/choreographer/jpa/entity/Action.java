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
@Table(name = "plan_action", uniqueConstraints = @UniqueConstraint(columnNames = { "planId", "name" }))
public class Action extends ArrowheadEntity {

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "planId", nullable = false)
	private Plan plan;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = false)
	private boolean firstAction;

	// name of the next action within the same plan; null for the last action
	@Column(nullable = true, length = VARCHAR_SMALL)
	private String nextActionName;

	public Action() {
	}

	public Action(final Plan plan, final String name, final boolean firstAction, final String nextActionName) {
		this.plan = plan;
		this.name = name;
		this.firstAction = firstAction;
		this.nextActionName = nextActionName;
	}

	public Plan getPlan() { return plan; }
	public String getName() { return name; }
	public boolean isFirstAction() { return firstAction; }
	public String getNextActionName() { return nextActionName; }
	public void setPlan(final Plan plan) { this.plan = plan; }
	public void setName(final String name) { this.name = name; }
	public void setFirstAction(final boolean firstAction) { this.firstAction = firstAction; }
	public void setNextActionName(final String nextActionName) { this.nextActionName = nextActionName; }
}
