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
package eu.arrowhead.serviceorchestration.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationSimpleStoreQueryRequest;

@ExtendWith(MockitoExtension.class)
public class OrchestrationStoreManagementServiceNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationStoreManagementServiceNormalization normalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeCreateOk() {
		final OrchestrationSimpleStoreRequestDTO dto = new OrchestrationSimpleStoreRequestDTO("Consumer", "Provider1|service1|1.0.0", 1);

		when(systemNameNormalizer.normalize("Consumer")).thenReturn("Consumer");
		when(serviceInstanceIdNormalizer.normalize("Provider1|service1|1.0.0")).thenReturn("Provider1|service1|1.0.0");

		final OrchestrationSimpleStoreRequestDTO result = normalizer.normalizeCreate(dto);

		assertNotNull(result);
		assertEquals("Consumer", result.consumer());
		assertEquals("Provider1|service1|1.0.0", result.serviceInstanceId());
		assertEquals(1, result.priority());

		verify(systemNameNormalizer).normalize("Consumer");
		verify(serviceInstanceIdNormalizer).normalize("Provider1|service1|1.0.0");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePriorityRequestDTOOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put(id1.toString(), 1);
		dto.put(id2.toString(), 2);

		final Map<UUID, Integer> result = normalizer.normalizePriorityRequestDTO(dto);

		assertEquals(2, result.size());
		assertEquals(1, result.get(id1));
		assertEquals(2, result.get(id2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePriorityRequestDTOTrimsWhitespace() {
		final UUID id1 = UUID.randomUUID();
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put("  " + id1.toString() + "  ", 1);

		final Map<UUID, Integer> result = normalizer.normalizePriorityRequestDTO(dto);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(1, result.get(id1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQueryWithNullLists() {
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(pagination, null, null, null, null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest result = normalizer.normalizeQuery(dto);

		assertEquals(pagination, result.pagination());
		assertNull(result.ids());
		assertNull(result.consumerNames());
		assertNull(result.serviceDefinitions());
		assertNull(result.serviceInstanceIds());
		assertNull(result.minPriority());
		assertNull(result.maxPriority());
		assertNull(result.createdBy());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	@SuppressWarnings("checkstyle:MagicNumber")
	public void testNormalizeQueryOk() {
		final UUID id1 = UUID.randomUUID();
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(
				pagination,
				List.of(id1.toString()),
				List.of("Consumer1"),
				List.of("serviceDef1"),
				List.of("Provider1|serviceDef1|1.0.0"),
				1,
				10,
				"Creator");

		when(systemNameNormalizer.normalize("Consumer1")).thenReturn("Consumer1");
		when(serviceDefNameNormalizer.normalize("serviceDef1")).thenReturn("serviceDef1");
		when(serviceInstanceIdNormalizer.normalize("Provider1|serviceDef1|1.0.0")).thenReturn("Provider1|serviceDef1|1.0.0");
		when(systemNameNormalizer.normalize("Creator")).thenReturn("Creator");

		final NormalizedOrchestrationSimpleStoreQueryRequest result = normalizer.normalizeQuery(dto);

		assertEquals(1, result.ids().size());
		assertEquals(id1, result.ids().get(0));
		assertEquals(1, result.consumerNames().size());
		assertEquals("Consumer1", result.consumerNames().get(0));
		assertEquals(1, result.serviceDefinitions().size());
		assertEquals("serviceDef1", result.serviceDefinitions().get(0));
		assertEquals(1, result.serviceInstanceIds().size());
		assertEquals("Provider1|serviceDef1|1.0.0", result.serviceInstanceIds().get(0));
		assertEquals(1, result.minPriority());
		assertEquals(10, result.maxPriority());
		assertEquals("Creator", result.createdBy());

		verify(systemNameNormalizer).normalize("Consumer1");
		verify(serviceDefNameNormalizer).normalize("serviceDef1");
		verify(serviceInstanceIdNormalizer).normalize("Provider1|serviceDef1|1.0.0");
		verify(systemNameNormalizer).normalize("Creator");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQueryTrimsWhitespaceInIds() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(
				pagination,
				List.of(id1.toString(), "  " + id2.toString() + "  "),
				null, null, null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest result = normalizer.normalizeQuery(dto);

		assertEquals(2, result.ids().size());
		assertEquals(id1, result.ids().get(0));
		assertEquals(id2, result.ids().get(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRemoveOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final List<String> uuids = List.of(id1.toString(), id2.toString());

		final List<UUID> result = normalizer.normalizeRemove(uuids);

		assertEquals(2, result.size());
		assertEquals(id1, result.get(0));
		assertEquals(id2, result.get(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeRemoveTrimsWhitespaces() {
		final UUID id1 = UUID.randomUUID();
		final List<String> uuids = List.of("  " + id1.toString() + "  ");

		final List<UUID> result = normalizer.normalizeRemove(uuids);

		assertEquals(1, result.size());
		assertEquals(id1, result.get(0));
	}
}
