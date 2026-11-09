/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.service;

import org.springframework.stereotype.Service;

import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;

@Service
public class DataManagerNameService {

	private final SystemNameNormalizer systemNameNormalizer;
	private final SystemNameValidator systemNameValidator;
	private final ServiceDefinitionNameNormalizer serviceNameNormalizer;
	private final ServiceDefinitionNameValidator serviceNameValidator;

	public DataManagerNameService(
			final SystemNameNormalizer systemNameNormalizer,
			final SystemNameValidator systemNameValidator,
			final ServiceDefinitionNameNormalizer serviceNameNormalizer,
			final ServiceDefinitionNameValidator serviceNameValidator) {
		this.systemNameNormalizer = systemNameNormalizer;
		this.systemNameValidator = systemNameValidator;
		this.serviceNameNormalizer = serviceNameNormalizer;
		this.serviceNameValidator = serviceNameValidator;
	}

	public String normalizeSystemName(final String systemName) {
		final String normalized = systemNameNormalizer.normalize(systemName);
		systemNameValidator.validateSystemName(normalized);
		return normalized;
	}

	public String normalizeServiceName(final String serviceName) {
		final String normalized = serviceNameNormalizer.normalize(serviceName);
		serviceNameValidator.validateServiceDefinitionName(normalized);
		return normalized;
	}
}
