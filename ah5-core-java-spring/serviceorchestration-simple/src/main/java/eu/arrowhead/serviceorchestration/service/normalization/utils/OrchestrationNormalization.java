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

package eu.arrowhead.serviceorchestration.service.normalization.utils;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrchestrationNormalization {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void normalizePull(final SimpleOrchestrationRequest toNormalize) {
		logger.debug("normalizePull started...");

		normalizeOrchestrationRequest(toNormalize);
	}

	//-------------------------------------------------------------------------------------------------
	public void normalizeSubscribe(final SimpleOrchestrationSubscriptionRequest toNormalize) {
		logger.debug("normalizeSubscribe started...");

		// target system name
		toNormalize.setTargetSystemName(Utilities.isEmpty(toNormalize.getTargetSystemName()) ? null : systemNameNormalizer.normalize(toNormalize.getTargetSystemName()));

		// orchestration request
		normalizeOrchestrationRequest(toNormalize.getOrchestrationRequest());

		// notify interface
		final String normalizedProtocol = toNormalize.getNotifyInterface().protocol().trim().toUpperCase();

		final Map<String, String> normalizedNotifyProps = new HashMap<>();
		toNormalize.getNotifyInterface().properties().forEach((k, v) -> {
			normalizedNotifyProps.put(k.trim().toLowerCase(), v.trim());
		});

		toNormalize.setNotifyInterface(new OrchestrationNotifyInterfaceDTO(normalizedProtocol, normalizedNotifyProps));
	}

	//-------------------------------------------------------------------------------------------------
	public UUID normalizeUUID(final String uuid) {
		logger.debug("normalizeUUID started...");

		return UUID.fromString(uuid.trim());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void normalizeOrchestrationRequest(final SimpleOrchestrationRequest toNormalize) {
		logger.debug("normalizeOrchestrationRequest started...");

		// service definition
		toNormalize.setServiceDefinition(serviceDefNameNormalizer.normalize(toNormalize.getServiceDefinition()));


		// preferred providers
		if (!Utilities.isEmpty(toNormalize.getPreferredProviders())) {
			toNormalize.setPreferredProviders(toNormalize.getPreferredProviders().stream().map(p -> systemNameNormalizer.normalize(p)).collect(Collectors.toList()));
		}

		// orchestration flags
		if (!Utilities.isEmpty(toNormalize.getOrchestrationFlags())) {
			final Map<String, Boolean> normalizedFlags = new HashMap<>();
			toNormalize.getOrchestrationFlags().forEach((k, v) -> normalizedFlags.put(k.trim().toUpperCase(), v));
			toNormalize.setOrchestrationFlags(normalizedFlags);
		}
	}
}
