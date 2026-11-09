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
package eu.arrowhead.serviceregistry.service.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListResponseDTO;
import eu.arrowhead.dto.DeviceResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionListResponseDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListResponseDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateResponseDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.jpa.entity.SystemAddress;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DeviceListResponseDTO convertDeviceAndDeviceAddressEntriesToDTO(final Iterable<Entry<Device, List<DeviceAddress>>> entries, final long count) {
		logger.debug("convertDeviceAndDeviceAddressEntriesToDTO started...");
		Assert.notNull(entries, "entry list is null");

		final List<DeviceResponseDTO> dtos = new ArrayList<>();
		entries.forEach(entry -> dtos.add(convertDeviceEntityToDeviceResponseDTO(entry.getKey(), entry.getValue())));

		return new DeviceListResponseDTO(dtos, count);
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceResponseDTO convertDeviceEntityToDeviceResponseDTO(final Device deviceEntity, final List<DeviceAddress> addressEntities) {
		logger.debug("convertDeviceAddressEntityListToDTO started...");
		Assert.notNull(deviceEntity, "device entity is null");
		Assert.isTrue(!Utilities.isEmpty(addressEntities), "device address entities is empty");
		Assert.isTrue(!Utilities.containsNull(addressEntities), "device address entities contains null");

		return new DeviceResponseDTO(
				deviceEntity.getName(),
				Utilities.fromJson(deviceEntity.getMetadata(), new TypeReference<Map<String, Object>>()	{
				}),
				addressEntities
						.stream()
						.map(address -> new AddressDTO(address.getAddressType().name(), address.getAddress()))
						.collect(Collectors.toList()),
				Utilities.convertZonedDateTimeToUTCString(deviceEntity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(deviceEntity.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO convertServiceDefinitionEntityListToDTO(final List<ServiceDefinition> entities) {
		logger.debug("convertServiceDefinitionEntityListToDTO started...");
		Assert.notNull(entities, "entity list is null");

		final List<ServiceDefinitionResponseDTO> converted = entities
				.stream()
				.map(e -> convertServiceDefinitionEntityToDTO(e))
				.collect(Collectors.toList());
		return new ServiceDefinitionListResponseDTO(converted, converted.size());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionListResponseDTO convertServiceDefinitionEntityPageToDTO(final Page<ServiceDefinition> entities) {
		logger.debug("convertServiceDefinitionEntityPageToDTO started...");
		Assert.notNull(entities, "entity page is null");

		final List<ServiceDefinitionResponseDTO> converted = entities
				.stream()
				.map(e -> convertServiceDefinitionEntityToDTO(e))
				.collect(Collectors.toList());
		return new ServiceDefinitionListResponseDTO(converted, entities.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceDefinitionResponseDTO convertServiceDefinitionEntityToDTO(final ServiceDefinition entity) {
		logger.debug("convertServiceDefinitionEntityToDTO started...");
		Assert.notNull(entity, "entity is null");
		Assert.isTrue(!Utilities.isEmpty(entity.getName()), "name is empty");

		return new ServiceDefinitionResponseDTO(entity.getName(), Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()), Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO convertSystemTripletPageToDTO(final Page<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> entities) {
		logger.debug("convertSystemTripletPageToDTO started...");
		Assert.notNull(entities, "entity page is null");

		final List<SystemResponseDTO> result = new ArrayList<>(entities.getNumberOfElements());
		for (final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity : entities) {
			result.add(convertSystemTripletToDTO(entity));
		}

		return new SystemListResponseDTO(result, entities.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO convertSystemTripletListToDTO(final List<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> entities) {
		logger.debug("convertSystemTripletListToDTO started...");
		Assert.notNull(entities, "entity list is null");

		final List<SystemResponseDTO> result = new ArrayList<>(entities.size());
		for (final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity : entities) {
			result.add(convertSystemTripletToDTO(entity));
		}

		return new SystemListResponseDTO(result, result.size());
	}

	//-------------------------------------------------------------------------------------------------
	public SystemResponseDTO convertSystemTripletToDTO(final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> entity) {
		logger.debug("convertSystemTripletToDTO started...");
		Assert.notNull(entity, "entity is null");
		Assert.notNull(entity.getLeft(), "the System in the triplet is null");
		Assert.isTrue(!Utilities.isEmpty(entity.getMiddle()), "the system address list in the triplet is empty");
		Assert.isTrue(entity.getRight() == null || !Utilities.isEmpty(entity.getRight().getValue()), "the device data list in the triplet is null or empty");

		final System system = entity.getLeft();
		final List<SystemAddress> systemAddressList = entity.getMiddle();
		final Device device = entity.getRight() == null ? null : entity.getRight().getKey();
		final List<DeviceAddress> deviceAddresses = entity.getRight() == null ? null : entity.getRight().getValue();

		return new SystemResponseDTO(
				system.getName(),
				Utilities.fromJson(system.getMetadata(), new TypeReference<Map<String, Object>>() {
				}),
				system.getVersion(),
				systemAddressList
						.stream()
						.map(a -> new AddressDTO(a.getAddressType().toString(), a.getAddress()))
						.collect(Collectors.toList()),
				device == null ? null
						: new DeviceResponseDTO(
								device.getName(),
								Utilities.fromJson(device.getMetadata(), new TypeReference<Map<String, Object>>() {
								}),
								deviceAddresses.stream().map(a -> new AddressDTO(a.getAddressType().toString(), a.getAddress())).collect(Collectors.toList()),
								Utilities.convertZonedDateTimeToUTCString(device.getCreatedAt()),
								Utilities.convertZonedDateTimeToUTCString(device.getUpdatedAt())),
				Utilities.convertZonedDateTimeToUTCString(system.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(system.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public SystemListResponseDTO convertSystemListResponseDtoToTerse(final SystemListResponseDTO verbose) {
		logger.debug("convertSystemListResponseDtoToTerse started...");
		Assert.notNull(verbose, "response is null");

		final List<SystemResponseDTO> terse = new ArrayList<SystemResponseDTO>(verbose.entries().size());
		for (final SystemResponseDTO systemResponseDTO : verbose.entries()) {
			DeviceResponseDTO device = null;
			if (systemResponseDTO.device() != null) {
				device = new DeviceResponseDTO(systemResponseDTO.device().name(), null, null, null, null);
			}

			terse.add(new SystemResponseDTO(
					systemResponseDTO.name(),
					systemResponseDTO.metadata(),
					systemResponseDTO.version(),
					systemResponseDTO.addresses(),
					device,
					systemResponseDTO.createdAt(),
					systemResponseDTO.updatedAt()));
		}

		return new SystemListResponseDTO(terse, verbose.count());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceResponseDTO convertServiceInstanceEntityToDTO(
			final Entry<ServiceInstance, List<ServiceInstanceInterface>> instanceEntry,
			final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemTriplet) {
		logger.debug("convertServiceInstanceEntityToDTO started...");
		Assert.notNull(instanceEntry, "instance is null");
		Assert.isTrue(!Utilities.isEmpty(instanceEntry.getValue()), "instance interfaces list is empty");
		Assert.isTrue(!Utilities.containsNull(instanceEntry.getValue()), "instance interfaces list contains null element");

		final ServiceInstance instance = instanceEntry.getKey();
		final List<ServiceInstanceInterface> interfaceList = instanceEntry.getValue();

		return new ServiceInstanceResponseDTO(
				instance.getServiceInstanceId(),
				// systemTriplet can be null (when verbose=false)
				systemTriplet != null ? convertSystemTripletToDTO(systemTriplet)
						: new SystemResponseDTO(
								instance.getSystem().getName(),
								Utilities.fromJson(instance.getSystem().getMetadata(), new TypeReference<Map<String, Object>>() {
								}),
								instance.getSystem().getVersion(),
								null,
								null,
								Utilities.convertZonedDateTimeToUTCString(instance.getSystem().getCreatedAt()),
								Utilities.convertZonedDateTimeToUTCString(instance.getSystem().getUpdatedAt())),
				convertServiceDefinitionEntityToDTO(instance.getServiceDefinition()),
				instance.getVersion(),
				Utilities.convertZonedDateTimeToUTCString(instance.getExpiresAt()),
				Utilities.fromJson(instance.getMetadata(), new TypeReference<Map<String, Object>>() {
				}),
				interfaceList.stream().map(interf -> new ServiceInstanceInterfaceResponseDTO(
						interf.getServiceInterfaceTemplate().getName(),
						interf.getServiceInterfaceTemplate().getProtocol(),
						interf.getPolicy().toString(),
						Utilities.fromJson(interf.getProperties(), new TypeReference<Map<String, Object>>() {
						}))).toList(),
				Utilities.convertZonedDateTimeToUTCString(instance.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(instance.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceListResponseDTO convertServiceInstancePageToDTO(
			final Page<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces,
			final Iterable<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemsWithDevices) {
		logger.debug("convertServiceInstancePageToDTO started...");
		Assert.notNull(servicesWithInterfaces, "page is null");

		final List<ServiceInstanceResponseDTO> entries = new ArrayList<>(servicesWithInterfaces.getNumberOfElements());
		for (final Entry<ServiceInstance, List<ServiceInstanceInterface>> serviceEntry : servicesWithInterfaces) {
			Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemDeviceEntry = null;
			if (systemsWithDevices != null) {
				for (final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> triplet : systemsWithDevices) {
					if (triplet.getLeft().getId() == serviceEntry.getKey().getSystem().getId()) {
						systemDeviceEntry = triplet;
						break;
					}
				}
			}
			entries.add(convertServiceInstanceEntityToDTO(serviceEntry, systemDeviceEntry));
		}

		return new ServiceInstanceListResponseDTO(entries, servicesWithInterfaces.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceListResponseDTO convertServiceInstanceListToDTO(
			final Iterable<Entry<ServiceInstance, List<ServiceInstanceInterface>>> servicesWithInterfaces,
			final Iterable<Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>>> systemsWithDevices) {
		logger.debug("convertServiceInstanceListToDTO started...");
		Assert.notNull(servicesWithInterfaces, "collection is null");

		final List<ServiceInstanceResponseDTO> entries = new ArrayList<>();
		for (final Entry<ServiceInstance, List<ServiceInstanceInterface>> serviceEntry : servicesWithInterfaces) {
			Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> systemDeviceEntry = null;
			if (systemsWithDevices != null) {
				for (final Triple<System, List<SystemAddress>, Entry<Device, List<DeviceAddress>>> triplet : systemsWithDevices) {
					if (triplet.getLeft().getId() == serviceEntry.getKey().getSystem().getId()) {
						systemDeviceEntry = triplet;
						break;
					}
				}
			}
			entries.add(convertServiceInstanceEntityToDTO(serviceEntry, systemDeviceEntry));
		}

		return new ServiceInstanceListResponseDTO(entries, entries.size());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateListResponseDTO convertInterfaceTemplateEntriesToDTO(
			final Collection<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> entries,
			final long count) {
		logger.debug("convertInterfaceTemplateEntriesToDTO started...");
		Assert.notNull(entries, "entry collection is null");

		final List<ServiceInterfaceTemplateResponseDTO> dtos = new ArrayList<>(entries.size());
		for (final Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> entry : entries) {
			Assert.isTrue(!Utilities.isEmpty(entry.getValue()), "ServiceInterfaceTemplateProperty list is empty");
			Assert.isTrue(!Utilities.containsNull(entry.getValue()), "ServiceInterfaceTemplateProperty list contains null");
			final ServiceInterfaceTemplate template = entry.getKey();
			dtos.add(new ServiceInterfaceTemplateResponseDTO(
					template.getName(),
					template.getProtocol(),
					entry.getValue().stream()
							.map(prop -> convertServiceInterfaceTemplatePropertytoDTO(prop))
							.toList(),
					Utilities.convertZonedDateTimeToUTCString(template.getCreatedAt()),
					Utilities.convertZonedDateTimeToUTCString(template.getUpdatedAt())));
		}

		return new ServiceInterfaceTemplateListResponseDTO(dtos, count);
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplatePropertyDTO convertServiceInterfaceTemplatePropertytoDTO(final ServiceInterfaceTemplateProperty dto) {
		logger.debug("convertServiceInterfaceTemplatePropertytoDTO started...");
		Assert.notNull(dto, "dto is null");

		String validator = null;
		List<String> validatorParams = null;
		if (!Utilities.isEmpty(dto.getValidator())) {
			final String[] split = dto.getValidator().split("\\" + ServiceRegistryConstants.INTERFACE_PROPERTY_VALIDATOR_DELIMITER);
			validator = split[0];
			validatorParams = split.length <= 1 ? new ArrayList<>() : Arrays.asList(Arrays.copyOfRange(split, 1, split.length));
		}

		return new ServiceInterfaceTemplatePropertyDTO(dto.getPropertyName(), dto.isMandatory(), validator, validatorParams);
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceLookupFilterModel convertServiceInstanceQueryRequestDtoToFilterModel(final ServiceInstanceQueryRequestDTO dto) {
		logger.debug("convertServiceInstanceQueryRequestDtoToFilterModel started...");
		Assert.notNull(dto, "dto is null");

		return new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(
				dto.instanceIds(),
				dto.providerNames(),
				dto.serviceDefinitionNames(),
				dto.versions(),
				dto.alivesAt(),
				dto.metadataRequirementsList(),
				dto.addressTypes(),
				dto.interfaceTemplateNames(),
				dto.interfacePropertyRequirementsList(),
				dto.policies()));
	}
}