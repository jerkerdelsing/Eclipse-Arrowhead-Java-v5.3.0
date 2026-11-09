/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

// If-Match revision of a subscription does not match (HTTP 409).
public class SubscriptionConflictException extends RuntimeException {

	//=================================================================================================
	// members

	private static final long serialVersionUID = 1L;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SubscriptionConflictException(final String message) {
		super(message);
	}
}
