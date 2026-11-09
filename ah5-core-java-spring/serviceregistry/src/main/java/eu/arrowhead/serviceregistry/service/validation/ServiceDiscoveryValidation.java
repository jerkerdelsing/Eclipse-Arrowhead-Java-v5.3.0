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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.service.normalization.ServiceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;

@Service
public class ServiceDiscoveryValidation {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDiscoveryNormalization normalizer;

	@Autowired
	private VersionValidator versionValidator;

	@Autowired
	private InterfaceValidator interfaceValidator;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO validateAndNormalizeRegisterService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeRegisterService started");

		validateRegisterService(dto, origin);
		final ServiceInstanceRequestDTO normalizedInstance = normalizer.normalizeServiceInstanceRequestDTO(dto);

		try {
			systemNameValidator.validateSystemName(normalizedInstance.systemName());
			serviceDefNameValidator.validateServiceDefinitionName(normalizedInstance.serviceDefinitionName());

			versionValidator.validateNormalizedVersion(normalizedInstance.version());
			final List<ServiceInstanceInterfaceRequestDTO> normalizedInterfaces = interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(normalizedInstance.interfaces());

			normalizedInterfaces.forEach(i -> {
				if (!ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(i.policy()))) {
					throw new InvalidParameterException("Invalid interface policy: " + i.policy());
				}
			});

			if (!Utilities.isEmpty(normalizedInstance.metadata()) && normalizedInstance.metadata().containsKey(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY)) {
				try {
					final String str = (String) normalizedInstance.metadata().get(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY);
					Integer.parseInt(str);
				} catch (final ClassCastException | NumberFormatException ex) {
					throw new InvalidParameterException(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY + " metadata must have the string representation of an integer value.");
				}
			}

			return new ServiceInstanceRequestDTO(
					normalizedInstance.systemName(),
					normalizedInstance.serviceDefinitionName(),
					normalizedInstance.version(),
					normalizedInstance.expiresAt(),
					normalizedInstance.metadata(),
					normalizedInterfaces);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceLookupRequestDTO validateAndNormalizeLookupService(final ServiceInstanceLookupRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeLookupService started");

		validateLookupService(dto, origin);
		final ServiceInstanceLookupRequestDTO normalized = normalizer.normalizeServiceInstanceLookupRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.instanceIds())) {
				normalized.instanceIds().forEach(i -> serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(i));
			}

			if (!Utilities.isEmpty(normalized.providerNames())) {
				normalized.providerNames().forEach(n -> systemNameValidator.validateSystemName(n));
			}

			if (!Utilities.isEmpty(normalized.serviceDefinitionNames())) {
				normalized.serviceDefinitionNames().forEach(n -> serviceDefNameValidator.validateServiceDefinitionName(n));
			}

			if (!Utilities.isEmpty(normalized.versions())) {
				normalized.versions().forEach(v -> versionValidator.validateNormalizedVersion(v));
			}

			if (!Utilities.isEmpty(normalized.interfaceTemplateNames())) {
				normalized.interfaceTemplateNames().forEach(n -> interfaceTemplateNameValidator.validateInterfaceTemplateName(n));
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
	public Entry<String, String> validateAndNormalizeRevokeService(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateAndNormalizeRevokeService started");

		validateRevokeService(systemName, instanceId, origin);
		final String normalizedSystemName = normalizer.normalizeSystemName(systemName);
		final String normalizedInstanceId = normalizer.normalizeServiceInstanceId(instanceId);

		try {
			systemNameValidator.validateSystemName(normalizedSystemName);
			serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(normalizedInstanceId);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return Map.entry(normalizedSystemName, normalizedInstanceId);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateRegisterService(final ServiceInstanceRequestDTO dto, final String origin) {
		logger.debug("validateRegisterService started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.systemName())) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (Utilities.isEmpty(dto.serviceDefinitionName())) {
			throw new InvalidParameterException("Service definition name is empty", origin);
		}

		if (!Utilities.isEmpty(dto.expiresAt())) {
			ZonedDateTime expiresAt = null;
			try {
				expiresAt = Utilities.parseUTCStringToZonedDateTime(dto.expiresAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Expiration time has an invalid time format", origin);
			}

			if (Utilities.utcNow().isAfter(expiresAt)) {
				throw new InvalidParameterException("Expiration time is in the past", origin);
			}
		}

		if (!Utilities.isEmpty(dto.metadata())) {
			try {
				MetadataValidation.validateMetadataKey(dto.metadata());
			} catch (InvalidParameterException ex) {
				throw new InvalidParameterException(ex.getMessage(), origin);
			}
		}

		if (Utilities.isEmpty(dto.interfaces())) {
			throw new InvalidParameterException("Service interface list is empty", origin);
		}

		for (final ServiceInstanceInterfaceRequestDTO interfaceDTO : dto.interfaces()) {
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
				try {
					MetadataValidation.validateMetadataKey(interfaceDTO.properties());
				} catch (InvalidParameterException ex) {
					throw new InvalidParameterException(ex.getMessage(), origin);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateLookupService(final ServiceInstanceLookupRequestDTO dto, final String origin) {
		logger.debug("validateLookupService started");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(dto.instanceIds()) && Utilities.isEmpty(dto.providerNames()) && Utilities.isEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("One of the following filters must be used: 'instanceIds', 'providerNames', 'serviceDefinitionNames'", origin);
		}

		if (!Utilities.isEmpty(dto.instanceIds()) && Utilities.containsNullOrEmpty(dto.instanceIds())) {
			throw new InvalidParameterException("Instance id list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.providerNames()) && Utilities.containsNullOrEmpty(dto.providerNames())) {
			throw new InvalidParameterException("Provider name list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.serviceDefinitionNames()) && Utilities.containsNullOrEmpty(dto.serviceDefinitionNames())) {
			throw new InvalidParameterException("Service definition name list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.versions()) && Utilities.containsNullOrEmpty(dto.versions())) {
			throw new InvalidParameterException("Version list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.alivesAt())) {
			try {
				Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Alive time has an invalid time format", origin);
			}
		}

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

		if (!Utilities.isEmpty(dto.interfaceTemplateNames()) && Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.interfacePropertyRequirementsList()) && Utilities.containsNull(dto.interfacePropertyRequirementsList())) {
			throw new InvalidParameterException("Interface property requirements list contains null element", origin);
		}

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
	private void validateRevokeService(final String systemName, final String instanceId, final String origin) {
		logger.debug("validateRevokeService started");

		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is empty", origin);
		}

		if (Utilities.isEmpty(instanceId)) {
			throw new InvalidParameterException("Service instance ID is missing", origin);
		}
	}
}