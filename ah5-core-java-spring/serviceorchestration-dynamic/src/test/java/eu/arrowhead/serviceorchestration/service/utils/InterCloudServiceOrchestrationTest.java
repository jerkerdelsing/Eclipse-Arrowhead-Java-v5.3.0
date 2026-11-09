/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceorchestration.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@ExtendWith(MockitoExtension.class)
public class InterCloudServiceOrchestrationTest {

    @Mock
    private eu.arrowhead.serviceorchestration.service.PlantRuleService plantRules;

	@InjectMocks
	private InterCloudServiceOrchestration orchestration;

	@Mock
	private ArrowheadHttpService ahHttpService;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Mock
	private eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService orchJobDbService;

	private static OrchestrationForm form() {
		return new OrchestrationForm("RandomDataProducer", new OrchestrationRequestDTO(
				new OrchestrationServiceRequirementDTO("dataHistorian", List.of("put-data"), null, null, null, List.of("generic_https"), null, null, null, null),
				Map.of("ONLY_INTERCLOUD", true), null, null));
	}

    @Test
    public void managedPlantScopeCannotEscapeThroughOnlyIntercloud() {
        when(plantRules.find(any(), any())).thenReturn(new eu.arrowhead.dto.OrchestrationPlantRuleDTO("Consumer", "dataHistorian", List.of()));
        assertTrue(orchestration.doInterCloudServiceOrchestration(UUID.randomUUID(), form()).results().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(ahHttpService);
    }

	@Test
	public void testDisabledReturnsNoResults() {
		when(sysInfo.isIntercloudEnabled()).thenReturn(false);
		final OrchestrationResponseDTO response = orchestration.doInterCloudServiceOrchestration(UUID.randomUUID(), form());
		assertTrue(response.results().isEmpty());
		assertTrue(response.warnings().contains("interCloudDisabled"));
		verify(ahHttpService, never()).consumeService(any(), any(), any(), any(), any(Map.class));
	}

	@Test
	public void testNegotiatedProviderBecomesTunnelResult() {
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq("globalServiceDiscovery"), eq("query"), eq("Gatekeeper"), eq(Map.class), any(Map.class)))
				.thenReturn(Map.of("results", List.of(Map.of("cloud", "GoCloud|Company", "providerName", "DataManager"))));
		when(ahHttpService.consumeService(eq("interCloudNegotiation"), eq("negotiate"), eq("Gatekeeper"), eq(Map.class), any(Map.class)))
				.thenReturn(Map.of("negotiated", true, "providerCloud", "GoCloud|Company", "providerName", "DataManager",
						"serviceDefinition", "dataHistorian", "version", "1.0.0",
						"interface", Map.of("templateName", "generic_https", "protocol", "https", "policy", "CERT_AUTH",
								"properties", Map.of("accessAddresses", List.of("gatetunnel"), "accessPort", 8200, "basePath", "/datamanager/data-historian"))));

		final OrchestrationResponseDTO response = orchestration.doInterCloudServiceOrchestration(UUID.randomUUID(), form());

		assertEquals(1, response.results().size());
		final OrchestrationResultDTO result = response.results().get(0);
		assertEquals("GoCloud|Company", result.cloudIdentitifer());
		assertEquals("DataManager", result.providerName());
		assertEquals(List.of("gatetunnel"), result.interfaces().get(0).properties().get("accessAddresses"));
		assertTrue(response.warnings().contains("interCloud"));
	}

	@Test
	public void testRefusedNegotiationGivesNoResult() {
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq("globalServiceDiscovery"), eq("query"), eq("Gatekeeper"), eq(Map.class), any(Map.class)))
				.thenReturn(Map.of("results", List.of(Map.of("cloud", "GoCloud|Company", "providerName", "DataManager"))));
		when(ahHttpService.consumeService(eq("interCloudNegotiation"), eq("negotiate"), eq("Gatekeeper"), eq(Map.class), any(Map.class)))
				.thenReturn(Map.of("negotiated", false, "reason", "Not authorized by the provider cloud"));
		assertTrue(orchestration.doInterCloudServiceOrchestration(UUID.randomUUID(), form()).results().isEmpty());
	}

	@Test
	public void testGatekeeperFailureIsAWarning() {
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq("globalServiceDiscovery"), eq("query"), eq("Gatekeeper"), eq(Map.class), any(Map.class)))
				.thenThrow(new ExternalServerError("relay down"));
		final OrchestrationResponseDTO response = orchestration.doInterCloudServiceOrchestration(UUID.randomUUID(), form());
		assertTrue(response.results().isEmpty());
		assertTrue(response.warnings().contains("interCloudFailed"));
	}

	@Test
	public void testResultMessageNamesProvidersWithinColumn() {
		assertEquals("1 inter-cloud result: DataManager@GoCloud|Company", InterCloudServiceOrchestration.resultMessage("1 inter-cloud result", List.of("DataManager@GoCloud|Company")));
		assertEquals("0 local result", InterCloudServiceOrchestration.resultMessage("0 local result", List.of()));
		assertTrue(InterCloudServiceOrchestration.resultMessage("9 local result", java.util.Collections.nCopies(50, "VeryLongProviderSystemName")).length() <= 250);
	}
}
