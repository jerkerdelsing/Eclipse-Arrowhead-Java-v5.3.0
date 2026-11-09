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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

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
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationHistoryManagementValidation;

@SuppressWarnings("checkstyle:MagicNumber")
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

	@Spy
	private DTOConverter dtoConverter;

	@Captor
	private ArgumentCaptor<NormalizedOrchestrationJobQueryRequest> jobQueryRequestCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuery() {
		final String origin = "test.origin";
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "TestSys", "TestSys", "testService", UUID.randomUUID().toString());
		final OrchestrationHistoryQueryRequestDTO requestDTO = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, null, null, List.of(job.getId().toString()));
		final PageRequest pageRequest = PageRequest.of(0, 5, Direction.ASC, "id");
		final NormalizedOrchestrationJobQueryRequest normalizedRequest = new NormalizedOrchestrationJobQueryRequest(
				pageRequest,
				List.of(),
				List.of(),
				null,
				List.of(),
				List.of(),
				List.of(),
				requestDTO.subscriptionIds().stream().map(id -> UUID.fromString(id)).toList());

		when(validator.validateAndNormalizeQueryService(eq(requestDTO), eq(origin))).thenReturn(normalizedRequest);
		final PageImpl<OrchestrationJob> pageResult = new PageImpl<OrchestrationJob>(List.of(job));
		when(jobDbService.query(any(NormalizedOrchestrationJobQueryRequest.class))).thenReturn(pageResult);

		final OrchestrationHistoryResponseDTO result = assertDoesNotThrow(() -> historyService.query(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryService(eq(requestDTO), eq(origin));
		verify(jobDbService).query(jobQueryRequestCaptor.capture());
		verify(dtoConverter).convertOrchestrationJobPageToHistoryDTO(eq(pageResult));

		assertEquals(requestDTO.subscriptionIds().get(0), jobQueryRequestCaptor.getValue().getSubscriptionIds().get(0).toString());
		assertEquals(job.getSubscriptionId(), result.entries().get(0).subscriptionId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDBError() {
		final String origin = "test.origin";
		final OrchestrationHistoryQueryRequestDTO requestDTO = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, null, null, List.of(UUID.randomUUID().toString()));
		final PageRequest pageRequest = PageRequest.of(0, 5, Direction.ASC, "id");
		final NormalizedOrchestrationJobQueryRequest normalizedRequest = new NormalizedOrchestrationJobQueryRequest(
				pageRequest,
				List.of(),
				List.of(),
				null,
				List.of(),
				List.of(),
				List.of(),
				requestDTO.subscriptionIds().stream().map(id -> UUID.fromString(id)).toList());

		when(validator.validateAndNormalizeQueryService(eq(requestDTO), eq(origin))).thenReturn(normalizedRequest);
		doThrow(new InternalServerError("test message")).when(jobDbService).query(any(NormalizedOrchestrationJobQueryRequest.class));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> historyService.query(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryService(eq(requestDTO), eq(origin));
		verify(jobDbService).query(jobQueryRequestCaptor.capture());
		verify(dtoConverter, never()).convertOrchestrationJobPageToHistoryDTO(any());

		assertEquals(requestDTO.subscriptionIds().get(0), jobQueryRequestCaptor.getValue().getSubscriptionIds().get(0).toString());
		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ex.getOrigin());
	}
}
