/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service.dto;

// One entry of GET /gatetunnel/mgmt/sessions (field names and order of 5.2.2; sessionStartedAt in epoch seconds).
public record ControlSessionDTO(
		String sessionId,
		String queueId,
		String peerName,
		String serviceDefinition,
		int tunnelPort,
		String state,
		Object consumer,
		Object provider,
		Object consumerCloud,
		Object providerCloud,
		Object relay,
		double sessionStartedAt) {
}
