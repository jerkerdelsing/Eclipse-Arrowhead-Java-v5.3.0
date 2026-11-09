/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.service;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
@Service
public class DiscoveryService {
    private final ArrowheadHttpService http;
    public DiscoveryService(ArrowheadHttpService http) { this.http = http; }
    public List<ServiceInstanceResponseDTO> discover() {
        var query = new LinkedMultiValueMap<String, String>();
        query.add("verbose", "false");
        var payload = new ServiceInstanceLookupRequestDTO.Builder()
            .providerName(Constants.SYS_NAME_SERVICE_REGISTRY)
            .serviceDefinitionNames(List.of("deviceDiscovery", "systemDiscovery", "serviceDiscovery"))
            .interfaceTemplateName(Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME).build();
        var response = http.consumeService(Constants.SERVICE_DEF_SERVICE_DISCOVERY, Constants.SERVICE_OP_LOOKUP,
            Constants.SYS_NAME_SERVICE_REGISTRY, ServiceInstanceListResponseDTO.class, payload, query);
        if (response == null || response.entries() == null ||
            !response.entries().stream().filter(e -> e.serviceDefinition() != null)
            .map(e -> e.serviceDefinition().name()).toList().containsAll(payload.serviceDefinitionNames())) {
            throw new InternalServerError("Required v5 registry services are unavailable");
        }
        return response.entries();
    }
}
