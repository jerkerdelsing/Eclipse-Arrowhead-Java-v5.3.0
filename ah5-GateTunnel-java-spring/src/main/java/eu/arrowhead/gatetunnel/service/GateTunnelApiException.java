/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

// An API error with the exact HTTP status of the 5.2.2 contract (some, such as 413, 502 and 503, have no Arrowhead exception type).
public class GateTunnelApiException extends RuntimeException {

	//=================================================================================================
	// members

	private static final long serialVersionUID = 1L;

	private final int status;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public GateTunnelApiException(final int status, final String message) {
		super(message);
		this.status = status;
	}

	//-------------------------------------------------------------------------------------------------
	public int getStatus() {
		return status;
	}
}
