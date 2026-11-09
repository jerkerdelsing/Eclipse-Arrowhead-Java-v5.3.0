/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.entity;

import java.util.List;

import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// State of one step in one execution of a session.
@Entity
@Table(name = "session_step", uniqueConstraints = @UniqueConstraint(columnNames = { "sessionId", "stepId", "executionNumber" }))
public class SessionStep extends ArrowheadEntity {

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "status", "createdAt", "updatedAt");

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "sessionId", nullable = false)
	private Session session;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "stepId", nullable = false)
	private Step step;

	// null after the executor has been removed
	@ManyToOne(fetch = FetchType.EAGER, optional = true)
	@JoinColumn(name = "executorId", nullable = true)
	private Executor executor;

	@Column(nullable = false, length = VARCHAR_TINY)
	@Enumerated(EnumType.STRING)
	private SessionStepStatus status = SessionStepStatus.WAITING;

	@Column(nullable = false)
	private long executionNumber;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message = "";

	public SessionStep() {
	}

	public SessionStep(final Session session, final Step step, final Executor executor, final long executionNumber) {
		this.session = session;
		this.step = step;
		this.executor = executor;
		this.executionNumber = executionNumber;
	}

	public Session getSession() { return session; }
	public Step getStep() { return step; }
	public Executor getExecutor() { return executor; }
	public SessionStepStatus getStatus() { return status; }
	public long getExecutionNumber() { return executionNumber; }
	public String getMessage() { return message; }
	public void setExecutor(final Executor executor) { this.executor = executor; }
	public void setStatus(final SessionStepStatus status) { this.status = status; }
	public void setMessage(final String message) { this.message = message; }
}
