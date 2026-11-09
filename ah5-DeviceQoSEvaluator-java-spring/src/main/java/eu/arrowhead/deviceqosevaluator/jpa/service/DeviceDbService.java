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
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.repository.DeviceRepository;

@Service
public class DeviceDbService {

	//=================================================================================================
	// members

	@Autowired
	private DeviceRepository deviceRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<Device> findById(final UUID id) {
		logger.debug("findById started");
		Assert.notNull(id, "id is null");

		try {
			return deviceRepo.findById(id);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<Device> findByAddresses(final Set<String> addresses) {
		logger.debug("findByAddresses started");
		Assert.isTrue(!Utilities.isEmpty(addresses), "address set is empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(addresses), "address set contains empty element");

		try {
			return deviceRepo.findAllByAddressIn(addresses);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Device> getPage(final Pageable page) {
		logger.debug("getPage started");
		Assert.notNull(page, "page is null");

		try {
			return deviceRepo.findAll(page);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}

	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Device create(final String address, final boolean augmented) {
		logger.debug("create started");
		Assert.isTrue(!Utilities.isEmpty(address), "address is empty");

		try {
			return deviceRepo.saveAndFlush(new Device(UUID.randomUUID(), address, null, augmented, false));
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Device update(final Device device) {
		logger.debug("update started");
		Assert.notNull(device, "device is null");
		Assert.notNull(device.getId(), "device.id is null");
		Assert.isTrue(!Utilities.isEmpty(device.getAddress()), "device.address is empty");

		try {
			return deviceRepo.saveAndFlush(device);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void update(final Iterable<Device> devices) {
		logger.debug("update started");
		Assert.notNull(devices, "devices is null");
		Assert.isTrue(!Utilities.containsNull(devices), "devices contains null");

		try {
			deviceRepo.saveAllAndFlush(devices);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void remove(final Iterable<Device> devices) {
		logger.debug("remove started");
		Assert.notNull(devices, "devices is null");

		try {
			deviceRepo.deleteAllInBatch(devices);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
