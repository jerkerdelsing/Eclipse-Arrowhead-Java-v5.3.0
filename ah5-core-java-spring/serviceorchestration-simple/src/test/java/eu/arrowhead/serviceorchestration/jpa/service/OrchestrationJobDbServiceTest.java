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
package eu.arrowhead.serviceorchestration.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationJobRepository;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class OrchestrationJobDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationJobDbService dbService;

	@Mock
	private OrchestrationJobRepository jobRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOk() {
		final OrchestrationJob job1 = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		final OrchestrationJob job2 = new OrchestrationJob(OrchestrationType.PUSH, "Requester2", "Target2", "serviceDef2", UUID.randomUUID().toString());
		final List<OrchestrationJob> jobs = List.of(job1, job2);

		when(jobRepo.saveAllAndFlush(jobs)).thenReturn(jobs);

		final List<OrchestrationJob> result = dbService.create(jobs);

		assertNotNull(result);
		assertEquals(2, result.size());
		verify(jobRepo).saveAllAndFlush(jobs);
	}
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateThrowsInternalServerError() {
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		final List<OrchestrationJob> jobs = List.of(job);

		when(jobRepo.saveAllAndFlush(jobs)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.create(jobs));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdOk() {

		final UUID id = UUID.randomUUID();
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", UUID.randomUUID().toString());
		job.setId(id);

		when(jobRepo.findById(id)).thenReturn(Optional.of(job));

		final Optional<OrchestrationJob> result = dbService.getById(id);

		assertTrue(result.isPresent());
		assertEquals(job, result.get());
		verify(jobRepo).findById(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdNotFound() {
		final UUID id = UUID.randomUUID();

		when(jobRepo.findById(id)).thenReturn(Optional.empty());

		final Optional<OrchestrationJob> result = dbService.getById(id);

		assertTrue(result.isEmpty());
		verify(jobRepo).findById(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdThrowsInternalServerError() {

		final UUID id = UUID.randomUUID();
		when(jobRepo.findById(id)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.getById(id));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusInOk() {
		final List<OrchestrationJobStatus> statuses = List.of(OrchestrationJobStatus.PENDING, OrchestrationJobStatus.IN_PROGRESS);
		final OrchestrationJob job1 = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		job1.setStatus(OrchestrationJobStatus.PENDING);
		final OrchestrationJob job2 = new OrchestrationJob(OrchestrationType.PUSH, "Requester2", "Target2", "serviceDef2", UUID.randomUUID().toString());
		job2.setStatus(OrchestrationJobStatus.IN_PROGRESS);

		when(jobRepo.findAllByStatusIn(statuses)).thenReturn(List.of(job1, job2));

		final List<OrchestrationJob> result = dbService.getAllByStatusIn(statuses);

		assertNotNull(result);
		assertEquals(2, result.size());
		verify(jobRepo).findAllByStatusIn(statuses);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusInThrowsInternalServerError() {

		final List<OrchestrationJobStatus> statuses = List.of(OrchestrationJobStatus.PENDING);
		when(jobRepo.findAllByStatusIn(statuses)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.getAllByStatusIn(statuses));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToInProgressOk() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		job.setStatus(OrchestrationJobStatus.PENDING);
		job.setId(jobId);

		when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));
		when(jobRepo.saveAndFlush(any(OrchestrationJob.class))).thenReturn(job);

		OrchestrationJob result = null;
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
		    utilitiesMock.when(Utilities::utcNow)
		                 .thenReturn(ZonedDateTime.of(202, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")));

		    result = dbService.setStatus(jobId, OrchestrationJobStatus.IN_PROGRESS, "Starting");
		}

		assertEquals(OrchestrationJobStatus.IN_PROGRESS, result.getStatus());
		assertEquals(OrchestrationJobStatus.IN_PROGRESS, job.getStatus());
		assertEquals(ZonedDateTime.of(202, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), job.getStartedAt());
		assertNull(result.getFinishedAt());
		verify(jobRepo).findById(jobId);
		verify(jobRepo).saveAndFlush(job);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToDoneOk() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);

		when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));
		when(jobRepo.saveAndFlush(any(OrchestrationJob.class))).thenReturn(job);

		OrchestrationJob result = null;
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
		    utilitiesMock.when(Utilities::utcNow)
		                 .thenReturn(ZonedDateTime.of(202, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")));

		    result = dbService.setStatus(jobId, OrchestrationJobStatus.DONE, "Completed");
		}


		assertNotNull(result);
		assertEquals(OrchestrationJobStatus.DONE, result.getStatus());
		assertEquals(ZonedDateTime.of(202, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), job.getFinishedAt());
		verify(jobRepo).findById(jobId);
		verify(jobRepo).saveAndFlush(job);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToErrorOk() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);

		when(jobRepo.findById(jobId)).thenReturn(Optional.of(job));
		when(jobRepo.saveAndFlush(any(OrchestrationJob.class))).thenReturn(job);

		OrchestrationJob result = null;
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {
		    utilitiesMock.when(Utilities::utcNow)
		                 .thenReturn(ZonedDateTime.of(202, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")));

		    result = dbService.setStatus(jobId, OrchestrationJobStatus.ERROR, "Error");
		}

		assertNotNull(result);
		assertEquals(OrchestrationJobStatus.ERROR, result.getStatus());
		assertEquals("Error", result.getMessage());
		assertEquals(ZonedDateTime.of(202, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), job.getFinishedAt());
		verify(jobRepo).findById(jobId);
		verify(jobRepo).saveAndFlush(job);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusThrowsInternalServerError() {

		final UUID jobId = UUID.randomUUID();
		when(jobRepo.findById(jobId)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.setStatus(jobId, OrchestrationJobStatus.DONE, null));
		assertEquals("Database operation error", ex.getMessage());
		verify(jobRepo).findById(jobId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsOk() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(), null, List.of(), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAllById(List.of(id));
		verify(jobRepo).findAllByIdIn(anyList(), eq(pagination));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsAndStatusNoMatch() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(OrchestrationJobStatus.DONE), null, List.of(), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByStatusesAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(OrchestrationJobStatus.PENDING), null, List.of(), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAllByStatusIn(List.of(OrchestrationJobStatus.PENDING))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAllByStatusIn(List.of(OrchestrationJobStatus.PENDING));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByOrchestrationTypeAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(), OrchestrationType.PULL, List.of(), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAll()).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAll();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsAndOrchestrationTypeNoMatch() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(), OrchestrationType.PUSH, List.of(), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);

		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByRequesterSystemsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(), null, List.of("Requester1"), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAllByRequesterSystemIn(List.of("Requester1"))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAllByRequesterSystemIn(List.of("Requester1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsAndRequesterSystemsNoMatch() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(), null, List.of("Requester1"), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester2", "Target1", "serviceDef1", null);
		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByTargetSystemsOk() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(), null, List.of(), List.of("Target1"), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAllByTargetSystemIn(List.of("Target1"))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAllByTargetSystemIn(List.of("Target1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsAndTargetSystemsNoMatch() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(), null, List.of(), List.of("Target1"), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target2", "serviceDef1", null);
		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByServiceDefinitionsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(), null, List.of(), List.of(), List.of("serviceDef1"), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAllByServiceDefinitionIn(List.of("serviceDef1"))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAllByServiceDefinitionIn(List.of("serviceDef1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsAndServiceDefinitionsNoMatch() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(), null, List.of(), List.of(), List.of("serviceDef1"), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef2", null);
		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryBySubscriptionIdsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID subscriptionId = UUID.randomUUID();
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of(subscriptionId));

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PUSH, "Requester1", "Target1", "serviceDef1", subscriptionId.toString());
		when(jobRepo.findAll()).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(1, result.getContent().size());
		verify(jobRepo).findAll();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByIdsAndSubscriptionIdsNoMatch() {
		final UUID id = UUID.randomUUID();
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID subscriptionId = UUID.randomUUID();
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(id), List.of(), null, List.of(), List.of(), List.of(), List.of(subscriptionId));

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef2", null);
		when(jobRepo.findAllById(List.of(id))).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryWithoutFiltersOk() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(
				pagination, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of());

		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "Requester1", "Target1", "serviceDef1", null);
		when(jobRepo.findAll()).thenReturn(List.of(job));
		when(jobRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(job)));

		final Page<OrchestrationJob> result = dbService.query(queryRequest);

		assertNotNull(result);
		verify(jobRepo).findAll();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryThrowsInternalServerError() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final NormalizedOrchestrationJobQueryRequest queryRequest = new NormalizedOrchestrationJobQueryRequest(pagination, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of());

		when(jobRepo.findAll()).thenThrow(new RuntimeException("DB error"));

		assertThrows(InternalServerError.class, () -> dbService.query(queryRequest));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final Collection<UUID> ids = List.of(id1, id2);

		dbService.deleteInBatch(ids);

		verify(jobRepo).deleteAllByIdInBatch(ids);
		verify(jobRepo).flush();
	}
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchThrowsInternalServerError() {
		final Collection<UUID> ids = List.of(UUID.randomUUID());

		doThrow(new RuntimeException("DB error")).when(jobRepo).deleteAllByIdInBatch(ids);

		assertThrows(InternalServerError.class, () -> dbService.deleteInBatch(ids));
		verify(jobRepo).deleteAllByIdInBatch(ids);
	}
}
