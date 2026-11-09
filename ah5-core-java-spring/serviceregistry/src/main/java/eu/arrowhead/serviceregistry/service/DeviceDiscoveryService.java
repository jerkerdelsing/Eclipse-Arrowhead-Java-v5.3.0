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
package eu.arrowhead.serviceregistry.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.matching.AddressMatching;
import eu.arrowhead.serviceregistry.service.validation.DeviceDiscoveryValidation;

@Service
public class DeviceDiscoveryService {

	//=================================================================================================
	// members

	@Autowired
	private DeviceDbService dbService;

	@Autowired
	private DeviceDiscoveryValidation validator;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private AddressMatching addressMatcher;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Entry<DeviceResponseDTO, Boolean> registerDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("registerDevice started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final NormalizedDeviceRequestDTO normalized = validator.validateAndNormalizeRegisterDevice(dto, origin);

		try {
			final Optional<Entry<Device, List<DeviceAddress>>> optional = dbService.getByName(normalized.name());

			// Existing device
			if (optional.isPresent()) {
				final Device existing = optional.get().getKey();

				if (!Utilities.isEmpty(existing.getMetadata()) || !Utilities.isEmpty(normalized.metadata())) {
					if ((!Utilities.isEmpty(existing.getMetadata()) && Utilities.isEmpty(normalized.metadata()))
							|| Utilities.isEmpty(existing.getMetadata())) {
						throw new InvalidParameterException("Device with name '" + normalized.name() + "' already exists, but provided metadata is not matching");
					}

					// compared parsed: the stored text depends on key order and the line separator of the host that wrote it
					final Map<String, Object> existingMetadata = Utilities.fromJson(existing.getMetadata(), new TypeReference<Map<String, Object>>() {
					});
					if (!existingMetadata.equals(normalized.metadata())) {
						throw new InvalidParameterException("Device with name '" + normalized.name() + "' already exists, but provided metadata is not matching");
					}
				}

				final List<AddressDTO> existingAddresses = optional.get().getValue()
						.stream()
						.map(a -> new AddressDTO(a.getAddressType().toString(), a.getAddress()))
						.collect(Collectors.toList());
				if (!addressMatcher.isAddressListMatching(existingAddresses, normalized.addresses())) {
					throw new InvalidParameterException("Device with name '" + normalized.name() + "' already exists, but provided addresses are not matching");
				}

				return Map.entry(dtoConverter.convertDeviceEntityToDeviceResponseDTO(existing, optional.get().getValue()), false);
			}

			// New device
			final Entry<Device, List<DeviceAddress>> entities = dbService.create(normalized);

			return Map.entry(dtoConverter.convertDeviceEntityToDeviceResponseDTO(entities.getKey(), entities.getValue()), true);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO lookupDevice(final DeviceLookupRequestDTO dto, final String origin) {
		logger.debug("lookupDevice started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final DeviceLookupRequestDTO normalized = validator.validateAndNormalizeLookupDevice(dto, origin);

		try {
			final List<Entry<Device, List<DeviceAddress>>> entries = dbService.getByFilters(normalized.deviceNames(), normalized.addresses(),
					Utilities.isEmpty(normalized.addressType()) ? null : AddressType.valueOf(normalized.addressType()), normalized.metadataRequirementList());

			return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(entries, entries.size());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeDevice(final String name, final String origin) {
		logger.debug("revokeDevice started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalized = validator.validateAndNormalizeRevokeDevice(name, origin);

		try {
			return dbService.deleteByName(normalized);
		} catch (final LockedException ex) {
			throw new LockedException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}