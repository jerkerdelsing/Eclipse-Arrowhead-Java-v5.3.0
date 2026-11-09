/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.OrchestrationPlantRulesDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceQueryRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemQueryRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;

// The PDE's view of the local cloud: ServiceRegistry management queries, DSO plant rules and sysMonitor history.
@Component
public class CloudClient {

	private static final int PAGE_SIZE = 1000;

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private HttpService plainHttpService;

	@Autowired
	private ServiceCollector collector;

	@Autowired
	private SystemInfo sysInfo;

	// The collector reuses a provider's endpoint (orchestration results until they expire). After a failed synchronization
	// the endpoints are forgotten, so a provider that moved (new address or port) is orchestrated again on the next run.
	public void forgetEndpoints() {
		for (final String definition : List.of(
				Constants.SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT,
				PlantDescriptionEngineConstants.SERVICE_DEF_DSO_PLANT_MANAGEMENT,
				PlantDescriptionEngineConstants.SERVICE_DEF_SYS_MONITOR_HISTORY)) {
			collector.forget(definition);
		}
	}

	public List<SystemResponseDTO> registeredSystems() {
		final SystemQueryRequestDTO payload = new SystemQueryRequestDTO(new PageDTO(0, PAGE_SIZE, null, null), null, null, null, null, null, null);
		final SystemListResponseDTO response = httpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT, Constants.SERVICE_OP_SYSTEM_QUERY, SystemListResponseDTO.class, payload);
		return response == null || response.entries() == null ? List.of() : response.entries();
	}

	public List<ServiceInstanceResponseDTO> serviceInstances(final List<String> serviceDefinitions) {
		if (serviceDefinitions.isEmpty()) {
			return List.of();
		}
		final ServiceInstanceQueryRequestDTO payload = new ServiceInstanceQueryRequestDTO(
				new PageDTO(0, PAGE_SIZE, null, null), null, null, serviceDefinitions, null, null, null, null, null, null, null);
		final ServiceInstanceListResponseDTO response = httpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_REGISTRY_MANAGEMENT, Constants.SERVICE_OP_SERVICE_QUERY, ServiceInstanceListResponseDTO.class, payload);
		return response == null || response.entries() == null ? List.of() : response.entries();
	}

    public OrchestrationPlantRulesDTO ownRules() {
        final OrchestrationPlantRulesDTO response = httpService.consumeService(
                PlantDescriptionEngineConstants.SERVICE_DEF_DSO_PLANT_MANAGEMENT, "query",
                "DynamicServiceOrchestration", OrchestrationPlantRulesDTO.class);
        if (response == null || response.rules() == null) { throw new IllegalStateException("DSO returned no plant-rule snapshot"); }
        return response;
    }

    public void replaceRules(final OrchestrationPlantRulesDTO snapshot) {
        final OrchestrationPlantRulesDTO response = httpService.consumeService(PlantDescriptionEngineConstants.SERVICE_DEF_DSO_PLANT_MANAGEMENT, "replace",
                "DynamicServiceOrchestration", OrchestrationPlantRulesDTO.class, snapshot);
        if (!snapshot.equals(response)) { throw new IllegalStateException("DSO did not acknowledge the plant-rule snapshot"); }
    }

	// sysMonitorHistory list-systems: one entry per system that published observations, with lastObservationTime.
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> monitoredSystems() {
		final Map<String, Object> response = httpService.consumeService(
				PlantDescriptionEngineConstants.SERVICE_DEF_SYS_MONITOR_HISTORY, PlantDescriptionEngineConstants.SERVICE_OP_HISTORY_LIST_SYSTEMS, Map.class);
		final Object systems = response == null ? null : response.get("systems");
		return systems instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
	}

	// sysMonitorHistory latest: the newest observation of each type for one system. SysMonitor registers the templated
	// path /systems/{systemName}/latest, which ArrowheadHttpService cannot fill (it appends path parameters), so the
	// operation is resolved here and the placeholder substituted.
	@SuppressWarnings("unchecked")
	public Map<String, Object> latestObservations(final String systemName) {
		final String template = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final ServiceModel model = collector.getServiceModel(PlantDescriptionEngineConstants.SERVICE_DEF_SYS_MONITOR_HISTORY, template, null, PlantDescriptionEngineConstants.SERVICE_OP_HISTORY_LATEST);
		if (model == null) {
			throw new DataNotFoundException("Service definition is not found: " + PlantDescriptionEngineConstants.SERVICE_DEF_SYS_MONITOR_HISTORY);
		}
		final HttpInterfaceModel intf = (HttpInterfaceModel) model.interfaces().get(0);
		final HttpOperationModel operation = intf.operations().get(PlantDescriptionEngineConstants.SERVICE_OP_HISTORY_LATEST);
		if (operation == null) {
			throw new DataNotFoundException("sysMonitorHistory does not define " + PlantDescriptionEngineConstants.SERVICE_OP_HISTORY_LATEST);
		}
		final String path = intf.basePath() + operation.path().replace("{systemName}", UriUtils.encodePathSegment(systemName, StandardCharsets.UTF_8));
		final UriComponents uri = UriComponentsBuilder.newInstance()
				.scheme(intf.protocol())
				.host(intf.accessAddresses().get(0))
				.port(intf.accessPort())
				.path(path)
				.build(true);
		final Map<String, String> headers = new HashMap<>();
		final String authorization = HttpUtilities.calculateAuthorizationHeader(sysInfo);
		if (authorization != null) {
			headers.put(HttpHeaders.AUTHORIZATION, authorization);
		}
		final Map<String, Object> response = plainHttpService.sendRequest(uri, HttpMethod.valueOf(operation.method()), Map.class, null, null, headers);
		final Object latest = response == null ? null : response.get("latest");
		return latest instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
	}
}
