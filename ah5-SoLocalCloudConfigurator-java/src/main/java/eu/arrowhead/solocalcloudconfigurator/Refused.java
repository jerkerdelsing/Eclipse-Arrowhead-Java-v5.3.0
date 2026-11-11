/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator;

/** A request the configurator refuses with a specific HTTP status and message. */
public final class Refused extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public Refused(final int status, final String message) {
		super(message);
		this.status = status;
	}

	public int status() {
		return status;
	}
}
