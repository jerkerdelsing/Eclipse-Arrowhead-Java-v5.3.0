/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.dto;

import java.util.List;
import java.util.Map;

// A plant system as monitored: the registered system it matches and its latest sysMonitor observations
// (stored by SysMonitor), replacing the v4.6 monitorable systemData and inventory data.
public record MonitorSystemDTO(
		String systemId,
		String systemName,
		Map<String, String> metadata,
		List<PortDTO> ports,
		String registeredSystemName,
		Map<String, Object> monitoring,
		String lastObservationTime) {
}
