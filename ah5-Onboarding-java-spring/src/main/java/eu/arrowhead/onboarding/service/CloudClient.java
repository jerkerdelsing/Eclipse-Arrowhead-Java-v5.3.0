/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.service;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.BlacklistCreateListRequestDTO;
import eu.arrowhead.dto.BlacklistCreateRequestDTO;
import eu.arrowhead.dto.DeviceListRequestDTO;
import eu.arrowhead.dto.DeviceRequestDTO;
import eu.arrowhead.dto.IdentityListMgmtCreateRequestDTO;
import eu.arrowhead.dto.IdentityMgmtRequestDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;
/**
 * The core and support management operations of the onboarding procedure. Each is granted to Onboarding individually
 * (ServiceRegistry management whitelist; ConsumerAuthorization MGMT policies for Authentication and Blacklist).
 */
@Service
public class CloudClient {
    private final ArrowheadHttpService http;
    public CloudClient(ArrowheadHttpService http) { this.http = http; }
    public void createIdentity(String systemName, String password) {
        var request = new IdentityListMgmtCreateRequestDTO(AuthenticationMethod.PASSWORD.name(),
            List.of(new IdentityMgmtRequestDTO(systemName, Map.of("password", password), false)));
        http.consumeService("identityManagement", "identity-mgmt-create", Constants.SYS_NAME_AUTHENTICATION, Map.class, request);
    }
    public void removeIdentity(String systemName) {
        http.consumeService("identityManagement", "identity-mgmt-remove", Constants.SYS_NAME_AUTHENTICATION, Void.class, null, names(systemName));
    }
    public void createDevice(String deviceName, List<String> addresses, Map<String, Object> metadata) {
        http.consumeService("serviceRegistryManagement", "device-create", Constants.SYS_NAME_SERVICE_REGISTRY, Map.class,
            new DeviceListRequestDTO(List.of(new DeviceRequestDTO(deviceName, metadata, addresses))));
    }
    public void updateDevice(String deviceName, List<String> addresses, Map<String, Object> metadata) {
        http.consumeService("serviceRegistryManagement", "device-update", Constants.SYS_NAME_SERVICE_REGISTRY, Map.class,
            new DeviceListRequestDTO(List.of(new DeviceRequestDTO(deviceName, metadata, addresses))));
    }
    public void removeDevice(String deviceName) {
        http.consumeService("serviceRegistryManagement", "device-remove", Constants.SYS_NAME_SERVICE_REGISTRY, Void.class, null, names(deviceName));
    }
    /** Removes the system and, with it, its service instances. */
    public void removeSystem(String systemName) {
        http.consumeService("serviceRegistryManagement", "system-remove", Constants.SYS_NAME_SERVICE_REGISTRY, Void.class, null, names(systemName));
    }
    public void blacklist(String systemName, String expiresAt, String reason) {
        http.consumeService("blacklistManagement", "create", "Blacklist", Map.class,
            new BlacklistCreateListRequestDTO(List.of(new BlacklistCreateRequestDTO(systemName, expiresAt, reason))));
    }
    public void unblacklist(List<String> systemNames) {
        var query = new LinkedMultiValueMap<String, String>(); query.put("names", systemNames);
        http.consumeService("blacklistManagement", "remove", "Blacklist", Void.class, null, query);
    }
    private static LinkedMultiValueMap<String, String> names(String name) {
        var query = new LinkedMultiValueMap<String, String>(); query.add("names", name); return query;
    }
}
