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

package eu.arrowhead.serviceorchestration.service.utils;

import eu.arrowhead.common.service.util.ServiceInstanceIdUtils;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ServiceOrchestration {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private SimpleStoreDbService storeDbService;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<OrchestrationStore> orchestrate(final UUID jobId, final String consumer, final SimpleOrchestrationRequest request) {
		logger.debug("orchestrate started...");

		orchJobDbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

		// orchestration flags
		final boolean onlyPreferred = request.getOrchestrationFlags() != null
				&& request.getOrchestrationFlags().getOrDefault(OrchestrationFlag.ONLY_PREFERRED.toString(), false);
		final boolean matchmaking = request.getOrchestrationFlags() != null
				&& request.getOrchestrationFlags().getOrDefault(OrchestrationFlag.MATCHMAKING.toString(), false);

		// matching entries
		List<OrchestrationStore> sortedMatchingEntries = request.getServiceDefinition() != null
				? storeDbService.getByConsumerAndServiceDefinition(consumer, request.getServiceDefinition())
				: storeDbService.getByConsumer(consumer);

		if (request.getPreferredProviders() != null) {
			sortedMatchingEntries = sortByPreferredProviders(sortedMatchingEntries, request.getPreferredProviders(), onlyPreferred);
		}

		if (matchmaking) {
			// converting list to LinkedHashSet so there will be no duplicates but the order remains
			final Set<String> serviceDefs = new LinkedHashSet<>(sortedMatchingEntries.stream().map(OrchestrationStore::getServiceDefinition).toList());

			// finding entries with the highest priority for each service definition
			final List<OrchestrationStore> sortedWithMatchmaking = new ArrayList<>(serviceDefs.size());
			for (final String serviceDef : serviceDefs) {
				for (final OrchestrationStore entry : sortedMatchingEntries) {
					if (entry.getServiceDefinition().equals(serviceDef)) {
						sortedWithMatchmaking.add(entry);
						// stopping after the first match since that is the one with the highest priority
						break;
					}
				}
			}
			sortedMatchingEntries = sortedWithMatchmaking;
		}

		orchJobDbService.setStatus(jobId, OrchestrationJobStatus.DONE, sortedMatchingEntries.size() + " local result(s)");

		return sortedMatchingEntries;
	}


	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// Store entries with preferred providers will be prioritized
	private List<OrchestrationStore> sortByPreferredProviders(final List<OrchestrationStore> entries, final List<String> preferred, final boolean onlyPreferred) {
		logger.debug("sortByPreferredProviders started...");

		List<OrchestrationStore> preferredEntries = new ArrayList<>();
		List<OrchestrationStore> notPreferredEntries = new ArrayList<>();

		for (final OrchestrationStore entry : entries) {
			if (preferred.contains(ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId(entry.getServiceInstanceId()))) {
				preferredEntries.add(entry);
			} else {
				notPreferredEntries.add(entry);
			}
		}

		if (!onlyPreferred) {
			preferredEntries.addAll(notPreferredEntries);
		}
		return preferredEntries;
	}
}
