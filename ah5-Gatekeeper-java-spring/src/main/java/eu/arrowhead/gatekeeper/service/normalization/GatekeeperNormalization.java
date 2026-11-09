/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service.normalization;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.list;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;

import java.util.List;

// Request shapes the native 5.2.2 Gatekeeper accepted.
public final class GatekeeperNormalization {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// a management body may be a JSON array, an object with "list", or one object
	public static List<Object> managementItems(final Object raw) {
		return raw instanceof List<?> ? list(raw) : list(obj(raw).getOrDefault("list", List.of(raw)));
	}

	//-------------------------------------------------------------------------------------------------
	// cloud name of "<Cloud>", "<Cloud>|<Organization>" or {"name", "operator"}
	public static String targetName(final Object value) {
		return value instanceof String s ? s.split("\\|", 2)[0] : eu.arrowhead.gatekeeper.service.dto.JsonValues.str(obj(value).get("name"));
	}

	//-------------------------------------------------------------------------------------------------
	// requested organisation of the same forms, or "" when none is named
	public static String targetOrganization(final Object value) {
		if (value instanceof String s) {
			return s.contains("|") ? s.split("\\|", 2)[1] : "";
		}

		return eu.arrowhead.gatekeeper.service.dto.JsonValues.str(obj(value).get("operator"));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private GatekeeperNormalization() {
		throw new UnsupportedOperationException();
	}
}
