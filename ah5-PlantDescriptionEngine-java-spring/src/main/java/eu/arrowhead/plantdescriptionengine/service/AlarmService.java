/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeAlarmListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeAlarmResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeAlarmUpdateDTO;
import eu.arrowhead.plantdescriptionengine.jpa.entity.PdeAlarm;
import eu.arrowhead.plantdescriptionengine.jpa.repository.PdeAlarmRepository;

@Service
public class AlarmService {

	public static final List<String> SORT_FIELDS = List.of("id", "createdAt", "updatedAt", "clearedAt");
	public static final String SEVERITY_WARNING = "WARNING";
	public static final String SEVERITY_CLEARED = "CLEARED";

	@Autowired
	private PdeAlarmRepository repository;

	@Autowired
	private PageService pageService;

	// v4.6 replaceAlarms: raises newly detected alarms and clears the active alarms of the given causes that are no longer detected.
	@Transactional
	public void replace(final List<AlarmDetector.Detected> detected, final Set<AlarmDetector.Cause> causes) {
		final Map<String, PdeAlarm> active = new HashMap<>();
		for (final PdeAlarm alarm : repository.findAllByClearedAtIsNull()) {
			active.put(alarm.getCause() + "|" + alarm.getSystemId() + "|" + alarm.getSystemName(), alarm);
		}

		final List<PdeAlarm> changed = new ArrayList<>();
		for (final AlarmDetector.Detected d : detected) {
			if (active.remove(d.key()) == null) {
				changed.add(new PdeAlarm(d.systemId(), d.systemName(), d.cause().name(), d.description()));
			}
		}
		for (final PdeAlarm stale : active.values()) {
			if (causes.stream().anyMatch(c -> c.name().equals(stale.getCause()))) {
				stale.setClearedAt(Utilities.utcNow());
				changed.add(stale);
			}
		}
		repository.saveAll(changed);
	}

	@Transactional(readOnly = true)
	public long activeCount() {
		return repository.findAllByClearedAtIsNull().size();
	}

	@Transactional(readOnly = true)
	public PdeAlarmListResponseDTO query(final PageDTO page, final String systemName, final String severity, final Boolean acknowledged, final String origin) {
		final PageRequest request = pageService.getPageRequest(page, SORT_FIELDS, "id", origin);
		Specification<PdeAlarm> spec = (root, q, cb) -> cb.conjunction();
		if (!Utilities.isEmpty(systemName)) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("systemName"), systemName));
		}
		if (!Utilities.isEmpty(severity)) {
			final String value = severity.trim().toUpperCase();
			if (SEVERITY_CLEARED.equals(value)) {
				spec = spec.and((root, q, cb) -> cb.isNotNull(root.get("clearedAt")));
			} else if (SEVERITY_WARNING.equals(value) || "NOT_CLEARED".equals(value)) {
				spec = spec.and((root, q, cb) -> cb.isNull(root.get("clearedAt")));
			} else {
				throw new InvalidParameterException("Unknown severity: " + severity, origin);
			}
		}
		if (acknowledged != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("acknowledged"), acknowledged));
		}
		final Page<PdeAlarm> result = repository.findAll(spec, request);
		return new PdeAlarmListResponseDTO(result.getContent().stream().map(AlarmService::toResponse).toList(), result.getTotalElements());
	}

	@Transactional(readOnly = true)
	public PdeAlarmResponseDTO get(final long id, final String origin) {
		return toResponse(find(id, origin));
	}

	@Transactional
	public PdeAlarmResponseDTO acknowledge(final long id, final PdeAlarmUpdateDTO dto, final String origin) {
		if (dto == null || dto.acknowledged() == null) {
			throw new InvalidParameterException("acknowledged is missing", origin);
		}
		final PdeAlarm alarm = find(id, origin);
		if (dto.acknowledged() != alarm.isAcknowledged()) {
			alarm.setAcknowledged(dto.acknowledged());
			alarm.setAcknowledgedAt(dto.acknowledged() ? Utilities.utcNow() : null);
			repository.saveAndFlush(alarm);
		}
		return toResponse(alarm);
	}

	private PdeAlarm find(final long id, final String origin) {
		return repository.findById(id).orElseThrow(() -> new DataNotFoundException("Alarm " + id + " not found", origin));
	}

	static PdeAlarmResponseDTO toResponse(final PdeAlarm alarm) {
		return new PdeAlarmResponseDTO(
				alarm.getId(),
				alarm.getSystemId(),
				alarm.getSystemName(),
				alarm.isAcknowledged(),
				alarm.getSeverity(),
				alarm.getCause(),
				alarm.getDescription(),
				Utilities.convertZonedDateTimeToUTCString(alarm.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(alarm.getUpdatedAt()),
				alarm.getClearedAt() == null ? null : Utilities.convertZonedDateTimeToUTCString(alarm.getClearedAt()),
				alarm.getAcknowledgedAt() == null ? null : Utilities.convertZonedDateTimeToUTCString(alarm.getAcknowledgedAt()));
	}
}
