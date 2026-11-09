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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

// Inter-cloud orchestration through the local Gatekeeper (local extension; upstream 5.2.2 returns no results):
// 1. globalServiceDiscovery.query: the Gatekeeper asks the neighbour clouds' Gatekeepers over the inter-cloud relay;
//    each provider cloud returns only the providers its ConsumerAuthorization grants to the consumer of this cloud.
// 2. interCloudNegotiation.negotiate: the Gatekeepers open a GateTunnel session; the result's interface points to the
//    local GateTunnel, which forwards the consumer's calls over the relay to the provider cloud's GateTunnel.
@Service
public class InterCloudServiceOrchestration {

	//=================================================================================================
	// members

	static final String GATEKEEPER = "Gatekeeper";
	static final String SERVICE_DEF_GSD = "globalServiceDiscovery";
	static final String SERVICE_OP_GSD = "query";
	static final String SERVICE_DEF_ICN = "interCloudNegotiation";
	static final String SERVICE_OP_ICN = "negotiate";
	static final String ORCH_WARN_INTER_CLOUD_DISABLED = "interCloudDisabled";
	static final String ORCH_WARN_INTER_CLOUD_FAILED = "interCloudFailed";

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private ArrowheadHttpService ahHttpService;

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Autowired
	private OrchestrationJobDbService orchJobDbService;

	@Autowired
	private eu.arrowhead.serviceorchestration.service.PlantRuleService plantRules;

	static final int MAX_MESSAGE_LENGTH = 250;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public OrchestrationResponseDTO doInterCloudServiceOrchestration(final UUID jobId, final OrchestrationForm form) {
		logger.debug("doInterCloudServiceOrchestration started...");

        final String target = Utilities.isEmpty(form.getTargetSystemName()) ? form.getRequesterSystemName() : form.getTargetSystemName();
        if (plantRules.find(target, form.getServiceDefinition()) != null) {
            setStatus(jobId, OrchestrationJobStatus.DONE, "Managed plant scope has no permitted inter-cloud provider");
            return new OrchestrationResponseDTO(List.of(), List.of());
        }

		final Set<String> warnings = new HashSet<>();
		warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_INTER_CLOUD);
		if (form.addFlag(OrchestrationFlag.MATCHMAKING, true)) {
			warnings.add(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING);
		}

		if (!sysInfo.isIntercloudEnabled()) {
			warnings.add(ORCH_WARN_INTER_CLOUD_DISABLED);
			setStatus(jobId, OrchestrationJobStatus.DONE, "Inter-cloud orchestration is disabled");
			return new OrchestrationResponseDTO(List.of(), warnings.stream().toList());
		}
		setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, null);

		final String consumer = Utilities.isEmpty(form.getTargetSystemName()) ? form.getRequesterSystemName() : form.getTargetSystemName();
		final List<OrchestrationResultDTO> results = new ArrayList<>();
		try {
			final Map<String, Object> gsd = ahHttpService.consumeService(SERVICE_DEF_GSD, SERVICE_OP_GSD, GATEKEEPER, Map.class,
					Map.of("serviceRequirement", serviceRequirement(form), "consumerSystem", consumer));
			final List<Map<String, Object>> candidates = gsd == null ? List.of() : (List<Map<String, Object>>) gsd.getOrDefault("results", List.of());
			for (final Map<String, Object> candidate : candidates) {
				final OrchestrationResultDTO result = negotiate(candidate, form, consumer);
				if (result != null) {
					results.add(result);
					if (form.getFlag(OrchestrationFlag.MATCHMAKING)) {
						break;
					}
				}
			}
		} catch (final Exception ex) {
			logger.error("Inter-cloud orchestration failed: {}", ex.getMessage());
			logger.debug(ex);
			warnings.add(ORCH_WARN_INTER_CLOUD_FAILED);
			setStatus(jobId, OrchestrationJobStatus.ERROR, "Inter-cloud orchestration failed: " + ex.getMessage());
			return new OrchestrationResponseDTO(results, warnings.stream().toList());
		}

		setStatus(jobId, OrchestrationJobStatus.DONE, results.isEmpty()
				? "No inter-cloud results were found"
				: resultMessage(results.size() + " inter-cloud result", results.stream().map(r -> r.providerName() + "@" + r.cloudIdentitifer()).toList()));
		return new OrchestrationResponseDTO(results, warnings.stream().toList());
	}

	//-------------------------------------------------------------------------------------------------
	// "<n> local result: DataManager" / "<n> inter-cloud result: DataManager@GoCloud|Company", within the message column
	public static String resultMessage(final String prefix, final List<String> providers) {
		final String message = providers.isEmpty() ? prefix : prefix + ": " + String.join(", ", providers);
		return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
	}

	//-------------------------------------------------------------------------------------------------
	private void setStatus(final UUID jobId, final OrchestrationJobStatus status, final String message) {
		if (jobId != null) {
			try {
				orchJobDbService.setStatus(jobId, status, message);
			} catch (final Exception ex) {
				logger.debug("Orchestration job {} status not recorded: {}", jobId, ex.getMessage());
			}
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	static Map<String, Object> serviceRequirement(final OrchestrationForm form) {
		final Map<String, Object> requirement = new HashMap<>();
		requirement.put("serviceDefinition", form.getServiceDefinition());
		putIfPresent(requirement, "operations", form.getOperations());
		putIfPresent(requirement, "versions", form.getVersions());
		putIfPresent(requirement, "interfaceTemplateNames", form.getInterfaceTemplateNames());
		putIfPresent(requirement, "securityPolicies", form.getSecurityPolicies());
		putIfPresent(requirement, "preferredProviders", form.getPreferredProviders());
		return requirement;
	}

	//-------------------------------------------------------------------------------------------------
	private static void putIfPresent(final Map<String, Object> map, final String key, final List<String> value) {
		if (!Utilities.isEmpty(value)) {
			map.put(key, value);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private OrchestrationResultDTO negotiate(final Map<String, Object> candidate, final OrchestrationForm form, final String consumer) {
		final Map<String, Object> payload = new HashMap<>();
		payload.put("targetCloud", candidate.get("cloud"));
		payload.put("providerName", candidate.get("providerName"));
		payload.put("serviceDefinition", form.getServiceDefinition());
		payload.put("consumerSystem", consumer);
		if (!Utilities.isEmpty(form.getOperations())) {
			payload.put("operations", form.getOperations());
		}

		final Map<String, Object> icn = ahHttpService.consumeService(SERVICE_DEF_ICN, SERVICE_OP_ICN, GATEKEEPER, Map.class, payload);
		if (icn == null || !Boolean.TRUE.equals(icn.get("negotiated"))) {
			logger.info("Inter-cloud negotiation with {} refused: {}", candidate.get("cloud"), icn == null ? null : icn.get("reason"));
			return null;
		}

		return toResult(icn);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	static OrchestrationResultDTO toResult(final Map<String, Object> icn) {
		final Map<String, Object> intf = (Map<String, Object>) icn.get("interface");
		final ServiceInstanceInterfaceResponseDTO tunnelInterface = new ServiceInstanceInterfaceResponseDTO(
				(String) intf.get("templateName"),
				(String) intf.get("protocol"),
				(String) intf.get("policy"),
				(Map<String, Object>) intf.get("properties"));
		final String providerName = (String) icn.get("providerName");
		final String serviceDefinition = (String) icn.get("serviceDefinition");
		final String version = (String) icn.get("version");

		return new OrchestrationResultDTO(
				providerName + "|" + serviceDefinition + "|" + version,
				(String) icn.get("providerCloud"),
				providerName,
				serviceDefinition,
				version,
				null,
				null,
				(Map<String, Object>) icn.getOrDefault("metadata", Map.of()),
				List.of(tunnelInterface),
				null);
	}
}
