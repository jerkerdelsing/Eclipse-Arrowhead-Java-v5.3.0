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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.common.service.validation.MetadataRequirementsMatcher;
import eu.arrowhead.common.service.validation.meta.MetaOps;
import eu.arrowhead.common.service.validation.meta.MetadataRequirementTokenizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationDataModelTranslatorInitializationResponseDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationInterfaceTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.translationmanager.TranslationManagerConstants;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService.AbortResult;
import eu.arrowhead.translationmanager.service.dto.DTOConverter;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationDiscoveryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;
import eu.arrowhead.translationmanager.service.matchmaking.DataModelTranslatorMatchmaker;
import eu.arrowhead.translationmanager.service.matchmaking.InterfaceTranslatorMatchmaker;

@Service
public class TranslatorBridgeEngine {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private CoreSystemsDriver csDriver;

	@Autowired
	private InterfaceTranslatorDriver itDriver;

	@Autowired
	private DataModelTranslatorFactoryDriver dmfDriver;

	@Autowired
	private DataModelIdentifierNormalizer dataModelIdentifierNormalizer;

	@Autowired
	private DataModelIdentifierValidator dataModelIdentifierValidator;

	@Autowired
	private InterfaceTranslatorMatchmaker interfaceTranslatorMatchmaker;

	@Autowired
	private DataModelTranslatorMatchmaker dataModelTranslatorMatchmaker;

	@Autowired
	private BridgeDbService dbService;

	@Autowired
	private DTOConverter converter;

	@Autowired
	private TranslationManagerSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TranslationDiscoveryResponseDTO doDiscovery(final NormalizedTranslationDiscoveryRequestDTO dto, final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags, final String origin) {
		logger.debug("doDiscovery started...");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(discoveryFlags, "discoveryFlags is null");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (discoveryFlags.getOrDefault(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK, false)
				&& csDriver.isBlacklisted(dto.consumer())) {
			// consumer is on the blacklist
			throw new ForbiddenException(dto.consumer() + " system is blacklisted", origin);
		}

		try {
			// removing all candidates that not appropriate as a translation target
			final List<NormalizedServiceInstanceDTO> candidates = filterCandidates(dto, discoveryFlags);
			if (candidates.isEmpty()) {
				return new TranslationDiscoveryResponseDTO(null, List.of());
			}

			// collecting interface translator candidates
			final List<ServiceInstanceResponseDTO> appropriateInterfaceTranslatorCandidates = collectAppropriateInterfaceTranslators(dto, discoveryFlags, candidates);
			if (appropriateInterfaceTranslatorCandidates.isEmpty()) {
				return new TranslationDiscoveryResponseDTO(null, List.of());
			}

			// create tokens (if necessary) for using interface translator's translationBridgeManagement service
			final Map<String, String> tokens = csDriver.generateTokenForManagerToInterfaceBridgeManagementService(appropriateInterfaceTranslatorCandidates);

			// create a structure that maps targets (their instanceId) to a list a pairs where each pair contains an interface translator and a list of target interfaces that have
			// every information to make possible to communicate with the target by the interface translator
			final Map<String, List<Pair<ServiceInstanceResponseDTO, List<String>>>> candidatesWithAppropriateInterfaceTranslators = calculateInterfaceTranslatorMap(
					appropriateInterfaceTranslatorCandidates,
					tokens,
					dto.operation(),
					candidates);
			if (candidatesWithAppropriateInterfaceTranslators.isEmpty()) {
				return new TranslationDiscoveryResponseDTO(null, List.of());
			}

			// creating initial discovery models
			List<TranslationDiscoveryModel> models = createDiscoveryModels(dto, candidates, candidatesWithAppropriateInterfaceTranslators, tokens);
			if (models.isEmpty()) {
				return new TranslationDiscoveryResponseDTO(null, List.of());
			}

			if (dto.inputDataModelId() != null || dto.outputDataModelId() != null) {
				// need to select data model translators as well

				// collecting data model translator candidates
				final List<ServiceInstanceResponseDTO> dataModelTranslatorCandidates = collectAppropriateDataModelTranslators(models, discoveryFlags);

				// we don't return if no candidate is available at this point because data model translator factories may create the necessary translators

				// adding data model translators to models
				models = extendDiscoveryModels(models, dataModelTranslatorCandidates, discoveryFlags);
				if (models.isEmpty()) {
					return new TranslationDiscoveryResponseDTO(null, List.of());
				}
			}

			final UUID bridgeId = UUID.randomUUID();
			dbService.storeBridgeDiscoveries(bridgeId, dto.createdBy(), models);

			return converter.convertDiscoveryModels(bridgeId, models);
		} catch (final AuthException ex) {
			throw new AuthException(ex.getMessage(), origin);
		} catch (final ForbiddenException ex) {
			throw new ForbiddenException(ex.getMessage(), origin);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			throw new ExternalServerError(ex.getMessage(), origin);
		} catch (final Exception ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public TranslationNegotiationResponseDTO doNegotiation(final UUID bridgeId, final String targetInstanceId, final String origin) {
		logger.debug("doNegotiation started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(targetInstanceId), "targetInstanceId is empty");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		boolean storeException = false;
		try {
			final Pair<TranslationDiscoveryModel, BridgeDetails> bridgePair = dbService.selectBridgeFromDiscoveries(bridgeId, targetInstanceId);
			storeException = true;
			final TranslationDiscoveryModel model = bridgePair.getFirst();
			final BridgeDetails detailsRecord = bridgePair.getSecond();

			// interface translator
			final Map<String, Object> interfaceTranslatorSettings = getCustomConfiguration(model.getInterfaceTranslator());
			detailsRecord.setInterfaceTranslatorData(Utilities.toJson(new TranslationInterfaceTranslationDataDescriptorDTO(
					model.getFromInterfaceTemplate(),
					model.getToInterfaceTemplate(),
					model.getInterfaceTranslatorToken(),
					model.getInterfaceTranslatorProperties(),
					interfaceTranslatorSettings)));

			// input data model translator
			Map<String, Object> inputTranslatorSettings = null;
			if (model.getInputDataModelIdRequirement() != null) {
				inputTranslatorSettings = handleInputDataModelTranslatorInNegotiation(model, detailsRecord);
			}

			// output data model translator
			Map<String, Object> outputTranslatorSettings = null;
			if (model.getOutputDataModelIdRequirement() != null) {
				outputTranslatorSettings = handleOutputDataModelTranslatorInNegotiation(model, detailsRecord);
			}

			// update details record
			boolean isBridgeInEndState = dbService.updateDetailsRecord(detailsRecord);
			if (isBridgeInEndState) {
				throw new InvalidParameterException("Bridge is already in an end state");
			}

			// token handling for target
			AuthorizationTokenResponseDTO tokenInfo = null;
			if (model.getTargetPolicy().endsWith(Constants.AUTHORIZATION_TOKEN_VARIANT_SUFFIX)) {
				tokenInfo = csDriver.generateTokenForInterfaceTranslatorToTargetOperation(
						model.getTargetPolicy(),
						model.getInterfaceTranslator(),
						ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId(model.getTargetInstanceId()),
						model.getServiceDefinition(),
						model.getOperation());
			}

			final Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> bridgeResult = itDriver.initializeBridge(
					bridgeId,
					model,
					tokenInfo != null ? tokenInfo.token() : null,
					interfaceTranslatorSettings,
					model.getInterfaceTranslatorToken(),
					inputTranslatorSettings,
					outputTranslatorSettings);

			if (bridgeResult.getSecond().isEmpty()) {
				// success

				if (!TranslationManagerConstants.POLICY_TRANSLATION_BRIDGE_TOKEN_AUTH
						.equals(bridgeResult.getFirst().get().policy().trim().toUpperCase())) {
					// policy is not match with the expected
					itDriver.abortBridge(bridgeId, model.getInterfaceTranslatorProperties(), model.getInterfaceTranslatorToken());

					throw new ExternalServerError("Interface provider returns with invalid data", origin);
				}

				isBridgeInEndState = dbService.bridgeInitialized(detailsRecord.getHeader());
				if (isBridgeInEndState) {
					itDriver.abortBridge(bridgeId, model.getInterfaceTranslatorProperties(), model.getInterfaceTranslatorToken());

					throw new InvalidParameterException("Bridge is already in an end state");
				}

				return new TranslationNegotiationResponseDTO(
						bridgeId.toString(),
						bridgeResult.getFirst().get(),
						tokenInfo != null ? tokenInfo.expiresAt() : null,
						tokenInfo != null ? tokenInfo.usageLimit() : null);
			}

			// error in bridge initialization
			throw bridgeResult.getSecond().get();
		} catch (final InvalidParameterException ex) {
			if (storeException) {
				dbService.storeBridgeProblem(bridgeId, ex.getMessage());
			}

			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final ExternalServerError ex) {
			// only thrown after storeException is set to true
			dbService.storeBridgeProblem(bridgeId, ex.getMessage());

			throw new ExternalServerError(ex.getMessage(), origin);
		} catch (final Exception ex) {
			if (storeException) {
				dbService.storeBridgeProblem(bridgeId, ex.getMessage());
			}

			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Boolean> doAbort(final List<UUID> bridgeIds, final String requester, final String origin) {
		logger.debug("doAbort started...");
		Assert.isTrue(!Utilities.isEmpty(bridgeIds), "bridge identifier list is empty");
		Assert.isTrue(!Utilities.containsNull(bridgeIds), "bridge identifier list contains null element");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final Map<String, Boolean> result = new HashMap<>(bridgeIds.size());

		try {
			bridgeIds.forEach(id -> {
				final AbortResult dbResult = dbService.abortBridge(id, requester);
				if (dbResult.abortHappened()) {
					// abort in this transaction
					result.put(id.toString(), true);
					if (dbResult.fromStatus().isActiveStatus()) {
						// need to tell the bridge to abort itself
						final TranslationInterfaceTranslationDataDescriptorDTO interfaceTranslationData = Utilities.fromJson(
								dbResult.detailsRecord().getInterfaceTranslatorData(),
								TranslationInterfaceTranslationDataDescriptorDTO.class);
						itDriver.abortBridge(id, interfaceTranslationData.interfaceProperties(), interfaceTranslationData.token());
					}
				} else {
					if (dbResult.fromStatus() == null) {
						// unknown bridge id
						result.put(id.toString(), false);
					} else if (dbResult.fromStatus().isEndStatus()) {
						// bridge is already in an end state
						result.put(id.toString(), true);
					} else {
						// bridge is not aborted
						result.put(id.toString(), false);
					}
				}
			});

			return result;
		} catch (final ForbiddenException ex) {
			throw new ForbiddenException(ex.getMessage(), origin);
		} catch (final Exception ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<NormalizedServiceInstanceDTO> filterCandidates(final NormalizedTranslationDiscoveryRequestDTO dto, final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags) {
		logger.debug("filterCandidates started...");

		List<NormalizedServiceInstanceDTO> candidates = filterOutNotAppropriateCandidates(dto);
		if (candidates.isEmpty()) {
			return List.of();
		}

		if (discoveryFlags.getOrDefault(TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK, false)) {
			// blacklist check for the remaining candidates
			final List<String> notBlacklistedProviders = csDriver.filterOutBlacklistedSystems(candidates
					.stream()
					.map(c -> c.provider())
					.toList());

			if (notBlacklistedProviders.isEmpty()) {
				return List.of();
			} else {
				candidates = candidates
						.stream()
						.filter(c -> notBlacklistedProviders.contains(c.provider()))
						.toList();
			}
		}

		if (discoveryFlags.getOrDefault(TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK, false)) {
			// authorization check that the consumer have access to providers' service/operation combination
			final List<String> allowedProviders = csDriver.filterOutProvidersBecauseOfUnauthorization(
					candidates.stream().map(c -> c.provider()).toList(),
					dto.consumer(),
					candidates.get(0).serviceDefinition(),
					dto.operation());

			if (allowedProviders.isEmpty()) {
				return List.of();
			} else {
				candidates = candidates
						.stream()
						.filter(c -> allowedProviders.contains(c.provider()))
						.toList();
			}
		}

		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private List<NormalizedServiceInstanceDTO> filterOutNotAppropriateCandidates(final NormalizedTranslationDiscoveryRequestDTO dto) {
		logger.debug("filterOutNotAppropriateCandidates started...");

		return dto.candidates()
				.stream()
				.map(c -> removeNotAppropriateInterfaces(c, dto.operation(), dto.interfaceTemplateNames(), dto.inputDataModelId(), dto.outputDataModelId()))
				.filter(c -> !c.interfaces().isEmpty())
				.toList();
	}

	//-------------------------------------------------------------------------------------------------
	private NormalizedServiceInstanceDTO removeNotAppropriateInterfaces(
			final NormalizedServiceInstanceDTO candidate,
			final String operation,
			final List<String> acceptedTemplateNames,
			final String inputDataModelId,
			final String outputDataModelId) {
		logger.debug("filterOutNotAppropriateCandidates started...");

		final List<ServiceInstanceInterfaceResponseDTO> goodInterfaces = new ArrayList<>();
		candidate.interfaces().forEach(intf -> {
			final ServiceInstanceInterfaceResponseDTO nIntf = getNormalizedInterfaceIfAppropriate(intf, operation, acceptedTemplateNames, inputDataModelId, outputDataModelId);
			if (nIntf != null) {
				goodInterfaces.add(nIntf);
			}
		});

		return new NormalizedServiceInstanceDTO(
				candidate.instanceId(),
				candidate.provider(),
				candidate.serviceDefinition(),
				goodInterfaces);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceInterfaceResponseDTO getNormalizedInterfaceIfAppropriate(
			final ServiceInstanceInterfaceResponseDTO intf,
			final String operation,
			final List<String> acceptedTemplateNames,
			final String inputDataModelIdReq,
			final String outputDataModelIdReq) {
		logger.debug("getNormalizedInterfaceIfAppropriate started...");

		try {
			String specifiedInputDataModel = null;
			String specifiedOutputDataModel = null;

			if (intf.properties().containsKey(Constants.PROPERTY_KEY_DATA_MODELS)
					&& (intf.properties().get(Constants.PROPERTY_KEY_DATA_MODELS) instanceof final Map dataModels)
					&& dataModels.containsKey(operation)
					&& (dataModels.get(operation) instanceof final Map operationDmMap)) {

				if (operationDmMap.containsKey(Constants.PROPERTY_KEY_INPUT)
						&& operationDmMap.get(Constants.PROPERTY_KEY_INPUT) != null) {
					specifiedInputDataModel = operationDmMap.get(Constants.PROPERTY_KEY_INPUT).toString();
					specifiedInputDataModel = dataModelIdentifierNormalizer.normalize(specifiedInputDataModel);
					dataModelIdentifierValidator.validateDataModelIdentifier(specifiedInputDataModel);
				}

				if (operationDmMap.containsKey(Constants.PROPERTY_KEY_OUTPUT)
						&& operationDmMap.get(Constants.PROPERTY_KEY_OUTPUT) != null) {
					specifiedOutputDataModel = operationDmMap.get(Constants.PROPERTY_KEY_OUTPUT).toString();
					specifiedOutputDataModel = dataModelIdentifierNormalizer.normalize(specifiedOutputDataModel);
					dataModelIdentifierValidator.validateDataModelIdentifier(specifiedOutputDataModel);
				}
			}

			if (Objects.equals(inputDataModelIdReq, specifiedInputDataModel)
					&& Objects.equals(outputDataModelIdReq, specifiedOutputDataModel)
					&& acceptedTemplateNames.contains(intf.templateName())) {
				// this means there is no need for translation at all (should not happen) => this interface is not appropriate for translation
				return null;
			}

			if ((inputDataModelIdReq == null && specifiedInputDataModel != null)
					|| (inputDataModelIdReq != null && specifiedInputDataModel == null)
					|| (outputDataModelIdReq == null && specifiedOutputDataModel != null)
					|| (outputDataModelIdReq != null && specifiedOutputDataModel == null)) {
				// these are the cases when translation is not possible because of missing properties (or there are properties that shouldn't be)
				// 1. no input data model according to the consumer => there is input according to the provider
				// 2. input data model according to the consumer => no input according to the provider
				// 3. no output data model according to the consumer => there is output according to the provider
				// 4. output data model according to the consumer => no output according to the provider
				return null;
			}

			final Map<String, Object> normalizedProps = shallowCopyUnrelatedProperties(intf.properties());
			final Map<String, Object> valueMap = new HashMap<>(2);
			valueMap.put(Constants.PROPERTY_KEY_INPUT, specifiedInputDataModel);
			valueMap.put(Constants.PROPERTY_KEY_OUTPUT, specifiedOutputDataModel);
			normalizedProps.put(Constants.PROPERTY_KEY_DATA_MODELS, Map.of(operation, valueMap));

			return new ServiceInstanceInterfaceResponseDTO(
					intf.templateName(),
					intf.protocol(),
					intf.policy(),
					normalizedProps);
		} catch (final InvalidParameterException __) {
			logger.debug("Invalid data model indentifier in one of the candidates");
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> shallowCopyUnrelatedProperties(final Map<String, Object> original) {
		logger.debug("shallowCopyUnrelatedProperties started...");

		final Map<String, Object> result = new HashMap<>(original.size());
		original.forEach((k, v) -> {
			if (!k.equals(Constants.PROPERTY_KEY_DATA_MODELS)) {
				result.put(k, v);
			}
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private List<ServiceInstanceResponseDTO> collectAppropriateInterfaceTranslators(
			final NormalizedTranslationDiscoveryRequestDTO dto,
			final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags,
			final List<NormalizedServiceInstanceDTO> candidates) {
		logger.debug("collectAppropriateInterfaceTranslators started...");

		List<ServiceInstanceResponseDTO> interfaceTranslatorCandidates = csDriver.collectInterfaceTranslatorCandidates(dto.interfaceTemplateNames(), candidates);
		if (interfaceTranslatorCandidates.isEmpty()) {
			return List.of();
		}

		if (discoveryFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false)) {
			// blacklist check for the interface translator candidates
			final List<String> notBlacklistedInterfaceTranslators = csDriver.filterOutBlacklistedSystems(interfaceTranslatorCandidates
					.stream()
					.map(c -> c.provider().name())
					.toList());

			if (notBlacklistedInterfaceTranslators.isEmpty()) {
				return List.of();
			} else {
				interfaceTranslatorCandidates = interfaceTranslatorCandidates
						.stream()
						.filter(c -> notBlacklistedInterfaceTranslators.contains(c.provider().name()))
						.toList();
			}
		}

		if (discoveryFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false)) {
			// authorization check that the TranslationManager has access to interface translators' service
			final List<String> allowedInterfaceTranslators = csDriver.filterOutProvidersBecauseOfUnauthorization(
					interfaceTranslatorCandidates
							.stream()
							.map(c -> c.provider().name())
							.toList(),
					sysInfo.getSystemName(),
					Constants.SERVICE_DEF_INTERFACE_BRIDGE_MANAGEMENT,
					null);

			if (allowedInterfaceTranslators.isEmpty()) {
				return List.of();
			} else {
				interfaceTranslatorCandidates = interfaceTranslatorCandidates
						.stream()
						.filter(c -> allowedInterfaceTranslators.contains(c.provider().name()))
						.toList();
			}
		}

		// authorization check for the interface translators (regarding the consumer) are not necessary, because they "create" a new service/operation for the consumer
		// and the consumer with the related bridge identifier can use it without any further authorization

		return interfaceTranslatorCandidates;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private Map<String, List<Pair<ServiceInstanceResponseDTO, List<String>>>> calculateInterfaceTranslatorMap(
			final List<ServiceInstanceResponseDTO> interfaceTranslators,
			final Map<String, String> tokens,
			final String targetOperation,
			final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("calculateInterfaceTranslatorMap started...");

		final Map<String, List<Pair<ServiceInstanceResponseDTO, List<String>>>> result = new HashMap<>(targets.size());
		interfaceTranslators.forEach(itp -> {
			final Map<String, Object> interfaceBridge = (Map<String, Object>) itp.metadata().get(Constants.METADATA_KEY_INTERFACE_BRIDGE);
			final String toInterface = interfaceBridge.get(Constants.METADATA_KEY_TO).toString();

			List<NormalizedServiceInstanceDTO> relatedTargets = targets
					.stream()
					.filter(t -> t.interfaces()
							.stream()
							.map(intf -> intf.templateName())
							.toList()
							.contains(toInterface))
					.toList();

			relatedTargets = itDriver.filterOutNotAppropriateTargetsForInterfaceTranslator(itp, tokens.get(itp.provider().name()), targetOperation, relatedTargets);
			relatedTargets.forEach(t -> {
				if (!result.containsKey(t.instanceId())) {
					result.put(t.instanceId(), new ArrayList<>());
				}

				result.get(t.instanceId())
						.add(Pair.of(
								itp,
								t.interfaces()
										.stream()
										.map(intf -> intf.templateName())
										.toList()));
			});
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private List<TranslationDiscoveryModel> createDiscoveryModels(
			final NormalizedTranslationDiscoveryRequestDTO dto,
			final List<NormalizedServiceInstanceDTO> candidates,
			final Map<String, List<Pair<ServiceInstanceResponseDTO, List<String>>>> candidatesWithAppropriateTranslators,
			final Map<String, String> tokens) {
		logger.debug("createDiscoveryModels started...");

		final List<TranslationDiscoveryModel> models = new ArrayList<>(candidates.size());
		candidates.forEach(c -> {
			final Pair<ServiceInstanceResponseDTO, String> selected = selectInterfaceTranslator(candidatesWithAppropriateTranslators.get(c.instanceId()), dto.interfaceTemplateNames());
			// selected contains the interface translator and one common interface template name with the consumer
			if (selected != null) {
				final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
						c.instanceId(),
						c.provider(),
						c.serviceDefinition(),
						dto.operation(),
						dto.consumer(),
						dto.inputDataModelId(),
						dto.outputDataModelId());
				model.setFromInterfaceTemplate(selected.getSecond());
				model.setToInterfaceTemplate(((Map) selected.getFirst().metadata().get(Constants.METADATA_KEY_INTERFACE_BRIDGE)).get(Constants.METADATA_KEY_TO).toString());
				model.setInterfaceTranslator(selected.getFirst().provider().name());
				final ServiceInstanceInterfaceResponseDTO interfaceDTO = selected.getFirst().interfaces().get(0);
				model.setInterfaceTranslatorPolicy(interfaceDTO.policy());
				model.setInterfaceTranslatorToken(tokens.get(model.getInterfaceTranslator()));
				model.setInterfaceTranslatorProperties(interfaceDTO.properties());

				final ServiceInstanceInterfaceResponseDTO selectedTargetInterface = c.interfaces()
						.stream()
						.filter(intf -> intf.templateName().equals(model.getToInterfaceTemplate()))
						.findFirst()
						.get();
				model.setTargetPolicy(selectedTargetInterface.policy());
				final Map<String, Object> toInterfaceProperties = selectedTargetInterface.properties();
				model.setTargetProperties(toInterfaceProperties);

				if (dto.inputDataModelId() != null) {
					final String targetInputDataModelId = getTargetDataModelId(toInterfaceProperties, dto.operation(), true);
					if (targetInputDataModelId == null) {
						// something is not OK in the target's data model specification (should not happen, already checked in a previous method)
						throw new InvalidParameterException("Input data model identifier is not found in target " + c.instanceId() + " and interface " + model.getToInterfaceTemplate());
					}
					model.setTargetInputDataModelId(targetInputDataModelId);
				}

				if (dto.outputDataModelId() != null) {
					final String targetOutputDataModelId = getTargetDataModelId(toInterfaceProperties, dto.operation(), false);
					if (targetOutputDataModelId == null) {
						// something is not OK in the target's data model specification (should not happen, already checked in a previous method)
						throw new InvalidParameterException("Output data model identifier is not found in target " + c.instanceId() + " and interface " + model.getToInterfaceTemplate());
					}
					model.setTargetOutputDataModelId(targetOutputDataModelId);
				}

				models.add(model);
			}
		});

		return models;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String getTargetDataModelId(final Map<String, Object> targetInterfaceProperties, final String operation, final boolean input) {
		logger.debug("getTargetDataModelId started...");

		final Map<String, Object> operationDmMap = (Map<String, Object>) ((Map) targetInterfaceProperties.getOrDefault(Constants.PROPERTY_KEY_DATA_MODELS, Map.of()))
				.getOrDefault(operation, Map.of());
		final Object dmIdObj = input ? operationDmMap.get(Constants.PROPERTY_KEY_INPUT) : operationDmMap.get(Constants.PROPERTY_KEY_OUTPUT);

		return dmIdObj == null ? null : dmIdObj.toString();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private Pair<ServiceInstanceResponseDTO, String> selectInterfaceTranslator(
			final List<Pair<ServiceInstanceResponseDTO, List<String>>> interfaceTranslatorCandidates,
			final List<String> consumerInterfaces) {
		logger.debug("selectInterfaceTranslator started...");

		// collect acceptable interface translators
		final List<ServiceInstanceResponseDTO> acceptableInterfaceTranslators = interfaceTranslatorCandidates
				.stream()
				.filter((p -> {
					final MetadataRequirementDTO req = new MetadataRequirementDTO();
					req.put(String.join(Constants.DOT, Constants.METADATA_KEY_INTERFACE_BRIDGE, Constants.METADATA_KEY_TO), Map.of(
							MetadataRequirementTokenizer.OP, MetaOps.IN.name(),
							MetadataRequirementTokenizer.VALUE, p.getSecond()));
					return MetadataRequirementsMatcher.isMetadataMatch(p.getFirst().metadata(), req);
				}))
				.map(p -> p.getFirst())
				.toList();

		// select one interface translator
		final ServiceInstanceResponseDTO selected = interfaceTranslatorMatchmaker.doMatchmaking(acceptableInterfaceTranslators, Map.of());
		if (selected == null) {
			return null;
		}

		// select one interface between consumer and the selected interface translator
		final Map<String, Object> bridgeMetadata = (Map<String, Object>) selected.metadata().get(Constants.METADATA_KEY_INTERFACE_BRIDGE);
		final List<String> fromMetadata = (List<String>) bridgeMetadata.get(Constants.METADATA_KEY_FROM);
		final List<String> commonInterfaces = new ArrayList<>(consumerInterfaces);
		commonInterfaces.retainAll(fromMetadata);

		if (commonInterfaces.isEmpty()) {
			// should not happen
			return null;
		}

		return Pair.of(selected, commonInterfaces.get(0));
	}

	//-------------------------------------------------------------------------------------------------
	private List<ServiceInstanceResponseDTO> collectAppropriateDataModelTranslators(
			final List<TranslationDiscoveryModel> discoveryModels,
			final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags) {
		logger.debug("collectAppropriateDataModelTranslators started...");

		List<ServiceInstanceResponseDTO> dataModelTranslatorCandidates = csDriver.collectDataModelTranslatorCandidates(discoveryModels);
		if (dataModelTranslatorCandidates.isEmpty()) {
			return List.of();
		}

		if (discoveryFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK, false)) {
			// blacklist check for the data model translator candidates
			final List<String> notBlacklistedDataModelTranslators = csDriver.filterOutBlacklistedSystems(dataModelTranslatorCandidates
					.stream()
					.map(c -> c.provider().name())
					.toList());

			if (notBlacklistedDataModelTranslators.isEmpty()) {
				return List.of();
			} else {
				dataModelTranslatorCandidates = dataModelTranslatorCandidates
						.stream()
						.filter(c -> notBlacklistedDataModelTranslators.contains(c.provider().name()))
						.toList();
			}
		}

		// authorization checks can't be done here, because they depends on the selected interface translator (which is the consumer in this case)

		return dataModelTranslatorCandidates;
	}

	//-------------------------------------------------------------------------------------------------
	private List<TranslationDiscoveryModel> extendDiscoveryModels(
			final List<TranslationDiscoveryModel> models,
			final List<ServiceInstanceResponseDTO> dataModelTranslatorCandidates,
			final Map<TranslationDiscoveryFlag, Boolean> discoveryFlags) {
		logger.debug("extendDiscoveryModels started...");

		final List<TranslationDiscoveryModel> result = new ArrayList<>(models.size());
		models.forEach(m -> {
			boolean keep = true;
			if (m.getInputDataModelIdRequirement() != null) {
				// need a data model translator for input translation
				boolean isFactory = false;
				ServiceInstanceResponseDTO selectedInputDataModelTranslator = selectDataModelTranslator(
						m,
						dataModelTranslatorCandidates,
						discoveryFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false),
						true);

				if (selectedInputDataModelTranslator == null) {
					// select a data model factory if possible
					selectedInputDataModelTranslator = selectDataModelTranslatorFactoryIfPossible(m.getInputDataModelIdRequirement(), m.getTargetInputDataModelId());
					isFactory = true;
				}

				if (selectedInputDataModelTranslator == null) {
					keep = false;
				} else {
					m.setInputDataModelTranslator(selectedInputDataModelTranslator.provider().name());
					m.setInputDataModelTranslatorProperties(selectedInputDataModelTranslator.interfaces().get(0).properties());
					m.setInputDataModelTranslatorFactory(isFactory);
				}
			}

			if (keep && m.getOutputDataModelIdRequirement() != null) {
				// need a data model translator for result translation
				boolean isFactory = false;
				ServiceInstanceResponseDTO selectedOutputDataModelTranslator = selectDataModelTranslator(
						m,
						dataModelTranslatorCandidates,
						discoveryFlags.getOrDefault(TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK, false),
						false);

				if (selectedOutputDataModelTranslator == null) {
					// select a data model factory if possible
					selectedOutputDataModelTranslator = selectDataModelTranslatorFactoryIfPossible(m.getTargetOutputDataModelId(), m.getOutputDataModelIdRequirement());
					isFactory = true;
				}

				if (selectedOutputDataModelTranslator == null) {
					keep = false;
				} else {
					m.setOutputDataModelTranslator(selectedOutputDataModelTranslator.provider().name());
					m.setOutputDataModelTranslatorProperties(selectedOutputDataModelTranslator.interfaces().get(0).properties());
					m.setOutputDataModelTranslatorFactory(isFactory);
				}
			}

			if (keep) {
				result.add(m);
			}
		});

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO selectDataModelTranslator(
			final TranslationDiscoveryModel model,
			final List<ServiceInstanceResponseDTO> dataModelTranslatorCandidates,
			final boolean authorizationCheck,
			final boolean input) {
		logger.debug("selectDataModelTranslator started...");

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		if (input) {
			final List<String> translationDirection = List.of(model.getInputDataModelIdRequirement(), model.getTargetInputDataModelId());
			req.put(Constants.METADATA_KEY_DATA_MODEL_IDS, Map.of(
					MetadataRequirementTokenizer.OP, MetaOps.CONTAINS.name(),
					MetadataRequirementTokenizer.VALUE, translationDirection));
		} else {
			final List<String> translationDirection = List.of(model.getTargetOutputDataModelId(), model.getOutputDataModelIdRequirement());
			req.put(Constants.METADATA_KEY_DATA_MODEL_IDS, Map.of(
					MetadataRequirementTokenizer.OP, MetaOps.CONTAINS.name(),
					MetadataRequirementTokenizer.VALUE, translationDirection));
		}

		List<ServiceInstanceResponseDTO> relatedTranslators = dataModelTranslatorCandidates
				.stream()
				.filter(dmc -> MetadataRequirementsMatcher.isMetadataMatch(dmc.metadata(), req))
				.toList();

		if (relatedTranslators.isEmpty()) {
			return null;
		}

		if (authorizationCheck) {
			// authorization check that the interface translator have access to data model translators' service
			final List<String> allowedTranslators = csDriver.filterOutProvidersBecauseOfUnauthorization(
					relatedTranslators.stream().map(c -> c.provider().name()).toList(),
					model.getInterfaceTranslator(),
					Constants.SERVICE_DEF_DATA_MODEL_TRANSLATION,
					null);

			if (allowedTranslators.isEmpty()) {
				logger.warn("Check the data model translators in the local cloud, because some of them are not accessible by the interface translator {}", model.getInterfaceTranslator());
				return null;
			}

			relatedTranslators = relatedTranslators
					.stream()
					.filter(c -> allowedTranslators.contains(c.provider().name()))
					.toList();
		}

		return dataModelTranslatorMatchmaker.doMatchmaking(relatedTranslators, Map.of());
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO selectDataModelTranslatorFactoryIfPossible(final String from, final String to) {
		logger.debug("selectDataModelTranslatorFactoryIfPossible started...");

		final List<ServiceInstanceResponseDTO> dmtfCandidates = csDriver.collectDataModelTranslatorFactoryCandidates();
		if (Utilities.isEmpty(dmtfCandidates)) {
			// no available factories

			return null;
		}

		// shuffle to give every factory instance an "equal" chance
		Collections.shuffle(dmtfCandidates);

		for (final ServiceInstanceResponseDTO factory : dmtfCandidates) {
			final boolean supported = dmfDriver.isFactorySupportsTranslation(
					factory.provider().name(),
					factory.interfaces().get(0).properties(),
					from,
					to);

			if (supported) {
				return factory;
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> getCustomConfiguration(final String translatorName) {
		logger.debug("getCustomConfiguration started...");

		return sysInfo.isCustomConfigurationEnabled()
				? csDriver.getConfigurationForSystem(translatorName)
				: null;
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> handleInputDataModelTranslatorInNegotiation(final TranslationDiscoveryModel model, final BridgeDetails detailsRecord) {
		logger.debug("handleInputDataModelTranslatorInNegotiation started...");

		Map<String, Object> inputDataModelTranslatorSettings = null;
		if (model.isInputDataModelTranslatorFactory()) { // asking the factory to initialize a data model translator
			final TranslationDataModelTranslatorInitializationResponseDTO initResponse = dmfDriver.initializeDataModelTranslator(
					model.getInputDataModelTranslator(),
					model.getInputDataModelTranslatorProperties(),
					model.getInputDataModelIdRequirement(),
					model.getTargetInputDataModelId());

			model.setInputDataModelTranslator(initResponse.dataModelTranslatorName());
			model.setInputDataModelTranslatorFactory(false);
			model.setInputDataModelTranslatorProperties(initResponse.interfaceProperties());
			detailsRecord.setInputDmTranslator(initResponse.dataModelTranslatorName());
			detailsRecord.setInputDmTranslatorData(Utilities.toJson(new TranslationDataModelTranslationDataDescriptorDTO(
					model.getInputDataModelIdRequirement(),
					model.getTargetInputDataModelId(),
					initResponse.interfaceProperties(),
					null))); // settings should be handled by the factory
		} else {
			inputDataModelTranslatorSettings = getCustomConfiguration(model.getInputDataModelTranslator());
			detailsRecord.setInputDmTranslatorData(Utilities.toJson(new TranslationDataModelTranslationDataDescriptorDTO(
					model.getInputDataModelIdRequirement(),
					model.getTargetInputDataModelId(),
					model.getInputDataModelTranslatorProperties(),
					inputDataModelTranslatorSettings)));

		}

		return inputDataModelTranslatorSettings;
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> handleOutputDataModelTranslatorInNegotiation(final TranslationDiscoveryModel model, final BridgeDetails detailsRecord) {
		logger.debug("handleOutputDataModelTranslatorInNegotiation started...");

		Map<String, Object> outputDataModelTranslatorSettings = null;
		if (model.isOutputDataModelTranslatorFactory()) { // asking the factory to initialize a data model translator
			final TranslationDataModelTranslatorInitializationResponseDTO initResponse = dmfDriver.initializeDataModelTranslator(
					model.getOutputDataModelTranslator(),
					model.getOutputDataModelTranslatorProperties(),
					model.getTargetOutputDataModelId(),
					model.getOutputDataModelIdRequirement());

			model.setOutputDataModelTranslator(initResponse.dataModelTranslatorName());
			model.setOutputDataModelTranslatorFactory(false);
			model.setOutputDataModelTranslatorProperties(initResponse.interfaceProperties());
			detailsRecord.setResultDmTranslator(initResponse.dataModelTranslatorName());
			detailsRecord.setResultDmTranslatorData(Utilities.toJson(new TranslationDataModelTranslationDataDescriptorDTO(
					model.getTargetOutputDataModelId(),
					model.getOutputDataModelIdRequirement(),
					initResponse.interfaceProperties(),
					null))); // settings should be handled by the factory
		} else {
			outputDataModelTranslatorSettings = getCustomConfiguration(model.getOutputDataModelTranslator());
			detailsRecord.setResultDmTranslatorData(Utilities.toJson(new TranslationDataModelTranslationDataDescriptorDTO(
					model.getTargetOutputDataModelId(),
					model.getOutputDataModelIdRequirement(),
					model.getOutputDataModelTranslatorProperties(),
					outputDataModelTranslatorSettings)));

		}

		return outputDataModelTranslatorSettings;
	}
}