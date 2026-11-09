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
package eu.arrowhead.translationmanager.service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationNegotiationRequestDTO;

@Service
public class TranslationBridgeValidation {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	@Autowired
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefinitionNameNormalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	@Autowired
	private ServiceOperationNameNormalizer operationNormalizer;

	@Autowired
	private ServiceOperationNameValidator operationValidator;

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Autowired
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public String validateAndNormalizeRequester(final String requester, final String origin) {
		logger.debug("validateAndNormalizeRequester started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateRequester(requester, origin);
		final String normalized = systemNameNormalizer.normalize(requester);

		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedTranslationDiscoveryRequestDTO validateAndNormalizeDiscoveryRequest(final String requester, final TranslationDiscoveryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeDiscoveryRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateDiscoveryRequest(dto, origin);
		final NormalizedTranslationDiscoveryRequestDTO normalized = normalizeDiscoveryRequest(requester, dto);
		validateNormalizedDiscoveryRequest(normalized, origin);

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedTranslationNegotiationRequestDTO validateAndNormalizeNegotiationRequest(final TranslationNegotiationRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeNegotiationRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateNegotiationRequest(dto, origin);
		final NormalizedTranslationNegotiationRequestDTO normalized = normalizeNegotiationRequest(dto);
		validateNormalizedNegotiationRequest(normalized, origin);

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public UUID validateAndNormalizeBridgeId(final String bridgeId, final String origin) {
		logger.debug("validateAndNormalizeBridgeId started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(bridgeId)) {
			throw new InvalidParameterException("Bridge identifier is missing", origin);
		}

		try {
			return UUID.fromString(bridgeId.trim());
		} catch (final IllegalArgumentException __) {
			throw new InvalidParameterException("Bridge identifier is invalid: " + bridgeId, origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateRequester(final String requester, final String origin) {
		logger.debug("validateRequester started...");

		if (Utilities.isEmpty(requester)) {
			throw new InvalidParameterException("Requester name is missing or empty", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateDiscoveryRequest(final TranslationDiscoveryRequestDTO dto, final String origin) {
		logger.debug("validateDiscoveryRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request is missing", origin);
		}

		if (Utilities.isEmpty(dto.candidates())) {
			throw new InvalidParameterException("candidates list is missing", origin);
		}

		if (Utilities.containsNull(dto.candidates())) {
			throw new InvalidParameterException("candidates list contains null element", origin);
		}

		dto.candidates().forEach(c -> validateCandidate(c, origin));

		if (Utilities.isEmpty(dto.operation())) {
			throw new InvalidParameterException("operation is missing", origin);
		}

		if (Utilities.isEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template names list is missing", origin);
		}

		if (Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template names list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateCandidate(final ServiceInstanceResponseDTO candidate, final String origin) {
		logger.debug("validateCandidate started...");

		if (Utilities.isEmpty(candidate.instanceId())) {
			throw new InvalidParameterException("Service instance id is missing", origin);
		}

		if (candidate.provider() == null || Utilities.isEmpty(candidate.provider().name())) {
			throw new InvalidParameterException("Provider name is missing", origin);
		}

		if (candidate.serviceDefinition() == null || Utilities.isEmpty(candidate.serviceDefinition().name())) {
			throw new InvalidParameterException("Service definition name is missing", origin);
		}

		if (Utilities.isEmpty(candidate.interfaces())) {
			throw new InvalidParameterException("Interface list is missing", origin);
		}

		if (Utilities.containsNull(candidate.interfaces())) {
			throw new InvalidParameterException("Interface list contains null element", origin);
		}

		candidate.interfaces().forEach(intf -> validateServiceInstanceInterface(intf, origin));
	}

	//-------------------------------------------------------------------------------------------------
	private void validateServiceInstanceInterface(final ServiceInstanceInterfaceResponseDTO intf, final String origin) {
		logger.debug("validateServiceInstanceInterface started...");

		if (Utilities.isEmpty(intf.templateName())) {
			throw new InvalidParameterException("Template name is missing", origin);
		}

		if (Utilities.isEmpty(intf.policy())) {
			throw new InvalidParameterException("Policy is missing", origin);
		}

		if (!Utilities.isEnumValue(intf.policy().trim().toUpperCase(), ServiceInterfacePolicy.class)) {
			throw new InvalidParameterException("Policy is invalid: " + intf.policy(), origin);
		}

		if (Utilities.isEmpty(intf.properties())) {
			throw new InvalidParameterException("Interface properties are missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedDiscoveryRequest(final NormalizedTranslationDiscoveryRequestDTO normalized, final String origin) {
		logger.debug("validateNormalizedDiscoveryRequest started...");

		try {
			// normalized createdBy is already validated
			// discovery assumes that all candidate contains the same service definition
			final String serviceDefinition = normalized.candidates().get(0).serviceDefinition();
			normalized.candidates().forEach(c -> validateNormalizedCandidate(c, serviceDefinition));
			// normalized consumer is already validated
			operationValidator.validateServiceOperationName(normalized.operation());
			normalized.interfaceTemplateNames().forEach(iName -> interfaceTemplateNameValidator.validateInterfaceTemplateName(iName));
			if (normalized.inputDataModelId() != null) {
				dataModelIdentifierValidator.validateDataModelIdentifier(normalized.inputDataModelId());
			}

			if (normalized.outputDataModelId() != null) {
				dataModelIdentifierValidator.validateDataModelIdentifier(normalized.outputDataModelId());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedCandidate(final NormalizedServiceInstanceDTO candidate, final String serviceDefinition) {
		logger.debug("validateNormalizedCandidate started...");

		serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(candidate.instanceId());
		systemNameValidator.validateSystemName(candidate.provider());
		serviceDefinitionNameValidator.validateServiceDefinitionName(candidate.serviceDefinition());
		if (!candidate.serviceDefinition().equals(serviceDefinition)) {
			throw new InvalidParameterException("All candidates must contain the same service definition name");
		}

		candidate.interfaces().forEach(intf -> interfaceTemplateNameValidator.validateInterfaceTemplateName(intf.templateName()));
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNegotiationRequest(final TranslationNegotiationRequestDTO dto, final String origin) {
		logger.debug("validateNegotiationRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request is missing", origin);
		}

		if (dto.target() == null) {
			throw new InvalidParameterException("Target is missing", origin);
		}

		if (!Utilities.isEmpty(dto.bridgeId())) {
			// bridge id is defined => using data from database, only the target's instanceId is needed (besides the bridge id)

			if (!Utilities.isUUID(dto.bridgeId().trim())) {
				throw new InvalidParameterException("Bridge id is invalid: " + dto.bridgeId(), origin);
			}

			if (Utilities.isEmpty(dto.target().instanceId())) {
				throw new InvalidParameterException("Service instance id is missing", origin);
			}
		} else {
			// bridge id is not defined => a discovery step will precede the negotiation step
			final List<ServiceInstanceResponseDTO> candidates = new ArrayList<>(1);
			candidates.add(dto.target());

			final List<String> interfaceTemplateNames = new ArrayList<>(1);
			interfaceTemplateNames.add(dto.interfaceTemplateName());

			final TranslationDiscoveryRequestDTO discoveryRequest = new TranslationDiscoveryRequestDTO(
					candidates,
					dto.operation(),
					interfaceTemplateNames,
					dto.inputDataModelId(),
					dto.outputDataModelId());
			validateDiscoveryRequest(discoveryRequest, origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedNegotiationRequest(final NormalizedTranslationNegotiationRequestDTO normalized, final String origin) {
		logger.debug("validateNormalizedNegotiationRequest started...");

		try {
			if (normalized.bridgeId() == null) {
				validateNormalizedCandidate(normalized.target(), normalized.target().serviceDefinition());
				operationValidator.validateServiceOperationName(normalized.operation());
				interfaceTemplateNameValidator.validateInterfaceTemplateName(normalized.interfaceTemplateName());
				if (normalized.inputDataModelId() != null) {
					dataModelIdentifierValidator.validateDataModelIdentifier(normalized.inputDataModelId());
				}
				if (normalized.outputDataModelId() != null) {
					dataModelIdentifierValidator.validateDataModelIdentifier(normalized.outputDataModelId());
				}
			} else {
				serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(normalized.target().instanceId());
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	private NormalizedTranslationDiscoveryRequestDTO normalizeDiscoveryRequest(final String requester, final TranslationDiscoveryRequestDTO dto) {
		logger.debug("normalizeDiscoveryRequest started...");

		return new NormalizedTranslationDiscoveryRequestDTO(
				requester,
				dto.candidates().stream().map(c -> normalizeCandidate(c)).toList(),
				requester,
				operationNormalizer.normalize(dto.operation()),
				dto.interfaceTemplateNames().stream().map(iName -> interfaceTemplateNameNormalizer.normalize(iName)).toList(),
				dataModelIdentifierNormalizer.normalize(dto.inputDataModelId()),
				dataModelIdentifierNormalizer.normalize(dto.outputDataModelId()));
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedServiceInstanceDTO normalizeCandidate(final ServiceInstanceResponseDTO candidate) {
		logger.debug("normalizeCandidate started...");

		return new NormalizedServiceInstanceDTO(
				serviceInstanceIdentifierNormalizer.normalize(candidate.instanceId()),
				systemNameNormalizer.normalize(candidate.provider().name()),
				serviceDefinitionNameNormalizer.normalize(candidate.serviceDefinition().name()),
				candidate.interfaces().stream().map(intf -> normalizeInterface(intf)).toList());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceInterfaceResponseDTO normalizeInterface(final ServiceInstanceInterfaceResponseDTO intf) {
		logger.debug("normalizeInterface started...");

		return new ServiceInstanceInterfaceResponseDTO(
				interfaceTemplateNameNormalizer.normalize(intf.templateName()),
				Utilities.isEmpty(intf.protocol()) ? "" : intf.protocol().trim().toLowerCase(),
				intf.policy().trim().toUpperCase(),
				intf.properties());
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedTranslationNegotiationRequestDTO normalizeNegotiationRequest(final TranslationNegotiationRequestDTO dto) {
		logger.debug("normalizeNegotiationRequest started...");

		NormalizedServiceInstanceDTO normalizedTarget = null;
		if (Utilities.isEmpty(dto.bridgeId())) {
			normalizedTarget = normalizeCandidate(dto.target());
		} else {
			// only the instance id is necessary

			normalizedTarget = new NormalizedServiceInstanceDTO(
					serviceInstanceIdentifierNormalizer.normalize(dto.target().instanceId()),
					null,
					null,
					null);
		}

		return new NormalizedTranslationNegotiationRequestDTO(
				Utilities.isEmpty(dto.bridgeId()) ? null : UUID.fromString(dto.bridgeId().trim()),
				normalizedTarget,
				operationNormalizer.normalize(dto.operation()),
				interfaceTemplateNameNormalizer.normalize(dto.interfaceTemplateName()),
				dataModelIdentifierNormalizer.normalize(dto.inputDataModelId()),
				dataModelIdentifierNormalizer.normalize(dto.outputDataModelId()));
	}
}