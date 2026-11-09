/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service.normalization;

import static eu.arrowhead.gatetunnel.service.Wire.integer;
import static eu.arrowhead.gatetunnel.service.Wire.list;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class GateTunnelNormalization {

	//=================================================================================================
	// members

	private static final int DEFAULT_LIMIT = 200;
	private static final int MAX_LIMIT = 1000;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// ?limit= of mgmt/sessions: default 200, clamped to 1..1000; an unparsable value counts as the default.
	public int normalizeLimit(final String raw) {
		return Math.min(MAX_LIMIT, Math.max(1, integer(raw == null ? String.valueOf(DEFAULT_LIMIT) : raw, DEFAULT_LIMIT)));
	}

	//-------------------------------------------------------------------------------------------------
	// close_sessions accepts a JSON array of ports or {"ports":[...]}; duplicates removed, order kept, invalid entries are 0.
	public List<Integer> normalizePorts(final Object raw) {
		final List<Object> values = raw instanceof List<?> ? list(raw) : list(raw instanceof Map<?, ?> map ? map.get("ports") : null);
		final List<Integer> result = new ArrayList<>();
		for (final Object value : new LinkedHashSet<>(values)) {
			result.add(integer(value, 0));
		}

		return result;
	}
}
