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

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.DeviceNameNormalizer;
import eu.arrowhead.common.service.validation.name.DeviceNameValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceQueryRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceDefinitionListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceCreateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateListRequestDTO;
import eu.arrowhead.dto.ServiceInstanceUpdateRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateListRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateQueryRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.dto.SystemListRequestDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.System;
import eu.arrowhead.serviceregistry.service.dto.NormalizedDeviceRequestDTO;
import eu.arrowhead.serviceregistry.service.dto.NormalizedSystemRequestDTO;
import eu.arrowhead.serviceregistry.service.normalization.ManagementNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;

@Service
public class ManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressTypeValidator;

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private VersionValidator versionValidator;

	@Autowired
	private InterfaceValidator interfaceValidator;

	@Autowired
	private DeviceNameValidator deviceNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private DeviceNameNormalizer deviceNameNormalizer; // for checking duplications

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer; // for checking duplications

	@Autowired
	private SystemNameNormalizer systemNameNormalizer; // for checking duplications

	@Autowired
	private VersionNormalizer versionNormalizer; // for checking duplications

	@Autowired
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer; // for checking duplications

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer; // for checking duplications

	@Autowired
	private ManagementNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// DEVICE VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedDeviceRequestDTO> validateAndNormalizeCreateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateDevices started");

		validateCreateDevices(dto, origin);

		final List<NormalizedDeviceRequestDTO> normalized = normalizer.normalizeDeviceRequestDTOList(dto.devices());

		try {
			normalized.forEach(n -> {
				deviceNameValidator.validateDeviceName(n.name());
				n.addresses().forEach(address -> addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(address.type()), address.address()));
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedDeviceRequestDTO> validateAndNormalizeUpdateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateDevices started");

		validateUpdateDevices(dto, origin);

		final List<NormalizedDeviceRequestDTO> normalized = normalizer.normalizeDeviceRequestDTOList(dto.devices());

		try {
			normalized.forEach(n -> {
				deviceNameValidator.validateDeviceName(n.name());
				n.addresses().forEach(address -> addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(address.type()), address.address()));
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public DeviceQueryRequestDTO validateAndNormalizeQueryDevices(final DeviceQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryDevices started");

		validateQueryDevices(dto, origin);
		final DeviceQueryRequestDTO normalized = normalizer.normalizeDeviceQueryRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.deviceNames())) {
				normalized.deviceNames().forEach(n -> deviceNameValidator.validateDeviceName(n));
			}

			if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
				normalized.addresses().forEach(a -> {
					final AddressDTO aDto = new AddressDTO(normalized.addressType(), a);
					addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(aDto.type()), aDto.address());
				});
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveDevices(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeRemoveDevices started");

		validateRemoveDevices(names, origin);
		final List<String> normalized = normalizer.normalizeDeviceNames(names);

		try {
			normalized.forEach(n -> deviceNameValidator.validateDeviceName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	// SERVICE DEFINITION VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeCreateServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateServiceDefinitions started");

		validateCreateServiceDefinitions(dto, origin);

		final List<String> normalized = normalizer.normalizeCreateServiceDefinitions(dto);

		try {
			normalized.forEach(n -> serviceDefNameValidator.validateServiceDefinitionName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public void validateQueryServiceDefinitions(final PageDTO dto, final String origin) {
		logger.debug("validateQueryServiceDefinitions started");

		if (dto != null) {
			pageValidator.validatePageParameter(dto, ServiceDefinition.SORTABLE_FIELDS_BY, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveServiceDefinitions(final List<String> names, final String origin) {
		logger.debug("validateAndNormalizeRemoveServiceDefinitions started");

		validateRemoveServiceDefinitions(names, origin);
		final List<String> normalized = normalizer.normalizeRemoveServiceDefinitions(names);
		try {
			normalized.forEach(n -> serviceDefNameValidator.validateServiceDefinitionName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	// SYSTEM VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedSystemRequestDTO> validateAndNormalizeCreateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateSystems started");

		validateCreateSystems(dto, origin);

		final List<NormalizedSystemRequestDTO> normalized = normalizer.normalizeSystemRequestDTOs(dto);

		try {
			normalized.forEach(n -> {
				systemNameValidator.validateSystemName(n.name());
				versionValidator.validateNormalizedVersion(n.version());
				n.addresses().forEach(a -> addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(a.type()), a.address()));
				if (!Utilities.isEmpty(n.deviceName())) {
					deviceNameValidator.validateDeviceName(n.deviceName());
				}
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<NormalizedSystemRequestDTO> validateAndNormalizeUpdateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateSystems started");

		return validateAndNormalizeCreateSystems(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public SystemQueryRequestDTO validateAndNormalizeQuerySystems(final SystemQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQuerySystems started");

		validateQuerySystems(dto, origin);
		final SystemQueryRequestDTO normalized = normalizer.normalizeSystemQueryRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.systemNames())) {
				normalized.systemNames().forEach(n -> systemNameValidator.validateSystemName(n));
			}

			if (!Utilities.isEmpty(normalized.addressType()) && !Utilities.isEmpty(normalized.addresses())) {
				normalized.addresses().forEach(na -> {
					final AddressDTO aDto = new AddressDTO(normalized.addressType(), na);
					addressTypeValidator.validateNormalizedAddress(AddressType.valueOf(aDto.type()), aDto.address());
				});
			}

			if (!Utilities.isEmpty(normalized.versions())) {
				normalized.versions().forEach(nv -> versionValidator.validateNormalizedVersion(nv));
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
	public List<String> validateAndNormalizeRemoveSystems(final List<String> originalNames, final String origin) {
		logger.debug("validateAndNormalizeRemoveSystems started");

		validateRemoveSystems(originalNames, origin);
		final List<String> normalized = normalizer.normalizeRemoveSystemNames(originalNames);

		try {
			normalized.forEach(n -> systemNameValidator.validateSystemName(n));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	// SERVICE INSTANCE VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceRequestDTO> validateAndNormalizeCreateServiceInstances(final ServiceInstanceCreateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateServiceInstances started");

		validateCreateServiceInstances(dto, origin);
		final List<ServiceInstanceRequestDTO> normalized = normalizer.normalizeCreateServiceInstances(dto);

		try {
			normalized.forEach(n -> {
				systemNameValidator.validateSystemName(n.systemName());
				serviceDefNameValidator.validateServiceDefinitionName(n.serviceDefinitionName());
				versionValidator.validateNormalizedVersion(n.version());
				interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(n.interfaces());
				n.interfaces().forEach(i -> {
					if (!ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(i.policy()))) {
						throw new InvalidParameterException("Invalid interface policy: " + i.policy());
					}
				});
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceUpdateRequestDTO> validateAndNormalizeUpdateServiceInstances(final ServiceInstanceUpdateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeUpdateServiceInstances started");

		validateUpdateServiceInstances(dto, origin);
		final List<ServiceInstanceUpdateRequestDTO> normalized = normalizer.normalizeUpdateServiceInstances(dto);

		try {
			normalized.forEach(n -> {
				serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(n.instanceId());
				interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(n.interfaces());
				n.interfaces().forEach(i -> {
					if (!ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(i.policy()))) {
						throw new InvalidParameterException("Invalid interface policy: " + i.policy());
					}
				});
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveServiceInstances(final List<String> instanceIds, final String origin) {
		logger.debug("validateAndNormalizeRevokeServiceInstances started");

		validateRemoveServiceInstances(instanceIds, origin);
		final List<String> normalized = normalizer.normalizeRemoveServiceInstances(instanceIds);

		try {
			normalized.forEach(i -> serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(i));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceQueryRequestDTO validateAndNormalizeQueryServiceInstances(final ServiceInstanceQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryServiceInstances");

		validateQueryServiceInstances(dto, origin);
		final ServiceInstanceQueryRequestDTO normalized = normalizer.normalizeQueryServiceInstances(dto);

		try {
			if (!Utilities.isEmpty(normalized.instanceIds())) {
				normalized.instanceIds().forEach(i -> serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(i));
			}

			if (!Utilities.isEmpty(normalized.providerNames())) {
				normalized.providerNames().forEach(n -> systemNameValidator.validateSystemName(n));
			}

			if (!Utilities.isEmpty(normalized.serviceDefinitionNames())) {
				normalized.serviceDefinitionNames().forEach(n -> serviceDefNameNormalizer.normalize(n));
			}

			if (!Utilities.isEmpty(normalized.versions())) {
				normalized.versions().forEach(v -> versionValidator.validateNormalizedVersion(v));
			}

			if (!Utilities.isEmpty(normalized.interfaceTemplateNames())) {
				normalized.interfaceTemplateNames().forEach(i -> interfaceTemplateNameValidator.validateInterfaceTemplateName(i));
			}

			if (!Utilities.isEmpty(normalized.policies())) {
				normalized.policies().forEach(p -> {
					if (!ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(p))) {
						throw new InvalidParameterException("Invalid interface policy: " + p);
					}
				});
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	// INTERFACE VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateListRequestDTO validateAndNormalizeCreateInterfaceTemplates(final ServiceInterfaceTemplateListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateInterfaceTemplates started");

		validateCreateInterfaceTemplates(dto, origin);
		final ServiceInterfaceTemplateListRequestDTO normalized = normalizer.normalizeServiceInterfaceTemplateListRequestDTO(dto);

		try {
			interfaceValidator.validateNormalizedInterfaceTemplates(normalized.interfaceTemplates());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateQueryRequestDTO validateAndNormalizeQueryInterfaceTemplates(final ServiceInterfaceTemplateQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryInterfaceTemplates started");

		validateQueryInterfaceTemplates(dto, origin);
		final ServiceInterfaceTemplateQueryRequestDTO normalized = normalizer.normalizeServiceInterfaceTemplateQueryRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.templateNames())) {
				normalized.templateNames().forEach(i -> interfaceTemplateNameValidator.validateInterfaceTemplateName(i));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> validateAndNormalizeRemoveInterfaceTemplates(final List<String> originalNames, final String origin) {
		logger.debug("validateAndNormalizeRemoveInterfaceTemplates started");

		validateRemoveInterfaceTemplates(originalNames, origin);
		final List<String> normalized = normalizer.normalizeRemoveInterfaceTemplates(originalNames);

		try {
			normalized.forEach(i -> interfaceTemplateNameValidator.validateInterfaceTemplateName(i));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// DEVICE VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateCreateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateCreateDevice started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.devices())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> names = new HashSet<>();
		for (final DeviceRequestDTO device : dto.devices()) {
			if (device == null) {
				throw new InvalidParameterException("Device list contains null element", origin);
			}

			if (Utilities.isEmpty(device.name())) {
				throw new InvalidParameterException("Device name is empty", origin);
			}

			final String normalized = deviceNameNormalizer.normalize(device.name());
			if (names.contains(normalized)) {
				throw new InvalidParameterException("Duplicated device name: " + normalized, origin);
			}

			names.add(normalized);

			if (Utilities.isEmpty(device.addresses())) {
				throw new InvalidParameterException("At least one device address is needed for every device", origin);
			}

			for (final String address : device.addresses()) {
				if (Utilities.isEmpty(address)) {
					throw new InvalidParameterException("Address is missing", origin);
				}
			}

			if (!Utilities.isEmpty(device.metadata())) {
				try {
					MetadataValidation.validateMetadataKey(device.metadata());
				} catch (InvalidParameterException ex) {
					throw new InvalidParameterException(ex.getMessage(), origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateUpdateDevices(final DeviceListRequestDTO dto, final String origin) {
		logger.debug("validateUpdateDevice started");

		validateCreateDevices(dto, origin);
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryDevices(final DeviceQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryDevices started");

		if (dto != null) {
			pageValidator.validatePageParameter(dto.pagination(), Device.SORTABLE_FIELDS_BY, origin);

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
	private void validateRemoveDevices(final List<String> names, final String origin) {
		logger.debug("validateRemoveDevices started");

		if (Utilities.isEmpty(names)) {
			throw new InvalidParameterException("Device name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(names)) {
			throw new InvalidParameterException("Device name list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// SERVICE DEFINITION VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateCreateServiceDefinitions(final ServiceDefinitionListRequestDTO dto, final String origin) {
		logger.debug("validateCreateServiceDefinition started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list is empty", origin);
		}

		if (Utilities.containsNullOrEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}

		final List<String> names = new ArrayList<>(dto.serviceDefinitionNames().size());

		for (final String name : dto.serviceDefinitionNames()) {
			final String normalized = serviceDefNameNormalizer.normalize(name);
			if (names.contains(normalized)) {
				throw new InvalidParameterException("Duplicated service defitition name: " + name, origin);
			}

			names.add(normalized);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRemoveServiceDefinitions(final List<String> names, final String origin) {
		logger.debug("validateRemoveServiceDefinitions started");

		if (Utilities.isEmpty(names)) {
			throw new InvalidParameterException("Service definition name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(names)) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// SYSTEM VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateCreateSystems(final SystemListRequestDTO dto, final String origin) {
		logger.debug("validateCreateSystems started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systems())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> names = new HashSet<>();

		for (final SystemRequestDTO system : dto.systems()) {
			if (system == null) {
				throw new InvalidParameterException("System list contains null element", origin);
			}

			if (Utilities.isEmpty(system.name())) {
				throw new InvalidParameterException("System name is empty", origin);
			}

			final String normalized = systemNameNormalizer.normalize(system.name());
			if (names.contains(normalized)) {
				throw new InvalidParameterException("Duplicated system name: " + normalized, origin);
			}

			names.add(normalized);

			if (!Utilities.isEmpty(system.addresses())) {
				for (final String address : system.addresses()) {
					if (Utilities.isEmpty(address)) {
						throw new InvalidParameterException("Address value is missing", origin);
					}
				}
			}

			if (Utilities.isEmpty(system.addresses()) && Utilities.isEmpty(system.deviceName())) {
				throw new InvalidParameterException("At least one system address is needed for every system", origin);
			}

			if (!Utilities.isEmpty(system.metadata())) {
				try {
					MetadataValidation.validateMetadataKey(system.metadata());
				} catch (final InvalidParameterException ex) {
					throw new InvalidParameterException(ex.getMessage(), origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQuerySystems(final SystemQueryRequestDTO dto, final String origin) {
		logger.debug("validateQuerySystems started");

		if (dto != null) {
			pageValidator.validatePageParameter(dto.pagination(), System.SORTABLE_FIELDS_BY, origin);

			if (!Utilities.isEmpty(dto.systemNames()) && Utilities.containsNullOrEmpty(dto.systemNames())) {
				throw new InvalidParameterException("System name list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addresses()) && Utilities.containsNullOrEmpty(dto.addresses())) {
				throw new InvalidParameterException("Address list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.addressType()) && !Utilities.isEnumValue(dto.addressType(), AddressType.class)) {
				throw new InvalidParameterException("Invalid address type: " + dto.addressType(), origin);
			}

			if (!Utilities.isEmpty(dto.metadataRequirementList()) && Utilities.containsNull(dto.metadataRequirementList())) {
				throw new InvalidParameterException("Metadata requirement list contains null element", origin);
			}

			if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNullOrEmpty(dto.versions())) {
				throw new InvalidParameterException("Version list contains null or empty element", origin);
			}

			if (!Utilities.isEmpty(dto.deviceNames()) && Utilities.containsNullOrEmpty(dto.deviceNames())) {
				throw new InvalidParameterException("Device name list contains null or empty element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRemoveSystems(final List<String> originalNames, final String origin) {
		logger.debug("validateRemoveSystems started");

		if (Utilities.isEmpty(originalNames)) {
			throw new InvalidParameterException("System name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(originalNames)) {
			throw new InvalidParameterException("System name list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// SERVICE INSTANCE VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateCreateServiceInstances(final ServiceInstanceCreateListRequestDTO dto, final String origin) {
		logger.debug("validateCreateServiceInstances started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.instances())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> instanceIds = new HashSet<>();

		for (final ServiceInstanceRequestDTO instance : dto.instances()) {
			// system name
			if (Utilities.isEmpty(instance.systemName())) {
				throw new InvalidParameterException("System name is empty", origin);
			}

			// service definition name
			if (Utilities.isEmpty(instance.serviceDefinitionName())) {
				throw new InvalidParameterException("Service definition name is empty", origin);
			}

			// version -> can be empty (default will be set at normalization)

			// check for duplication
			final String instanceId = ServiceInstanceIdUtils.calculateInstanceId(
					systemNameNormalizer.normalize(instance.systemName()),
					serviceDefNameNormalizer.normalize(instance.serviceDefinitionName()),
					versionNormalizer.normalize(instance.version()));
			if (instanceIds.contains(instanceId)) {
				throw new InvalidParameterException("Duplicated instance: " + instanceId, origin);
			}
			instanceIds.add(instanceId);

			// expires at
			if (!Utilities.isEmpty(instance.expiresAt())) {
				ZonedDateTime expiresAt = null;
				try {
					expiresAt = Utilities.parseUTCStringToZonedDateTime(instance.expiresAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", origin);
				}

				if (Utilities.utcNow().isAfter(expiresAt)) {
					throw new InvalidParameterException("Expiration time is in the past", origin);
				}
			}

			// metadata
			if (!Utilities.isEmpty(instance.metadata())) {
				MetadataValidation.validateMetadataKey(instance.metadata());
			}

			// interfaces
			if (Utilities.isEmpty(instance.interfaces())) {
				throw new InvalidParameterException("Service interface list is empty", origin);
			}

			for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : instance.interfaces()) {
				if (Utilities.isEmpty(interfaceDTO.templateName())) {
					throw new InvalidParameterException("Interface template name is missing", origin);
				}

				if (Utilities.isEmpty(interfaceDTO.policy())) {
					throw new InvalidParameterException("Interface policy is missing", origin);
				}

				if (!Utilities.isEnumValue(interfaceDTO.policy().toUpperCase(), ServiceInterfacePolicy.class)) {
					throw new InvalidParameterException("Invalid interface policy", origin);
				}

				if (Utilities.isEmpty(interfaceDTO.properties())) {
					throw new InvalidParameterException("Interface properties are missing", origin);
				} else {
					MetadataValidation.validateMetadataKey(interfaceDTO.properties());
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateUpdateServiceInstances(final ServiceInstanceUpdateListRequestDTO dto, final String origin) {
		logger.debug("ServiceInstanceUpdateListRequestDTO started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.instances())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> instanceIds = new HashSet<String>();
		for (final ServiceInstanceUpdateRequestDTO instance : dto.instances()) {
			// instance id
			if (Utilities.isEmpty(instance.instanceId())) {
				throw new InvalidParameterException("Instance id is empty", origin);
			}

			final String normalized = serviceInstanceIdentifierNormalizer.normalize(instance.instanceId());
			if (instanceIds.contains(normalized)) {
				throw new InvalidParameterException("Duplicated instance id: " + instance.instanceId(), origin);
			}

			instanceIds.add(normalized);

			// expires at
			if (!Utilities.isEmpty(instance.expiresAt())) {
				ZonedDateTime expiresAt = null;
				try {
					expiresAt = Utilities.parseUTCStringToZonedDateTime(instance.expiresAt());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Expiration time has an invalid time format, UTC string expected (example: 2024-10-11T14:30:00Z)", origin);
				}

				if (Utilities.utcNow().isAfter(expiresAt)) {
					throw new InvalidParameterException("Expiration time is in the past", origin);
				}
			}

			// metadata
			if (!Utilities.isEmpty(instance.metadata())) {
				MetadataValidation.validateMetadataKey(instance.metadata());
			}

			// interfaces
			if (Utilities.isEmpty(instance.interfaces())) {
				throw new InvalidParameterException("Service interface list is empty", origin);
			}

			for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : instance.interfaces()) {
				if (Utilities.isEmpty(interfaceDTO.templateName())) {
					throw new InvalidParameterException("Interface template name is missing", origin);
				}

				if (Utilities.isEmpty(interfaceDTO.policy())) {
					throw new InvalidParameterException("Interface policy is missing", origin);
				}

				if (!Utilities.isEnumValue(interfaceDTO.policy().toUpperCase(), ServiceInterfacePolicy.class)) {
					throw new InvalidParameterException("Invalid interface policy", origin);
				}

				if (Utilities.isEmpty(interfaceDTO.properties())) {
					throw new InvalidParameterException("Interface properties are missing", origin);
				} else {
					MetadataValidation.validateMetadataKey(interfaceDTO.properties());
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRemoveServiceInstances(final List<String> instanceIds, final String origin) {
		logger.debug("validateRemoveServiceInstances started");

		if (Utilities.isEmpty(instanceIds)) {
			throw new InvalidParameterException("Instance id list is empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryServiceInstances(final ServiceInstanceQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryServiceInstances started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		// pagination
		pageValidator.validatePageParameter(dto.pagination(), ServiceInstance.SORTABLE_FIELDS_BY, origin);

		// check if instanceIds, providerNames and serviceDefinitionNames are all empty
		if (Utilities.isEmpty(dto.instanceIds()) && Utilities.isEmpty(dto.providerNames()) && Utilities.isEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("One of the following filters must be used: 'instanceIds', 'providerNames', 'serviceDefinitionNames'", origin);
		}

		// instanceIds
		if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}

		// providerNames
		if (!Utilities.isEmpty(dto.providerNames()) && Utilities.containsNullOrEmpty(dto.providerNames())) {
			throw new InvalidParameterException("Provider name list contains null or empty element", origin);
		}

		// serviceDefinitionNames
		if (!Utilities.isEmpty(dto.serviceDefinitionNames()) && Utilities.containsNullOrEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}

		// versions
		if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNullOrEmpty(dto.versions())) {
			throw new InvalidParameterException("Version list contains null or empty element", origin);
		}

		// alivesAt
		if (!Utilities.isEmpty(dto.alivesAt())) {
			try {
				Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Alive time has an invalid time format", origin);
			}
		}

		// metadataRequirementsList
		if (!Utilities.isEmpty(dto.metadataRequirementsList()) && Utilities.containsNull(dto.metadataRequirementsList())) {
			throw new InvalidParameterException("Metadata requirements list contains null element", origin);
		}

		if (!Utilities.isEmpty(dto.addressTypes())) {
			for (final String type : dto.addressTypes()) {
				if (Utilities.isEmpty(type)) {
					throw new InvalidParameterException("Address type list contains null or empty element", origin);
				}

				if (!Utilities.isEnumValue(type.toUpperCase(), AddressType.class)) {
					throw new InvalidParameterException("Address type list contains invalid element: " + type, origin);
				}
			}
		}

		// interfaceTemplateNames
		if (!Utilities.isEmpty(dto.interfaceTemplateNames())
				&& Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template list contains null or empty element", origin);
		}

		// interfacePropertyRequirementsList
		if (!Utilities.isEmpty(dto.interfacePropertyRequirementsList())
				&& Utilities.containsNull(dto.interfacePropertyRequirementsList())) {
			throw new InvalidParameterException("Interface property requirements list contains null element", origin);
		}

		// policies
		if (!Utilities.isEmpty(dto.policies())) {
			for (final String policy : dto.policies()) {
				if (Utilities.isEmpty(policy)) {
					throw new InvalidParameterException("Policy list contains null or empty element", origin);
				}

				if (!Utilities.isEnumValue(policy.toUpperCase(), ServiceInterfacePolicy.class)) {
					throw new InvalidParameterException("Policy list contains invalid element: " + policy, origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	// INTERFACE VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateCreateInterfaceTemplates(final ServiceInterfaceTemplateListRequestDTO dto, final String origin) {
		logger.debug("validateCreateInterfaceTemplates started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.interfaceTemplates())) {
			throw new InvalidParameterException("Request payload is empty", origin);
		}

		final Set<String> templateNames = new HashSet<>();
		for (final ServiceInterfaceTemplateRequestDTO templateDTO : dto.interfaceTemplates()) {
			if (templateDTO == null) {
				throw new InvalidParameterException("Interface template list contains null element", origin);
			}

			if (Utilities.isEmpty(templateDTO.name())) {
				throw new InvalidParameterException("Interface template name is empty", origin);
			}

			final String normalized = interfaceTemplateNameNormalizer.normalize(templateDTO.name());
			if (templateNames.contains(normalized)) {
				throw new InvalidParameterException("Duplicated interface template name: " + templateDTO.name(), origin);
			}
			templateNames.add(normalized);

			if (Utilities.isEmpty(templateDTO.protocol())) {
				throw new InvalidParameterException("Interface template protocol is empty", origin);
			}

			if (!Utilities.isEmpty(templateDTO.propertyRequirements())) {
				final Set<String> propertyNames = new HashSet<>();
				for (final ServiceInterfaceTemplatePropertyDTO propertyDTO : templateDTO.propertyRequirements()) {
					if (propertyDTO == null) {
						throw new InvalidParameterException("Interface template contains null property", origin);
					}

					if (Utilities.isEmpty(propertyDTO.name())) {
						throw new InvalidParameterException("Interface template property name is empty", origin);
					}

					if (propertyDTO.name().contains(MetadataValidation.METADATA_COMPOSITE_KEY_DELIMITER)) {
						throw new InvalidParameterException("Invalid interface template property name: " + propertyDTO.name() + ", it should not contain "
								+ MetadataValidation.METADATA_COMPOSITE_KEY_DELIMITER + " character", origin);
					}

					if (propertyNames.contains(propertyDTO.name().trim())) {
						throw new InvalidParameterException("Duplicated interface template property name: " + templateDTO.name() + Constants.COMPOSITE_ID_DELIMITER + propertyDTO.name(), origin);
					}
					propertyNames.add(propertyDTO.name().trim());

					if (!Utilities.isEmpty(propertyDTO.validatorParams())) {
						if (Utilities.isEmpty(propertyDTO.validator())) {
							throw new InvalidParameterException("Interface template property validator is empty while validator params are defined", origin);
						}

						if (Utilities.containsNullOrEmpty(propertyDTO.validatorParams())) {
							throw new InvalidParameterException("Interface template property validator parameter list contains null or empty element", origin);
						}
					}
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryInterfaceTemplates(final ServiceInterfaceTemplateQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryInterfaceTemplates started");

		if (dto != null) {
			pageValidator.validatePageParameter(dto.pagination(), ServiceInterfaceTemplate.SORTABLE_FIELDS_BY, origin);

			if (!Utilities.isEmpty(dto.templateNames()) && Utilities.containsNullOrEmpty(dto.templateNames())) {
				throw new InvalidParameterException("Interface template name list contains empty element", origin);
			}

			if (!Utilities.isEmpty(dto.protocols()) && Utilities.containsNullOrEmpty(dto.protocols())) {
				throw new InvalidParameterException("Interface template protocol list contains empty element", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRemoveInterfaceTemplates(final List<String> originalNames, final String origin) {
		logger.debug("validateRemoveInterfaceTemplate started");

		if (Utilities.isEmpty(originalNames)) {
			throw new InvalidParameterException("Interface template name list is missing or empty", origin);
		}

		if (Utilities.containsNullOrEmpty(originalNames)) {
			throw new InvalidParameterException("Interface template name list contains null or empty element", origin);
		}
	}
}