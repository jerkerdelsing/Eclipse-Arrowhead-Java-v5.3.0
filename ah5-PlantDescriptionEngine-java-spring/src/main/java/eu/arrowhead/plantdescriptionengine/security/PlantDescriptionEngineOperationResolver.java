/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.security;

import java.util.Optional;

import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;

// Maps a plantDescriptionMonitor request to its operation; management paths contain "/mgmt/" and are left to the management filter.
public final class PlantDescriptionEngineOperationResolver {

	public record ServiceOperation(String serviceDefinition, String operation) {
	}

	public static Optional<ServiceOperation> resolve(final String method, final String path) {
		if (method == null || path == null || !path.startsWith(PlantDescriptionEngineConstants.HTTP_API_MONITOR_PATH + "/")) {
			return Optional.empty();
		}

		final String[] segments = path.substring(PlantDescriptionEngineConstants.HTTP_API_MONITOR_PATH.length()).replaceAll("/+$", "").split("/");
		// segments[0] is empty because the rest starts with "/"
		final boolean get = "GET".equalsIgnoreCase(method);
		final boolean patch = "PATCH".equalsIgnoreCase(method);
		final String operation;
		if (segments.length == 2 && "plant-descriptions".equals(segments[1]) && get) {
			operation = PlantDescriptionEngineConstants.SERVICE_OP_QUERY_PLANT_DESCRIPTIONS;
		} else if (segments.length == 3 && "plant-descriptions".equals(segments[1]) && get) {
			operation = PlantDescriptionEngineConstants.SERVICE_OP_GET_PLANT_DESCRIPTION;
		} else if (segments.length == 2 && "alarms".equals(segments[1]) && get) {
			operation = PlantDescriptionEngineConstants.SERVICE_OP_QUERY_ALARMS;
		} else if (segments.length == 3 && "alarms".equals(segments[1]) && get) {
			operation = PlantDescriptionEngineConstants.SERVICE_OP_GET_ALARM;
		} else if (segments.length == 3 && "alarms".equals(segments[1]) && patch) {
			operation = PlantDescriptionEngineConstants.SERVICE_OP_ACKNOWLEDGE_ALARM;
		} else {
			return Optional.empty();
		}

		return Optional.of(new ServiceOperation(PlantDescriptionEngineConstants.SERVICE_DEF_PLANT_DESCRIPTION_MONITOR, operation));
	}

	private PlantDescriptionEngineOperationResolver() {
		throw new UnsupportedOperationException();
	}
}
