/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static eu.arrowhead.sysmonitor.service.Json.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;

// Orchestration of the monitored providers and the bindings derived from the subscriptions.
public class MonitoringServiceTest {

	//=================================================================================================
	// members

	private ArrowheadHttpService http;
	private MonitorDbService store;
	private AmqpIntake intake;
	private MonitoringService service;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		http = mock(ArrowheadHttpService.class);
		store = mock(MonitorDbService.class);
		intake = mock(AmqpIntake.class);
		when(intake.getAddress()).thenReturn("amqp-broker");
		when(intake.getPort()).thenReturn(5671);
		service = new MonitoringService();
		ReflectionTestUtils.setField(service, "httpService", http);
		ReflectionTestUtils.setField(service, "store", store);
		ReflectionTestUtils.setField(service, "intake", intake);
		ReflectionTestUtils.setField(service, "defaultSubscription", "all");
		ReflectionTestUtils.setField(service, "retentionDays", 30.0);
		ReflectionTestUtils.setField(service, "cloud", "TestCloud");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void orchestrationRequestAndProviderSelection() throws Exception {
		when(http.consumeService(eq(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION), eq("pull"), eq(Map.class), any(Object.class))).thenReturn(map("results", List.of(
				offer("Pump", "TestCloud.sysmonitor.Pump.pump", "CERT_AUTH", List.of("status", "echo", "power")),
				offer("Valve", "TestCloud.sysmonitor.Valve.valve", "CERT_AUTH", List.of("echo")),
				offer("Spoof", "TestCloud.sysmonitor.Pump.pump", "CERT_AUTH", List.of("echo")),
				offer("Foreign", "OtherCloud.sysmonitor.Foreign.f", "CERT_AUTH", List.of("echo")),
				offer("Open", "TestCloud.sysmonitor.Open.o", "NONE", List.of("echo")))));
		final Map<String, Map<String, Object>> subscriptions = new TreeMap<>();
		subscriptions.put("Valve", map("providerSystem", "Valve", "observationTypes", List.of("echo", "unknown"), "enabled", true));
		when(store.subscriptions()).thenReturn(subscriptions);

		service.orchestrate();

		final ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
		verify(http).consumeService(eq(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION), eq("pull"), eq(Map.class), payload.capture());
		assertEquals(map("serviceRequirement", map("serviceDefinition", "sysMonitor", "versions", List.of("1.0.0"), "interfaceTemplateNames", List.of("generic_amqps"),
				"securityPolicies", List.of("CERT_AUTH")), "orchestrationFlags", map()), payload.getValue());
		assertEquals(Set.of("Pump", "Valve"), service.getProviders().keySet(), "only own-cloud CERT_AUTH offers under the provider's own routing key");
		assertEquals(List.of("echo", "power", "status"), service.getProviders().get("Pump").get("observationTypes"));
		verify(intake).apply(Set.of("TestCloud.sysmonitor.Pump.pump.*", "TestCloud.sysmonitor.Valve.valve.echo"));
		assertNull(service.getOrchestrationError());

		final List<Object> providers = service.providersView();
		assertEquals("default", ((Map<?, ?>) providers.get(0)).get("selection"));
		assertEquals(List.of("echo", "power", "status"), ((Map<?, ?>) providers.get(0)).get("subscribedTypes"));
		assertEquals("subscription", ((Map<?, ?>) providers.get(1)).get("selection"));
		final Map<?, ?> valve = (Map<?, ?>) service.subscriptionsView().get(0);
		assertEquals(true, valve.get("providerFound"));
		assertEquals(List.of("unknown"), valve.get("unknownTypes"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void noneDefaultAndDisabledSubscriptionsBindNothing() throws Exception {
		ReflectionTestUtils.setField(service, "defaultSubscription", "none");
		when(http.consumeService(eq(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION), eq("pull"), eq(Map.class), any(Object.class))).thenReturn(map("results", List.of(
				offer("Pump", "TestCloud.sysmonitor.Pump.pump", "CERT_AUTH", List.of("echo")),
				offer("Valve", "TestCloud.sysmonitor.Valve.valve", "CERT_AUTH", List.of("echo")))));
		when(store.subscriptions()).thenReturn(Map.of("Valve", map("observationTypes", List.of("*"), "enabled", false)));
		service.orchestrate();
		verify(intake).apply(Set.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void emptyOrchestrationIsReported() throws Exception {
		when(http.consumeService(eq(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION), eq("pull"), eq(Map.class), any(Object.class))).thenReturn(map("results", List.of()));
		when(store.subscriptions()).thenReturn(Map.of());
		service.orchestrate();
		assertEquals("Orchestration returned no authorized sysMonitor provider", service.getOrchestrationError());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void invalidConfigurationIsRefused() {
		ReflectionTestUtils.setField(service, "defaultSubscription", "some");
		assertThrows(IllegalArgumentException.class, service::validate);
		ReflectionTestUtils.setField(service, "defaultSubscription", "all");
		ReflectionTestUtils.setField(service, "retentionDays", 0.0);
		assertThrows(IllegalArgumentException.class, service::validate);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> offer(final String provider, final String baseRoutingKey, final String policy, final List<String> types) {
		return map("providerName", provider, "serviceInstanceId", provider + "|sysMonitor|1.0.0",
				"metadata", map("observationTypes", types),
				"interfaces", List.of(map("templateName", "generic_amqps", "protocol", "amqps", "policy", policy,
						"properties", map("accessAddresses", List.of("amqp-broker"), "accessPort", 5671, "exchange", "arrowhead", "baseRoutingKey", baseRoutingKey, "operations", types))));
	}
}
