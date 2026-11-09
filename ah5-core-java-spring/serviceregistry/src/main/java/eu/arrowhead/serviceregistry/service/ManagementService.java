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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.jpa.service.DeviceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceDefinitionDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;
import eu.arrowhead.serviceregistry.jpa.service.SystemDbService;
import eu.arrowhead.serviceregistry.service.dto.DTOConverter;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.ManagementValidation;

@Service
public class ManagementService {

	//=================================================================================================
	// members

	@Autowired
	private ManagementValidation validator;

	@Autowired
	private PageService pageService;

	@Autowired
	private DeviceDbService deviceDbService;

	@Autowired
	private ServiceDefinitionDbService serviceDefinitionDbService;

	@Autowired
	private SystemDbService systemDbService;

	@Autowired
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	@Autowired
	private DTOConverter dtoConverter;

	@Autowired
	private ServiceInstanceDbService instanceDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	// DEVICES

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO createDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("createDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<NormalizedDeviceRequestDTO> normalized = validator.validateAndNormalizeCreateDevices(dto, origin);

		try {
			final List<Entry<Device, List<DeviceAddress>>> entities = deviceDbService.createBulk(normalized);

			return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(entities, entities.size());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO updateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("updateDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<NormalizedDeviceRequestDTO> normalized = validator.validateAndNormalizeUpdateDevices(dto, origin);

		try {
			final List<Entry<Device, List<DeviceAddress>>> entities = deviceDbService.updateBulk(normalized);

			return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(entities, entities.size());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO queryDevices(final DeviceQueryRequestDTO dto, final String origin) {
		logger.debug("queryDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final DeviceQueryRequestDTO normalized = validator.validateAndNormalizeQueryDevices(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, Device.SORTABLE_FIELDS_BY, Device.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<Entry<Device, List<DeviceAddress>>> page = deviceDbService.getPage(
					pageRequest,
					normalized.deviceNames(),
					normalized.addresses(),
					Utilities.isEmpty(normalized.addressType()) ? null : AddressType.valueOf(normalized.addressType()),
					normalized.metadataRequirementList());

			return dtoConverter.convertDeviceAndDeviceAddressEntriesToDTO(page, page.getTotalElements());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeDevices(final List<String> names, final String origin) {
		logger.debug("removeDevices started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalized = validator.validateAndNormalizeRemoveDevices(names, origin);

		try {
			deviceDbService.deleteByNameList(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final LockedException ex) {
			throw new LockedException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	// SERVICE DEFINITIONS

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO getServiceDefinitions(final PageDTO dto, final String origin) {
		logger.debug("getServiceDefinitions started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validator.validateQueryServiceDefinitions(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(dto, Direction.DESC, ServiceDefinition.SORTABLE_FIELDS_BY, ServiceDefinition.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<ServiceDefinition> entities = serviceDefinitionDbService.getPage(pageRequest);

			return dtoConverter.convertServiceDefinitionEntityPageToDTO(entities);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO createServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("createServiceDefinitions started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalized = validator.validateAndNormalizeCreateServiceDefinitions(dto, origin);

		try {
			final List<ServiceDefinition> entities = serviceDefinitionDbService.createBulk(normalized);

			return dtoConverter.convertServiceDefinitionEntityListToDTO(entities);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeServiceDefinitions(final List<String> names, final String origin) {
		logger.debug("removeServiceDefinitions started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalized = validator.validateAndNormalizeRemoveServiceDefinitions(names, origin);

		try {
			serviceDefinitionDbService.removeBulk(normalized);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	// SYSTEMS

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO createSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("createSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<NormalizedSystemRequestDTO> normalized = validator.validateAndNormalizeCreateSystems(dto, origin);

		try {
			final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> entities = systemDbService.createBulk(normalized);

			return dtoConverter.convertSystemTripletListToDTO(entities);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO querySystems(final SystemQueryRequestDTO dto, final boolean verbose, final String origin) {
		logger.debug("querySystems started, verbose = {}", verbose);
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final SystemQueryRequestDTO normalized = validator.validateAndNormalizeQuerySystems(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, System.SORTABLE_FIELDS_BY, System.DEFAULT_SORT_FIELD, origin);

		try {
			final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> page = systemDbService.getPageByFilters(
					pageRequest,
					normalized.systemNames(),
					normalized.addresses(),
					Utilities.isEmpty(normalized.addressType()) ? null : AddressType.valueOf(normalized.addressType()),
					normalized.metadataRequirementList(),
					normalized.versions(),
					normalized.deviceNames());

			final SystemListResponseDTO result = dtoConverter.convertSystemTripletPageToDTO(page);
			if (!verbose) {
				return dtoConverter.convertSystemListResponseDtoToTerse(result);
			}

			return result;
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO updateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("updateSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<NormalizedSystemRequestDTO> normalized = validator.validateAndNormalizeUpdateSystems(dto, origin);

		try {
			final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> updatedEntities = systemDbService.updateBulk(normalized);

			return dtoConverter.convertSystemTripletListToDTO(updatedEntities);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeSystems(final List<String> names, final String origin) {
		logger.debug("removeSystems started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalizedNames = validator.validateAndNormalizeRemoveSystems(names, origin);

		try {
			systemDbService.deleteByNameList(normalizedNames);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	// SERVICE INSTANCES

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceListResponseDTO createServiceInstances(final ServiceInstanceCreateListRequestDTO dto, final String origin) {
		logger.debug("createServiceInstances started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<ServiceInstanceRequestDTO> normalized = validator.validateAndNormalizeCreateServiceInstances(dto, origin);

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> instanceEntries;
		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTriplets;

		try {
			instanceEntries = instanceDbService.createBulk(normalized);
			systemTriplets = systemDbService.getByNameList(
					instanceEntries.stream().map(e -> e.getKey().getSystem().getName()).collect(Collectors.toList()));

			return dtoConverter.convertServiceInstanceListToDTO(instanceEntries, systemTriplets);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceListResponseDTO updateServiceInstances(final ServiceInstanceUpdateListRequestDTO dto, final String origin) {
		logger.debug("updateServiceInstances started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<ServiceInstanceUpdateRequestDTO> normalized = validator.validateAndNormalizeUpdateServiceInstances(dto, origin);

		final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> updatedEntries;
		final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemTriplets;

		try {
			updatedEntries = instanceDbService.updateBulk(normalized);
			systemTriplets = systemDbService.getByNameList(
					updatedEntries.stream().map(e -> e.getKey().getSystem().getName()).collect(Collectors.toList()));

			return dtoConverter.convertServiceInstanceListToDTO(updatedEntries, systemTriplets);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeServiceInstances(final List<String> serviceInstanceIds, final String origin) {
		logger.debug("removeServiceInstances started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalized = validator.validateAndNormalizeRemoveServiceInstances(serviceInstanceIds, origin);

		try {
			instanceDbService.deleteByInstanceIds(normalized);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceListResponseDTO queryServiceInstances(final ServiceInstanceQueryRequestDTO dto, final boolean verbose, final String origin) {
		logger.debug("queryServiceInstances started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final ServiceInstanceQueryRequestDTO normalized = validator.validateAndNormalizeQueryServiceInstances(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(normalized.pagination(), Direction.DESC, ServiceInstance.SORTABLE_FIELDS_BY, ServiceInstance.DEFAULT_SORT_FIELD, origin);

		Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces;
		Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemsWithDevices = null;

		try {
			servicesWithInterfaces = instanceDbService.getPageByFilters(pageRequest, dtoConverter.convertServiceInstanceQueryRequestDtoToFilterModel(normalized));

			if (verbose) {
				systemsWithDevices = systemDbService.getPageByFilters(
						PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, System.DEFAULT_SORT_FIELD),
						List.copyOf(servicesWithInterfaces.stream().map(e -> e.getKey().getSystem().getName()).collect(Collectors.toSet())),
						null, null, null, null, null);
			}

			return dtoConverter.convertServiceInstancePageToDTO(servicesWithInterfaces, systemsWithDevices);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	// INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateListResponseDTO createInterfaceTemplates(final ServiceInterfaceTemplateListRequestDTO dto, final String origin) {
		logger.debug("createInterfaceTemplates started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final ServiceInterfaceTemplateListRequestDTO normalized = validator.validateAndNormalizeCreateInterfaceTemplates(dto, origin);

		try {
			final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> entries = interfaceTemplateDbService.createBulk(normalized.interfaceTemplates());

			return dtoConverter.convertInterfaceTemplateEntriesToDTO(entries.entrySet(), entries.size());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);

		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateListResponseDTO queryInterfaceTemplates(final ServiceInterfaceTemplateQueryRequestDTO dto, final String origin) {
		logger.debug("queryInterfaceTemplates started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final ServiceInterfaceTemplateQueryRequestDTO normalized = validator.validateAndNormalizeQueryInterfaceTemplates(dto, origin);
		final PageRequest pageRequest = pageService.getPageRequest(
				normalized.pagination(),
				Direction.DESC,
				ServiceInterfaceTemplate.SORTABLE_FIELDS_BY,
				ServiceInterfaceTemplate.DEFAULT_SORT_FIELD,
				origin);

		try {
			final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> entries = interfaceTemplateDbService.getPageByFilters(
					pageRequest,
					normalized.templateNames(),
					normalized.protocols());

			return dtoConverter.convertInterfaceTemplateEntriesToDTO(entries.toList(), entries.getTotalElements());
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void removeInterfaceTemplates(final List<String> names, final String origin) {
		logger.debug("removeInterfaceTemplates started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final List<String> normalizedNames = validator.validateAndNormalizeRemoveInterfaceTemplates(names, origin);

		try {
			interfaceTemplateDbService.deleteByTemplateNameList(normalizedNames);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}