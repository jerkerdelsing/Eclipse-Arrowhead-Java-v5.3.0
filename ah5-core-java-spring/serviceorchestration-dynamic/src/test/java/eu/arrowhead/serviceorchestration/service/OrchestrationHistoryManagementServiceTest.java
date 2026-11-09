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
package eu.arrowhead.serviceorchestration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationHistoryManagementValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationHistoryManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationHistoryManagementService historyService;

	@Mock
	private OrchestrationHistoryManagementValidation validator;

	@Mock
	private OrchestrationJobDbService jobDbService;

	@Mock
	private PageService pageService;

	@Spy
	private DTOConverter dtoConverter;

	@Captor
	private ArgumentCaptor<OrchestrationJobFilter> jobFilterCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuery() {
		final String origin = "test.origin";
		final OrchestrationHistoryQueryRequestDTO requestDTO = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, null, null, List.of(UUID.randomUUID().toString()));
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "TestSys", "TestSys", "testService", UUID.randomUUID().toString());

		when(validator.validateAndNormalizeQueryService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(pageService.getPageRequest(any(), any(), anyList(), anyString(), eq(origin))).thenReturn(PageRequest.of(0, 1));
		final PageImpl<OrchestrationJob> pageResult = new PageImpl<OrchestrationJob>(List.of(job));
		when(jobDbService.query(any(OrchestrationJobFilter.class), any(PageRequest.class))).thenReturn(pageResult);

		final OrchestrationHistoryResponseDTO result = assertDoesNotThrow(() -> historyService.query(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryService(eq(requestDTO), eq(origin));
		verify(pageService).getPageRequest(any(), eq(Direction.DESC), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq(OrchestrationJob.DEFAULT_SORT_FIELD), eq(origin));
		verify(jobDbService).query(jobFilterCaptor.capture(), any(PageRequest.class));
		verify(dtoConverter).convertOrchestrationJobPageToHistoryDTO(eq(pageResult));

		assertEquals(requestDTO.subscriptionIds().get(0), jobFilterCaptor.getValue().getSubscriptionIds().get(0));
		assertEquals(job.getSubscriptionId(), result.entries().get(0).subscriptionId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDBError() {
		final String origin = "test.origin";
		final OrchestrationHistoryQueryRequestDTO requestDTO = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, null, null, List.of(UUID.randomUUID().toString()));

		when(validator.validateAndNormalizeQueryService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(pageService.getPageRequest(any(), any(), anyList(), anyString(), eq(origin))).thenReturn(PageRequest.of(0, 1));
		doThrow(new InternalServerError("test message")).when(jobDbService).query(any(OrchestrationJobFilter.class), any(PageRequest.class));

		final Throwable ex = assertThrows(Throwable.class, () -> historyService.query(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryService(eq(requestDTO), eq(origin));
		verify(pageService).getPageRequest(any(), eq(Direction.DESC), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq(OrchestrationJob.DEFAULT_SORT_FIELD), eq(origin));
		verify(jobDbService).query(jobFilterCaptor.capture(), any(PageRequest.class));
		verify(dtoConverter, never()).convertOrchestrationJobPageToHistoryDTO(any());

		assertEquals(requestDTO.subscriptionIds().get(0), jobFilterCaptor.getValue().getSubscriptionIds().get(0));
		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}
}
