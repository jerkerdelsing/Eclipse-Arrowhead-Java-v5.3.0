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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Service
public class ServiceCollector {

	//=================================================================================================
	// members

	private static final String KEY_SEPARATOR = "|";

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final Map<String, Long> expiries = new ConcurrentHashMap<>();

	@Autowired
	private ICollectorDriver driver;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public ServiceModel getServiceModel(final String serviceDefinition, final String templateName, final String providerName) throws ArrowheadException {
		return getServiceModel(serviceDefinition, templateName, providerName, null);
	}

	//-------------------------------------------------------------------------------------------------
	// operation: the operation the caller is going to consume, so that an orchestrated provider is authorized for it
	@Nullable
	public ServiceModel getServiceModel(final String serviceDefinition, final String templateName, final String providerName, final String operation) throws ArrowheadException {
		logger.debug("getServiceModel started...");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "service definition is empty");
		Assert.isTrue(!Utilities.isEmpty(templateName), "template name is empty");

		final String nServiceDefinition = serviceDefNameNormalizer.normalize(serviceDefinition);
		final String nProviderName = !Utilities.isEmpty(providerName) ? systemNameNormalizer.normalize(providerName) : null;
		final long timeToLive = driver.cacheTimeToLiveMillis(nServiceDefinition);

		// an orchestration result is specific to the requested provider and operation and expires; a lookup result is kept per service
		final String key = timeToLive > 0
				? Constants.KEY_PREFIX_FOR_SERVICE_MODEL + nServiceDefinition + KEY_SEPARATOR + Objects.toString(nProviderName, "") + KEY_SEPARATOR + Objects.toString(operation, "")
				: Constants.KEY_PREFIX_FOR_SERVICE_MODEL + nServiceDefinition;
		final Long expiresAt = expiries.get(key);
		if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
			arrowheadContext.remove(key);
			expiries.remove(key);
		}

		if (!arrowheadContext.containsKey(key)) {
			final String nTemplateName = interfaceTemplateNameNormalizer.normalize(templateName);

			final ServiceModel model = driver.acquireService(nServiceDefinition, nTemplateName, nProviderName, operation);
			if (model != null) {
				arrowheadContext.put(key, model);
				if (timeToLive > 0) {
					expiries.put(key, System.currentTimeMillis() + timeToLive);
				}
			}
		}

		return (ServiceModel) arrowheadContext.get(key);
	}

	//-------------------------------------------------------------------------------------------------
	// forgets every acquired model of the service, so the next call looks it up or orchestrates it again
	public void forget(final String serviceDefinition) {
		logger.debug("forget started...");

		final String key = Constants.KEY_PREFIX_FOR_SERVICE_MODEL + serviceDefNameNormalizer.normalize(serviceDefinition);
		arrowheadContext.keySet().removeIf(k -> k.equals(key) || k.startsWith(key + KEY_SEPARATOR));
		expiries.keySet().removeIf(k -> k.startsWith(key + KEY_SEPARATOR));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() throws ArrowheadException {
		driver.init();
	}
}