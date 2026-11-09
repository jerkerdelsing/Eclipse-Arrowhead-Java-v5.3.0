/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.engine;

// Thrown during plan execution; the engine aborts the session when it reaches the worker boundary.
public class ChoreographerSessionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final long sessionId;
	private final Long sessionStepId;

	public ChoreographerSessionException(final long sessionId, final Long sessionStepId, final String message, final Throwable cause) {
		super(message, cause);
		this.sessionId = sessionId;
		this.sessionStepId = sessionStepId;
	}

	public ChoreographerSessionException(final long sessionId, final Long sessionStepId, final String message) {
		this(sessionId, sessionStepId, message, null);
	}

	public long getSessionId() { return sessionId; }
	public Long getSessionStepId() { return sessionStepId; }

	public String getDetailedMessage() {
		final Throwable cause = getCause();
		return cause == null || cause.getMessage() == null ? getMessage() : getMessage() + " " + cause.getMessage();
	}
}
