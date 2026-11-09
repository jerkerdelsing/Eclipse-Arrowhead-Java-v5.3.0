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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.repository.DeviceRepository;
import eu.arrowhead.deviceqosevaluator.jpa.repository.SystemRepository;

@Service
public class SystemDbService {

	//=================================================================================================
	// members

	@Autowired
	private SystemRepository systemRepo;

	@Autowired
	private DeviceRepository deviceRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void save(final Iterable<System> systems) {
		logger.debug("save started");
		Assert.notNull(systems, "systems is null");
		Assert.isTrue(!Utilities.containsNull(systems), "systems contains null");

		try {
			systemRepo.saveAllAndFlush(systems);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<System> findByNames(final Iterable<String> names) {
		logger.debug("findByNames started");
		Assert.notNull(names, "names is null");
		Assert.isTrue(!Utilities.containsNullOrEmpty(names), "names contains empty element");

		try {
			return systemRepo.findAllByNameIn(names);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<System> findByDeviceId(final UUID deviceId) {
		logger.debug("findByDeviceId started");
		Assert.notNull(deviceId, "deviceId is null");

		try {
			final Optional<Device> optional = deviceRepository.findById(deviceId);
			if (optional.isEmpty()) {
				return List.of();
			} else {
				return systemRepo.findAllByDevice(optional.get());
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public int deleteSystemsWithoutDevice() {
		logger.debug("deleteSystemsWithoutDevice started");

		try {
			final List<System> systems = systemRepo.findAllByDeviceIsNull();
			systemRepo.deleteAll(systems);
			systemRepo.flush();
			return systems.size();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}