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

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.PageValidator;
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
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationQueryRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationQueryRequestDTO;

@Service
public class TranslationBridgeMgmtValidation {

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

	@Autowired
	private PageValidator pageValidator;

	@Autowired
	private PageService pageService;

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
	public Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> validateAndNormalizeDiscoveryMgmtRequest(
			final String requester,
			final TranslationDiscoveryMgmtRequestDTO dto,
			final String origin) {
		logger.debug("validateAndNormalizeDiscoveryMgmtRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateDiscoveryMgmtRequest(dto, origin);
		final Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> normalized = normalizeDiscoveryMgmtRequest(requester, dto);
		validateNormalizedDiscoveryMgmtRequest(normalized.getFirst(), origin);

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<UUID, String> validateAndNormalizeNegotiationMgmtRequest(final TranslationNegotiationMgmtRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeNegotiationMgmtRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateNegotiationMgmtRequest(dto, origin);
		final Pair<UUID, String> normalized = normalizeNegotiationMgmtRequest(dto);

		try {
			serviceInstanceIdentifierValidator.validateServiceInstanceIdentifier(normalized.getSecond());
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public List<UUID> validateAndNormalizeAbortMgmtRequest(final List<String> ids, final String origin) {
		logger.debug("validateAndNormalizeAbortMgmtRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateAbortMgmtRequest(ids, origin);

		return ids
				.stream()
				.map(id -> {
					try {
						return UUID.fromString(id.trim());
					} catch (final IllegalArgumentException __) {
						throw new InvalidParameterException("Bridge identifier is invalid: " + id, origin);
					}
				}).toList();
	}

	//-------------------------------------------------------------------------------------------------
	public NormalizedTranslationQueryRequestDTO validateAndNormalizeQueryMgmtRequest(final TranslationQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryMgmtRequest started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		validateQueryMgmtRequest(dto, origin);
		final NormalizedTranslationQueryRequestDTO normalized = normalizeQueryMgmtRequest(dto);
		validateNormalizedQueryMgmtRequest(normalized, origin);

		return normalized;
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
	private void validateDiscoveryMgmtRequest(final TranslationDiscoveryMgmtRequestDTO dto, final String origin) {
		logger.debug("validateDiscoveryMgmtRequest started...");

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

		if (Utilities.isEmpty(dto.consumer())) {
			throw new InvalidParameterException("consumer is missing", origin);
		}

		if (Utilities.isEmpty(dto.operation())) {
			throw new InvalidParameterException("operation is missing", origin);
		}

		if (Utilities.isEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template names list is missing", origin);
		}

		if (Utilities.containsNullOrEmpty(dto.interfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template names list contains null or empty element", origin);
		}

		if (!Utilities.isEmpty(dto.flags())) {
			dto.flags()
					.keySet()
					.forEach(k -> {
						if (Utilities.isEmpty(k)) {
							throw new InvalidParameterException("Flag name is missing", origin);
						}

						if (!Utilities.isEnumValue(k.trim().toUpperCase(), TranslationDiscoveryFlag.class)) {
							throw new InvalidParameterException("Flag is invalid: " + k, origin);
						}
					});
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
			throw new InvalidParameterException("interfaces list is missing", origin);
		}

		if (Utilities.containsNull(candidate.interfaces())) {
			throw new InvalidParameterException("interfaces list contains null element", origin);
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
	private void validateNormalizedDiscoveryMgmtRequest(final NormalizedTranslationDiscoveryRequestDTO normalized, final String origin) {
		logger.debug("validateNormalizedDiscoveryMgmtRequest started...");

		try {
			// normalized createdBy is already validated
			// discovery assumes that all candidate contains the same service definition
			final String serviceDefinition = normalized.candidates().get(0).serviceDefinition();
			normalized.candidates().forEach(c -> validateNormalizedCandidate(c, serviceDefinition));
			systemNameValidator.validateSystemName(normalized.consumer());
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
	private void validateNegotiationMgmtRequest(final TranslationNegotiationMgmtRequestDTO dto, final String origin) {
		logger.debug("validateNegotiationMgmtRequest started...");

		if (dto == null) {
			throw new InvalidParameterException("Request is missing", origin);
		}

		if (Utilities.isEmpty(dto.bridgeId())) {
			throw new InvalidParameterException("Bridge id is missing", origin);
		}

		if (!Utilities.isUUID(dto.bridgeId().trim())) {
			throw new InvalidParameterException("Bridge id is invalid: " + dto.bridgeId(), origin);
		}

		if (Utilities.isEmpty(dto.targetInstanceId())) {
			throw new InvalidParameterException("Target service instance id is missing", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateAbortMgmtRequest(final List<String> ids, final String origin) {
		logger.debug("validateAbortMgmtRequest started...");

		if (Utilities.isEmpty(ids)) {
			throw new InvalidParameterException("Bridge id list is missing", origin);
		}

		if (Utilities.containsNullOrEmpty(ids)) {
			throw new InvalidParameterException("Bridge id list contains null or empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryMgmtRequest(final TranslationQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryMgmtRequest started...");

		if (dto != null) {
			// pagination
			pageValidator.validatePageParameter(dto.pagination(), BridgeDetails.ACCEPTABLE_SORT_FIELDS, origin);

			if (!Utilities.isEmpty(dto.bridgeIds())) {
				if (Utilities.containsNullOrEmpty(dto.bridgeIds())) {
					throw new InvalidParameterException("Bridge id list contains null or empty element", origin);
				}

				dto.bridgeIds()
						.forEach(id -> {
							if (!Utilities.isUUID(id.trim())) {
								throw new InvalidParameterException("Bridge id is invalid: " + id, origin);
							}
						});
			}

			if (!Utilities.isEmpty(dto.creators())) {
				if (Utilities.containsNullOrEmpty(dto.creators())) {
					throw new InvalidParameterException("Creator list contains null or empty element", origin);
				}
			}

			if (!Utilities.isEmpty(dto.statuses())) {
				if (Utilities.containsNullOrEmpty(dto.statuses())) {
					throw new InvalidParameterException("Status list contains null or empty element", origin);
				}

				dto.statuses()
						.forEach(status -> {
							if (!Utilities.isEnumValue(status.trim().toUpperCase(), TranslationBridgeStatus.class)) {
								throw new InvalidParameterException("Invalid status: " + status, origin);
							}
						});
			}

			if (!Utilities.isEmpty(dto.consumers())) {
				if (Utilities.containsNullOrEmpty(dto.consumers())) {
					throw new InvalidParameterException("Consumer list contains null or empty element", origin);
				}
			}

			if (!Utilities.isEmpty(dto.providers())) {
				if (Utilities.containsNullOrEmpty(dto.providers())) {
					throw new InvalidParameterException("Provider list contains null or empty element", origin);
				}
			}

			if (!Utilities.isEmpty(dto.serviceDefinitions())) {
				if (Utilities.containsNullOrEmpty(dto.serviceDefinitions())) {
					throw new InvalidParameterException("Service definition list contains null or empty element", origin);
				}
			}

			if (!Utilities.isEmpty(dto.interfaceTranslators())) {
				if (Utilities.containsNullOrEmpty(dto.interfaceTranslators())) {
					throw new InvalidParameterException("Interface translator list contains null or empty element", origin);
				}
			}

			if (!Utilities.isEmpty(dto.dataModelTranslators())) {
				if (Utilities.containsNullOrEmpty(dto.dataModelTranslators())) {
					throw new InvalidParameterException("Data model translator list contains null or empty element", origin);
				}
			}

			ZonedDateTime creationFrom = null;
			if (!Utilities.isEmpty(dto.creationFrom())) {
				try {
					creationFrom = Utilities.parseUTCStringToZonedDateTime(dto.creationFrom());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Minimum creation time has an invalid time format", origin);
				}
			}

			ZonedDateTime creationTo = null;
			if (!Utilities.isEmpty(dto.creationTo())) {
				try {
					creationTo = Utilities.parseUTCStringToZonedDateTime(dto.creationTo());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Maximum creation time has an invalid time format", origin);
				}
			}

			if (creationFrom != null && creationTo != null && creationTo.isBefore(creationFrom)) {
				throw new InvalidParameterException("Empty creation time interval", origin);
			}

			ZonedDateTime alivesFrom = null;
			if (!Utilities.isEmpty(dto.alivesFrom())) {
				try {
					alivesFrom = Utilities.parseUTCStringToZonedDateTime(dto.alivesFrom());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Minimum alive time has an invalid time format", origin);
				}
			}

			ZonedDateTime alivesTo = null;
			if (!Utilities.isEmpty(dto.alivesTo())) {
				try {
					alivesTo = Utilities.parseUTCStringToZonedDateTime(dto.alivesTo());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Maximum alive time has an invalid time format", origin);
				}
			}

			if (alivesFrom != null && alivesTo != null && alivesTo.isBefore(alivesFrom)) {
				throw new InvalidParameterException("Empty alive time interval", origin);
			}

			if (dto.minUsage() != null && dto.minUsage().intValue() < 0) {
				throw new InvalidParameterException("Minimum usage number must be a non-negative number", origin);
			}

			if (dto.maxUsage() != null && dto.maxUsage().intValue() < 0) {
				throw new InvalidParameterException("Maximum usage number must be a non-negative number", origin);
			}

			if (dto.minUsage() != null && dto.maxUsage() != null && dto.minUsage().intValue() > dto.maxUsage().intValue()) {
				throw new InvalidParameterException("Empty usage interval", origin);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateNormalizedQueryMgmtRequest(final NormalizedTranslationQueryRequestDTO dto, final String origin) {
		logger.debug("validateNormalizedQueryMgmtRequest started...");

		try {
			if (!Utilities.isEmpty(dto.creators())) {
				dto.creators()
						.forEach(c -> systemNameValidator.validateSystemName(c));
			}

			if (!Utilities.isEmpty(dto.consumers())) {
				dto.consumers()
						.forEach(c -> systemNameValidator.validateSystemName(c));
			}

			if (!Utilities.isEmpty(dto.providers())) {
				dto.providers()
						.forEach(p -> systemNameValidator.validateSystemName(p));
			}

			if (!Utilities.isEmpty(dto.serviceDefinitions())) {
				dto.serviceDefinitions()
						.forEach(s -> serviceDefinitionNameValidator.validateServiceDefinitionName(s));
			}

			if (!Utilities.isEmpty(dto.interfaceTranslators())) {
				dto.interfaceTranslators()
						.forEach(it -> systemNameValidator.validateSystemName(it));
			}

			if (!Utilities.isEmpty(dto.dataModelTranslators())) {
				dto.dataModelTranslators()
						.forEach(dmt -> systemNameValidator.validateSystemName(dmt));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	private Pair<NormalizedTranslationDiscoveryRequestDTO, Map<TranslationDiscoveryFlag, Boolean>> normalizeDiscoveryMgmtRequest(final String requester, final TranslationDiscoveryMgmtRequestDTO dto) {
		logger.debug("normalizeDiscoveryMgmtRequest started...");

		return Pair.of(
				new NormalizedTranslationDiscoveryRequestDTO(
						requester,
						dto.candidates().stream().map(c -> normalizeCandidate(c)).toList(),
						systemNameNormalizer.normalize(dto.consumer()),
						operationNormalizer.normalize(dto.operation()),
						dto.interfaceTemplateNames().stream().map(iName -> interfaceTemplateNameNormalizer.normalize(iName)).toList(),
						dataModelIdentifierNormalizer.normalize(dto.inputDataModelId()),
						dataModelIdentifierNormalizer.normalize(dto.outputDataModelId())),
				normalizeDiscoveryFlags(dto.flags()));
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
	private Map<TranslationDiscoveryFlag, Boolean> normalizeDiscoveryFlags(final Map<String, Boolean> flags) {
		logger.debug("normalizeDiscoveryFlags started...");

		if (Utilities.isEmpty(flags)) {
			return Map.of();
		}

		final Map<TranslationDiscoveryFlag, Boolean> result = new HashMap<>(flags.size());
		flags.forEach((k, v) -> {
			if (v != null) {
				result.put(TranslationDiscoveryFlag.valueOf(k.trim().toUpperCase()), v);
			}
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private Pair<UUID, String> normalizeNegotiationMgmtRequest(final TranslationNegotiationMgmtRequestDTO dto) {
		logger.debug("normalizeNegotiationMgmtRequest started...");

		return Pair.of(
				UUID.fromString(dto.bridgeId().trim()),
				serviceInstanceIdentifierNormalizer.normalize(dto.targetInstanceId()));
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedTranslationQueryRequestDTO normalizeQueryMgmtRequest(final TranslationQueryRequestDTO dto) {
		logger.debug("normalizeQueryMgmtRequest started...");

		if (dto == null) {
			return new NormalizedTranslationQueryRequestDTO(
					pageService.getPageRequest(null, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter"),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null);
		}

		final PageDTO normalizedPagination = dto.pagination() == null || Utilities.isEmpty(dto.pagination().sortField())
				? dto.pagination()
				: new PageDTO(
						dto.pagination().page(),
						dto.pagination().size(),
						dto.pagination().direction(),
						BridgeDetails.SORT_NAME_ALTERNATIVES.getOrDefault(dto.pagination().sortField().trim(), dto.pagination().sortField().trim()));

		return new NormalizedTranslationQueryRequestDTO(
				pageService.getPageRequest(normalizedPagination, Direction.DESC, BridgeDetails.SORTABLE_FIELDS_BY, BridgeDetails.DEFAULT_SORT_FIELD, "does not matter"),
				Utilities.isEmpty(dto.bridgeIds()) ? null : dto.bridgeIds().stream().map(id -> UUID.fromString(id.trim())).toList(),
				Utilities.isEmpty(dto.creators()) ? null : dto.creators().stream().map(c -> systemNameNormalizer.normalize(c)).toList(),
				Utilities.isEmpty(dto.statuses()) ? null : dto.statuses().stream().map(s -> TranslationBridgeStatus.valueOf(s.trim().toUpperCase())).toList(),
				Utilities.isEmpty(dto.consumers()) ? null : dto.consumers().stream().map(c -> systemNameNormalizer.normalize(c)).toList(),
				Utilities.isEmpty(dto.providers()) ? null : dto.providers().stream().map(p -> systemNameNormalizer.normalize(p)).toList(),
				Utilities.isEmpty(dto.serviceDefinitions()) ? null : dto.serviceDefinitions().stream().map(s -> serviceDefinitionNameNormalizer.normalize(s)).toList(),
				Utilities.isEmpty(dto.interfaceTranslators()) ? null : dto.interfaceTranslators().stream().map(it -> systemNameNormalizer.normalize(it)).toList(),
				Utilities.isEmpty(dto.dataModelTranslators()) ? null : dto.dataModelTranslators().stream().map(dmt -> systemNameNormalizer.normalize(dmt)).toList(),
				Utilities.parseUTCStringToZonedDateTime(dto.creationFrom()),
				Utilities.parseUTCStringToZonedDateTime(dto.creationTo()),
				Utilities.parseUTCStringToZonedDateTime(dto.alivesFrom()),
				Utilities.parseUTCStringToZonedDateTime(dto.alivesTo()),
				dto.minUsage(),
				dto.maxUsage());
	}
}