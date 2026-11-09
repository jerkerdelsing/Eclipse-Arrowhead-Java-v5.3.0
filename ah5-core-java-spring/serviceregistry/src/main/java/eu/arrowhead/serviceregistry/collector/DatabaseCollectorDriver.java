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
package eu.arrowhead.serviceregistry.collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.validators.HttpOperationsValidator;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstance;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInstanceInterface;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInstanceDbService;
import eu.arrowhead.serviceregistry.service.model.ServiceLookupFilterModel;

public class DatabaseCollectorDriver implements ICollectorDriver {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Autowired
	private ServiceInstanceDbService instanceDbService;

	@Autowired
	private HttpOperationsValidator httpOperationsValidator;

	private final List<String> supportedInterfaces = List.of(
			Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME,
			Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME,
			Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME,
			Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void init() throws ArrowheadException {
		logger.debug("DatabaseCollectorDriver.init started...");
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName) throws ArrowheadException {
		logger.debug("DatabaseCollectorDriver.acquireService started: service definition: {}, interface template: {}", serviceDefinitionName, interfaceTemplateName);
		Assert.isTrue(!Utilities.isEmpty(serviceDefinitionName), "service definition is empty");

		if (!supportedInterfaces.contains(interfaceTemplateName)) {
			throw new InvalidParameterException("This collector only supports the following interfaces: " + String.join(", ", supportedInterfaces));
		}

		// get the service instance entries from the database
		final Page<Map.Entry<ServiceInstance, List<ServiceInstanceInterface>>> instanceEntries = getInstanceEntries(serviceDefinitionName, interfaceTemplateName);
		if (instanceEntries.isEmpty()) {
			return null;
		}

		// only the first instance or the first matching system name entry will be returned
		ServiceInstance instance = null;
		List<ServiceInstanceInterface> interfaces = null;
		if (Utilities.isEmpty(providerName)) {
			instance = instanceEntries.getContent().getFirst().getKey();
			interfaces = instanceEntries.getContent().getFirst().getValue();
		} else {
			final List<Entry<ServiceInstance, List<ServiceInstanceInterface>>> matchingProvider = instanceEntries.stream()
					.filter(ie -> ie.getKey().getSystem().getName().equalsIgnoreCase(providerName))
					.toList();
			if (!Utilities.isEmpty(matchingProvider)) {
				instance = matchingProvider.getFirst().getKey();
				interfaces = matchingProvider.getFirst().getValue();
			}
		}

		if (instance == null) {
			return null;
		}

		// create the list of interface models
		final List<InterfaceModel> interfaceModelList = new ArrayList<>();
		for (final ServiceInstanceInterface interf : interfaces) {
			final String templateName = interf.getServiceInterfaceTemplate().getName();

			if (!interfaceTemplateName.equals(templateName)) {
				continue;
			}

			final Map<String, Object> properties = Utilities.fromJson(interf.getProperties(), new TypeReference<Map<String, Object>>() {
			});

			// HTTP or HTTPS
			if (templateName.equals(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME) || templateName.equals(Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME)) {
				final HttpInterfaceModel model = createHttpInterfaceModel(templateName, properties);
				if (model != null) {
					interfaceModelList.add(model);
				}
			}

			// MQTT or MQTTS
			if (templateName.equals(Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME) || templateName.equals(Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME)) {
				final MqttInterfaceModel model = createMqttInterfaceModel(templateName, properties);
				if (model != null) {
					interfaceModelList.add(model);
				}
			}
		}

		if (interfaceModelList.isEmpty()) {
			return null;
		}

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition(instance.getServiceDefinition().getName())
				.version(instance.getVersion())
				.serviceInterfaces(interfaceModelList)
				.metadata(Utilities.fromJson(instance.getMetadata(), new TypeReference<Map<String, Object>>() {
				}))
				.build();

		return serviceModel;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Page<Map.Entry<ServiceInstance, List<ServiceInstanceInterface>>> getInstanceEntries(final String serviceDefinitionName, final String interfaceTemplateName) {

		// normalize and validate
		final String nServiceDefinitionName = serviceDefNameNormalizer.normalize(serviceDefinitionName);
		serviceDefNameValidator.validateServiceDefinitionName(nServiceDefinitionName);

		final String nInterfaceTemplateName = interfaceTemplateNameNormalizer.normalize(interfaceTemplateName);
		interfaceTemplateNameValidator.validateInterfaceTemplateName(nInterfaceTemplateName);

		final PageRequest pagination = PageRequest.of(0, 1, Direction.DESC, ServiceInstance.DEFAULT_SORT_FIELD);

		final ServiceLookupFilterModel filterModel = new ServiceLookupFilterModel(
				new ServiceInstanceLookupRequestDTO.Builder()
						.serviceDefinitionName(nServiceDefinitionName)
						.interfaceTemplateName(nInterfaceTemplateName)
						.build());

		// get the instances from the database
		return instanceDbService.getPageByFilters(pagination, filterModel);
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

		final Map<String, HttpOperationModel> operations = (Map<String, HttpOperationModel>) httpOperationsValidator.validateAndNormalize(properties.get(HttpInterfaceModel.PROP_NAME_OPERATIONS));

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

		// base topic
		final String baseTopic = (String) properties.get(MqttInterfaceModel.PROP_NAME_BASE_TOPIC);

		// operations
		if (!properties.containsKey(MqttInterfaceModel.PROP_NAME_OPERATIONS)) {
			return null;
		}

		final Set<String> operations = new HashSet<String>((Collection<? extends String>) properties.get(MqttInterfaceModel.PROP_NAME_OPERATIONS));

		// create the interface model
		final MqttInterfaceModel model = new MqttInterfaceModel.Builder(templateName)
				.accessAddresses(accessAddresses)
				.accessPort(accessPort)
				.baseTopic(baseTopic)
				.operations(operations)
				.build();

		return model;
	}
}