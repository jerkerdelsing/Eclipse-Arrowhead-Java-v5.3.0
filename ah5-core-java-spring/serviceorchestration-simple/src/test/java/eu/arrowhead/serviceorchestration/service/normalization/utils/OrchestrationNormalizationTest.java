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
package eu.arrowhead.serviceorchestration.service.normalization.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;

@ExtendWith(MockitoExtension.class)
public class OrchestrationNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationNormalization normalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePullOk() {
		final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(
				"serviceDefinition",
				List.of("Provider1", "Provider2"),
				Map.of("matchmaking ", true, "only_preferred ", false),
				null);

		when(serviceDefNameNormalizer.normalize("serviceDefinition")).thenReturn("serviceDefinition");
		when(systemNameNormalizer.normalize("Provider1")).thenReturn("Provider1");
		when(systemNameNormalizer.normalize("Provider2")).thenReturn("Provider2");

		normalizer.normalizePull(request);

		assertEquals("serviceDefinition", request.getServiceDefinition());
		assertEquals(2, request.getPreferredProviders().size());
		assertEquals("Provider1", request.getPreferredProviders().get(0));
		assertEquals("Provider2", request.getPreferredProviders().get(1));
		assertEquals(2, request.getOrchestrationFlags().size());
		assertEquals(true, request.getOrchestrationFlags().get("MATCHMAKING"));
		assertEquals(false, request.getOrchestrationFlags().get("ONLY_PREFERRED"));

		verify(serviceDefNameNormalizer).normalize("serviceDefinition");
		verify(systemNameNormalizer).normalize("Provider1");
		verify(systemNameNormalizer).normalize("Provider2");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePullWithNullPreferredProviders() {
		final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(
				"serviceDefinition",
				null,
				null,
				null);

		when(serviceDefNameNormalizer.normalize("serviceDefinition")).thenReturn("serviceDefinition");

		normalizer.normalizePull(request);

		assertEquals("serviceDefinition", request.getServiceDefinition());
		assertNull(request.getPreferredProviders());
		assertNull(request.getOrchestrationFlags());

		verify(serviceDefNameNormalizer).normalize("serviceDefinition");
		verify(systemNameNormalizer, never()).normalize(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePullWithEmptyPreferredProviders() {
		final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(
				"ServiceDefinition",
				List.of(),
				null,
				null);

		when(serviceDefNameNormalizer.normalize("ServiceDefinition")).thenReturn("serviceDefinition");

		normalizer.normalizePull(request);

		assertEquals("serviceDefinition", request.getServiceDefinition());
		verify(systemNameNormalizer, never()).normalize(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePullWithEmptyOrchestrationFlags() {
		final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(
				"ServiceDefinition",
				null,
				Map.of(),
				null);

		when(serviceDefNameNormalizer.normalize("ServiceDefinition")).thenReturn("serviceDefinition");

		normalizer.normalizePull(request);

		assertEquals("serviceDefinition", request.getServiceDefinition());
		assertNotNull(request.getOrchestrationFlags());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSubscribeOk() {
		final Map<String, String> notifyProps = new HashMap<>();
		notifyProps.put("  Address  ", "  localhost  ");
		notifyProps.put("Port", "8080");

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("  https  ", notifyProps);
		final SimpleOrchestrationRequest orchRequest = new SimpleOrchestrationRequest(
				"serviceDefinition",
				List.of("Provider1"),
				Map.of("matchmaking", true),
				null);
		final SimpleOrchestrationSubscriptionRequest request = new SimpleOrchestrationSubscriptionRequest(
				"  TargetSystem  ",
				orchRequest,
				notifyInterface,
				60L);

		when(systemNameNormalizer.normalize("  TargetSystem  ")).thenReturn("TargetSystem");
		when(serviceDefNameNormalizer.normalize("serviceDefinition")).thenReturn("serviceDefinition");
		when(systemNameNormalizer.normalize("Provider1")).thenReturn("Provider1");

		normalizer.normalizeSubscribe(request);

		assertEquals("TargetSystem", request.getTargetSystemName());
		assertEquals("HTTPS", request.getNotifyInterface().protocol());
		assertEquals("localhost", request.getNotifyInterface().properties().get("address"));
		assertEquals("8080", request.getNotifyInterface().properties().get("port"));
		assertEquals("serviceDefinition", request.getOrchestrationRequest().getServiceDefinition());
		assertEquals("Provider1", request.getOrchestrationRequest().getPreferredProviders().get(0));
		assertEquals(true, request.getOrchestrationRequest().getOrchestrationFlags().get("MATCHMAKING"));

		verify(systemNameNormalizer).normalize("  TargetSystem  ");
		verify(serviceDefNameNormalizer).normalize("serviceDefinition");
		verify(systemNameNormalizer).normalize("Provider1");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSubscribeWithEmptyTargetSystemName() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("https", Map.of("address", "localhost"));
		final SimpleOrchestrationRequest orchRequest = new SimpleOrchestrationRequest(
				"serviceDefinition",
				null,
				null,
				null);
		final SimpleOrchestrationSubscriptionRequest request = new SimpleOrchestrationSubscriptionRequest(
				"",
				orchRequest,
				notifyInterface,
				60L);

		when(serviceDefNameNormalizer.normalize("serviceDefinition")).thenReturn("serviceDefinition");

		normalizer.normalizeSubscribe(request);

		assertNull(request.getTargetSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUUIDOk() {

		final UUID expected = UUID.randomUUID();
		final UUID result = normalizer.normalizeUUID(expected.toString());
		assertEquals(expected, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeUUIDRemovesWhitespaces() {

		final UUID expected = UUID.randomUUID();
		final UUID result = normalizer.normalizeUUID("  " + expected.toString() + "  ");
		assertEquals(expected, result);
	}
}
