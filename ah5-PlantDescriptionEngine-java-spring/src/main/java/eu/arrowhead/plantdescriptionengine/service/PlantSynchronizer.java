/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.OrchestrationPlantRulesDTO;

import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PortDTO;
import eu.arrowhead.plantdescriptionengine.service.PlantDescriptionService.ActivePlant;

// Keeps the DSO plant rules created by the PDE equal to the rules the active plant implies, and keeps the alarms current.
// Runs periodically (registrations change outside the PDE) and right after every plant description change.
@Service
public class PlantSynchronizer {

	public record SyncState(boolean ok, String detail, String time, int rules) {
	}

	private final Logger logger = LogManager.getLogger(getClass());
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "pde-sync"));

	@Autowired
	private PlantDescriptionService plantService;

	@Autowired
	private AlarmService alarmService;

	@Autowired
	private CloudClient cloud;

	@Value(PlantDescriptionEngineConstants.$RULE_PRIORITY_DEFAULT_WD)
	private int defaultPriority;

	@Value(PlantDescriptionEngineConstants.$MONITORING_ENABLED_WD)
	private boolean monitoringEnabled;

	@Value(PlantDescriptionEngineConstants.$MONITORING_STALE_AFTER_WD)
	private long staleAfterSeconds;

	@Value(PlantDescriptionEngineConstants.$ALARM_IGNORED_SYSTEMS_WD)
	private String ignoredSystems;

	private volatile SyncState state = new SyncState(false, "not synchronized yet", null, 0);

	@TransactionalEventListener(fallbackExecution = true)
	public void onChange(final PlantDescriptionChangedEvent event) {
		executor.submit(this::sync);
	}

	@Scheduled(initialDelay = 10_000, fixedDelayString = PlantDescriptionEngineConstants.$SYNC_INTERVAL_WD)
	public void scheduled() {
		sync();
	}

	public SyncState state() {
		return state;
	}

	public synchronized void sync() {
		try {
			final Optional<ActivePlant> active = plantService.activePlant();
			final List<PdeSystemDTO> systems = active.map(ActivePlant::systems).orElse(List.of());
			final List<SystemResponseDTO> registered = cloud.registeredSystems();
			final List<String> definitions = systems.stream()
					.flatMap(s -> PlantDescriptionValidator.ports(s).stream())
					.filter(p -> !PlantDescriptionValidator.isConsumer(p))
					.map(PortDTO::serviceDefinition)
					.distinct()
					.toList();
			final List<ServiceInstanceResponseDTO> instances = active.isPresent() ? cloud.serviceInstances(definitions) : List.of();
			final OrchestrationPlantRulesDTO desired = active.isPresent()
					? RuleResolver.snapshot(systems, active.get().connections(), registered, instances, defaultPriority)
					: new OrchestrationPlantRulesDTO(List.of());
			// alarms do not depend on the orchestration rules, so they are kept current even when DSO is unavailable
			updateAlarms(active, registered);
			final int rules = reconcileRules(desired);
			state = new SyncState(true, active.map(a -> "plant '" + a.description().getName() + "' in force").orElse("no active plant description"),
					Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()), rules);
		} catch (final Exception ex) {
			logger.warn("Plant synchronization failed: {}", ex.getMessage());
			cloud.forgetEndpoints();
			logger.debug(ex);
			state = new SyncState(false, "synchronization failed: " + ex.getMessage(), Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()), state.rules());
		}
	}

    private int reconcileRules(final OrchestrationPlantRulesDTO desired) {
        if (!desired.equals(cloud.ownRules())) {
            cloud.replaceRules(desired);
            logger.info("DSO plant rules replaced: {} managed consumer/service scopes", desired.rules().size());
        }
        return desired.rules().stream().mapToInt(r -> r.providers().size()).sum();
    }

	private void updateAlarms(final Optional<ActivePlant> active, final List<SystemResponseDTO> registered) {
		if (active.isEmpty()) {
			alarmService.replace(List.of(), EnumSet.allOf(AlarmDetector.Cause.class));
			return;
		}

		Map<String, String> observations = null;
		if (monitoringEnabled) {
			try {
				observations = cloud.monitoredSystems().stream()
						.filter(m -> m.get("systemName") != null && m.get("lastObservationTime") != null)
						.collect(Collectors.toMap(m -> m.get("systemName").toString(), m -> m.get("lastObservationTime").toString(), (a, b) -> b));
			} catch (final Exception ex) {
				// keep the monitoring alarms as they are rather than raising or clearing them on a lookup failure
				logger.warn("sysMonitorHistory not available: {}", ex.getMessage());
			}
		}

		final Set<String> ignored = Arrays.stream(ignoredSystems.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
		final List<AlarmDetector.Detected> detected = AlarmDetector.detect(active.get().systems(), registered, observations, staleAfterSeconds, Utilities.utcNow(), ignored);
		final Set<AlarmDetector.Cause> causes = EnumSet.allOf(AlarmDetector.Cause.class);
		if (monitoringEnabled && observations == null) {
			causes.removeIf(AlarmDetector.Cause::isMonitoring);
		}
		alarmService.replace(detected, causes);
	}
}
