/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.graphdbmanager.GraphDBManagerConstants;
import eu.arrowhead.graphdbmanager.api.dto.SenMLRecordDTO;
import eu.arrowhead.graphdbmanager.graphdb.HistorianPack;
import eu.arrowhead.graphdbmanager.graphdb.HistorianStore;

@Service
public class HistorianService {

	private static final TypeReference<List<SenMLRecordDTO>> SENML_LIST_TYPE = new TypeReference<>() { };

	private final HistorianStore repository;
	private final GraphDBManagerNameService nameService;
	private final SenMLValidation senMLValidation;
	private final ObjectMapper objectMapper;
	private final Logger logger = LogManager.getLogger(getClass());

	public HistorianService(
			final HistorianStore repository,
			final GraphDBManagerNameService nameService,
			final SenMLValidation senMLValidation,
			final ObjectMapper objectMapper) {
		this.repository = repository;
		this.nameService = nameService;
		this.senMLValidation = senMLValidation;
		this.objectMapper = objectMapper;
	}

	public List<String> getSystems() {
		return repository.findDistinctSystemNames();
	}

	public List<String> getServices(final String systemName) {
		return repository.findDistinctServiceNames(nameService.normalizeSystemName(systemName));
	}

	public void put(final String systemName, final String serviceName, final List<SenMLRecordDTO> records, final String origin) {
		final String normalizedSystem = nameService.normalizeSystemName(systemName);
		final String normalizedService = nameService.normalizeServiceName(serviceName);
		final List<SenMLRecordDTO> normalizedRecords = senMLValidation.validateAndComplete(records, origin);
		final double baseTime = normalizedRecords.getFirst().bt();
		double minTime = baseTime;
		double maxTime = baseTime;
		final String baseUnit = normalizedRecords.getFirst().bu();
		final List<HistorianPack.Measurement> measurements = new ArrayList<>(normalizedRecords.size());
		for (final SenMLRecordDTO record : normalizedRecords) {
			final double timestamp = absoluteTime(record, baseTime);
			minTime = Math.min(minTime, timestamp);
			maxTime = Math.max(maxTime, timestamp);
			if (record.n() != null) {
				measurements.add(new HistorianPack.Measurement(record.n(), record.u() == null ? baseUnit : record.u(),
						record.v(), record.vs(), record.vb(), timestamp));
			}
		}

		try {
			repository.save(new HistorianPack(
					UUID.randomUUID(),
					normalizedSystem,
					normalizedService,
					baseTime,
					minTime,
					maxTime,
					objectMapper.writeValueAsString(normalizedRecords),
					measurements));
		} catch (final JsonProcessingException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Could not serialize the SenML pack", origin, ex);
		}
	}

	public List<SenMLRecordDTO> get(
			final String systemName,
			final String serviceName,
			final Double from,
			final Double to,
			final Integer count,
			final List<String> signals,
			final Integer signalCount,
			final String origin) {
		final String normalizedSystem = nameService.normalizeSystemName(systemName);
		final String normalizedService = nameService.normalizeServiceName(serviceName);
		final double normalizedFrom = from == null ? 0D : from;
		final double normalizedTo = to == null ? Instant.now().toEpochMilli() / 1000D + 1000D : to;
		final int normalizedCount = count == null ? GraphDBManagerConstants.DEFAULT_QUERY_COUNT : count;
		validateQuery(normalizedFrom, normalizedTo, normalizedCount, signalCount, origin);

		final int requestedPacks = signals == null || signals.isEmpty()
				? normalizedCount
				: GraphDBManagerConstants.MAX_QUERY_COUNT;
		final List<HistorianPack> entities = repository.findInTimeRange(
				normalizedSystem,
				normalizedService,
				normalizedFrom,
				normalizedTo,
				requestedPacks);
		if (entities.isEmpty()) {
			throw new DataNotFoundException("No historical data exists for " + normalizedSystem + "|" + normalizedService, origin);
		}

		final List<List<SenMLRecordDTO>> packs = entities.stream().map(entity -> deserialize(entity, origin)).toList();
		return signals == null || signals.isEmpty()
				? packs.stream().flatMap(List::stream).toList()
				: selectSignals(packs, normalizedService, normalizedFrom, normalizedTo, signals, signalCount == null ? 1 : signalCount);
	}

	private void validateQuery(final double from, final double to, final int count, final Integer signalCount, final String origin) {
		if (from < 0D || to < 0D || to < from) {
			throw new InvalidParameterException("Invalid historian time range", origin);
		}
		if (count < 1 || count > GraphDBManagerConstants.MAX_QUERY_COUNT) {
			throw new InvalidParameterException("count must be between 1 and " + GraphDBManagerConstants.MAX_QUERY_COUNT, origin);
		}
		if (signalCount != null && (signalCount < 1 || signalCount > GraphDBManagerConstants.MAX_QUERY_COUNT)) {
			throw new InvalidParameterException("signalCount must be between 1 and " + GraphDBManagerConstants.MAX_QUERY_COUNT, origin);
		}
	}

	private List<SenMLRecordDTO> deserialize(final HistorianPack entity, final String origin) {
		try {
			return objectMapper.readValue(entity.payload(), SENML_LIST_TYPE);
		} catch (final JsonProcessingException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Stored SenML data is invalid", origin, ex);
		}
	}

	private List<SenMLRecordDTO> selectSignals(
			final List<List<SenMLRecordDTO>> packs,
			final String serviceName,
			final double from,
			final double to,
			final List<String> signals,
			final int signalCount) {
		final Map<String, Integer> remaining = new HashMap<>();
		signals.forEach(signal -> remaining.put(signal, signalCount));
		final List<SenMLRecordDTO> result = new ArrayList<>();
		result.add(new SenMLRecordDTO(serviceName, 0D, null, null, null, null, null, null, null, null, null, null, null, null, null));

		for (final List<SenMLRecordDTO> pack : packs) {
			final double baseTime = pack.getFirst().bt();
			final String baseUnit = pack.getFirst().bu();
			for (final SenMLRecordDTO record : pack) {
				final Integer left = remaining.get(record.n());
				final double timestamp = absoluteTime(record, baseTime);
				if (left != null && left > 0 && timestamp >= from && timestamp <= to) {
					result.add(new SenMLRecordDTO(null, null, null, null, null, null, record.n(), record.u() == null ? baseUnit : record.u(),
							record.v(), record.vs(), record.vb(), record.vd(), record.s(), timestamp, record.ut()));
					remaining.put(record.n(), left - 1);
				}
			}
		}
		return List.copyOf(result);
	}

	private double absoluteTime(final SenMLRecordDTO record, final double baseTime) {
		if (record.t() == null) {
			return baseTime;
		}
		return record.t() < GraphDBManagerConstants.RELATIVE_TIMESTAMP_INDICATOR ? baseTime + record.t() : record.t();
	}
}
