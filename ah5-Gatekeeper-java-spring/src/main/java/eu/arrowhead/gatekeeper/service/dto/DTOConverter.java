/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service.dto;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.parse;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.str;

import java.util.List;
import java.util.Map;

import eu.arrowhead.gatekeeper.jpa.entity.Cloud;
import eu.arrowhead.gatekeeper.jpa.entity.Relay;

// Management views with the native 5.2.2 field names and order.
public final class DTOConverter {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> cloud(final Cloud cloud) {
		return m("name", cloud.getName(),
				"operator", cloud.getOperator(),
				"neighbor", true,
				"secure", true,
				"gatekeeperAddress", cloud.getGatekeeperAddress(),
				"gatekeeperPort", cloud.getGatekeeperPort(),
				"metadata", parse(str(cloud.getMetadata())));
	}

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> relay(final Relay relay) {
		return m("name", relay.getName(),
				"address", relay.getAddress(),
				"port", relay.getPort(),
				"type", relay.getType(),
				"secure", relay.isSecure(),
				"metadata", parse(str(relay.getMetadata())),
				// seconds since the epoch, as the native store kept it
				"createdAt", relay.getCreatedAt() == null ? null : relay.getCreatedAt().toInstant().toEpochMilli() / 1000.0);
	}

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> entries(final List<Map<String, Object>> rows) {
		return m("entries", rows, "count", rows.size());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DTOConverter() {
		throw new UnsupportedOperationException();
	}
}
