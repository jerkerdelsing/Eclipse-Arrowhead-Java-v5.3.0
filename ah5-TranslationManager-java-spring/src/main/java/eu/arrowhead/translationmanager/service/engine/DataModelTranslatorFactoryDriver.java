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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.dto.DataModelFactoryTranslatorInitiaizationResponseDTO;
import eu.arrowhead.dto.DataModelTranslatorFactoryRequestDTO;
import eu.arrowhead.dto.TranslationDataModelTranslatorInitializationResponseDTO;

@Service
public class DataModelTranslatorFactoryDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private HttpService httpService;

	@Autowired
	private PropertyValidators validators;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public boolean isFactorySupportsTranslation(
			final String factoryName,
			final Map<String, Object> factoryInterfaceProperties,
			final String fromDataModelId,
			final String toDataModelId) {
		logger.debug("isFactorySupportsTranslation started...");
		Assert.isTrue(!Utilities.isEmpty(factoryName), "Factory name is missing");
		Assert.isTrue(!Utilities.isEmpty(factoryInterfaceProperties), "Factory interface properties is missing");
		Assert.isTrue(!Utilities.isEmpty(fromDataModelId), "From data model identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(toDataModelId), "To data model identifier is missing");

		HttpMethod method = HttpMethod.GET; // default method
		String operationPath = "/check"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (factoryInterfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_FACTORY_CHECK)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_FACTORY_CHECK);
			try {
				final HttpOperationModel operationModel = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(operationModel.method());
				operationPath = operationModel.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for data model translator factory {}", factoryName);
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);
		final DataModelTranslatorFactoryRequestDTO payload = calculatePayload(fromDataModelId, toDataModelId);

		try {
			final Boolean response = httpService.sendRequest(
					uri,
					method,
					Boolean.class,
					payload);

			return response == null ? false : response.booleanValue();
		} catch (final ArrowheadException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);

			return false;
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public TranslationDataModelTranslatorInitializationResponseDTO initializeDataModelTranslator(
			final String factoryName,
			final Map<String, Object> factoryInterfaceProperties,
			final String fromDataModelId,
			final String toDataModelId) throws ExternalServerError {
		logger.debug("initializeDataModelTranslator started...");
		Assert.isTrue(!Utilities.isEmpty(factoryName), "Factory name is missing");
		Assert.isTrue(!Utilities.isEmpty(factoryInterfaceProperties), "Factory interface properties is missing");
		Assert.isTrue(!Utilities.isEmpty(fromDataModelId), "From data model identifier is missing");
		Assert.isTrue(!Utilities.isEmpty(toDataModelId), "To data model identifier is missing");

		HttpMethod method = HttpMethod.POST; // default method
		String operationPath = "/initialize-translator"; // default path
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final String host = ((List<String>) factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)).get(0);
		final int port = (int) factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
		final String basePath = factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH).toString();
		if (factoryInterfaceProperties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)
				&& (factoryInterfaceProperties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS) instanceof final Map operationsMap)
				&& operationsMap.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_FACTORY_INITIALIZE)) {
			final Object value = operationsMap.get(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_FACTORY_INITIALIZE);
			try {
				final HttpOperationModel operationModel = Utilities.fromJson(Utilities.toJson(value), HttpOperationModel.class);
				method = HttpMethod.valueOf(operationModel.method());
				operationPath = operationModel.path();
			} catch (final ArrowheadException ex) {
				logger.warn("Invalid operations property for data model translator factory {}", factoryName);
			}
		}

		final UriComponents uri = HttpUtilities.createURI(scheme, host, port, basePath + operationPath);
		final DataModelTranslatorFactoryRequestDTO payload = calculatePayload(fromDataModelId, toDataModelId);

		final DataModelFactoryTranslatorInitiaizationResponseDTO response = httpService.sendRequest(
				uri,
				method,
				DataModelFactoryTranslatorInitiaizationResponseDTO.class,
				payload);

		return validateAndNormalizeResponse(response);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private DataModelTranslatorFactoryRequestDTO calculatePayload(final String fromDataModelId, final String toDataModelId) {
		logger.debug("calculateCheckPayload started...");

		return new DataModelTranslatorFactoryRequestDTO(fromDataModelId, toDataModelId);
	}

	//-------------------------------------------------------------------------------------------------
	private TranslationDataModelTranslatorInitializationResponseDTO validateAndNormalizeResponse(final DataModelFactoryTranslatorInitiaizationResponseDTO response) {
		logger.debug("validateResponse started...");

		if (response == null) {
			throw new ExternalServerError("missing response");
		}

		if (Utilities.isEmpty(response.dataModelTranslatorName())) {
			throw new ExternalServerError("data model translator name is missing");
		}

		if (Utilities.isEmpty(response.interfaceProperties())) {
			throw new ExternalServerError("data model translator interface properties is missing");
		}

		final Map<String, Object> normalizedProperties = new HashMap<>(response.interfaceProperties().size());

		try {
			// access addresses
			if (!response.interfaceProperties().containsKey(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)) {
				throw new ExternalServerError("Missing property: " + HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES);
			}

			final IPropertyValidator addressesValidator = validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
			normalizedProperties.put(
					HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES,
					addressesValidator.validateAndNormalize(response.interfaceProperties().get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES)));

			// access port
			if (!response.interfaceProperties().containsKey(HttpInterfaceModel.PROP_NAME_ACCESS_PORT)) {
				throw new ExternalServerError("Missing property: " + HttpInterfaceModel.PROP_NAME_ACCESS_PORT);
			}

			final IPropertyValidator portValidator = validators.getValidator(PropertyValidatorType.PORT);
			normalizedProperties.put(
					HttpInterfaceModel.PROP_NAME_ACCESS_PORT,
					portValidator.validateAndNormalize(response.interfaceProperties().get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT)));

			// base path
			if (!response.interfaceProperties().containsKey(HttpInterfaceModel.PROP_NAME_BASE_PATH)) {
				throw new ExternalServerError("Missing property: " + HttpInterfaceModel.PROP_NAME_BASE_PATH);
			}

			if (response.interfaceProperties().get(HttpInterfaceModel.PROP_NAME_BASE_PATH) instanceof final String basePath) {
				normalizedProperties.put(
						HttpInterfaceModel.PROP_NAME_BASE_PATH,
						basePath.trim());
			} else {
				throw new ExternalServerError("Invalid property: " + HttpInterfaceModel.PROP_NAME_BASE_PATH);
			}

			// operations
			if (!response.interfaceProperties().containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)) {
				throw new ExternalServerError("Missing property: " + HttpInterfaceModel.PROP_NAME_OPERATIONS);
			}

			final IPropertyValidator operationsValidator = validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS);
			@SuppressWarnings("unchecked")
			final Map<String, HttpOperationModel> normalizedOperations = (Map<String, HttpOperationModel>) operationsValidator
					.validateAndNormalize(response.interfaceProperties().get(HttpInterfaceModel.PROP_NAME_OPERATIONS));

			if (!normalizedOperations.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_INIT_TRANSLATION)) {
				throw new ExternalServerError("Missing operation: " + Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_INIT_TRANSLATION);
			}

			if (!normalizedOperations.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_GET_TRANSLATION_RESULT)) {
				throw new ExternalServerError("Missing operation: " + Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_GET_TRANSLATION_RESULT);
			}

			if (!normalizedOperations.containsKey(Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_ABORT_TRANSLATION)) {
				throw new ExternalServerError("Missing operation: " + Constants.SERVICE_OP_DATA_MODEL_TRANSLATOR_ABORT_TRANSLATION);
			}

			normalizedProperties.put(HttpInterfaceModel.PROP_NAME_OPERATIONS, normalizedOperations);
		} catch (final InvalidParameterException ex) {
			throw new ExternalServerError(ex.getMessage());
		}

		return new TranslationDataModelTranslatorInitializationResponseDTO(response.dataModelTranslatorName(), normalizedProperties);
	}
}