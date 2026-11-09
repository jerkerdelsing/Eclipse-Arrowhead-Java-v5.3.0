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
package eu.arrowhead.serviceregistry.service.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.dto.DeviceLookupRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.DeviceDiscoveryNormalization;

@Service
public class DeviceDiscoveryValidation {

	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private DeviceNameValidator deviceNameValidator;

	@Autowired
	private DeviceDiscoveryNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public NormalizedDeviceRequestDTO validateAndNormalizeRegisterDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterDevice started");

		validateRegisterDevice(dto, origin);

		final NormalizedDeviceRequestDTO normalized = normalizer.normalizeDeviceRequestDTO(dto);

		try {
			deviceNameValidator.validateDeviceName(normalized.name());
			normalized.addresses().forEach(address -> addressValidator.validateNormalizedAddress(AddressType.valueOf(address.type()), address.address()));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceLookupRequestDTO validateAndNormalizeLookupDevice(final DeviceLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupDevice started");

		validateLookupDevice(dto, origin);
		final DeviceLookupRequestDTO normalized = normalizer.normalizeDeviceLookupRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.deviceNames())) {
				normalized.deviceNames().forEach(d -> deviceNameValidator.validateDeviceName(d));
			}

			if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
				normalized.addresses().forEach(a -> addressValidator.validateNormalizedAddress(AddressType.valueOf(normalized.addressType()), a));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRevokeDevice(final String name, final String origin) {
		logger.debug("validateAndNormalizeRevokeDevice started");

		validateRevokeDevice(name, origin);

		final String normalized = normalizer.normalizeDeviceName(name);

		try {
			deviceNameValidator.validateDeviceName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateRegisterDevice(final DeviceRequestDTO dto, final String origin) {
		logger.debug("validateRegisterDevice started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.name())) {
			throw new InvalidParameterException("Device name is empty", origin);
		}

		if (Utilities.isEmpty(dto.addresses())) {
			throw new InvalidParameterException("At least one device address is needed", origin);
		}

		for (final String address : dto.addresses()) {
			if (Utilities.isEmpty(address)) {
				throw new InvalidParameterException("Address is missing", origin);
			}
		}

		if (!Utilities.isEmpty(dto.metadata())) {
			try {
				MetadataValidation.validateMetadataKey(dto.metadata());
			} catch (final InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateLookupDevice(final DeviceLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupDevice started");

		if (dto != null) {
			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType().toUpperCase(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}

			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRevokeDevice(final String name, final String origin) {
		logger.debug("validateRevokeDevice started");

		if (Utilities.isEmpty(name)) {
			throw new InvalidParameterException("Device name is empty", origin);
		}
	}
}