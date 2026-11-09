/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.entity;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Directed edge "from" -> "to": "to" is a next step of "from" (v4.6 ChoreographerStepNextStepConnection).
@Entity
@Table(name = "plan_step_connection", uniqueConstraints = @UniqueConstraint(columnNames = { "fromId", "toId" }))
public class StepConnection extends UnmodifiableArrowheadEntity {

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "fromId", nullable = false)
	private Step from;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "toId", nullable = false)
	private Step to;

	public StepConnection() {
	}

	public StepConnection(final Step from, final Step to) {
		this.from = from;
		this.to = to;
	}

	public Step getFrom() { return from; }
	public Step getTo() { return to; }
}
