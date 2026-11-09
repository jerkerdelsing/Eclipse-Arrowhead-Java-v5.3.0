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
package eu.arrowhead.common.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import jakarta.annotation.PostConstruct;

@Service
public class ArrowheadHttpService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceCollector collector;

	@Autowired
	private HttpService httpService;

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private ServiceOperationNameNormalizer operationNameNormalizer;

	private String templateName;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public <T, P> T consumeService(
			final String serviceDefinition,
			final String operation,
			final String providerName,
			final Class<T> responseType,
			final P payload,
			final MultiValueMap<String, String> queryParams,
			final List<String> pathParams, // in order
			final Map<String, String> customHeaders) {
		logger.debug("consumeService started...");

		if (Utilities.isEmpty(serviceDefinition)) {
			throw new InvalidParameterException("Service definition is not specified");
		}

		if (Utilities.isEmpty(operation)) {
			throw new InvalidParameterException("Service operation is not specified");
		}

		final String nOperation = operationNameNormalizer.normalize(operation);
		final ServiceModel model = collector.getServiceModel(serviceDefinition, templateName, providerName, nOperation);
		if (model == null) {
			throw new DataNotFoundException("Service definition is not found: " + serviceDefinition);
		}

		final HttpInterfaceModel interfaceModel = (HttpInterfaceModel) model.interfaces().get(0);

		final HttpOperationModel operationModel = interfaceModel.operations().get(nOperation);
		if (operationModel == null) {
			throw new ExternalServerError("Service does not define the specified operation");
		}

		final Map<String, String> actualHeaders = new HashMap<>();
		if (customHeaders != null) {
			actualHeaders.putAll(customHeaders);
		}

		final String authorizationHeader = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		if (authorizationHeader != null) {
			actualHeaders.put(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}

		final String[] pathSegments = pathParams == null ? null : pathParams.toArray(String[]::new);
		final UriComponents uri = HttpUtilities.createURI(
				interfaceModel.protocol(),
				interfaceModel.accessAddresses().get(0),
				interfaceModel.accessPort(),
				queryParams,
				interfaceModel.basePath() + operationModel.path(),
				pathSegments);

		return httpService.sendRequest(uri, HttpMethod.valueOf(operationModel.method()), responseType, payload, null, actualHeaders);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T consumeService(final String serviceDefinition, final String operation, final String providerName, final Class<T> responseType, final P payload) {
		return consumeService(serviceDefinition, operation, providerName, responseType, payload, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T consumeService(final String serviceDefinition, final String operation, final Class<T> responseType, final P payload) {
		return consumeService(serviceDefinition, operation, null, responseType, payload, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T consumeService(final String serviceDefinition, final String operation, final String providerName, final Class<T> responseType, final P payload, final MultiValueMap<String, String> queryParams) {
		return consumeService(serviceDefinition, operation, providerName, responseType, payload, queryParams, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T consumeService(final String serviceDefinition, final String operation, final Class<T> responseType, final P payload, final MultiValueMap<String, String> queryParams) {
		return consumeService(serviceDefinition, operation, null, responseType, payload, queryParams, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T consumeService(final String serviceDefinition, final String operation, final String providerName, final Class<T> responseType, final MultiValueMap<String, String> queryParams) {
		return consumeService(serviceDefinition, operation, providerName, responseType, null, queryParams, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T consumeService(final String serviceDefinition, final String operation, final Class<T> responseType, final MultiValueMap<String, String> queryParams) {
		return consumeService(serviceDefinition, operation, null, responseType, null, queryParams, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T consumeService(final String serviceDefinition, final String operation, final String providerName, final Class<T> responseType) {
		return consumeService(serviceDefinition, operation, providerName, responseType, null, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T consumeService(final String serviceDefinition, final String operation, final Class<T> responseType) {
		return consumeService(serviceDefinition, operation, null, responseType, null, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	// path params in order
	public <T> T consumeService(final String serviceDefinition, final String operation, final String providerName, final Class<T> responseType, final List<String> pathParams) {
		return consumeService(serviceDefinition, operation, providerName, responseType, null, null, pathParams, null);
	}

	//-------------------------------------------------------------------------------------------------
	// path parameters in order
	public <T> T consumeService(final String serviceDefinition, final String operation, final Class<T> responseType, final List<String> pathParams) {
		return consumeService(serviceDefinition, operation, null, responseType, null, null, pathParams, null);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
	}
}