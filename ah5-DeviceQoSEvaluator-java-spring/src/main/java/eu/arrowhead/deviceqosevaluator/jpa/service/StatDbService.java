/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.deviceqosevaluator.jpa.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatCpuTotalLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatMemoryUsed;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatNetEgressLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatNetIngressLoad;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatRoundTripTime;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatCpuTotalLoadRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatMemoryUsedRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatNetEgressLoadRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatNetIngressLoadRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.StatRoundTripTimeRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.SystemRepository;
import eu.arrowhead.deviceqosevaluator.jpa.service.model.StatQueryResultModel;

@Service
public class StatDbService {

	//=================================================================================================
	// members

	@Autowired
	private StatRoundTripTimeRepository rttStatRepo;

	@Autowired
	private StatCpuTotalLoadRepository cpuStatRepo;

	@Autowired
	private StatMemoryUsedRepository memoryStatRepo;

	@Autowired
	private StatNetEgressLoadRepository netEgressStatRepo;

	@Autowired
	private StatNetIngressLoadRepository netIngressStatRepo;

	@Autowired
	private SystemRepository systemRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumberCheck")
	@Transactional(rollbackFor = ArrowheadException.class)
	public void save(final ZonedDateTime timestamp, final OidGroup oidGroup, final UUID deviceId, final List<Double> data) {
		logger.debug("save started");
		Assert.notNull(timestamp, "timestamp is null");
		Assert.notNull(oidGroup, "oidGroup is null");
		Assert.notNull(deviceId, "deviceId is null");
		Assert.isTrue(!Utilities.isEmpty(data), "data is empty");
		Assert.isTrue(!Utilities.containsNull(data), "data contains null element");
		Assert.isTrue(data.size() == 5, "data list has invalid size");

		try {
			switch (oidGroup) {
			case RTT:
				rttStatRepo.saveAndFlush(new StatRoundTripTime(deviceId, timestamp, data.get(0), data.get(1), data.get(2), data.get(3), data.get(4)));
				break;
			case CPU_TOTAL_LOAD:
				cpuStatRepo.saveAndFlush(new StatCpuTotalLoad(deviceId, timestamp, data.get(0), data.get(1), data.get(2), data.get(3), data.get(4)));
				break;
			case MEMORY_USED:
				memoryStatRepo.saveAndFlush(new StatMemoryUsed(deviceId, timestamp, data.get(0), data.get(1), data.get(2), data.get(3), data.get(4)));
				break;

			case NETWORK_EGRESS_LOAD:
				netEgressStatRepo.saveAndFlush(new StatNetEgressLoad(deviceId, timestamp, data.get(0), data.get(1), data.get(2), data.get(3), data.get(4)));
				break;

			case NETWORK_INGRESS_LOAD:
				netIngressStatRepo.saveAndFlush(new StatNetIngressLoad(deviceId, timestamp, data.get(0), data.get(1), data.get(2), data.get(3), data.get(4)));
				break;

			default:
				logger.error("Unhandled OID group: " + oidGroup);
			}

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<StatEntity> getByDeviceIdAfterTimestamp(final OidGroup oidGroup, final UUID deviceId, final ZonedDateTime timestamp) {
		logger.debug("getByDeviceIdAfterTimestamp started");
		Assert.notNull(oidGroup, "oidGroup is null");
		Assert.notNull(deviceId, "deviceId is null");
		Assert.notNull(timestamp, "timestamp is null");

		final List<StatEntity> result = new ArrayList<>();
		try {
			switch (oidGroup) {
			case RTT:
				result.addAll(rttStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp));
				break;
			case CPU_TOTAL_LOAD:
				result.addAll(cpuStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp));
				break;
			case MEMORY_USED:
				result.addAll(memoryStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp));
				break;

			case NETWORK_EGRESS_LOAD:
				result.addAll(netEgressStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp));
				break;

			case NETWORK_INGRESS_LOAD:
				result.addAll(netIngressStatRepo.findAllByUuidAndTimestampAfter(deviceId, timestamp));
				break;

			default:
				logger.error("Unhandled OID group: " + oidGroup);
			}

			return result;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasAny(final UUID deviceId) {
		logger.debug("hasAny started");
		Assert.notNull(deviceId, "deviceId is null");

		try {
			if (rttStatRepo.existsByUuid(deviceId)) {
				return true;
			}
			if (cpuStatRepo.existsByUuid(deviceId)) {
				return true;
			}
			if (memoryStatRepo.existsByUuid(deviceId)) {
				return true;
			}
			if (netEgressStatRepo.existsByUuid(deviceId)) {
				return true;
			}
			if (netIngressStatRepo.existsByUuid(deviceId)) {
				return true;
			}

			return false;

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeBeforeTimestamp(final ZonedDateTime timestamp) {
		logger.debug("removeBeforeTimestamp started");
		Assert.notNull(timestamp, "timestamp is null");

		try {
			rttStatRepo.deleteAllByTimestampBefore(timestamp);
			cpuStatRepo.deleteAllByTimestampBefore(timestamp);
			memoryStatRepo.deleteAllByTimestampBefore(timestamp);
			netEgressStatRepo.deleteAllByTimestampBefore(timestamp);
			netIngressStatRepo.deleteAllByTimestampBefore(timestamp);

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<StatQueryResultModel> query(final Collection<String> systemNames, final ZonedDateTime from, final ZonedDateTime to, final OidGroup oidGroup, final PageRequest pagination) {
		logger.debug("query started");
		Assert.notNull(oidGroup, "oidGroup is null");
		Assert.notNull(pagination, "pagination is null");

		try {
			final ZonedDateTime start = from == null ? Utilities.utcNow().minusYears(1) : from;
			final ZonedDateTime end = to == null ? Utilities.utcNow().plusMinutes(1) : to;

			Set<UUID> devices = new HashSet<>();
			if (!Utilities.isEmpty(systemNames)) {
				devices = systemRepo.findAllByNameIn(systemNames).stream().map(sys -> sys.getDevice().getId()).collect(Collectors.toSet());
			}

			final List<StatEntity> records = new ArrayList<>();
			Pageable pageable = null;
			long total = 0;
			switch (oidGroup) {
			case RTT:
				final Page<StatRoundTripTime> rttStatPage = queryRTT(devices, start, end, pagination);
				records.addAll(rttStatPage.getContent());
				pageable = rttStatPage.getPageable();
				total = rttStatPage.getTotalElements();
				break;

			case CPU_TOTAL_LOAD:
				final Page<StatCpuTotalLoad> cpuStatPage = queryCpuTotalLoad(devices, start, end, pagination);
				records.addAll(cpuStatPage.getContent());
				pageable = cpuStatPage.getPageable();
				total = cpuStatPage.getTotalElements();
				break;

			case MEMORY_USED:
				final Page<StatMemoryUsed> memoryStatPage = queryMemoryUsed(devices, start, end, pagination);
				records.addAll(memoryStatPage.getContent());
				pageable = memoryStatPage.getPageable();
				total = memoryStatPage.getTotalElements();
				break;

			case NETWORK_EGRESS_LOAD:
				final Page<StatNetEgressLoad> netEgressStatPage = queryNetEgressLoad(devices, start, end, pagination);
				records.addAll(netEgressStatPage.getContent());
				pageable = netEgressStatPage.getPageable();
				total = netEgressStatPage.getTotalElements();
				break;

			case NETWORK_INGRESS_LOAD:
				final Page<StatNetIngressLoad> netIngressStatPage = queryNetIngressLoad(devices, start, end, pagination);
				records.addAll(netIngressStatPage.getContent());
				pageable = netIngressStatPage.getPageable();
				total = netIngressStatPage.getTotalElements();
				break;

			default:
				logger.error("Unhandled OID group: " + oidGroup);
			}

			final Map<UUID, List<System>> deviceSysCache = new HashMap<>();
			final List<StatQueryResultModel> results = new ArrayList<>();
			for (final StatEntity stat : records) {
				if (!deviceSysCache.containsKey(stat.getUuid())) {
					deviceSysCache.put(stat.getUuid(), systemRepo.findAllByDevice_Id(stat.getUuid()));
				}

				results.add(new StatQueryResultModel(oidGroup, stat, deviceSysCache.get(stat.getUuid())));
			}

			return new PageImpl<>(results, pageable, total);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Page<StatRoundTripTime> queryRTT(final Set<UUID> devices, final ZonedDateTime start, final ZonedDateTime end, final PageRequest pagination) {
		logger.debug("queryRTT started");

		if (Utilities.isEmpty(devices)) {
			return rttStatRepo.findAllByTimestampBetween(start, end, pagination);
		}

		return rttStatRepo.findAllByUuidInAndTimestampBetween(devices, start, end, pagination);
	}

	//-------------------------------------------------------------------------------------------------
	private Page<StatCpuTotalLoad> queryCpuTotalLoad(final Set<UUID> devices, final ZonedDateTime start, final ZonedDateTime end, final PageRequest pagination) {
		logger.debug("queryCpuTotalLoad started");

		if (Utilities.isEmpty(devices)) {
			return cpuStatRepo.findAllByTimestampBetween(start, end, pagination);
		}

		return cpuStatRepo.findAllByUuidInAndTimestampBetween(devices, start, end, pagination);
	}

	//-------------------------------------------------------------------------------------------------
	private Page<StatMemoryUsed> queryMemoryUsed(final Set<UUID> devices, final ZonedDateTime start, final ZonedDateTime end, final PageRequest pagination) {
		logger.debug("queryMemoryUsed started");

		if (Utilities.isEmpty(devices)) {
			return memoryStatRepo.findAllByTimestampBetween(start, end, pagination);
		}

		return memoryStatRepo.findAllByUuidInAndTimestampBetween(devices, start, end, pagination);
	}

	//-------------------------------------------------------------------------------------------------
	private Page<StatNetEgressLoad> queryNetEgressLoad(final Set<UUID> devices, final ZonedDateTime start, final ZonedDateTime end, final PageRequest pagination) {
		logger.debug("queryNetEgressLoad started");

		if (Utilities.isEmpty(devices)) {
			return netEgressStatRepo.findAllByTimestampBetween(start, end, pagination);
		}

		return netEgressStatRepo.findAllByUuidInAndTimestampBetween(devices, start, end, pagination);
	}

	//-------------------------------------------------------------------------------------------------
	private Page<StatNetIngressLoad> queryNetIngressLoad(final Set<UUID> devices, final ZonedDateTime start, final ZonedDateTime end, final PageRequest pagination) {
		logger.debug("queryNetIngressLoad started");

		if (Utilities.isEmpty(devices)) {
			return netIngressStatRepo.findAllByTimestampBetween(start, end, pagination);
		}

		return netIngressStatRepo.findAllByUuidInAndTimestampBetween(devices, start, end, pagination);
	}
}