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
package eu.arrowhead.translationmanager.service.engine;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.translationmanager.TranslationManagerConstants;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@Service
public class CoreSystemsDriver {

	//=================================================================================================
	// members

	private static final int tokenIntervalForInterfaceBridgeManagement = 24; // in hours
	private static final int tokenUsageLimitForInterfaceBridgeManagement = 100;

	@Value(TranslationManagerConstants.$TRANSLATOR_SERVICE_MIN_AVAILABILITY_WD)
	private int translatorServiceMinAvailability;

	@Autowired
	private TranslationManagerSystemInfo sysInfo;

	@Autowired
	private ArrowheadHttpService ahHttpService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isBlacklisted(final String systemName) {
		logger.debug("isBlacklisted started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is missing");

		return filterOutBlacklistedSystems(List.of(systemName)).isEmpty();
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> filterOutBlacklistedSystems(final List<String> systemNames) {
		logger.debug("filterOutBlacklistedSystems started...");
		Assert.isTrue(!Utilities.isEmpty(systemNames), "systemNames is missing");
		Assert.isTrue(!Utilities.containsNullOrEmpty(systemNames), "systemNames list contains null or empty element");

		final List<String> candidates = systemNames
				.stream()
				.filter(sysName -> !sysInfo.getBlacklistCheckExcludeList().contains(sysName))
				.toList();

		if (candidates.isEmpty()) {
			// no need to check the blacklist
			return systemNames;
		}

		try {
			boolean hasMorePage = false;
			int pageNumber = 0;
			Integer pageSize = null;

			final List<BlacklistEntryDTO> blacklistEntries = new ArrayList<BlacklistEntryDTO>();
			do {
				final BlacklistEntryListResponseDTO response = ahHttpService.consumeService(
						Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT,
						Constants.SERVICE_OP_BLACKLIST_QUERY,
						Constants.SYS_NAME_BLACKLIST,
						BlacklistEntryListResponseDTO.class,
						new BlacklistQueryRequestDTO(
								new PageDTO(pageNumber == 0 ? null : pageNumber, pageSize, null, null),
								candidates,
								null,
								null,
								null,
								null,
								Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow())));
				blacklistEntries.addAll(response.entries());
				hasMorePage = blacklistEntries.size() < response.count();
				pageNumber = hasMorePage ? pageNumber + 1 : pageNumber;
				pageSize = pageSize == null ? response.entries().size() : pageSize;
			} while (hasMorePage);

			final List<String> result = systemNames
					.stream()
					.filter(name -> {
						boolean isBlacklisted = false;
						for (final BlacklistEntryDTO blDTO : blacklistEntries) {
							if (name.equals(blDTO.systemName())) {
								isBlacklisted = true;
								break;
							}
						}

						return !isBlacklisted;
					}).toList();

			return result;
		} catch (final ForbiddenException | AuthException ex) {
			throw ex;
		} catch (final ArrowheadException ex) {
			logger.error("Blacklist server is not available during the translation bridge process");
			logger.debug(ex);
			if (sysInfo.isBlacklistForced()) {
				logger.error("All the systems have been filtered out, because blacklist is forced");
				return List.of();
			} else {
				logger.error("All the systems have been passed, because blacklist is not forced");
				return systemNames;
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> filterOutProvidersBecauseOfUnauthorization(final List<String> candidates, final String consumer, final String serviceDefinition, final String operation) {
		logger.debug("filterOutProvidersBecauseOfUnauthorization started...");
		Assert.isTrue(!Utilities.isEmpty(candidates), "candidates list is missing");
		Assert.isTrue(!Utilities.containsNullOrEmpty(candidates), "candidates list contains null or empty value");
		Assert.isTrue(!Utilities.isEmpty(consumer), "consumer is missing");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is missing");

		final AuthorizationVerifyListRequestDTO payload = calculateVerifyPayload(candidates, consumer, serviceDefinition, operation);
		final AuthorizationVerifyListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationVerifyListResponseDTO.class,
				payload);

		if (response.entries().isEmpty()) {
			return List.of();
		}

		final List<String> result = new ArrayList<>();
		candidates.forEach(c -> {
			final boolean denied = response
					.entries()
					.stream()
					.anyMatch(e -> e.provider().equals(c) && !e.granted());

			if (!denied) {
				// has access the specified operation  (or all operations if nothing is specified)
				result.add(c);
			}
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> generateTokenForManagerToInterfaceBridgeManagementService(final List<ServiceInstanceResponseDTO> interfaceTranslators) {
		logger.debug("generateTokenForManagerToInterfaceBridgeManagementService started...");
		Assert.isTrue(!Utilities.isEmpty(interfaceTranslators), "interfaceTranslators list is missing");
		Assert.isTrue(!Utilities.containsNull(interfaceTranslators), "interfaceTranslators list contains null element");

		final AuthorizationTokenGenerationMgmtListRequestDTO payload = calculateTokenGenerationPayloadForInterfaceBridgeManagementService(interfaceTranslators);
		if (Utilities.isEmpty(payload.list())) {
			// no tokens
			return Map.of();
		}

		final AuthorizationTokenMgmtListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationTokenMgmtListResponseDTO.class,
				payload,
				new LinkedMultiValueMap<>(Map.of(Constants.UNBOUND, List.of(Boolean.TRUE.toString()))));

		final Map<String, String> result = new HashMap<>(response.entries().size());
		response.entries()
				.forEach(e -> result.put(e.provider(), e.token()));

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenResponseDTO generateTokenForInterfaceTranslatorToTargetOperation(
			final String policy,
			final String interfaceTranslator,
			final String targetProvider,
			final String serviceDefinition,
			final String operation) {
		logger.debug("generateTokenForInterfaceTranslatorToTargetOperation started...");
		Assert.isTrue(!Utilities.isEmpty(policy), "policy is missing");
		Assert.isTrue(!Utilities.isEmpty(interfaceTranslator), "interfaceTranslator is missing");
		Assert.isTrue(!Utilities.isEmpty(targetProvider), "targetProvider is missing");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "serviceDefinition is missing");
		Assert.isTrue(!Utilities.isEmpty(operation), "operation is missing");

		final AuthorizationTokenGenerationMgmtListRequestDTO payload = calculateTokenGenerationPayloadForTargetOperation(policy, interfaceTranslator, targetProvider, serviceDefinition, operation);
		final AuthorizationTokenMgmtListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationTokenMgmtListResponseDTO.class,
				payload,
				new LinkedMultiValueMap<>(Map.of(Constants.UNBOUND, List.of(Boolean.TRUE.toString()))));

		return response
				.entries()
				.get(0);
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceResponseDTO> collectInterfaceTranslatorCandidates(final List<String> inputInterfaceRequirements, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("collectInterfaceTranslatorCandidates started...");
		Assert.isTrue(!Utilities.isEmpty(inputInterfaceRequirements), "inputInterfaceRequirements list is missing");
		Assert.isTrue(!Utilities.containsNullOrEmpty(inputInterfaceRequirements), "inputInterfaceRequirements list contains null or empty element");
		Assert.isTrue(!Utilities.isEmpty(targets), "targets list is missing");
		Assert.isTrue(!Utilities.containsNull(targets), "targets list contains null element");

		final ServiceInstanceLookupRequestDTO payload = calculateInterfaceTranslatorLookupPayload(inputInterfaceRequirements, targets);
		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				payload);

		return response.entries();
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceResponseDTO> collectDataModelTranslatorCandidates(final List<TranslationDiscoveryModel> models) {
		logger.debug("collectDataModelTranslatorCandidates started...");
		Assert.isTrue(!Utilities.isEmpty(models), "models list is missing");
		Assert.isTrue(!Utilities.containsNull(models), "models list contains null element");

		final ServiceInstanceLookupRequestDTO payload = calculateDataModelTranslatorLookupPayload(models);
		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				payload);

		return response.entries();
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceResponseDTO> collectDataModelTranslatorFactoryCandidates() {
		logger.debug("collectDataModelTranslatorFactoryCandidates started...");

		final ServiceInstanceLookupRequestDTO payload = calculateDataModelTranslatorFactoryLookupPayload();
		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				payload);

		return response.entries();
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getConfigurationForSystem(final String systemName) {
		logger.debug("getConfigurationForSystem started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "system name is missing");

		// TODO: implement after configuration support system is available

		return Map.of();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationVerifyListRequestDTO calculateVerifyPayload(final List<String> candidates, final String consumer, final String serviceDefinition, final String operation) {
		logger.debug("calculateVerifyPayload started...");

		final List<AuthorizationVerifyRequestDTO> list = new ArrayList<>(candidates.size());
		candidates.forEach(c -> {
			list.add(new AuthorizationVerifyRequestDTO(
					c,
					consumer,
					null, // local cloud
					AuthorizationTargetType.SERVICE_DEF.name(),
					serviceDefinition,
					operation));
		});

		return new AuthorizationVerifyListRequestDTO(list);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenGenerationMgmtListRequestDTO calculateTokenGenerationPayloadForInterfaceBridgeManagementService(final List<ServiceInstanceResponseDTO> interfaceTranslators) {
		logger.debug("calculateTokenGenerationPayloadForInterfaceBridgeManagementService started...");

		final ZonedDateTime now = Utilities.utcNow();
		final List<AuthorizationTokenGenerationMgmtRequestDTO> list = new ArrayList<>(interfaceTranslators.size());

		interfaceTranslators
				.forEach(itp -> {
					final String policy = itp.interfaces().get(0).policy();
					if (policy.endsWith(Constants.AUTHORIZATION_TOKEN_VARIANT_SUFFIX)) {
						list.add(new AuthorizationTokenGenerationMgmtRequestDTO(
								policy,
								AuthorizationTargetType.SERVICE_DEF.name(),
								Defaults.DEFAULT_CLOUD,
								sysInfo.getSystemName(),
								itp.provider().name(),
								Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT,
								null,
								Utilities.convertZonedDateTimeToUTCString(now.plusHours(tokenIntervalForInterfaceBridgeManagement)),
								tokenUsageLimitForInterfaceBridgeManagement));
					}
				});

		return new AuthorizationTokenGenerationMgmtListRequestDTO(list);
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationTokenGenerationMgmtListRequestDTO calculateTokenGenerationPayloadForTargetOperation(
			final String policy,
			final String interfaceTranslator,
			final String targetProvider,
			final String serviceDefinition,
			final String operation) {
		logger.debug("calculateTokenGenerationPayloadForTargetOperation started...");

		return new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(
				new AuthorizationTokenGenerationMgmtRequestDTO(
						policy,
						AuthorizationTargetType.SERVICE_DEF.name(),
						Defaults.DEFAULT_CLOUD,
						interfaceTranslator,
						targetProvider,
						serviceDefinition,
						operation,
						null,
						null)));
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO calculateInterfaceTranslatorLookupPayload(final List<String> inputInterfaceRequirements, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("calculateInterfaceTranslatorLookupPayload started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		ServiceInstanceLookupRequestDTO.Builder builder = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT)
				.interfaceTemplateName(templateName)
				.metadataRequirementsList(calculateInterfaceTranslatorMetadataRequirements(inputInterfaceRequirements, targets));

		if (translatorServiceMinAvailability > 0) {
			final ZonedDateTime alivesAt = Utilities.utcNow().plusMinutes(translatorServiceMinAvailability);
			builder = builder.alivesAt(Utilities.convertZonedDateTimeToUTCString(alivesAt));
		}

		return builder.build();
	}

	//-------------------------------------------------------------------------------------------------
	private List<MetadataRequirementDTO> calculateInterfaceTranslatorMetadataRequirements(final List<String> inputInterfaceRequirements, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("calculateInterfaceTranslatorMetadataRequirements started...");

		final Set<MetadataRequirementDTO> resultSet = new HashSet<>();
		targets.forEach(t -> {
			final MetadataRequirementDTO req = new MetadataRequirementDTO();
			req.put(String.join(Constants.DOT, Constants.METADATA_KEY_INTERFACE_BRIDGE, Constants.METADATA_KEY_FROM), Map.of(
					MetadataRequirementTokenizer.OP, MetaOps.CONTAINS_ANY.name(),
					MetadataRequirementTokenizer.VALUE, inputInterfaceRequirements));
			req.put(String.join(Constants.DOT, Constants.METADATA_KEY_INTERFACE_BRIDGE, Constants.METADATA_KEY_TO), Map.of(
					MetadataRequirementTokenizer.OP, MetaOps.IN.name(),
					MetadataRequirementTokenizer.VALUE, t.interfaces()
							.stream()
							.map(intf -> intf.templateName())
							.toList()));
			resultSet.add(req);
		});

		return new ArrayList<>(resultSet);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO calculateDataModelTranslatorLookupPayload(final List<TranslationDiscoveryModel> models) {
		logger.debug("calculateDataModelTranslatorLookupPayload started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final List<String> policies = AuthenticationPolicy.CERTIFICATE == sysInfo.getAuthenticationPolicy()
				? List.of(ServiceInterfacePolicy.CERT_AUTH.name(), ServiceInterfacePolicy.NONE.name())
				: List.of(ServiceInterfacePolicy.NONE.name());

		ServiceInstanceLookupRequestDTO.Builder builder = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_DATA_MODEL_TRANSLATION)
				.interfaceTemplateName(templateName)
				.policies(policies)
				.metadataRequirementsList(calculateDataModelTranslatorMetadataRequirements(models));

		if (translatorServiceMinAvailability > 0) {
			final ZonedDateTime alivesAt = Utilities.utcNow().plusMinutes(translatorServiceMinAvailability);
			builder = builder.alivesAt(Utilities.convertZonedDateTimeToUTCString(alivesAt));
		}

		return builder.build();
	}

	//-------------------------------------------------------------------------------------------------
	private List<MetadataRequirementDTO> calculateDataModelTranslatorMetadataRequirements(final List<TranslationDiscoveryModel> models) {
		logger.debug("calculateDataModelTranslatorInterfacePropertyRequirements started...");

		final Set<MetadataRequirementDTO> resultSet = new HashSet<>();
		models.forEach(m -> {
			if (m.getInputDataModelIdRequirement() != null) {
				final MetadataRequirementDTO req = new MetadataRequirementDTO();
				final List<String> translationDirection = List.of(m.getInputDataModelIdRequirement(), m.getTargetInputDataModelId());
				req.put(Constants.METADATA_KEY_DATA_MODEL_IDS, Map.of(
						MetadataRequirementTokenizer.OP, MetaOps.CONTAINS.name(),
						MetadataRequirementTokenizer.VALUE, translationDirection));
				resultSet.add(req);
			}

			if (m.getOutputDataModelIdRequirement() != null) {
				final MetadataRequirementDTO req = new MetadataRequirementDTO();
				final List<String> translationDirection = List.of(m.getTargetOutputDataModelId(), m.getOutputDataModelIdRequirement());
				req.put(Constants.METADATA_KEY_DATA_MODEL_IDS, Map.of(
						MetadataRequirementTokenizer.OP, MetaOps.CONTAINS.name(),
						MetadataRequirementTokenizer.VALUE, translationDirection));
				resultSet.add(req);
			}
		});

		return new ArrayList<>(resultSet);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO calculateDataModelTranslatorFactoryLookupPayload() {
		logger.debug("calculateDataModelTranslatorFactoryLookupPayload started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final List<String> policies = AuthenticationPolicy.CERTIFICATE == sysInfo.getAuthenticationPolicy()
				? List.of(ServiceInterfacePolicy.CERT_AUTH.name(), ServiceInterfacePolicy.NONE.name())
				: List.of(ServiceInterfacePolicy.NONE.name());

		ServiceInstanceLookupRequestDTO.Builder builder = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(Constants.SERVICE_DEF_DATA_MODEL_TRANSLATOR_FACTORY_CONTROL)
				.interfaceTemplateName(templateName)
				.policies(policies);

		if (translatorServiceMinAvailability > 0) {
			final ZonedDateTime alivesAt = Utilities.utcNow().plusMinutes(translatorServiceMinAvailability);
			builder = builder.alivesAt(Utilities.convertZonedDateTimeToUTCString(alivesAt));
		}

		return builder.build();
	}
}