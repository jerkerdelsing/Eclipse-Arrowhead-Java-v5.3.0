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
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;
import eu.arrowhead.serviceregistry.service.validation.ServiceDiscoveryValidation;

@Service
public class ServiceDiscoveryService {

	//=================================================================================================
	// members

	@Autowired
	private ServiceRegistrySystemInfo sysInfo;

	@Autowired
	private ServiceDiscoveryValidation validator;

	@Autowired
	private ServiceInstanceDbService instanceDbService;

	@Autowired
	private SystemDbService systemDbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private ReservedServiceDefinitions reservedDefinitions;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceResponseDTO registerService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("registerService started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final ServiceInstanceRequestDTO normalized = validator.validateAndNormalizeRegisterService(dto, origin);
		// core and support service definitions only for their owning systems (J9)
		reservedDefinitions.check(normalized.systemName(), normalized.serviceDefinitionName(), origin);

		try {
			final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry = instanceDbService.createBulk(List.of(normalized)).getFirst();
			final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet = systemDbService.getByName(instanceEntry.getKey().getSystem().getName()).get();

			return dtoConverter.convertServiceInstanceEntityToDTO(instanceEntry, systemTriplet);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceListResponseDTO lookupServices(final ServiceInstanceLookupRequestDTO dto, final boolean verbose, final boolean restricted, final String origin) {
		logger.debug("lookupServices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final ServiceInstanceLookupRequestDTO normalized = validator.validateAndNormalizeLookupService(dto, origin);

		if (restricted) {
			if (Utilities.isEmpty(normalized.metadataRequirementsList())) {
				normalized.metadataRequirementsList().add(new MetadataRequirementDTO());
			}

			for (final MetadataRequirementDTO metadataReq : normalized.metadataRequirementsList()) {
				metadataReq.put(Constants.METADATA_KEY_UNRESTRICTED_DISCOVERY, true);
			}
		}

		try {
			final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces = instanceDbService.getPageByFilters(
					PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, ServiceInstance.DEFAULT_SORT_FIELD),
					new ServiceLookupFilterModel(normalized));

			if (verbose) {
				if (!sysInfo.isDiscoveryVerbose()) {
					throw new ForbiddenException("Verbose is not allowed");
				}

				final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemsWithDevices = systemDbService.getPageByFilters(
						PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, System.DEFAULT_SORT_FIELD),
						List.copyOf(servicesWithInterfaces.stream().map(e -> e.getKey().getSystem().getName()).collect(Collectors.toSet())),
						null, null, null, null, null);

				return dtoConverter.convertServiceInstanceListToDTO(servicesWithInterfaces, systemsWithDevices);
			}

			return dtoConverter.convertServiceInstanceListToDTO(servicesWithInterfaces, null);
		} catch (final ForbiddenException ex) {
			throw new ForbiddenException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean revokeService(final String identifiedSystemName, final String instanceId, final String origin) {
		logger.debug("revokeService started");
		Assert.isTrue(!Utilities.isEmpty(identifiedSystemName), "identifiedSystemName is empty");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final Entry<String, String> normalized = validator.validateAndNormalizeRevokeService(identifiedSystemName, instanceId, origin);

		if (!ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId(normalized.getValue()).equals(normalized.getKey())) {
			throw new ForbiddenException("Revoking other systems' service is forbidden", origin);
		}

		try {
			return instanceDbService.deleteByInstanceId(normalized.getValue());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}