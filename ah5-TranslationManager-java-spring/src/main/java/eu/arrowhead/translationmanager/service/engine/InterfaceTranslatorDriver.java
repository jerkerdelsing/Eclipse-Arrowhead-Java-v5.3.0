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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.service.util.ServiceInstanceIdParts;
import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.TranslationBridgeInitializationRequestDTO;
import eu.arrowhead.dto.TranslationCheckTargetsRequestDTO;
import eu.arrowhead.dto.TranslationCheckTargetsResponseDTO;
import eu.arrowhead.dto.TranslationDataModelTranslationDataDescriptorDTO;
import eu.arrowhead.dto.TranslationTargetDTO;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@Service
public class InterfaceTranslatorDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private TranslationManagerSystemInfo sysInfo;

	@Autowired
	private HttpService httpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public List<NormalizedServiceInstanceDTO> filterOutNotAppropriateTargetsForInterfaceTranslator(
			final ServiceInstanceResponseDTO interfaceTranslator,
			final String token,
			final String targetOperation,
			final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("filterOutNotAppropriateTargetsForInterfaceTranslator started...");
		Assert.notNull(interfaceTranslator, "interfaceTranslator is missing");
		Assert.isTrue(!Utilities.isEmpty(targetOperation), "targetOperation is missing");
		Assert.isTrue(!Utilities.isEmpty(targets), "targets list is missing");
		Assert.isTrue(!Utilities.containsNull(targets), "targets list contains null element");

		HttpMethod method = HttpMethod.POST; // default method
		String operationPath = "/check-targets"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final Map<String, Object> interfaceProperties = interfaceTranslator.interfaces().get(0).properties();
		final String host = ((List<String>) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = interfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (interfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (interfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_CHECK_TARGETS)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_CHECK_TARGETS);
			try {
				final HttpOperationModel model = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(model.method());
				operationPath = model.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for interface translator {}", interfaceTranslator.provider().name());
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);
		final TranslationCheckTargetsRequestDTO payload = calculateCheckTargetsPayload(targetOperation, targets);
		final Map<String, String> headers = new HashMap<>(1);
		if (!Utilities.isEmpty(token)) {
			headers.put(HttpHeaders.AUTHORIZATION, Constants.AUTHORIZATION_SCHEMA + " " + token);
		}

		try {
			final TranslationCheckTargetsResponseDTO response = httpService.sendRequest(
					uri,
					method,
					TranslationCheckTargetsResponseDTO.class,
					payload,
					null,
					headers);

			return getCheckedTargetsFromResponse(response);
		} catch (final ArrowheadException ex) {
			// can't use this interface translator instance => removing it from candidates by returning empty target list
			return List.of();
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Pair<Optional<ServiceInstanceInterfaceResponseDTO>, Optional<ArrowheadException>> initializeBridge(
			final UUID bridgeId,
			final TranslationDiscoveryModel model,
			final String targetToken,
			final Map<String, Object> interfaceTranslatorSettings,
			final String interfaceTranslatorToken,
			final Map<String, Object> inputDataModelTranslatorSettings,
			final Map<String, Object> outputDataModelTranslatorSettings) {
		logger.debug("initializeBridge started...");
		Assert.notNull(bridgeId, "bridgeId is missing");
		Assert.notNull(model, "model is missing");

		HttpMethod method = HttpMethod.POST; // default method
		String operationPath = "/initialize-bridge"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final Map<String, Object> interfaceProperties = model.getInterfaceTranslatorProperties();
		final String host = ((List<String>) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = interfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (interfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (interfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_INIT_BRIDGE)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_INIT_BRIDGE);
			try {
				final HttpOperationModel operationModel = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(operationModel.method());
				operationPath = operationModel.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for interface translator {}", model.getInterfaceTranslator());
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);
		final TranslationBridgeInitializationRequestDTO payload = calculateBridgeInitializationPayload(
				bridgeId,
				model,
				targetToken,
				interfaceTranslatorSettings,
				inputDataModelTranslatorSettings,
				outputDataModelTranslatorSettings);
		final Map<String, String> headers = new HashMap<>(1);
		if (!Utilities.isEmpty(interfaceTranslatorToken)) {
			headers.put(HttpHeaders.AUTHORIZATION, Constants.AUTHORIZATION_SCHEMA + " " + interfaceTranslatorToken);
		}

		try {
			final ServiceInstanceInterfaceResponseDTO response = httpService.sendRequest(
					uri,
					method,
					ServiceInstanceInterfaceResponseDTO.class,
					payload,
					null,
					headers);

			return Pair.of(Optional.of(response), Optional.empty());
		} catch (final ArrowheadException ex) {
			// some error happens during bridge initialization
			return Pair.of(Optional.empty(), Optional.of(ex));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void abortBridge(final UUID bridgeId, final Map<String, Object> interfaceProperties, final String interfaceTranslatorToken) {
		logger.debug("abortBridge started...");
		Assert.notNull(bridgeId, "bridgeId is missing");
		Assert.isTrue(!Utilities.isEmpty(interfaceProperties), "interfaceProperties is missing");

		HttpMethod method = HttpMethod.DELETE; // default method
		String operationPath = "/abort-bridge"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) interfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = interfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (interfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (interfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_ABORT_BRIDGE)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_INTERFACE_TRANSLATOR_ABORT_BRIDGE);
			try {
				final HttpOperationModel operationModel = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(operationModel.method());
				operationPath = operationModel.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for interface translator");
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath + "/" + bridgeId.toString());
		final Map<String, String> headers = new HashMap<>(1);
		if (!Utilities.isEmpty(interfaceTranslatorToken)) {
			headers.put(HttpHeaders.AUTHORIZATION, Constants.AUTHORIZATION_SCHEMA + " " + interfaceTranslatorToken);
		}

		try {
			httpService.sendRequest(
					uri,
					method,
					headers,
					Void.TYPE);
		} catch (final ArrowheadException ex) {
			// some error happens during bridge abortion, cannot do anything
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TranslationCheckTargetsRequestDTO calculateCheckTargetsPayload(final String targetOperation, final List<NormalizedServiceInstanceDTO> targets) {
		logger.debug("calculateCheckTargetsPayload started...");

		return new TranslationCheckTargetsRequestDTO(
				targetOperation,
				targets
					.stream()
					.map(t -> new TranslationTargetDTO(t.instanceId(), t.interfaces()))
					.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private List<NormalizedServiceInstanceDTO> getCheckedTargetsFromResponse(final TranslationCheckTargetsResponseDTO response) {
		logger.debug("getCheckedTargetsFromResponse started...");

		if (response == null || Utilities.isEmpty(response.targets())) {
			return List.of();
		}

		return response.targets()
				.stream()
				.map(t -> {
					final ServiceInstanceIdParts parts = ServiceInstanceIdUtils.breakDownInstanceId(t.instanceId());
					return new NormalizedServiceInstanceDTO(
							t.instanceId(),
							parts.systemName(),
							parts.serviceDefinition(),
							t.interfaces());
				}).toList();
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationBridgeInitializationRequestDTO calculateBridgeInitializationPayload(
			final UUID bridgeId,
			final TranslationDiscoveryModel model,
			final String token,
			final Map<String, Object> interfaceTranslatorSettings,
			final Map<String, Object> inputDataModelTranslatorSettings,
			final Map<String, Object> outputDataModelTranslatorSettings) {
		logger.debug("calculateBridgeInitializationPayload started...");

		final TranslationDataModelTranslationDataDescriptorDTO inputDataModelTranslator = model.getInputDataModelIdRequirement() == null
				? null
				: new TranslationDataModelTranslationDataDescriptorDTO(
						model.getInputDataModelIdRequirement(),
						model.getTargetInputDataModelId(),
						model.getInputDataModelTranslatorProperties(),
						inputDataModelTranslatorSettings);

		final TranslationDataModelTranslationDataDescriptorDTO resultDataModelTranslator = model.getOutputDataModelIdRequirement() == null
				? null
				: new TranslationDataModelTranslationDataDescriptorDTO(
						model.getTargetOutputDataModelId(),
						model.getOutputDataModelIdRequirement(),
						model.getOutputDataModelTranslatorProperties(),
						outputDataModelTranslatorSettings);

		return new TranslationBridgeInitializationRequestDTO(
				bridgeId.toString(),
				model.getFromInterfaceTemplate(),
				inputDataModelTranslator,
				resultDataModelTranslator,
				model.getInputDataModelIdRequirement(),
				model.getOutputDataModelIdRequirement(),
				model.getToInterfaceTemplate(),
				model.getTargetProperties(),
				model.getOperation(),
				token,
				interfaceTranslatorSettings);
	}
}