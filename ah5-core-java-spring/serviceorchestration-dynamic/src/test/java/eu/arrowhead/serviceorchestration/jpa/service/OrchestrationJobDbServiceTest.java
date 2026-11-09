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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationJobRepository;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;

@SuppressWarnings("checkstyle:MagicNumberCheck")
@ExtendWith(MockitoExtension.class)
public class OrchestrationJobDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationJobDbService dbService;

	@Mock
	private OrchestrationJobRepository jobRepo;

	@Captor
	private ArgumentCaptor<List<UUID>> uuidListCaptor;

	//=================================================================================================
	// methods

	// create()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreate() {
		final List<OrchestrationJob> candidateList = candidateList(2);

		when(jobRepo.saveAllAndFlush(eq(candidateList))).thenReturn(candidateListToEntityList(candidateList));

		final List<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.create(candidateList));

		verify(jobRepo).saveAllAndFlush(eq(candidateList));

		assertTrue(result.size() == 2);
		assertNotNull(result.getFirst().getId());
		assertEquals("RequesterSystem0", result.getFirst().getRequesterSystem());
		assertEquals("RequesterSystem1", result.getLast().getRequesterSystem());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDBError() {
		final List<OrchestrationJob> candidateList = candidateList(2);

		doThrow(new HibernateException("test message")).when(jobRepo).saveAllAndFlush(eq(candidateList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(jobRepo).saveAllAndFlush(eq(candidateList));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateNullInput() {
		final List<OrchestrationJob> candidateList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(jobRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateEmptyInput() {
		final List<OrchestrationJob> candidateList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(jobRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInputContainsNUll() {
		final List<OrchestrationJob> candidateList = candidateList(2);
		candidateList.set(1, null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(jobRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job list contains null element", ex.getMessage());
	}

	// getById()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetById() {
		final OrchestrationJob record = candidateListToEntityList(candidateList(1)).getFirst();
		final UUID id = record.getId();

		when(jobRepo.findById(eq(id))).thenReturn(Optional.of(record));

		final Optional<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.getById(id));

		verify(jobRepo).findById(eq(id));

		assertTrue(result.isPresent());
		assertEquals(id, result.get().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdNotFound() {
		final UUID id = UUID.randomUUID();

		when(jobRepo.findById(eq(id))).thenReturn(Optional.empty());

		final Optional<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.getById(id));

		verify(jobRepo).findById(eq(id));

		assertTrue(result.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdDBError() {
		final UUID id = UUID.randomUUID();

		doThrow(new HibernateException("test message")).when(jobRepo).findById(eq(id));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getById(id));

		verify(jobRepo).findById(eq(id));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdNullId() {
		final UUID id = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getById(id));

		verify(jobRepo, never()).findById(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("id is null", ex.getMessage());
	}

	// getAllByStatusIn()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusIn() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		final List<OrchestrationJobStatus> statusList = List.of(records.getFirst().getStatus());

		when(jobRepo.findAllByStatusIn(eq(statusList))).thenReturn(records);

		final List<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.getAllByStatusIn(statusList));

		verify(jobRepo).findAllByStatusIn(eq(statusList));

		assertTrue(result.size() == 2);
		assertNotNull(result.getFirst().getId());
		assertEquals("RequesterSystem0", result.getFirst().getRequesterSystem());
		assertEquals("RequesterSystem1", result.getLast().getRequesterSystem());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusInDBError() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		final List<OrchestrationJobStatus> statusList = List.of(records.getFirst().getStatus());

		doThrow(new HibernateException("test message")).when(jobRepo).findAllByStatusIn(eq(statusList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getAllByStatusIn(statusList));

		verify(jobRepo).findAllByStatusIn(eq(statusList));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusInNullInput() {
		final List<OrchestrationJobStatus> statusList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getAllByStatusIn(statusList));

		verify(jobRepo, never()).findAllByStatusIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("status list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusInEmptyInput() {
		final List<OrchestrationJobStatus> statusList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getAllByStatusIn(statusList));

		verify(jobRepo, never()).findAllByStatusIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("status list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllByStatusInInputContainsNull() {
		final List<OrchestrationJobStatus> statusList = new ArrayList<>(2);
		statusList.add(OrchestrationJobStatus.PENDING);
		statusList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getAllByStatusIn(statusList));

		verify(jobRepo, never()).findAllByStatusIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("status list contains null element", ex.getMessage());
	}

	// query()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBased() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = List.of(records.getLast().getId());
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(List.of(records.getLast()));
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithStatusFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = List.of(records.getFirst().getStatus());
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithStatusFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setStatus(OrchestrationJobStatus.DONE);

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus());
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithOwnerFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setRequesterSystem(records.getLast().getRequesterSystem());

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = List.of(records.getLast().getRequesterSystem());
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithOwnerFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = List.of(records.getLast().getRequesterSystem());
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithTargetFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setTargetSystem(records.getLast().getTargetSystem());

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithTargetFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithServiceFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setServiceDefinition(records.getLast().getServiceDefinition());

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithServiceFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithTypeFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = records.getLast().getType();
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithTypeFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setType(OrchestrationType.PUSH);

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = records.getLast().getType();
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithSubscriptionIdFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setSubscriptionId(records.getLast().getSubscriptionId());

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = List.of(records.getLast().getSubscriptionId());
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithSubscriptionIdFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = List.of(records.getLast().getSubscriptionId());
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithSubscriptionIdFilterOneHasNoSubscriptionId() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setSubscriptionId(null);

		final List<UUID> ids = records.stream().map(r -> r.getId()).toList();
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = List.of(records.getLast().getSubscriptionId());
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllById(eq(ids))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records.stream().filter(r -> !Utilities.isEmpty(r.getSubscriptionId())).toList()));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo).findAllById(eq(ids));
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBased() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setStatus(OrchestrationJobStatus.DONE);

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus());
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(List.of(records.getLast()));
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithOwnerFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setRequesterSystem(records.getLast().getRequesterSystem());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = List.of(records.getLast().getRequesterSystem());
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithOwnerFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = List.of(records.getLast().getRequesterSystem());
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithTargetFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setTargetSystem(records.getLast().getTargetSystem());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithTargetFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithServiceFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setServiceDefinition(records.getLast().getServiceDefinition());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithServiceFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithTypeFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = records.getLast().getType();
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithTypeFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setType(OrchestrationType.PUSH);

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = records.getLast().getType();
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithSubscriptionIdFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setSubscriptionId(records.getLast().getSubscriptionId());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = List.of(records.getLast().getSubscriptionId());
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryStatusBasedWithSubscriptionIdFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = List.of(records.getLast().getStatus()); // both has the same
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = List.of(records.getLast().getSubscriptionId());
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByStatusIn(eq(statuses))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo).findAllByStatusIn(eq(statuses));
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOwnerBased() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = List.of(records.getLast().getRequesterSystem());
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByRequesterSystemIn(eq(requesterSystems))).thenReturn(List.of(records.getLast()));
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo).findAllByRequesterSystemIn(eq(requesterSystems));
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOwnerBasedWithTargetFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setTargetSystem(records.getLast().getTargetSystem());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = records.stream().map(r -> r.getRequesterSystem()).toList();
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByRequesterSystemIn(eq(requesterSystems))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo).findAllByRequesterSystemIn(eq(requesterSystems));
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOwnerBasedWithTargetFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = records.stream().map(r -> r.getRequesterSystem()).toList();
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByRequesterSystemIn(eq(requesterSystems))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo).findAllByRequesterSystemIn(eq(requesterSystems));
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOwnerBasedWithServiceFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setServiceDefinition(records.getLast().getServiceDefinition());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = records.stream().map(r -> r.getRequesterSystem()).toList();
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByRequesterSystemIn(eq(requesterSystems))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo).findAllByRequesterSystemIn(eq(requesterSystems));
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOwnerBasedWithServiceFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = records.stream().map(r -> r.getRequesterSystem()).toList();
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByRequesterSystemIn(eq(requesterSystems))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo).findAllByRequesterSystemIn(eq(requesterSystems));
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTargetBased() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = List.of(records.getLast().getTargetSystem());
		final List<String> serviceDefinitions = null;
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByTargetSystemIn(eq(targetSystems))).thenReturn(List.of(records.getLast()));
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo).findAllByTargetSystemIn(eq(targetSystems));
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTargetBasedWithServiceFilter() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setServiceDefinition(records.getLast().getServiceDefinition());

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = records.stream().map(r -> r.getTargetSystem()).toList();
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByTargetSystemIn(eq(targetSystems))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo).findAllByTargetSystemIn(eq(targetSystems));
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTargetBasedWithServiceFilterOneIsNotMatching() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = records.stream().map(r -> r.getTargetSystem()).toList();
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByTargetSystemIn(eq(targetSystems))).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo).findAllByTargetSystemIn(eq(targetSystems));
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceBased() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final List<UUID> ids = null;
		final List<OrchestrationJobStatus> statuses = null;
		final OrchestrationType type = null;
		final List<String> requesterSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = List.of(records.getLast().getServiceDefinition());
		final List<String> subscriptionIds = null;
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(ids, statuses, type, requesterSystems, targetSystems, serviceDefinitions, subscriptionIds);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAllByServiceDefinitionIn(eq(serviceDefinitions))).thenReturn(List.of(records.getLast()));
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(List.of(records.getLast())));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo).findAllByServiceDefinitionIn(eq(serviceDefinitions));
		verify(jobRepo, never()).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoBase() {
		final List<OrchestrationJob> records = candidateListToEntityList(candidateList(2));

		final OrchestrationJobFilter filter = new OrchestrationJobFilter(null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(jobRepo.findAll()).thenReturn(records);
		when(jobRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationJob>(records));

		final Page<OrchestrationJob> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo).findAll();
		verify(jobRepo).findAllByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(uuidListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDBError() {
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		doThrow(new HibernateException("test message")).when(jobRepo).findAll();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo).findAll();
		verify(jobRepo, never()).findAllByIdIn(anyCollection(), any());

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNullFilter() {
		final OrchestrationJobFilter filter = null;
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo, never()).findAllByIdIn(anyCollection(), any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("filter is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNullPageRequest() {
		final OrchestrationJobFilter filter = new OrchestrationJobFilter(null, null, null, null, null, null, null);
		final PageRequest pageRequest = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(filter, pageRequest));

		verify(jobRepo, never()).findAllById(anyList());
		verify(jobRepo, never()).findAllByStatusIn(anyList());
		verify(jobRepo, never()).findAllByRequesterSystemIn(anyList());
		verify(jobRepo, never()).findAllByTargetSystemIn(anyList());
		verify(jobRepo, never()).findAllByServiceDefinitionIn(anyList());
		verify(jobRepo, never()).findAll();
		verify(jobRepo, never()).findAllByIdIn(anyCollection(), any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("pagination is null", ex.getMessage());
	}

	// setStatus()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToInProgress() {
		final OrchestrationJob record = candidateListToEntityList(candidateList(1)).getFirst();
		record.setStatus(OrchestrationJobStatus.PENDING);

		final UUID id = record.getId();
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.IN_PROGRESS;
		final String newMessage = "this is a new message";

		when(jobRepo.findById(eq(id))).thenReturn(Optional.of(record));
		when(jobRepo.saveAndFlush(eq(record))).thenReturn(record);

		final OrchestrationJob result = assertDoesNotThrow(() -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo).findById(eq(id));
		verify(jobRepo).saveAndFlush(eq(record));

		assertEquals(record.getId(), result.getId());
		assertEquals(newMessage, result.getMessage());
		assertEquals(newStatus, result.getStatus());
		assertNotNull(result.getStartedAt());
		assertNull(result.getFinishedAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToDone() {
		final OrchestrationJob record = candidateListToEntityList(candidateList(1)).getFirst();
		record.setStatus(OrchestrationJobStatus.IN_PROGRESS);
		record.setStartedAt(Utilities.utcNow().minusMinutes(10));

		final UUID id = record.getId();
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.DONE;
		final String newMessage = "this is a new message";

		when(jobRepo.findById(eq(id))).thenReturn(Optional.of(record));
		when(jobRepo.saveAndFlush(eq(record))).thenReturn(record);

		final OrchestrationJob result = assertDoesNotThrow(() -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo).findById(eq(id));
		verify(jobRepo).saveAndFlush(eq(record));

		assertEquals(record.getId(), result.getId());
		assertEquals(newMessage, result.getMessage());
		assertEquals(newStatus, result.getStatus());
		assertNotNull(result.getStartedAt());
		assertNotNull(result.getFinishedAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToError() {
		final OrchestrationJob record = candidateListToEntityList(candidateList(1)).getFirst();
		record.setStatus(OrchestrationJobStatus.IN_PROGRESS);
		record.setStartedAt(Utilities.utcNow().minusMinutes(10));

		final UUID id = record.getId();
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.ERROR;
		final String newMessage = "";

		when(jobRepo.findById(eq(id))).thenReturn(Optional.of(record));
		when(jobRepo.saveAndFlush(eq(record))).thenReturn(record);

		final OrchestrationJob result = assertDoesNotThrow(() -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo).findById(eq(id));
		verify(jobRepo).saveAndFlush(eq(record));

		assertEquals(record.getId(), result.getId());
		assertNull(result.getMessage());
		assertEquals(newStatus, result.getStatus());
		assertNotNull(result.getStartedAt());
		assertNotNull(result.getFinishedAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusDBError() {
		final UUID id = UUID.randomUUID();
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.IN_PROGRESS;
		final String newMessage = "this is a new message";

		doThrow(new HibernateException("test message")).when(jobRepo).findById(eq(id));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo).findById(eq(id));
		verify(jobRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusNullId() {
		final UUID id = null;
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.IN_PROGRESS;
		final String newMessage = "this is a new message";

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo, never()).findById(any());
		verify(jobRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("jobId is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusNullStatus() {
		final UUID id = UUID.randomUUID();
		final OrchestrationJobStatus newStatus = null;
		final String newMessage = "this is a new message";

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo, never()).findById(any());
		verify(jobRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("status is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusToPending() {
		final UUID id = UUID.randomUUID();
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.PENDING;
		final String newMessage = "this is a new message";

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo, never()).findById(any());
		verify(jobRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("status can't be changed to PENDING", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetStatusNotExists() {
		final UUID id = UUID.randomUUID();
		final OrchestrationJobStatus newStatus = OrchestrationJobStatus.IN_PROGRESS;
		final String newMessage = "this is a new message";

		when(jobRepo.findById(eq(id))).thenReturn(Optional.empty());

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.setStatus(id, newStatus, newMessage));

		verify(jobRepo).findById(any());
		verify(jobRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job does not exists: " + id, ex.getMessage());
	}

	// deleteInBatch()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatch() {
		final List<UUID> idList = List.of(UUID.randomUUID(), UUID.randomUUID());

		assertDoesNotThrow(() -> dbService.deleteInBatch(idList));

		verify(jobRepo).deleteAllByIdInBatch(eq(idList));
		verify(jobRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchDBError() {
		final List<UUID> idList = List.of(UUID.randomUUID(), UUID.randomUUID());

		doThrow(new HibernateException("test message")).when(jobRepo).deleteAllByIdInBatch(eq(idList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(jobRepo).deleteAllByIdInBatch(eq(idList));
		verify(jobRepo, never()).flush();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchNullInput() {
		final List<UUID> idList = null;
		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(jobRepo, never()).deleteAllByIdInBatch(anyIterable());
		verify(jobRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchEmptyInput() {
		final List<UUID> idList = null;
		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(jobRepo, never()).deleteAllByIdInBatch(anyIterable());
		verify(jobRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchInputContainsNull() {
		final List<UUID> idList = new ArrayList<>(2);
		idList.add(UUID.randomUUID());
		idList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(jobRepo, never()).deleteAllByIdInBatch(anyIterable());
		verify(jobRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job id list contains null element", ex.getMessage());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationJob candidate(final int num) {
		final OrchestrationJob candidate = new OrchestrationJob(OrchestrationType.PULL, "RequesterSystem" + num, "TargetSystem" + num, "testService" + num, "subs-id-" + num);
		candidate.setId(null);
		return candidate;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationJob> candidateList(final int num) {
		final List<OrchestrationJob> candidates = new ArrayList<>(num);
		for (int i = 0; i < num; ++i) {
			candidates.add(candidate(i));
		}
		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationJob> candidateListToEntityList(final List<OrchestrationJob> candidates) {
		for (final OrchestrationJob job : candidates) {
			job.setId(UUID.randomUUID());
		}
		return new ArrayList<OrchestrationJob>(candidates);
	}
}
