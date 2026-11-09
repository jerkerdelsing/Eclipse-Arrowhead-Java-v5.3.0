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
package eu.arrowhead.common.collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.common.intf.properties.validators.NotEmptyStringSetValidator;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import jakarta.annotation.Nullable;

public class HttpCollectorDriver implements ICollectorDriver {

	//=================================================================================================
	// members
	private static final String SR_LOOKUP_PATH = "/serviceregistry/service-discovery/lookup";
	private static final String VERBOSE_KEY = "verbose";
	private static final String VERBOSE_VALUE = "false";

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(Constants.$HTTP_COLLECTOR_MODE_WD)
	private HttpCollectorMode mode;

	@Value(Constants.$HTTP_COLLECTOR_BOOTSTRAP_SERVICES_WD)
	private String[] bootstrapServiceNames;

	private List<String> bootstrapServices = List.of();

	@Value(Constants.$HTTP_COLLECTOR_ORCHESTRATION_TTL_WD)
	private long orchestrationTimeToLive; // seconds

	@Autowired
	private HttpService httpService;

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private PropertyValidators validators;

	private final List<String> supportedInterfaces = List.of(
			Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME,
			Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME,
			Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME,
			Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME);

	private ServiceModel orchestrationCache = null;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void init() throws ArrowheadException {
		logger.debug("HttpCollectorDriver.init started...");

		bootstrapServices = bootstrapServiceNames == null ? List.of() : Arrays.stream(bootstrapServiceNames).map(String::trim).filter(s -> !s.isEmpty()).toList();
		logger.info("Service collector mode: {}{}", mode, HttpCollectorMode.ORCHESTRATION == mode ? ", ServiceRegistry lookup only for " + bootstrapServices : "");
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	@Nullable
	public ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName) throws ArrowheadException {
		return acquireService(serviceDefinitionName, interfaceTemplateName, providerName, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	@Nullable
	public ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName, final String operation) throws ArrowheadException {
		logger.debug("acquireService started...");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinitionName), "service definition is empty");

		if (!supportedInterfaces.contains(interfaceTemplateName)) {
			throw new InvalidParameterException("This collector only supports the following interfaces: " + String.join(", ", supportedInterfaces));
		}

		if (isOrchestrated(serviceDefinitionName)) {
			// no ServiceRegistry fallback: the provider is the one the orchestrator selects and authorizes for this system
			return acquireServiceFromOrchestration(serviceDefinitionName, interfaceTemplateName, providerName, operation);
		}

		ServiceModel result = acquireServiceFromSR(serviceDefinitionName, interfaceTemplateName, providerName);
		if (result == null && HttpCollectorMode.SR_AND_ORCH == mode) {
			result = acquireServiceFromOrchestration(serviceDefinitionName, interfaceTemplateName, providerName, null);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public long cacheTimeToLiveMillis(final String serviceDefinitionName) {
		return isOrchestrated(serviceDefinitionName) ? Math.max(orchestrationTimeToLive, 0) * 1000 : 0;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceModel acquireServiceFromSR(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName) {
		logger.debug("acquireServiceFromSR started...");

		// 1. uri
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final UriComponents uri = HttpUtilities.createURI(scheme, sysInfo.getServiceRegistryAddress(), sysInfo.getServiceRegistryPort(), SR_LOOKUP_PATH, VERBOSE_KEY, VERBOSE_VALUE);

		// 2. payload
		final ServiceInstanceLookupRequestDTO payload = providerName == null
				? createSRRequestPayload(serviceDefinitionName, interfaceTemplateName)
				: createSRRequestPayload(serviceDefinitionName, interfaceTemplateName, providerName);

		// 3. headers
		final String authorizationHeader = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		final Map<String, String> headers = new HashMap<>();
		if (authorizationHeader != null) {
			headers.put(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}

		final ServiceInstanceListResponseDTO response = httpService.sendRequest(uri, HttpMethod.POST, ServiceInstanceListResponseDTO.class, payload, null, headers);

		return convertLookupResponse(response, interfaceTemplateName);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isOrchestrated(final String serviceDefinitionName) {
		return HttpCollectorMode.ORCHESTRATION == mode && !bootstrapServices.contains(serviceDefinitionName);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel acquireServiceFromOrchestration(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName, final String operation) {
		logger.debug("acquireServiceFromOrchestration started...");

		// if no orchestration service is cached, it will lookup for it first
		if (orchestrationCache == null) {
			lookupAndCacheOrchestration();
			if (orchestrationCache == null) {
				logger.debug("Lookup and cache orchestration service was unsuccessful");
				return null;
			}
		}

		// orchestrate the service

		// 1. finding the corresponding interface
		final String intfTemplateNameToSendRequest = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final HttpInterfaceModel orchestrationIntf = (HttpInterfaceModel) orchestrationCache
				.interfaces()
				.stream()
				.filter(i -> i.templateName().equals(intfTemplateNameToSendRequest))
				.findFirst()
				.get();

		// 2. uri
		final String orchestrationPullPath = orchestrationIntf.operations().get(Constants.SERVICE_OP_ORCHESTRATION_PULL).path();
		final String scheme = sysInfo.isSslEnabled() ? Constants.HTTPS : Constants.HTTP;
		final UriComponents uri = HttpUtilities.createURI(
				scheme,
				orchestrationIntf.accessAddresses().getFirst(),
				orchestrationIntf.accessPort(),
				orchestrationIntf.basePath() + orchestrationPullPath);

		// 3. method
		final HttpMethod orchestrationPullMethod = HttpMethod.valueOf(orchestrationIntf.operations().get(Constants.SERVICE_OP_ORCHESTRATION_PULL).method());

		// 4. payload
		final OrchestrationRequestDTO payload = providerName == null ? createOrchRequestPayload(serviceDefinitionName, interfaceTemplateName, null, operation, false)
				: createOrchRequestPayload(serviceDefinitionName, interfaceTemplateName, providerName, operation, true);

		// 5. headers
		final String authorizationHeader = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		final Map<String, String> headers = new HashMap<>();
		if (authorizationHeader != null) {
			headers.put(HttpHeaders.AUTHORIZATION, authorizationHeader);
		}

		final OrchestrationResponseDTO response = httpService.sendRequest(uri, orchestrationPullMethod, OrchestrationResponseDTO.class, payload, null, headers);
		final ServiceModel result = convertPullResponse(response, interfaceTemplateName);
		if (result == null) {
			logger.warn("Orchestration found no authorized provider of {}{}", serviceDefinitionName, operation == null ? "" : " (" + operation + ")");
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private void lookupAndCacheOrchestration() {
		logger.debug("lookupAndCacheOrchestration started...");

		final String intfTemplateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		// try to lookup for dynamic orchestration service
		orchestrationCache = acquireServiceFromSR(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION, intfTemplateName, Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION);

		// if unsuccessful, try to lookup for flexible store orchestration service
		if (orchestrationCache == null) {
			orchestrationCache = acquireServiceFromSR(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION, intfTemplateName, Constants.SYS_NAME_FLEXIBLE_SERVICE_ORCHESTRATION);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO createSRRequestPayload(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName) {
		logger.debug("createSRRequestPayload started...");

		return new ServiceInstanceLookupRequestDTO.Builder()
				.providerName(providerName)
				.serviceDefinitionName(serviceDefinitionName)
				.interfaceTemplateName(interfaceTemplateName)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceLookupRequestDTO createSRRequestPayload(final String serviceDefinitionName, final String interfaceTemplateName) {
		logger.debug("createSRRequestPayload started...");

		return new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName(serviceDefinitionName)
				.interfaceTemplateName(interfaceTemplateName)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationRequestDTO createOrchRequestPayload(
			final String serviceDefinitionName,
			final String interfaceTemplateName,
			final String providerName,
			final String operation,
			final boolean onlyPreferred) {
		logger.debug("createOrchRequestPayload started...");

		final OrchestrationServiceRequirementDTO.Builder serviceRequirement = new OrchestrationServiceRequirementDTO.Builder()
				.serviceDefinition(serviceDefinitionName)
				.interfaceTemplateName(interfaceTemplateName);
		if (providerName != null) {
			serviceRequirement.preferredProvider(providerName);
		}
		if (!Utilities.isEmpty(operation)) {
			// the orchestrator then also checks that this system is authorized for the operation
			serviceRequirement.operation(operation);
		}

		final Map<String, Boolean> flags = new HashMap<>(Map.of(
				OrchestrationFlag.MATCHMAKING.toString(), true,
				OrchestrationFlag.ALLOW_INTERCLOUD.toString(), false,
				OrchestrationFlag.ALLOW_TRANSLATION.toString(), false));
		if (onlyPreferred) {
			flags.put(OrchestrationFlag.ONLY_PREFERRED.toString(), true);
		}

		return new OrchestrationRequestDTO.Builder()
				.serviceRequirement(serviceRequirement.build())
				.orchestrationFlags(flags)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel convertLookupResponse(final ServiceInstanceListResponseDTO response, final String interfaceTemplateName) {
		logger.debug("convertLookupResponse started...");

		if (response.entries().isEmpty()) {
			return null;
		}

		// convert the first instance
		final ServiceInstanceResponseDTO instance = response.entries().getFirst();

		// create the list of interface models for the service model
		final List<InterfaceModel> interfaceModelList = convertInterfaceResponsesToInterfaceModels(instance.interfaces(), interfaceTemplateName);

		if (interfaceModelList.isEmpty()) {
			return null;
		}

		// build the service model
		return new ServiceModel.Builder()
				.serviceDefinition(instance.serviceDefinition().name())
				.version(instance.version())
				.metadata(instance.metadata())
				.serviceInterfaces(interfaceModelList)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel convertPullResponse(final OrchestrationResponseDTO response, final String interfaceTemplateName) {
		logger.debug("convertPullResponse started...");

		if (response.results().isEmpty()) {
			return null;
		}

		// convert the first instance
		final OrchestrationResultDTO instance = response.results().getFirst();

		// create the list of interface models for the service model
		final List<InterfaceModel> interfaceModelList = convertInterfaceResponsesToInterfaceModels(instance.interfaces(), interfaceTemplateName);

		if (interfaceModelList.isEmpty()) {
			return null;
		}

		// build the service model
		return new ServiceModel.Builder()
				.serviceDefinition(instance.serviceDefinitition())
				.version(instance.version())
				.metadata(instance.metadata())
				.serviceInterfaces(interfaceModelList)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private List<InterfaceModel> convertInterfaceResponsesToInterfaceModels(final List<ServiceInstanceInterfaceResponseDTO> interfaces, final String interfaceTemplateName) {
		logger.debug("convertInterfaceResponsesToInterfaceModels started...");

		final List<InterfaceModel> interfaceModelList = new ArrayList<>();
		for (final ServiceInstanceInterfaceResponseDTO interf : interfaces) {

			final String templateName = interf.templateName();
			final Map<String, Object> properties = interf.properties();

			if (!interfaceTemplateName.equals(templateName)) {
				continue;
			}

			// HTTP or HTTPS
			if (templateName.contains(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME)) {
				final HttpInterfaceModel model = createHttpInterfaceModel(templateName, properties);
				if (model != null) {
					interfaceModelList.add(model);
				}
			}

			// MQTT or MQTTS
			if (templateName.contains(Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME)) {
				final MqttInterfaceModel model = createMqttInterfaceModel(templateName, properties);
				if (model != null) {
					interfaceModelList.add(model);
				}
			}
		}

		return interfaceModelList;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private HttpInterfaceModel createHttpInterfaceModel(final String templateName, final Map<String, Object> properties) {

		// access addresses
		final List<String> accessAddresses = (List<String>) properties.get(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES);

		// access port
		final int accessPort = (int) properties.get(HttpInterfaceModel.PROP_NAME_ACCESS_PORT);

		// base path
		final String basePath = (String) properties.get(HttpInterfaceModel.PROP_NAME_BASE_PATH);

		// operations
		if (!properties.containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)) {
			return null;
		}

		final Map<String, HttpOperationModel> operations = (Map<String, HttpOperationModel>) validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)
				.validateAndNormalize(properties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS));

		// create the interface model
		final HttpInterfaceModel model = new HttpInterfaceModel.Builder(templateName)
				.accessAddresses(accessAddresses)
				.accessPort(accessPort)
				.basePath(basePath)
				.operations(operations)
				.build();

		return model;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private MqttInterfaceModel createMqttInterfaceModel(final String templateName, final Map<String, Object> properties) {

		// access addresses
		final List<String> accessAddresses = (List<String>) properties.get(MqttInterfaceModel.PROP_NAME_ACCESS_ADDRESSES);

		// access port
		final int accessPort = (int) properties.get(MqttInterfaceModel.PROP_NAME_ACCESS_PORT);

		// topic
		final String topic = (String) properties.get(MqttInterfaceModel.PROP_NAME_BASE_TOPIC);

		// operations
		if (!properties.containsKey(MqttInterfaceModel.PROP_NAME_OPERATIONS)) {
			return null;
		}

		final Set<String> operations = (Set<String>) validators.getValidator(PropertyValidatorType.NOT_EMPTY_STRING_SET)
				.validateAndNormalize(properties.get(MqttInterfaceModel.PROP_NAME_OPERATIONS), NotEmptyStringSetValidator.ARG_OPERATION);

		// create the interface model
		final MqttInterfaceModel model = new MqttInterfaceModel.Builder(templateName)
				.accessAddresses(accessAddresses)
				.accessPort(accessPort)
				.baseTopic(topic)
				.operations(operations)
				.build();

		return model;
	}
}