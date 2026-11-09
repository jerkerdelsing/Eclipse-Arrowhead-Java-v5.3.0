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
package eu.arrowhead.serviceregistry.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceSystemConnector;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceAddressRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceRepository;
import eu.arrowhead.serviceregistry.jpa.repository.DeviceSystemConnectorRepository;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;

@Service
public class DeviceDbService {

	//=================================================================================================
	// members

	@Autowired
	private DeviceRepository deviceRepo;

	@Autowired
	private DeviceAddressRepository addressRepo;

	@Autowired
	private DeviceSystemConnectorRepository connectorRepo;

	private static final Object LOCK = new Object();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<Entry<Device, List<DeviceAddress>>> getByFilters(
			final List<String> names,
			final List<String> addresses,
			final AddressType addressType,
			final List<MetadataRequirementDTO> metadataRequirementList) {
		logger.debug("getByFilters started");

		final Page<Entry<Device, List<DeviceAddress>>> page = getPage(PageRequest.of(0, Integer.MAX_VALUE), names, addresses, addressType, metadataRequirementList);

		return page.toList();
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Entry<Device, List<DeviceAddress>>> getPage(final PageRequest pagination) {
		return getPage(pagination, null, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Entry<Device, List<DeviceAddress>>> getPage(
			final PageRequest pagination,
			final List<String> names,
			final List<String> addresses,
			final AddressType addressType,
			final List<MetadataRequirementDTO> metadataRequirementList) {
		logger.debug("getPage started");
		Assert.notNull(pagination, "page is null");

		try {
			Page<Device> deviceEntries = null;

			// Without filters
			if (Utilities.isEmpty(names)
					&& Utilities.isEmpty(addresses)
					&& addressType == null
					&& Utilities.isEmpty(metadataRequirementList)) {
				deviceEntries = deviceRepo.findAll(pagination);
			}

			// With filters
			if (deviceEntries == null) {
				final List<String> matchings = new ArrayList<>();

				synchronized (LOCK) {
					final List<Device> toFilter = Utilities.isEmpty(names) ? deviceRepo.findAll() : deviceRepo.findAllByNameIn(names);
					for (final Device device : toFilter) {
						if (addressType != null && Utilities.isEmpty(addressRepo.findAllByDeviceAndAddressType(device, addressType))) {
							continue;
						}

						if (!Utilities.isEmpty(addresses) && Utilities.isEmpty(addressRepo.findAllByDeviceAndAddressIn(device, addresses))) {
							continue;
						}

						if (!Utilities.isEmpty(metadataRequirementList)) {
							final Map<String, Object> metadata = Utilities.fromJson(device.getMetadata(), new TypeReference<Map<String, Object>>() {
							});

							boolean metadataMatch = false;
							for (final MetadataRequirementDTO requirement : metadataRequirementList) {
								if (MetadataRequirementsMatcher.isMetadataMatch(metadata, requirement)) {
									metadataMatch = true;
									break;
								}
							}
							if (!metadataMatch) {
								continue;
							}
						}

						matchings.add(device.getName());
					}

					deviceEntries = deviceRepo.findAllByNameIn(matchings, pagination);
				}
			}

			// Finalize
			final List<Entry<Device, List<DeviceAddress>>> resultList = new ArrayList<>();
			for (final Device device : deviceEntries) {
				resultList.add(Map.entry(
						device,
						addressRepo.findAllByDevice(device)));
			}

			return new PageImpl<Entry<Device, List<DeviceAddress>>>(resultList, pagination, deviceEntries.getTotalElements());
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<Entry<Device, List<DeviceAddress>>> getByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "device name is empty");

		try {
			final Optional<Device> deviceOpt = deviceRepo.findByName(name);
			if (deviceOpt.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(Map.entry(deviceOpt.get(), addressRepo.findAllByDevice(deviceOpt.get())));
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Entry<Device, List<DeviceAddress>>> createBulk(final List<NormalizedDeviceRequestDTO> candidates) {
		logger.debug("createBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "device candidate list is empty");

		try {
			final List<String> names = candidates.stream()
					.map(d -> d.name())
					.collect(Collectors.toList());

			final List<Device> existing = deviceRepo.findAllByNameIn(names);
			if (!Utilities.isEmpty(existing)) {
				final String existingNames = existing.stream()
						.map(e -> e.getName())
						.collect(Collectors.joining(", "));
				throw new InvalidParameterException("Device with names already exists: " + existingNames);
			}

			List<Device> deviceEntities = candidates.stream()
					.map(d -> new Device(d.name(), Utilities.toJson(d.metadata())))
					.collect(Collectors.toList());
			deviceEntities = deviceRepo.saveAllAndFlush(deviceEntities);

			final List<DeviceAddress> deviceAddressEntities = new ArrayList<>();
			for (final NormalizedDeviceRequestDTO deviceEntry : candidates) {
				if (!Utilities.isEmpty(deviceEntry.addresses())) {
					final Device device = deviceEntities.stream()
							.filter(d -> d.getName().equals(deviceEntry.name()))
							.findFirst()
							.get();

					final List<DeviceAddress> addresses = deviceEntry.addresses().stream()
							.map(a -> new DeviceAddress(
									device,
									AddressType.valueOf(a.type()),
									a.address()))
							.collect(Collectors.toList());
					deviceAddressEntities.addAll(addresses);
				}
			}

			addressRepo.saveAllAndFlush(deviceAddressEntities);

			final List<Entry<Device, List<DeviceAddress>>> results = new ArrayList<>(deviceEntities.size());
			for (final Device device : deviceEntities) {
				results.add(Map.entry(device, addressRepo.findAllByDevice(device)));
			}

			return results;
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Entry<Device, List<DeviceAddress>> create(final NormalizedDeviceRequestDTO candidate) {
		logger.debug("create started");
		Assert.notNull(candidate, "device candidate is null");
		Assert.isTrue(!Utilities.isEmpty(candidate.name()), "device candidate name is empty");

		try {
			final Optional<Device> existingOpt = deviceRepo.findByName(candidate.name());
			if (existingOpt.isPresent()) {
				throw new InvalidParameterException("Device with name '" + candidate.name() + "' already exists");
			}

			final Device deviceEntity = deviceRepo.saveAndFlush(new Device(candidate.name(), Utilities.toJson(candidate.metadata())));

			final Entry<Device, List<DeviceAddress>> result = Map.entry(deviceEntity, new ArrayList<>());
			if (!Utilities.isEmpty(candidate.addresses())) {
				final List<DeviceAddress> addresses = candidate.addresses().stream()
						.map(a -> new DeviceAddress(deviceEntity, AddressType.valueOf(a.type()), a.address()))
						.collect(Collectors.toList());

				result.getValue().addAll(addressRepo.saveAllAndFlush(addresses));
			}

			return result;
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Entry<Device, List<DeviceAddress>>> updateBulk(final List<NormalizedDeviceRequestDTO> candidates) {
		logger.debug("updateBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidate list is missing or empty");

		try {
			final List<String> candidateNames = candidates.stream()
					.map(c -> c.name())
					.collect(Collectors.toList());

			List<Device> deviceEntries = deviceRepo.findAllByNameIn(candidateNames);

			if (deviceEntries.size() < candidates.size()) {
				final List<String> existingDeviceNames = deviceEntries.stream()
						.map(d -> d.getName())
						.collect(Collectors.toList());

				final String notExists = candidates.stream()
						.map(c -> c.name())
						.filter(n -> !existingDeviceNames.contains(n))
						.collect(Collectors.joining(", "));
				throw new InvalidParameterException("Device(s) not exists: " + notExists);
			}

			for (final Device device : deviceEntries) {
				final Map<String, Object> metadata = candidates.stream()
						.filter(c -> c.name().equals(device.getName()))
						.findFirst()
						.get()
						.metadata();
				device.setMetadata(Utilities.toJson(metadata));
			}

			deviceEntries = deviceRepo.saveAllAndFlush(deviceEntries);

			addressRepo.deleteAllByDeviceIn(deviceEntries);
			final List<DeviceAddress> newAddresses = new ArrayList<>();
			for (final NormalizedDeviceRequestDTO candidate : candidates) {
				if (!Utilities.isEmpty(candidate.addresses())) {
					final Device device = deviceEntries.stream()
							.filter(d -> candidate.name().equals(d.getName()))
							.findFirst()
							.get();

					newAddresses.addAll(candidate.addresses().stream()
							.map(a -> new DeviceAddress(device, AddressType.valueOf(a.type()), a.address()))
							.collect(Collectors.toList()));
				}
			}

			addressRepo.saveAllAndFlush(newAddresses);

			final List<Entry<Device, List<DeviceAddress>>> results = new ArrayList<>(deviceEntries.size());
			for (final Device device : deviceEntries) {
				results.add(Map.entry(device, addressRepo.findAllByDevice(device)));
			}

			return results;
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteByNameList(final List<String> names) {
		logger.debug("deleteByNameList started");
		Assert.isTrue(!Utilities.isEmpty(names), "device name list is missing or empty");

		try {
			final List<Device> entries = deviceRepo.findAllByNameIn(names);

			final List<DeviceSystemConnector> connections = connectorRepo.findByDeviceIn(entries);
			if (!connections.isEmpty()) {
				throw new LockedException("At least one system is assigned to these devices");
			}

			deviceRepo.deleteAll(entries);
			deviceRepo.flush();
		} catch (final LockedException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteByName(final String name) {
		logger.debug("deleteByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "device name is empty");

		try {
			final Optional<Device> optional = deviceRepo.findByName(name);
			if (optional.isPresent()) {

				final List<DeviceSystemConnector> connections = connectorRepo.findByDevice(optional.get());
				if (!connections.isEmpty()) {
					throw new LockedException("At least one system is assigned to this device");
				}

				deviceRepo.delete(optional.get());
				deviceRepo.flush();

				return true;
			}

			return false;
		} catch (final LockedException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}