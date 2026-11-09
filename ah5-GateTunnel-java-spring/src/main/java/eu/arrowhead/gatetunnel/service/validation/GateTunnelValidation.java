/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service.validation;

import static eu.arrowhead.gatetunnel.service.Wire.str;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class GateTunnelValidation {

	//=================================================================================================
	// members

	private static final List<String> CONTROL_REQUIRED = List.of("consumer", "provider", "consumerCloud", "providerCloud", "relay", "serviceDefinition");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateControlSessionCreate(final Map<String, Object> payload) {
		for (final String required : CONTROL_REQUIRED) {
			if (payload.get(required) == null || str(payload.get(required)).isBlank()) {
				throw new IllegalArgumentException(required + " is required");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQueueId(final String queueId) {
		if (queueId == null || queueId.isBlank()) {
			throw new IllegalArgumentException("queueId required");
		}
	}
}
