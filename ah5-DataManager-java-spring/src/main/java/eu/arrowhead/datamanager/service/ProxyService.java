/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.datamanager.api.dto.SenMLRecordDTO;

@Service
public class ProxyService {

	private final DataManagerNameService nameService;
	private final SenMLValidation senMLValidation;
	private final Map<EndpointKey, List<SenMLRecordDTO>> cache = new ConcurrentHashMap<>();

	public ProxyService(final DataManagerNameService nameService, final SenMLValidation senMLValidation) {
		this.nameService = nameService;
		this.senMLValidation = senMLValidation;
	}

	public List<String> getSystems() {
		return cache.keySet().stream().map(EndpointKey::systemName).distinct().sorted().toList();
	}

	public List<String> getServices(final String systemName) {
		final String normalizedSystem = nameService.normalizeSystemName(systemName);
		return cache.keySet().stream()
				.filter(key -> key.systemName().equals(normalizedSystem))
				.map(EndpointKey::serviceName)
				.sorted()
				.toList();
	}

	public List<SenMLRecordDTO> get(final String systemName, final String serviceName, final String origin) {
		final EndpointKey key = normalizeKey(systemName, serviceName);
		final List<SenMLRecordDTO> result = cache.get(key);
		if (result == null) {
			throw new DataNotFoundException("No cached data exists for " + key.systemName() + "|" + key.serviceName(), origin);
		}
		return result;
	}

	public void put(final String systemName, final String serviceName, final List<SenMLRecordDTO> records, final String origin) {
		cache.put(normalizeKey(systemName, serviceName), senMLValidation.validateAndComplete(records, origin));
	}

	private EndpointKey normalizeKey(final String systemName, final String serviceName) {
		return new EndpointKey(nameService.normalizeSystemName(systemName), nameService.normalizeServiceName(serviceName));
	}

	private record EndpointKey(String systemName, String serviceName) {
	}
}
