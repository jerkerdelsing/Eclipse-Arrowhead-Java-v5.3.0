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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;
import eu.arrowhead.serviceorchestration.service.normalization.utils.OrchestrationNormalization;

@ExtendWith(MockitoExtension.class)
public class OrchestrationHistoryManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationHistoryManagementNormalization normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private OrchestrationNormalization orchNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private PageService pageService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationHistoryQueryRequestDTOWithNullDto() {
		final PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "id");

		when(pageService.getPageRequest(eq(null), eq(Sort.Direction.DESC), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq(OrchestrationJob.DEFAULT_SORT_FIELD), any())).thenReturn(pageRequest);

		final NormalizedOrchestrationJobQueryRequest result = normalizer.normalizeOrchestrationHistoryQueryRequestDTO(null, "test origin");

		assertNotNull(result);
		assertEquals(pageRequest, result.getPagination());
		assertTrue(result.getIds().isEmpty());
		assertTrue(result.getStatuses().isEmpty());
		assertNull(result.getType());
		assertTrue(result.getRequesterSystems().isEmpty());
		assertTrue(result.getTargetSystems().isEmpty());
		assertTrue(result.getServiceDefinitions().isEmpty());
		assertTrue(result.getSubscriptionIds().isEmpty());
		verify(pageService).getPageRequest(eq(null), eq(Sort.Direction.DESC), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq(OrchestrationJob.DEFAULT_SORT_FIELD), eq("test origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationHistoryQueryRequestDTOWithEmptyDto() {
		final PageDTO pagination = new PageDTO(0, 10, "id", "DESC");
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(pagination, null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "id");

		when(pageService.getPageRequest(eq(pagination), eq(Sort.Direction.DESC), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq(OrchestrationJob.DEFAULT_SORT_FIELD), any())).thenReturn(pageRequest);

		final NormalizedOrchestrationJobQueryRequest result = normalizer.normalizeOrchestrationHistoryQueryRequestDTO(dto, "test-origin");

		assertNotNull(result);
		assertEquals(pageRequest, result.getPagination());
		assertTrue(result.getIds().isEmpty());
		assertTrue(result.getStatuses().isEmpty());
		assertNull(result.getType());
		assertTrue(result.getRequesterSystems().isEmpty());
		assertTrue(result.getTargetSystems().isEmpty());
		assertTrue(result.getServiceDefinitions().isEmpty());
		assertTrue(result.getSubscriptionIds().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationHistoryQueryRequestWithFullDto() {
		final UUID id = UUID.randomUUID();
		final UUID subscriptionId = UUID.randomUUID();
		final PageDTO pagination = new PageDTO(0, 10, "id", "DESC");
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(
				pagination,
				List.of(id.toString()),
				List.of("PENDING", "done \n"),
				"pull \n",
				List.of("Requester1"),
				List.of("Target1"),
				List.of("serviceDef1"),
				List.of(subscriptionId.toString()));
		final PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "id");

		when(pageService.getPageRequest(eq(pagination), eq(Sort.Direction.DESC), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq(OrchestrationJob.DEFAULT_SORT_FIELD), any())).thenReturn(pageRequest);
		when(orchNormalizer.normalizeUUID(id.toString())).thenReturn(id);
		when(orchNormalizer.normalizeUUID(subscriptionId.toString())).thenReturn(subscriptionId);
		when(systemNameNormalizer.normalize("Requester1")).thenReturn("Requester1");
		when(systemNameNormalizer.normalize("Target1")).thenReturn("Target1");
		when(serviceDefNameNormalizer.normalize("serviceDef1")).thenReturn("serviceDef1");

		final NormalizedOrchestrationJobQueryRequest result = normalizer.normalizeOrchestrationHistoryQueryRequestDTO(dto, "test origin");

		assertNotNull(result);
		assertEquals(pageRequest, result.getPagination());
		assertEquals(1, result.getIds().size());
		assertEquals(id, result.getIds().get(0));
		assertEquals(2, result.getStatuses().size());
		assertEquals(OrchestrationJobStatus.PENDING, result.getStatuses().get(0));
		assertEquals(OrchestrationJobStatus.DONE, result.getStatuses().get(1));
		assertEquals(OrchestrationType.PULL, result.getType());
		assertEquals(1, result.getRequesterSystems().size());
		assertEquals("Requester1", result.getRequesterSystems().get(0));
		assertEquals(1, result.getTargetSystems().size());
		assertEquals("Target1", result.getTargetSystems().get(0));
		assertEquals(1, result.getServiceDefinitions().size());
		assertEquals("serviceDef1", result.getServiceDefinitions().get(0));
		assertEquals(1, result.getSubscriptionIds().size());
		assertEquals(subscriptionId, result.getSubscriptionIds().get(0));

		verify(orchNormalizer).normalizeUUID(id.toString());
		verify(orchNormalizer).normalizeUUID(subscriptionId.toString());
		verify(systemNameNormalizer).normalize("Requester1");
		verify(systemNameNormalizer).normalize("Target1");
		verify(serviceDefNameNormalizer).normalize("serviceDef1");
	}
}
