/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;
import eu.arrowhead.plantdescriptionengine.api.dto.MonitorPlantDescriptionListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.MonitorPlantDescriptionResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.MonitorSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionResponseDTO;

// plantDescriptionMonitor: plant descriptions enriched with the registered system each plant system matches and its
// sysMonitor observations (the v4.6 monitorable systemData/inventory data).
@Service
public class MonitorService {

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private PlantDescriptionService plantService;

	@Autowired
	private CloudClient cloud;

	@Value(PlantDescriptionEngineConstants.$MONITORING_ENABLED_WD)
	private boolean monitoringEnabled;

	private record CloudView(List<SystemResponseDTO> registered, Map<String, String> lastObservations) {
	}

	public MonitorPlantDescriptionListResponseDTO query(final PageDTO page, final Boolean active, final String origin) {
		final PlantDescriptionListResponseDTO list = plantService.query(page, active, origin);
		final CloudView view = view();
		return new MonitorPlantDescriptionListResponseDTO(list.entries().stream().map(pd -> enrich(pd, view, false)).toList(), list.count());
	}

	public MonitorPlantDescriptionResponseDTO get(final long id, final String origin) {
		return enrich(plantService.get(id, origin), view(), true);
	}

	private MonitorPlantDescriptionResponseDTO enrich(final PlantDescriptionResponseDTO pd, final CloudView view, final boolean withLatest) {
		final Map<String, List<SystemResponseDTO>> matched = RuleResolver.matchSystems(pd.systems(), view.registered());
		final List<MonitorSystemDTO> systems = pd.systems().stream().map(s -> system(s, matched.get(s.systemId()), view, withLatest)).toList();
		return new MonitorPlantDescriptionResponseDTO(pd.id(), pd.plantDescription(), pd.active(), pd.include(), systems, pd.connections(), pd.createdAt(), pd.updatedAt());
	}

	private MonitorSystemDTO system(final PdeSystemDTO s, final List<SystemResponseDTO> matches, final CloudView view, final boolean withLatest) {
		final String registered = matches != null && matches.size() == 1 ? matches.get(0).name() : null;
		Map<String, Object> monitoring = null;
		if (registered != null && withLatest && monitoringEnabled) {
			try {
				monitoring = cloud.latestObservations(registered);
			} catch (final Exception ex) {
				logger.debug("No sysMonitor observations for {}: {}", registered, ex.getMessage());
			}
		}
		final String last = registered == null ? null : view.lastObservations().get(registered);
		return new MonitorSystemDTO(s.systemId(), s.systemName(), s.metadata(), s.ports(), registered, monitoring, last);
	}

	private CloudView view() {
		List<SystemResponseDTO> registered = List.of();
		final Map<String, String> last = new HashMap<>();
		try {
			registered = cloud.registeredSystems();
		} catch (final Exception ex) {
			logger.warn("ServiceRegistry query failed: {}", ex.getMessage());
		}
		if (monitoringEnabled) {
			try {
				for (final Map<String, Object> m : cloud.monitoredSystems()) {
					if (m.get("systemName") != null && m.get("lastObservationTime") != null) {
						last.put(m.get("systemName").toString(), m.get("lastObservationTime").toString());
					}
				}
			} catch (final Exception ex) {
				logger.warn("sysMonitorHistory query failed: {}", ex.getMessage());
			}
		}
		return new CloudView(registered, last);
	}
}
