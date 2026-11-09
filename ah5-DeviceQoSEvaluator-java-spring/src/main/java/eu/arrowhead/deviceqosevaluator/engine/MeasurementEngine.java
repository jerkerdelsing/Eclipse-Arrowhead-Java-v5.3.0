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
package eu.arrowhead.deviceqosevaluator.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.deviceqosevaluator.engine.SystemDeviceMap.Address;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.SystemDbService;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.AugmentedMeasurementJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.RttMeasurementJobScheduler;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.enums.AddressType;

@Service
public class MeasurementEngine {

	//=================================================================================================
	// members

	@Autowired
	private ArrowheadHttpService ahHttpService;

	@Autowired
	private DeviceDbService deviceDbService;

	@Autowired
	private SystemDbService systemDbService;

	@Autowired
	private RttMeasurementJobScheduler rttMeasurementJobScheduler;

	@Autowired
	private AugmentedMeasurementJobScheduler augmentedMeasurementJobScheduler;

	private boolean working = false;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<Integer, Integer> organize() throws SchedulerException, ArrowheadException {
		logger.debug("organize started");

		if (working) {
			return null;
		}

		working = true;
		final SystemDeviceMap systemDeviceMap = acquireSystemsAndDevices();
		final int newSysCnt = arrangeDatabaseAndMeasurements(systemDeviceMap);
		final int removedSysCnt = cleanDatabase();
		working = false;
		return Pair.of(newSysCnt, removedSysCnt);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SystemDeviceMap acquireSystemsAndDevices() {
		logger.debug("acquireSystemsAndDevices started");

		final SystemDeviceMap map = new SystemDeviceMap();

		boolean hasMorePage = false;
		int pageNumber = 0;
		Integer pageSize = null;
		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(1);
		queryParams.put(Constants.VERBOSE, List.of(String.valueOf(true)));

		long counter = 0;
		do {
			final SystemListResponseDTO response = ahHttpService.consumeService(
					Constants.SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT,
					Constants.SERVICE_OP_SYSTEM_QUERY,
					SystemListResponseDTO.class,
					new SystemQueryRequestDTO(new PageDTO(pageNumber == 0 ? null : pageNumber, pageSize, null, null), null, null, null, null, null, null),
					queryParams);

			counter = counter + response.entries().size();
			hasMorePage = counter < response.count();
			pageNumber = hasMorePage ? pageNumber + 1 : pageNumber;
			pageSize = pageSize == null ? response.entries().size() : pageSize;

			map.load(response.entries());
		} while (hasMorePage);

		return map;
	}

	//-------------------------------------------------------------------------------------------------
	private int arrangeDatabaseAndMeasurements(final SystemDeviceMap systemDeviceMap) throws SchedulerException {
		logger.debug("arrangeDatabaseAndMeasurements started");

		int newSysCnt = 0;
		for (int i = 0; i < systemDeviceMap.getDeviceSize(); ++i) {

			// Handle devices
			final Set<Address> deviceAddresses = systemDeviceMap.getDeviceAddresses(i);
			final List<Device> deviceRecords = deviceDbService.findByAddresses(deviceAddresses.stream().map(a -> a.address()).collect(Collectors.toSet()));

			Device deviceRecord = null;
			if (Utilities.isEmpty(deviceRecords)) {
				// New device
				final String address = selectAddress(deviceAddresses);
				if (!Utilities.isEmpty(address)) {
					deviceRecord = deviceDbService.create(address, systemDeviceMap.hasAugmented(i));
					rttMeasurementJobScheduler.start(deviceRecord);
					if (deviceRecord.isAugmented()) {
						augmentedMeasurementJobScheduler.start(deviceRecord);
					}
				}
			} else {
				// Existing device
				deviceRecord = specifyDevice(deviceRecords); // It can happen that a device was considered as multiple device before, but now the new data shows the truth
				boolean needUpdate = false;
				if (deviceRecord.isAugmented() != systemDeviceMap.hasAugmented(i)) {
					deviceRecord.setAugmented(systemDeviceMap.hasAugmented(i));
					needUpdate = true;
				}
				if (deviceRecord.isInactive()) {
					deviceRecord.setInactive(false);
					needUpdate = true;
				}
				if (needUpdate) {
					deviceDbService.update(deviceRecord);
				}
				if (!rttMeasurementJobScheduler.isScheduled(deviceRecord)) {
					rttMeasurementJobScheduler.start(deviceRecord);
				}
				if (deviceRecord.isAugmented() && !augmentedMeasurementJobScheduler.isScheduled(deviceRecord)) {
					augmentedMeasurementJobScheduler.start(deviceRecord);
				} else if (!deviceRecord.isAugmented() && augmentedMeasurementJobScheduler.isScheduled(deviceRecord)) {
					augmentedMeasurementJobScheduler.stop(List.of(deviceRecord));
				}
			}

			// new device without any address or empty hostname (not very likely)
			if (deviceRecord == null) {
				continue;
			}

			// Handle systems
			final Set<String> deviceSystems = systemDeviceMap.getDeviceSystems(i);
			final List<System> toSave = new ArrayList<>(deviceSystems.size());

			final List<System> systemRecordsByDevice = systemDbService.findByDeviceId(deviceRecord.getId());
			for (final System sysRecord : systemRecordsByDevice) {
				if (!deviceSystems.contains(sysRecord.getName())) {
					sysRecord.setDevice(null);
					toSave.add(sysRecord);
				}
			}

			if (!toSave.isEmpty()) {
				systemDbService.save(toSave);
				toSave.clear();
			}

			final List<System> systemRecords = systemDbService.findByNames(deviceSystems);
			for (final String sysName : deviceSystems) {
				boolean systemExists = false;
				for (final System sysRecord : systemRecords) {
					if (sysRecord.getName().equals(sysName)) {
						systemExists = true;
						if (sysRecord.getDevice() == null || !sysRecord.getDevice().getId().equals(deviceRecord.getId())) {
							sysRecord.setDevice(deviceRecord);
							toSave.add(sysRecord);
						}
						break;
					}
				}

				if (!systemExists) {
					toSave.add(new System(sysName, deviceRecord));
					newSysCnt++;
				}
			}

			if (!toSave.isEmpty()) {
				systemDbService.save(toSave);
			}
		}

		return newSysCnt;
	}

	//-------------------------------------------------------------------------------------------------
	private String selectAddress(final Set<Address> addresses) {
		logger.debug("selectAddress started");

		Address selected = null;

		for (final Address address : addresses) {
			if (address.type() == AddressType.MAC) {
				// does not happen in theory
				continue;
			}

			if (selected == null || selected.type() == AddressType.HOSTNAME) {
				selected = address;
			}

			if (selected.deviceRelated() && selected.type() != AddressType.HOSTNAME) {
				break;
			}
		}

		return selected.address();
	}

	//-------------------------------------------------------------------------------------------------
	private Device specifyDevice(final List<Device> devices) {
		logger.debug("specifyDevice started");

		Device selected = devices.getFirst();

		if (devices.size() > 1) {
			for (int i = 1; i < devices.size(); ++i) {
				final Device device = devices.get(i);
				if (selected.isInactive() && !device.isInactive()) {
					selected = device;

				} else if (selected.isInactive() == device.isInactive()) {
					if (!selected.isAugmented() && device.isAugmented()) {
						selected = device;
					} else if (selected.getCreatedAt().isAfter(device.getCreatedAt())) {
						selected = device;
					}
				}
			}
		}

		return selected;
	}

	//-------------------------------------------------------------------------------------------------
	private int cleanDatabase() {
		logger.debug("cleanDatabase started");

		return systemDbService.deleteSystemsWithoutDevice();
	}
}