/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.entity;

import java.util.List;

import eu.arrowhead.choreographer.api.dto.enums.SessionStatus;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One run of a plan, possibly repeated quantityGoal times; createdAt is the start time.
@Entity
@Table(name = "plan_session")
public class Session extends ArrowheadEntity {

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "status", "createdAt", "updatedAt");

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "planId", nullable = false)
	private Plan plan;

	@Column(nullable = false, length = VARCHAR_TINY)
	@Enumerated(EnumType.STRING)
	private SessionStatus status = SessionStatus.INITIATED;

	@Column(nullable = false)
	private long quantityDone;

	@Column(nullable = false)
	private long quantityGoal = 1;

	@Column(nullable = false)
	private long executionNumber;

	// system that started the session (choreographerSession) or that operated it (management)
	@Column(nullable = false, length = VARCHAR_SMALL)
	private String owner;

	@Column(nullable = false)
	private boolean allowInterCloud;

	@Column(nullable = false)
	private boolean chooseOptimalExecutor;

	@Column(nullable = true, length = VARCHAR_LARGE)
	private String notifyUri;

	public Session() {
	}

	public Session(final Plan plan, final long quantityGoal, final String owner, final boolean allowInterCloud, final boolean chooseOptimalExecutor, final String notifyUri) {
		this.plan = plan;
		this.quantityGoal = quantityGoal;
		this.owner = owner;
		this.allowInterCloud = allowInterCloud;
		this.chooseOptimalExecutor = chooseOptimalExecutor;
		this.notifyUri = notifyUri;
	}

	public Plan getPlan() { return plan; }
	public SessionStatus getStatus() { return status; }
	public long getQuantityDone() { return quantityDone; }
	public long getQuantityGoal() { return quantityGoal; }
	public long getExecutionNumber() { return executionNumber; }
	public String getOwner() { return owner; }
	public boolean isAllowInterCloud() { return allowInterCloud; }
	public boolean isChooseOptimalExecutor() { return chooseOptimalExecutor; }
	public String getNotifyUri() { return notifyUri; }
	public void setStatus(final SessionStatus status) { this.status = status; }
	public void setQuantityDone(final long quantityDone) { this.quantityDone = quantityDone; }
	public void setExecutionNumber(final long executionNumber) { this.executionNumber = executionNumber; }
}
