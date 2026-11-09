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
package eu.arrowhead.serviceregistry.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.exception.ForbiddenException;

// Service definitions of the core and support systems may only be registered by their owning systems (security
// finding J9). Without this, any authenticated system could register e.g. certificateSigning or identity under its own
// name and receive requests meant for the CertificateAuthority or Authentication from consumers that orchestrate by
// service definition alone. Shared definitions that every system offers (generalManagement, sysMonitor) are not listed.
// service.definition.reserved (environment SERVICE_DEFINITION_RESERVED) adds or replaces entries:
// "<definition>:<Owner>[|<Owner>...],..."; an empty owner list ("<definition>:") lifts the reservation.
@Component
public class ReservedServiceDefinitions {

	//=================================================================================================
	// members

	static final Map<String, Set<String>> DEFAULTS = Map.ofEntries(
			Map.entry("serviceDiscovery", Set.of("ServiceRegistry")),
			Map.entry("systemDiscovery", Set.of("ServiceRegistry")),
			Map.entry("deviceDiscovery", Set.of("ServiceRegistry")),
			Map.entry("serviceRegistryManagement", Set.of("ServiceRegistry")),
			Map.entry("identity", Set.of("Authentication")),
			Map.entry("identityManagement", Set.of("Authentication")),
			Map.entry("authorization", Set.of("ConsumerAuthorization")),
			Map.entry("authorizationManagement", Set.of("ConsumerAuthorization")),
			Map.entry("authorizationToken", Set.of("ConsumerAuthorization")),
			Map.entry("authorizationTokenManagement", Set.of("ConsumerAuthorization")),
			Map.entry("serviceOrchestration", Set.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration")),
			Map.entry("serviceOrchestrationHistoryManagement", Set.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration")),
			Map.entry("serviceOrchestrationPushManagement", Set.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration")),
			Map.entry("serviceOrchestrationPlantManagement", Set.of("DynamicServiceOrchestration")),
			Map.entry("serviceOrchestrationLockManagement", Set.of("DynamicServiceOrchestration")),
			Map.entry("serviceOrchestrationSimpleStoreManagement", Set.of("SimpleStoreServiceOrchestration")),
			Map.entry("certificateSigning", Set.of("CertificateAuthority")),
			Map.entry("certificateValidation", Set.of("CertificateAuthority")),
			Map.entry("certificateAuthorityManagement", Set.of("CertificateAuthority")),
			Map.entry("onboarding", Set.of("Onboarding")),
			Map.entry("onboardingManagement", Set.of("Onboarding")),
			Map.entry("blacklistDiscovery", Set.of("Blacklist")),
			Map.entry("blacklistManagement", Set.of("Blacklist")),
			Map.entry("globalServiceDiscovery", Set.of("Gatekeeper")),
			Map.entry("interCloudNegotiation", Set.of("Gatekeeper")),
			Map.entry("gatekeeperManagement", Set.of("Gatekeeper")),
			Map.entry("gateTunnelConnection", Set.of("GateTunnel")),
			Map.entry("gateTunnelKey", Set.of("GateTunnel")),
			Map.entry("gateTunnelManagement", Set.of("GateTunnel")),
			Map.entry("translationBridge", Set.of("TranslationManager")),
			Map.entry("translationBridgeManagement", Set.of("TranslationManager")),
			Map.entry("translationReport", Set.of("TranslationManager")),
			Map.entry("qualityEvaluation", Set.of("DeviceQoSEvaluator")),
			Map.entry("deviceQualityDataManagement", Set.of("DeviceQoSEvaluator")),
			Map.entry("dataHistorian", Set.of("DataManager")),
			Map.entry("dataProxy", Set.of("DataManager")),
			Map.entry("eventPublish", Set.of("EventHandler")),
			Map.entry("eventSubscription", Set.of("EventHandler")),
			Map.entry("eventHandlerManagement", Set.of("EventHandler")),
			Map.entry("time", Set.of("TimeManager")),
			Map.entry("configuration", Set.of("Configuration")),
			Map.entry("configurationProxy", Set.of("Configuration")),
			Map.entry("configurationManagement", Set.of("Configuration")),
			Map.entry("choreographerSession", Set.of("Choreographer")),
			Map.entry("choreographerExecutor", Set.of("Choreographer")),
			Map.entry("choreographerManagement", Set.of("Choreographer")),
			Map.entry("plantDescriptionManagement", Set.of("PlantDescriptionEngine")),
			Map.entry("plantDescriptionMonitor", Set.of("PlantDescriptionEngine")),
			Map.entry("sysMonitorData", Set.of("SysMonitor")),
			Map.entry("sysMonitorHistory", Set.of("SysMonitor")),
			Map.entry("sysMonitorManagement", Set.of("SysMonitor")),
			Map.entry("aiGym", Set.of("AIGym")),
			Map.entry("localCloudManagement", Set.of("LocalCloudManager")));

	private final Map<String, Set<String>> owners;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ReservedServiceDefinitions(@Value("${service.definition.reserved:}") final String overrides) {
		final Map<String, Set<String>> merged = new HashMap<>(DEFAULTS);
		for (final String entry : overrides.split(",")) {
			final String trimmed = entry.trim();
			if (trimmed.isEmpty()) {
				continue;
			}

			final int colon = trimmed.indexOf(':');
			if (colon <= 0) {
				throw new IllegalArgumentException("service.definition.reserved entry must be <definition>:<Owner>[|<Owner>...]: " + trimmed);
			}

			final String definition = trimmed.substring(0, colon).trim();
			final Set<String> names = Set.of(trimmed.substring(colon + 1).trim().split("\\s*\\|\\s*")).stream()
					.filter(name -> !name.isBlank())
					.collect(java.util.stream.Collectors.toUnmodifiableSet());
			if (names.isEmpty()) {
				merged.remove(definition);
			} else {
				merged.put(definition, names);
			}
		}

		owners = Map.copyOf(merged);
	}

	//-------------------------------------------------------------------------------------------------
	// Refuses (403) a registration of a reserved service definition by any system other than its owners.
	public void check(final String systemName, final String serviceDefinitionName, final String origin) {
		final Set<String> allowed = owners.get(serviceDefinitionName);
		if (allowed != null && !allowed.contains(systemName)) {
			throw new ForbiddenException("Service definition " + serviceDefinitionName + " is reserved for " + String.join(", ", allowed.stream().sorted().toList()), origin);
		}
	}
}
