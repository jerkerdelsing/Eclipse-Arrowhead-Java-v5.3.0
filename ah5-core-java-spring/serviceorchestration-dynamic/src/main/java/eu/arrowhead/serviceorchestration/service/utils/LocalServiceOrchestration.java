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
package eu.arrowhead.serviceorchestration.service.utils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataKeyEvaluator;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementExpression;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.QoSOperation;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.driver.QoSDriver;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.matchmaker.ServiceInstanceMatchmaker;
import jakarta.annotation.Resource;

@Service
public class LocalServiceOrchestration {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private eu.arrowhead.serviceorchestration.service.PlantRuleService plantRules;

	@Autowired
	private InterCloudServiceOrchestration interCloudOrch;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private OrchestrationLockDbService orchLockDbService;

	@Autowired
	private ArrowheadHttpService ahHttpService;

	@Autowired
	private QoSDriver qosDriver;

	@Resource(name = DynamicServiceOrchestrationConstants.SERVICE_INSTANCE_MATCHMAKER)
	private ServiceInstanceMatchmaker matchmaker;

	@Autowired
	private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

	@Autowired
	private DataModelIdentifierNormalizer dataModelIDNormalizer;

	@Autowired
	private DataModelIdentifierValidator dataModelIDValidator;

	private static final int TEMPORARY_LOCK_DURATION = 60; // sec

	private static final int RESERVATION_BUFFER = 10; // sec

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodLengthCheck")
	public OrchestrationResponseDTO doLocalServiceOrchestration(final UUID jobId, final OrchestrationForm form) {
		logger.debug("doLocalServiceOrchestration started...");

		orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

		try {
			final Set<String> warnings = new HashSet<>();

            final String consumer = Utilities.isEmpty(form.getTargetSystemName()) ? form.getRequesterSystemName() : form.getTargetSystemName();
            final eu.arrowhead.dto.OrchestrationPlantRuleDTO plantRule = plantRules.find(consumer, form.getServiceDefinition());
            if (plantRule != null) { form.addFlag(OrchestrationFlag.ALLOW_INTERCLOUD, false); }

			// Discovering the service
			final boolean translationAllowed = form.getFlag(OrchestrationFlag.ALLOW_TRANSLATION) && sysInfo.isTranslationEnabled();
			List<OrchestrationCandidate> candidates = serviceDiscovery(form, translationAllowed, form.getFlag(OrchestrationFlag.ONLY_PREFERRED));
            candidates = eu.arrowhead.serviceorchestration.service.PlantRuleService.select(plantRule, candidates, false);
			candidates = filterOutLockedOnes(jobId, candidates);

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// Dealing with exclusivity
			if (form.exclusivityIsPreferred()) {
				markExclusivityIfFeasible(candidates);
			}

			if (form.getFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
				candidates = filterOutWhereExclusivityIsNotPossible(candidates);

				if (Utilities.isEmpty(candidates)) {
					return doInterCloudOrReturn(jobId, form);
				}

				if (form.addFlag(OrchestrationFlag.MATCHMAKING, true)) {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
				}
			}

			// Blacklist cross-check
			if (sysInfo.isBlacklistEnabled()) {
				candidates = filterOutBlacklistedOnes(candidates);
			}

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// Authorization cross-check
			if (sysInfo.isAuthorizationEnabled()) {
				candidates = filterOutUnauthorizedOnes(form, candidates);
			}

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// Temporary lock if required and possible
			if (form.exclusivityIsPreferred()) {
				candidates = filterOutLockedOnesAndTemporaryLockIfCanBeExclusive(jobId, form.getRequesterSystemName(), candidates);
			} else {
				candidates = filterOutLockedOnes(jobId, candidates);
			}

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// Collect matching interfaces or interfaces for which translation is an option
			assortInterfacesAndMarkIfNonNative(form, candidates, translationAllowed);
			candidates = filterOutNonInterfaceableOnes(candidates);

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// Deal with translations
			String translationBridgeId = null;
			if (translationAllowed) {
				if (checkIfHasNativeOnes(candidates)) {
					candidates = filterOutNonNativeOnes(candidates);
				} else {
					final Pair<String, List<OrchestrationCandidate>> translatable = filterOutNotTranslatableOnesAndChooseTranslatableInterfaces(form, candidates); // translation discovery
					translationBridgeId = translatable.getFirst();
					candidates = translatable.getSecond();
					if (Utilities.isEmpty(candidates)) {
						return doInterCloudOrReturn(jobId, form);
					}
					if (form.addFlag(OrchestrationFlag.MATCHMAKING, true)) {
						warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
					}
				}
			}

			// Dealing with preferences
			if (form.exclusivityIsPreferred() && !form.getFlag(OrchestrationFlag.ONLY_EXCLUSIVE)) {
				if (containsReservable(candidates)) {
					candidates = filterOutWhereExclusivityIsNotPossible(candidates);
					if (form.addFlag(OrchestrationFlag.MATCHMAKING, true)) {
						warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
					}
				} else {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_NOT_EXCLUSIVE);
				}
			}

			if (form.hasPreferredProviders() && !form.getFlag(OrchestrationFlag.ONLY_PREFERRED)) {
				if (searchForPreferredProviders(form, candidates)) {
					candidates = filterOutNonPreferredProviders(candidates);
				}
			}

			if (Utilities.isEmpty(candidates)) {
				return doInterCloudOrReturn(jobId, form);
			}

			// QoS cross-check
			if (form.hasQualityRequirements()) {
				if (!sysInfo.isQoSEnabled()) {
					warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_QOS_NOT_ENABLED);
				} else {
					candidates = qosDriver.doQoSCompliance(jobId, candidates, form.getQualityRequirements(), warnings);
				}
			}

            // Rank only surviving authorized/interfaceable/QoS-compliant candidates. Lower plant priority wins.
            candidates = eu.arrowhead.serviceorchestration.service.PlantRuleService.select(plantRule, candidates, false);

			// Matchmaking if required
			if (form.getFlag(OrchestrationFlag.MATCHMAKING)) {
				final OrchestrationCandidate match = plantRule == null ? matchmaking(form, candidates) : candidates.getFirst();
				if (form.exclusivityIsPreferred()) {
					reserve(jobId, form.getRequesterSystemName(), match, form.getExclusivityDuration());
					final String matchInstanceId = match.getServiceInstance().instanceId();
					releaseTemporaryLockIfItWasLocked(jobId, candidates.stream().filter(c -> !c.getServiceInstance().instanceId().equals(matchInstanceId)).collect(Collectors.toList()));
					if (form.getExclusivityDuration() > match.getExclusivityDuration()) {
						warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_PART_TIME_EXCLUSIVITY);
					}
				}

				candidates.clear();
				candidates.add(match);
			}

			// Obtain Authorization tokens when required
			if (sysInfo.isAuthorizationEnabled() && Utilities.isEmpty(translationBridgeId)) {
				obtainAuthorizationTokensIfRequired(form, candidates);
			}

			// Create translation bridge if necessary
			if (!Utilities.isEmpty(translationBridgeId)) {
				Assert.isTrue(form.getFlag(OrchestrationFlag.MATCHMAKING), "There was no matchmaking, while translation bridge should have been initiated");
				buildTranslationBridge(translationBridgeId, form.getOperations().getFirst(), candidates.getFirst());
				warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_FORCED_INTERFACE_SECURITY_POLICY);
			}

			final OrchestrationResponseDTO result = convertToOrchestrationResponse(candidates, warnings);
			// the providers are recorded so that the orchestration history shows who consumes what from whom
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, InterCloudServiceOrchestration.resultMessage(candidates.size() + " local result",
					candidates.stream().map(c -> c.getServiceInstance().provider().name()).distinct().toList()));

			return result;
		} catch (final Exception ex) {
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.ERROR, ex.getMessage());
			throw ex;
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> serviceDiscovery(final OrchestrationForm form, final boolean withoutInterface, final boolean onlyPreferred) {
		logger.debug("serviceDiscovery started...");

		final ServiceInstanceLookupRequestDTO lookupDTO = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(form.getServiceDefinition())
				.versions(form.getVersions())
				.alivesAt(form.getAlivesAt())
				.metadataRequirementsList(form.getMetadataRequirements())
				.policies(form.getSecurityPolicies())
				.interfaceTemplateNames(withoutInterface ? null : form.getInterfaceTemplateNames())
				.interfacePropertyRequirementsList(withoutInterface ? null : form.getInterfacePropertyRequirements())
				.addressTypes(withoutInterface ? null : form.getInterfaceAddressTypes())
				.providerNames(onlyPreferred ? form.getPreferredProviders() : null)
				.build();

		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(Map.of(Constants.VERBOSE, List.of(Boolean.FALSE.toString())));
		final ServiceInstanceListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_LOOKUP,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceListResponseDTO.class,
				lookupDTO,
				queryParams);

		if (Utilities.isEmpty(response.entries())) {
			return List.of();
		}

		return response.entries()
				.stream()
				.map(instance -> new OrchestrationCandidate(instance, true, onlyPreferred))
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutLockedOnes(final UUID jobId, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutLockedOnes started...");

		if (Utilities.isEmpty(candidates)) {
			return candidates;
		}

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_ORCH_LOCK) {
			final String jobIdStr = jobId.toString();
			final List<OrchestrationLock> lockRecords = orchLockDbService.getByServiceInstanceId(candidates.stream().map(c -> c.getServiceInstance().instanceId()).collect(Collectors.toList()));

			final ZonedDateTime now = Utilities.utcNow();
			final Set<Long> expiredLocks = new HashSet<>();
			final Set<String> lockedServiceInstanceIds = new HashSet<>();

			for (final OrchestrationLock lockRecord : lockRecords) {
				if (!Utilities.isEmpty(lockRecord.getOrchestrationJobId()) && lockRecord.getOrchestrationJobId().equals(jobIdStr)) {
					// lock belongs to this session (in theory it doesn't happen)
					continue;
				} else if (lockRecord.getExpiresAt() != null && lockRecord.getExpiresAt().isBefore(now)) {
					expiredLocks.add(lockRecord.getId());
				} else {
					lockedServiceInstanceIds.add(lockRecord.getServiceInstanceId());
				}
			}

			if (!Utilities.isEmpty(expiredLocks)) {
				orchLockDbService.deleteInBatch(expiredLocks);
			}

			return candidates
					.stream()
					.filter(c -> !lockedServiceInstanceIds.contains(c.getServiceInstance().instanceId()))
					.collect(Collectors.toList());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutLockedOnesAndTemporaryLockIfCanBeExclusive(final UUID jobId, final String consumerSystem, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutLockedOnesAndTemporaryLockIfCanBeExclusive started...");

		if (Utilities.isEmpty(candidates)) {
			return candidates;
		}

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_ORCH_LOCK) {
			final List<OrchestrationCandidate> freeCandidates = filterOutLockedOnes(jobId, candidates);
			if (!Utilities.isEmpty(freeCandidates)) {
				final String jobIdStr = jobId.toString();
				final ZonedDateTime tempLockExpiresAt = Utilities.utcNow().plusSeconds(TEMPORARY_LOCK_DURATION);

				final List<OrchestrationLock> toLock = new ArrayList<>(freeCandidates.size());
				for (final OrchestrationCandidate candidate : freeCandidates) {
					if (candidate.canBeExclusive()) {
						candidate.setLocked(true);
						toLock.add(new OrchestrationLock(jobIdStr, candidate.getServiceInstance().instanceId(), consumerSystem, tempLockExpiresAt, true));
					}
				}

				if (!Utilities.isEmpty(toLock)) {
					orchLockDbService.create(toLock);
				}
			}
			return freeCandidates;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void markExclusivityIfFeasible(final List<OrchestrationCandidate> candidates) {
		logger.debug("markExclusivityIfFeasible started...");

		for (final OrchestrationCandidate candidate : candidates) {
			final Object allowedExclusivityDurationObj = candidate.getServiceInstance().metadata() == null ? null : candidate.getServiceInstance().metadata().get(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY);
			if (allowedExclusivityDurationObj != null) {
				try {
					final int allowedExclusivityDuration = Utilities.parseToInt(allowedExclusivityDurationObj);
					candidate.setCanBeExclusive(allowedExclusivityDuration > 0);
					candidate.setExclusivityDuration(allowedExclusivityDuration);

				} catch (final NumberFormatException ex) {
					logger.warn(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY + " metadata is not an integer. Service instance id: " + candidate.getServiceInstance().instanceId());
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutWhereExclusivityIsNotPossible(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutWhereExclusivityIsNotPossible started...");

		return candidates
				.stream()
				.filter(c -> c.canBeExclusive())
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private boolean containsReservable(final List<OrchestrationCandidate> candidates) {
		logger.debug("containsReservable started...");

		for (final OrchestrationCandidate candidate : candidates) {
			if (candidate.canBeExclusive()) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutBlacklistedOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutBlacklistedOnes started...");

		final List<String> systemNames = candidates.stream().map(c -> c.getServiceInstance().provider().name()).filter(sysName -> !sysInfo.getBlacklistCheckExcludeList().contains(sysName)).toList();
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
						new BlacklistQueryRequestDTO(new PageDTO(pageNumber == 0 ? null : pageNumber, pageSize, null, null), systemNames, null, null, null, null, Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow())));

				blacklistEntries.addAll(response.entries());
				hasMorePage = blacklistEntries.size() < response.count();
				pageNumber = hasMorePage ? pageNumber + 1 : pageNumber;
				pageSize = pageSize == null ? response.entries().size() : pageSize;

			} while (hasMorePage);

			final List<OrchestrationCandidate> result = candidates.stream().filter(candidate -> {
				boolean isBlacklisted = false;
				for (final BlacklistEntryDTO blDTO : blacklistEntries) {
					if (candidate.getServiceInstance().provider().name().equals(blDTO.systemName())) {
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
			logger.error("Blacklist server is not available during the orchestration process");
			if (sysInfo.isBlacklistForced()) {
				logger.error("All the provider candidate has been filtered out, because blacklist filter is forced");
				return List.of();
			} else {
				logger.error("All the provider candidate has been passed, because blacklist filter is not forced");
				return candidates;
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutUnauthorizedOnes(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutUnauthorizedOnes started...");

		final AuthorizationVerifyListRequestDTO payload = calculateVerifyPayload(form.getTargetSystemName(), form.getOperations(), candidates);
		final AuthorizationVerifyListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationVerifyListResponseDTO.class,
				payload);

		if (response.entries().isEmpty()) {
			return List.of();
		}

		final List<OrchestrationCandidate> result = new ArrayList<>();
		candidates.forEach(c -> {
			final boolean denied = response
					.entries()
					.stream()
					.anyMatch(e -> e.provider().equals(c.getServiceInstance().provider().name()) && !e.granted());

			if (!denied) {
				// have access to all specified operations (or all operations if nothing is specified)
				result.add(c);
			}
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private AuthorizationVerifyListRequestDTO calculateVerifyPayload(final String consumerName, final List<String> operations, final List<OrchestrationCandidate> candidates) {
		logger.debug("calculateVerifyPayload started...");

		final int capacity = Utilities.isEmpty(operations) ? candidates.size() : candidates.size() * operations.size();
		final List<AuthorizationVerifyRequestDTO> list = new ArrayList<>(capacity);
		candidates.forEach(c -> {
			if (Utilities.isEmpty(operations)) {
				list.add(new AuthorizationVerifyRequestDTO(
						c.getServiceInstance().provider().name(),
						consumerName,
						null, // local cloud
						AuthorizationTargetType.SERVICE_DEF.name(),
						c.getServiceInstance().serviceDefinition().name(),
						null)); // no scope
			} else {
				for (final String operation : operations) {
					list.add(new AuthorizationVerifyRequestDTO(
							c.getServiceInstance().provider().name(),
							consumerName,
							null, // local cloud
							AuthorizationTargetType.SERVICE_DEF.name(),
							c.getServiceInstance().serviceDefinition().name(),
							operation));
				}
			}
		});

		return new AuthorizationVerifyListRequestDTO(list);
	}

	//-------------------------------------------------------------------------------------------------
	private void assortInterfacesAndMarkIfNonNative(final OrchestrationForm form, final List<OrchestrationCandidate> candidates, final boolean translationAllowed) {
		logger.debug("assortInterfacesAndMarkIfNonNative started...");

		if (Utilities.isEmpty(form.getInterfaceTemplateNames())
				&& Utilities.isEmpty(form.getInterfacePropertyRequirements())
				&& Utilities.isEmpty(form.getInterfaceAddressTypes())) {
			for (final OrchestrationCandidate candidate : candidates) {
				candidate.addMatchingInterfaces(candidate.getServiceInstance().interfaces());
			}
			// No interface requirements at all, everything is considered as matching
			return;
		}

		final boolean hasTemplateRequirement = !Utilities.isEmpty(form.getInterfaceTemplateNames());
		final boolean hasPropRequirements = !Utilities.isEmpty(form.getInterfacePropertyRequirements());
		final boolean hasAddressTypeRequirements = !Utilities.isEmpty(form.getInterfaceAddressTypes());
		final Pair<Optional<String>, Optional<String>> interfaceModelIDRequirements = extractInterfaceModelIDRequirements(form.getOperations(), form.getInterfacePropertyRequirements());
		final boolean hasInputModelIDRequirement = interfaceModelIDRequirements.getFirst().isPresent();
		final boolean hasOutputModelIDRequirement = interfaceModelIDRequirements.getSecond().isPresent();

		for (final OrchestrationCandidate candidate : candidates) {
			boolean hasNativeInterface = false;
			for (final ServiceInstanceInterfaceResponseDTO offeredInterface : candidate.getServiceInstance().interfaces()) {
				boolean templateIsOk = true;
				boolean propsAreOk = true;
				boolean addressTypeIsOk = true;

				// Checking interface template names
				if (hasTemplateRequirement
						&& !form.getInterfaceTemplateNames().contains(offeredInterface.templateName())) {
					templateIsOk = false;

					// Checking interface properties
				} else if (hasPropRequirements) {
					boolean propReqMatch = false;
					for (final MetadataRequirementDTO interfacePropertyRequirement : form.getInterfacePropertyRequirements()) {
						if (MetadataRequirementsMatcher.isMetadataMatch(offeredInterface.properties(), interfacePropertyRequirement)) {
							propReqMatch = true;
							break;
						}
					}
					propsAreOk = propReqMatch;
				}

				// Checking address types
				if (translationAllowed // Has to consider address types because it wasn't sent to SR as filter parameter
						&& hasAddressTypeRequirements) {
					addressTypeIsOk = interfaceAddressPropertyProcessor.filterOnAddressTypes(offeredInterface.properties(), form.getInterfaceAddressTypes());
				}

				if (templateIsOk && propsAreOk) {
					// This might be a matching interface
					if (!addressTypeIsOk) {
						// This is a not matching interface and translation is not an option
						continue;
					}
					// This is a matching interface
					hasNativeInterface = true;
					candidate.addMatchingInterface(offeredInterface);

				} else if (!hasNativeInterface && translationAllowed && hasTemplateRequirement) {
					// This is a not matching interface, but translation might be an option

					final Pair<Optional<String>, Optional<String>> offeredInterfaceModelID = extractOfferedInterfaceModelID(form.getOperations(), offeredInterface.properties());
					final boolean candidateOffersInputModelId = offeredInterfaceModelID.getFirst().isPresent();
					final boolean candidateOffersOutputModelId = offeredInterfaceModelID.getSecond().isPresent();

					if ((hasInputModelIDRequirement && !candidateOffersInputModelId)
							|| (hasOutputModelIDRequirement && !candidateOffersOutputModelId)) {
						// Requester expects input/output, but candidate has no
						continue;
					}

					if ((!hasInputModelIDRequirement && candidateOffersInputModelId)
							|| (!hasOutputModelIDRequirement && candidateOffersOutputModelId)) {
						// Requester not expects input/output, but candidate has
						continue;
					}

					candidate.addTranslatableInterface(offeredInterface);
				}
			}
			candidate.setNonNative(!hasNativeInterface);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private final List<OrchestrationCandidate> filterOutNonInterfaceableOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNonInterfaceableOnes started...");
		return candidates.stream().filter(c -> !Utilities.isEmpty(c.getMatchingInterfaces()) || !Utilities.isEmpty(c.getTranslatableInterfaces())).collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private boolean hasSortingQualityRequirements(final OrchestrationForm form) {
		logger.debug("hasSortingQualityRequirements started...");

		if (!form.hasQualityRequirements()) {
			return false;
		}

		for (final QoSRequirementDTO qosPref : form.getQualityRequirements()) {
			if (qosPref.operation().equalsIgnoreCase(QoSOperation.SORT.name())) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean checkIfHasNativeOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("checkIfHasNativeOnes started...");

		for (final OrchestrationCandidate candidate : candidates) {
			if (!candidate.isNonNative()) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonNativeOnes(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNonNativeOnes started...");

		return candidates
				.stream()
				.filter(c -> !c.isNonNative())
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private Pair<String, List<OrchestrationCandidate>> filterOutNotTranslatableOnesAndChooseTranslatableInterfaces(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNotTranslatableOnesAndChooseTranslatableInterfaces started...");
		Assert.isTrue(!Utilities.isEmpty(form.getOperations()) && form.getOperations().size() == 1,
				"There should be exactly one operation requested for performing translationDiscovery, but there is more/all"); // responsibility of OrchestrationFromContextValidation
		Assert.isTrue(!Utilities.isEmpty(form.getInterfaceTemplateNames()),
				"There should be at least one interface name requested for performing translationDiscovery, but there is zero"); // responsibility of OrchestrationFromContextValidation

		final List<ServiceInstanceResponseDTO> discoveryCandidates = candidates.stream().map(candidate -> {
			Assert.isTrue(candidate.isNonNative(), "There is a native candidate within the translation discovery candidates");
			candidate.getServiceInstance().interfaces().clear();
			candidate.getServiceInstance().interfaces().addAll(candidate.getTranslatableInterfaces());
			Assert.isTrue(!Utilities.isEmpty(candidate.getServiceInstance().interfaces()), "There is candidate without interface within the translation discovery candidates");
			return candidate.getServiceInstance();
		}).toList();

		final Pair<Optional<String>, Optional<String>> interfaceModelIDRequirements = extractInterfaceModelIDRequirements(form.getOperations(), form.getInterfacePropertyRequirements());

		final TranslationDiscoveryMgmtRequestDTO discoveryRequest = new TranslationDiscoveryMgmtRequestDTO(
				discoveryCandidates,
				form.getTargetSystemName(),
				form.getOperations().getFirst(),
				form.getInterfaceTemplateNames(),
				interfaceModelIDRequirements.getFirst().orElseGet(() -> null),
				interfaceModelIDRequirements.getSecond().orElseGet(() -> null),
				Map.of(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK.toString(), false,
						TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK.toString(), false,
						TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK.toString(), false,
						TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK.toString(), sysInfo.isBlacklistEnabled(),
						TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK.toString(), sysInfo.isAuthorizationEnabled()));

		final TranslationDiscoveryResponseDTO discoveryResults = ahHttpService.consumeService(
				Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT,
				Constants.SERVICE_OP_DISCOVERY,
				Constants.SYS_NAME_TRANSLATION_MANAGER,
				TranslationDiscoveryResponseDTO.class,
				discoveryRequest);

		if (Utilities.isEmpty(discoveryResults.candidates())) {
			return Pair.of("", List.of());
		}

		Assert.isTrue(!Utilities.isEmpty(discoveryResults.bridgeId()), "No bridgeId has been provided by " + Constants.SYS_NAME_TRANSLATION_MANAGER);

		final List<OrchestrationCandidate> translatableCandidates = new ArrayList<>(discoveryResults.candidates().size());
		for (final OrchestrationCandidate candidate : candidates) {
			candidate.getTranslatableInterfaces().clear();
			for (final ServiceInstanceInterfaceResponseDTO candidateInterface : candidate.getServiceInstance().interfaces()) {
				if (!Utilities.isEmpty(candidate.getTranslatableInterfaces())) {
					break;
				}
				for (final TranslationBridgeCandidateDTO discovered : discoveryResults.candidates()) {
					if (!candidate.getServiceInstance().instanceId().equals(discovered.serviceInstanceId())) {
						continue;
					} else if (candidateInterface.templateName().equals(discovered.interfaceTemplateName())) {
						candidate.addTranslatableInterface(candidateInterface);
						break;
					}
				}
			}
			if (!Utilities.isEmpty(candidate.getTranslatableInterfaces())) {
				translatableCandidates.add(candidate);
			}
		}

		return Pair.of(discoveryResults.bridgeId(), translatableCandidates);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean searchForPreferredProviders(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("searchForPreferredProviders started...");

		boolean hasPreferred = false;
		for (final OrchestrationCandidate candidate : candidates) {
			if (form.getPreferredProviders().contains(candidate.getServiceInstance().provider().name())) {
				hasPreferred = true;
				candidate.setPreferred(true);
			}
		}

		return hasPreferred;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationCandidate> filterOutNonPreferredProviders(final List<OrchestrationCandidate> candidates) {
		logger.debug("filterOutNonPreferredProviders started...");

		return candidates
				.stream()
				.filter(c -> c.isPreferred())
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate matchmaking(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("matchmaking started...");

		if (hasSortingQualityRequirements(form)) {
			return candidates.getFirst();
		}

		return matchmaker.doMatchmaking(form, candidates);
	}

	//-------------------------------------------------------------------------------------------------
	private void reserve(final UUID jobId, final String consumerSystem, final OrchestrationCandidate candidate, final int duration) {
		logger.debug("reserve started...");

		if (!candidate.canBeExclusive()) {
			return;
		}

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_ORCH_LOCK) {
			Assert.isTrue(candidate.isLocked(), "Unlocked candidate is intended to being reserved");

			final String jobIdStr = jobId.toString();
			final ZonedDateTime expiresAt = Utilities.utcNow().plusSeconds(RESERVATION_BUFFER + Math.min(duration, candidate.getExclusivityDuration()));

			final Optional<OrchestrationLock> optional = orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(
					jobIdStr,
					candidate.getServiceInstance().instanceId(),
					expiresAt,
					false);

			// the temporary lock may expire
			if (optional.isEmpty()) {
				orchLockDbService.create(List.of(new OrchestrationLock(jobIdStr, candidate.getServiceInstance().instanceId(), consumerSystem, expiresAt, false)));
			}

			candidate.setExclusiveUntil(expiresAt);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void releaseTemporaryLockIfItWasLocked(final UUID jobId, final List<OrchestrationCandidate> candidates) {
		logger.debug("releaseTemporaryLockIfItWasLocked started...");

		if (Utilities.isEmpty(candidates)) {
			return;
		}

		synchronized (DynamicServiceOrchestrationConstants.SYNC_LOCK_ORCH_LOCK) {
			final String jobIdStr = jobId.toString();

			final List<String> lockedOnes = candidates
					.stream()
					.filter(c -> c.isLocked())
					.map(c -> c.getServiceInstance().instanceId())
					.collect(Collectors.toList());

			if (Utilities.isEmpty(lockedOnes)) {
				return;
			}

			final List<OrchestrationLock> lockRecords = orchLockDbService.getByServiceInstanceId(lockedOnes);

			final List<Long> toRelease = lockRecords
					.stream()
					.filter(lr -> !Utilities.isEmpty(lr.getOrchestrationJobId()) && lr.getOrchestrationJobId().equals(jobIdStr))
					.map(lr -> lr.getId())
					.collect(Collectors.toList());

			if (!Utilities.isEmpty(toRelease)) {
				orchLockDbService.deleteInBatch(toRelease);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void obtainAuthorizationTokensIfRequired(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("obtainAuthorizationTokensIfRequired started...");

		final List<String> scopeList = Utilities.isEmpty(form.getOperations()) ? List.of("") : form.getOperations();
		final List<AuthorizationTokenGenerationMgmtRequestDTO> requestPayloadEntries = new ArrayList<>();
		for (final OrchestrationCandidate candidate : candidates) {
			Assert.isTrue(!candidate.isNonNative(), "There is a non-native candidate before generate-token attempt");

			candidate.getMatchingInterfaces().forEach(interf -> {
				if (interf.policy().endsWith(Constants.AUTHORIZATION_TOKEN_VARIANT_SUFFIX)) {
					scopeList.forEach(scope -> {
						requestPayloadEntries.add(new AuthorizationTokenGenerationMgmtRequestDTO(
								interf.policy(),
								AuthorizationTargetType.SERVICE_DEF.name(),
								Defaults.DEFAULT_CLOUD,
								form.getTargetSystemName(),
								candidate.getServiceInstance().provider().name(),
								candidate.getServiceInstance().serviceDefinition().name(),
								scope,
								null,
								null));
					});
				}
			});
		}

		if (Utilities.isEmpty(requestPayloadEntries)) {
			return;
		}

		final AuthorizationTokenMgmtListResponseDTO response = ahHttpService.consumeService(
				Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT,
				Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS,
				Constants.SYS_NAME_CONSUMER_AUTHORIZATION,
				AuthorizationTokenMgmtListResponseDTO.class,
				new AuthorizationTokenGenerationMgmtListRequestDTO(requestPayloadEntries),
				new LinkedMultiValueMap<>(Map.of(Constants.UNBOUND, List.of(Boolean.TRUE.toString()))));

		for (final AuthorizationTokenResponseDTO tokenResult : response.entries()) {
			for (final OrchestrationCandidate candidate : candidates) {
				if (candidate.getServiceInstance().provider().name().equals(tokenResult.provider())) {
					candidate.addAuthorizationToken(
							tokenResult.variant(),
							Utilities.isEmpty(tokenResult.scope()) ? tokenResult.target() : tokenResult.scope(),
							new AuthorizationTokenGenerationResponseDTO(tokenResult.tokenType(), tokenResult.targetType(), tokenResult.token(), tokenResult.usageLimit(), tokenResult.expiresAt()));
					break;
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void buildTranslationBridge(final String translationBridgeId, final String operation, final OrchestrationCandidate candidate) {
		logger.debug("buildTranslationBridge started...");
		Assert.isTrue(candidate.isNonNative(), "Candidate for translation bridge initiation is native");

		final TranslationNegotiationMgmtRequestDTO negotiationRequest = new TranslationNegotiationMgmtRequestDTO(translationBridgeId, candidate.getServiceInstance().instanceId());
		final TranslationNegotiationResponseDTO negotiationResponse = ahHttpService.consumeService(
				Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT,
				Constants.SERVICE_OP_NEGOTIATION,
				Constants.SYS_NAME_TRANSLATION_MANAGER,
				TranslationNegotiationResponseDTO.class,
				negotiationRequest);
		logger.debug("Translation bridge has been created with id of '" + negotiationResponse.bridgeId() + "' and with target interface '" + candidate.getTranslatableInterfaces().getFirst().templateName() + "'");

		Assert.isTrue(translationBridgeId.equals(negotiationResponse.bridgeId()), "Translation bridge ID is different in negotiation response");
		Assert.isTrue(Utilities.isEnumValue(negotiationResponse.bridgeInterface().policy(), ServiceInterfacePolicy.class), "Translation bridge interface policy should be a ServiceInterfacePolicy enum value");

		candidate.getMatchingInterfaces().clear(); // just for sure
		candidate.addMatchingInterface(negotiationResponse.bridgeInterface());

		candidate.addAuthorizationToken(
				negotiationResponse.bridgeInterface().policy(),
				operation,
				new AuthorizationTokenGenerationResponseDTO(
						AuthorizationTokenType.fromServiceInterfacePolicy(ServiceInterfacePolicy.valueOf(negotiationResponse.bridgeInterface().policy())),
						AuthorizationTargetType.SERVICE_DEF,
						translationBridgeId,
						negotiationResponse.tokenUsageLimit(),
						negotiationResponse.tokenExpiresAt()));
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO doInterCloudOrReturn(final UUID jobId, final OrchestrationForm form) {
		logger.debug("doInterCloudOrReturn started...");

		if (sysInfo.isIntercloudEnabled() && form.getFlag(OrchestrationFlag.ALLOW_INTERCLOUD)) {
			return interCloudOrch.doInterCloudServiceOrchestration(jobId, form);
		} else {
			orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, "No results were found");
			return new OrchestrationResponseDTO(List.of(), List.of());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationResponseDTO convertToOrchestrationResponse(final List<OrchestrationCandidate> candidates, final Set<String> warnings) {
		logger.debug("convertToOrchestrationResponse started...");

		final List<OrchestrationResultDTO> results = candidates.stream()
				.map(c -> new OrchestrationResultDTO(
						c.getServiceInstance().instanceId(),
						Defaults.DEFAULT_CLOUD, // local cloud
						c.getServiceInstance().provider().name(),
						c.getServiceInstance().serviceDefinition().name(),
						c.getServiceInstance().version(),
						c.getServiceInstance().expiresAt(),
						Utilities.convertZonedDateTimeToUTCString(c.getExclusiveUntil()),
						c.getServiceInstance().metadata(),
						c.getMatchingInterfaces(),
						c.getAuthorizationTokens()))
				.collect(Collectors.toList());

		return new OrchestrationResponseDTO(results, new ArrayList<>(warnings));
	}

	//-------------------------------------------------------------------------------------------------
	private Pair<Optional<String>, Optional<String>> extractInterfaceModelIDRequirements(final List<String> operations, final List<MetadataRequirementDTO> propRequirements) {
		logger.debug("extractInterfaceModelIDRequirements started...");

		if (Utilities.isEmpty(operations) || operations.size() > 1) {
			return Pair.of(Optional.empty(), Optional.empty());
		}

		final String operation = operations.getFirst();
		final String inputDataModelIdKey = Constants.PROPERTY_KEY_DATA_MODELS + Constants.DOT + operation + Constants.DOT + Constants.PROPERTY_KEY_INPUT;
		final String outputDataModelIdKey = Constants.PROPERTY_KEY_DATA_MODELS + Constants.DOT + operation + Constants.DOT + Constants.PROPERTY_KEY_OUTPUT;
		String inputDataModelId = null;
		String outputDataModelId = null;
		if (!Utilities.isEmpty(propRequirements)) {
			for (final MetadataRequirementDTO requirementBundle : propRequirements) {
				if (inputDataModelId != null && outputDataModelId != null) {
					break;
				}
				for (final MetadataRequirementExpression requirement : MetadataRequirementTokenizer.parseRequirements(requirementBundle)) {
					if (inputDataModelId == null && requirement.operation() == MetaOps.EQUALS && requirement.keyPath().equals(inputDataModelIdKey)) {
						inputDataModelId = dataModelIDNormalizer.normalize(requirement.value().toString());
					}
					if (outputDataModelId == null && requirement.operation() == MetaOps.EQUALS && requirement.keyPath().equals(outputDataModelIdKey)) {
						outputDataModelId = dataModelIDNormalizer.normalize(requirement.value().toString());
					}
				}
			}
		}

		try {
			dataModelIDValidator.validateDataModelIdentifier(inputDataModelId);
		} catch (final InvalidParameterException ex) {
			inputDataModelId = null;
		}

		try {
			dataModelIDValidator.validateDataModelIdentifier(outputDataModelId);
		} catch (final InvalidParameterException ex) {
			outputDataModelId = null;
		}

		return Pair.of(Optional.ofNullable(inputDataModelId), Optional.ofNullable(outputDataModelId));
	}

	//-------------------------------------------------------------------------------------------------
	private Pair<Optional<String>, Optional<String>> extractOfferedInterfaceModelID(final List<String> operations, final Map<String, Object> offeredInterfaceProps) {
		logger.debug("extractOfferedInterfaceModelID started...");

		if (Utilities.isEmpty(operations) || operations.size() > 1) {
			return Pair.of(Optional.empty(), Optional.empty());
		}

		final String operation = operations.getFirst();
		final Object inputIDObj = MetadataKeyEvaluator.getMetadataValueForCompositeKey(offeredInterfaceProps, Constants.PROPERTY_KEY_DATA_MODELS + Constants.DOT + operation + Constants.DOT + Constants.PROPERTY_KEY_INPUT);
		final Object outputIDObj = MetadataKeyEvaluator.getMetadataValueForCompositeKey(offeredInterfaceProps, Constants.PROPERTY_KEY_DATA_MODELS + Constants.DOT + operation + Constants.DOT + Constants.PROPERTY_KEY_OUTPUT);

		final String inputDataModelId = inputIDObj == null ? null : Utilities.isEmpty(inputIDObj.toString()) ? null : inputIDObj.toString();
		final String outputDataModelId = outputIDObj == null ? null : Utilities.isEmpty(outputIDObj.toString()) ? null : outputIDObj.toString();

		return Pair.of(Optional.ofNullable(inputDataModelId), Optional.ofNullable(outputDataModelId));
	}
}