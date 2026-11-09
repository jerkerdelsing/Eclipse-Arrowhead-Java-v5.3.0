/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service.validation;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.integer;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.json;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.str;

import java.util.Map;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.service.ApiError;
import eu.arrowhead.gatekeeper.service.dto.CloudInput;
import eu.arrowhead.gatekeeper.service.dto.RelayInput;

// The checks of the native 5.2.2 Gatekeeper, with the same messages (HTTP 400 for invalid input, 403 for the consumer rule).
public final class GatekeeperValidation {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static CloudInput cloud(final Object value) {
		final Map<String, Object> item = obj(value);
		final String name = str(item.get("name"));
		if (name.isBlank()) {
			throw new IllegalArgumentException("Name required");
		}

		final String operator = str(item.get("operator"));
		if (operator.isEmpty()) {
			throw new IllegalArgumentException("Operator required");
		}

		final Object address = item.getOrDefault("gatekeeperAddress", item.get("address"));
		final Object port = item.getOrDefault("gatekeeperPort", item.get("port"));

		return new CloudInput(
				fits(name),
				fits(operator),
				address == null ? null : fits(address.toString()),
				port == null ? null : portOrNull(port),
				json(item.getOrDefault("metadata", m())));
	}

	//-------------------------------------------------------------------------------------------------
	public static RelayInput relay(final Object value) {
		final Map<String, Object> item = obj(value);
		final String name = str(item.get("name"));
		if (name.isBlank()) {
			throw new IllegalArgumentException("Name required");
		}

		final String address = str(item.get("address"));
		final int port = integer(item.get("port"), 0);
		if (address.isEmpty() || port < 1 || port > 65535) {
			throw new IllegalArgumentException("Relay address and valid port required");
		}

		final String type = str(item.getOrDefault("type", "MQTT"));
		if (type.length() > UnmodifiableArrowheadEntity.VARCHAR_SMALL) {
			throw new IllegalArgumentException("Value is too long");
		}

		return new RelayInput(fits(name), fits(address), port, type.isEmpty() ? "MQTT" : type, !Boolean.FALSE.equals(item.get("secure")),
				json(item.getOrDefault("metadata", m())));
	}

	//-------------------------------------------------------------------------------------------------
	// only the orchestrators, LocalCloudManager and the system operator may name another consumer
	public static void actingConsumer(final String actor, final boolean sysop, final String consumer) {
		if (!consumer.equals(actor) && !sysop && !GatekeeperConstants.MAY_ACT_FOR_OTHERS.contains(actor)) {
			throw new ApiError(403, "Caller cannot act for another consumer");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static String fits(final String value) {
		if (value.length() > UnmodifiableArrowheadEntity.VARCHAR_MEDIUM) {
			throw new IllegalArgumentException("Value is too long");
		}

		return value;
	}

	//-------------------------------------------------------------------------------------------------
	// the native store kept whatever was sent; a non-numeric port is not kept
	private static Integer portOrNull(final Object port) {
		if (port instanceof Number n) {
			return n.intValue();
		}

		try {
			return Integer.valueOf(port.toString().trim());
		} catch (final NumberFormatException ex) {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private GatekeeperValidation() {
		throw new UnsupportedOperationException();
	}
}
