/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.entity;

import java.util.List;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Execution journal of sessions; createdAt is the entry date.
@Entity
@Table(name = "worklog")
public class Worklog extends UnmodifiableArrowheadEntity {

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "createdAt", "sessionId");

	@Column(nullable = true, length = VARCHAR_SMALL)
	private String planName;

	@Column(nullable = true, length = VARCHAR_SMALL)
	private String actionName;

	@Column(nullable = true, length = VARCHAR_SMALL)
	private String stepName;

	@Column(nullable = true)
	private Long sessionId;

	@Column(nullable = true)
	private Long executionNumber;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String message;

	@Column(nullable = true, columnDefinition = "MEDIUMTEXT")
	private String exception;

	public Worklog() {
	}

	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public Worklog(final String planName, final String actionName, final String stepName, final Long sessionId, final Long executionNumber, final String message, final String exception) {
		this.planName = planName;
		this.actionName = actionName;
		this.stepName = stepName;
		this.sessionId = sessionId;
		this.executionNumber = executionNumber;
		this.message = message;
		this.exception = exception;
	}

	public String getPlanName() { return planName; }
	public String getActionName() { return actionName; }
	public String getStepName() { return stepName; }
	public Long getSessionId() { return sessionId; }
	public Long getExecutionNumber() { return executionNumber; }
	public String getMessage() { return message; }
	public String getException() { return exception; }
}
