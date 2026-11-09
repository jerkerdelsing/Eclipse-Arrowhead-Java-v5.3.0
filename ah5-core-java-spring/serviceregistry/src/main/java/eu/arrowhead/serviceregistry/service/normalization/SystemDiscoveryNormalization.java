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
package eu.arrowhead.serviceregistry.service.normalization;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;

@Service
public class SystemDiscoveryNormalization {

	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressValidator;

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Autowired
	private VersionNormalizer versionNormalizer;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private DeviceNameNormalizer deviceNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public NormalizedSystemRequestDTO normalizeSystemRequestDTO(final SystemRequestDTO dto) {
		logger.debug("normalizeSystemRequestDTO started");
		Assert.notNull(dto, "SystemRequestDTO is null");

		return new NormalizedSystemRequestDTO(
				systemNameNormalizer.normalize(dto.name()),
				dto.metadata(),
				versionNormalizer.normalize(dto.version()),
				Utilities.isEmpty(dto.addresses())
						? new ArrayList<>()
						: dto.addresses()
								.stream()
								.map(a -> addressNormalizer.normalize(a))
								.map(na -> new AddressDTO(addressValidator.detectType(na).name(), na))
								.collect(Collectors.toList()),
				Utilities.isEmpty(dto.deviceName()) ? null : deviceNameNormalizer.normalize(dto.deviceName()));
	}

	//-------------------------------------------------------------------------------------------------
	public SystemLookupRequestDTO normalizeSystemLookupRequestDTO(final SystemLookupRequestDTO dto) {
		logger.debug("normalizeSystemLookupRequestDTO started");

		if (dto == null) {
			return new SystemLookupRequestDTO(null, null, null, null, null, null);
		}

		return new SystemLookupRequestDTO(
				Utilities.isEmpty(dto.systemNames()) ? null : dto.systemNames().stream().map(n -> systemNameNormalizer.normalize(n)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addresses()) ? null : dto.addresses().stream().map(a -> addressNormalizer.normalize(a)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.addressType()) ? null : dto.addressType().trim().toUpperCase(),
				dto.metadataRequirementList(),
				Utilities.isEmpty(dto.versions()) ? null : dto.versions().stream().map(v -> versionNormalizer.normalize(v)).collect(Collectors.toList()),
				Utilities.isEmpty(dto.deviceNames()) ? null : dto.deviceNames().stream().map(dn -> deviceNameNormalizer.normalize(dn)).collect(Collectors.toList()));
	}

	//-------------------------------------------------------------------------------------------------
	public String normalizeRevokeSystemName(final String name) {
		logger.debug("normalizeRevokeSystemName started");
		Assert.notNull(name, "System name is null");

		return systemNameNormalizer.normalize(name);
	}
}