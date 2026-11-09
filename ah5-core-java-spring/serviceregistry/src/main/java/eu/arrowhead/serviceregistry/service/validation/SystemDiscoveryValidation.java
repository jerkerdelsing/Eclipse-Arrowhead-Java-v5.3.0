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
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.SystemDiscoveryNormalization;

@Service
public class SystemDiscoveryValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemDiscoveryNormalization normalizer;

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private DeviceNameValidator deviceNameValidator;

	@Autowired
	private VersionValidator versionValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public NormalizedSystemRequestDTO validateAndNormalizeRegisterSystem(final SystemRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterSystem started");

		validateRegisterSystem(dto, origin);
		final NormalizedSystemRequestDTO normalized = normalizer.normalizeSystemRequestDTO(dto);

		try {
			systemNameValidator.validateSystemName(normalized.name());
			versionValidator.validateNormalizedVersion(normalized.version());
			normalized.addresses().forEach(address -> addressValidator.validateNormalizedAddress(AddressType.valueOf(address.type()), address.address()));
			if (!Utilities.isEmpty(normalized.deviceName())) {
				deviceNameValidator.validateDeviceName(normalized.deviceName());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public SystemLookupRequestDTO validateAndNormalizeLookupSystem(final SystemLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupSystem started");

		validateLookupSystem(dto, origin);
		final SystemLookupRequestDTO normalized = normalizer.normalizeSystemLookupRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.systemNames())) {
				normalized.systemNames().forEach(n -> systemNameValidator.validateSystemName(n));
			}

			if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
				normalized.addresses().forEach(a -> addressValidator.validateNormalizedAddress(AddressType.valueOf(normalized.addressType()), a));
			}

			if (!Utilities.isEmpty(normalized.versions())) {
				normalized.versions().forEach(v -> versionValidator.validateNormalizedVersion(v));
			}

			if (!Utilities.isEmpty(normalized.deviceNames())) {
				normalized.deviceNames().forEach(n -> deviceNameValidator.validateDeviceName(n));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRevokeSystem(final String name, final String origin) {
		logger.debug("validateRevokeSystem started");

		validateRevokeSystem(name, origin);
		final String normalized = normalizer.normalizeRevokeSystemName(name);

		try {
			systemNameValidator.validateSystemName(normalized);
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
	private void validateRegisterSystem(final SystemRequestDTO dto, final String origin) {
		logger.debug("validateRegisterSystem started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.name())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (!Utilities.isEmpty(dto.addresses())) {
			for (final String address : dto.addresses()) {
				if (Utilities.isEmpty(address)) {
					throw new InvalidParameterException("Address is missing", origin);
				}
			}
		}

		if (Utilities.isEmpty(dto.addresses()) && Utilities.isEmpty(dto.deviceName())) {
			throw new InvalidParameterException("At least one system address is needed for every system", origin);
		}

		if (!Utilities.isEmpty(dto.metadata())) {
			MetadataValidation.validateMetadataKey(dto.metadata());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateLookupSystem(final SystemLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupSystem started");

		if (dto != null) {
			// system name list
			if (!Utilities.isEmpty(dto.systemNames()) && Utilities.containsNullOrEmpty(dto.systemNames())) {
				throw new InvalidParameterException("System name list contains null or empty element", origin);
			}

			// address list
			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null or empty element", origin);
			}

			// address type
			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType().toUpperCase(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}

			// metadata
			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}

			// version list
			if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNull(dto.versions())) {
				throw new InvalidParameterException("Version list contains null element", origin);
			}

			// device name list
			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRevokeSystem(final String name, final String origin) {
		logger.debug("validateRevokeSystem started");

		if (Utilities.isEmpty(name)) {
			throw new InvalidParameterException("System name is empty", origin);
		}
	}
}