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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationLockRepository;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationLockFilter;

@SuppressWarnings("checkstyle:MagicNumberCheck")
@ExtendWith(MockitoExtension.class)
public class OrchestrationLockDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationLockDbService dbService;

	@Mock
	private OrchestrationLockRepository lockRepo;

	@Captor
	private ArgumentCaptor<List<Long>> idListCaptor;

	//=================================================================================================
	// methods

	// create()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreate() {
		final List<OrchestrationLock> candidateList = candidateList(2);

		when(lockRepo.saveAllAndFlush(eq(candidateList))).thenReturn(candidateListToEntityList(candidateList));

		final List<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.create(candidateList));

		verify(lockRepo).saveAllAndFlush(eq(candidateList));

		assertTrue(result.size() == 2);
		assertEquals(0, result.get(0).getId());
		assertEquals(candidateList.get(0).getServiceInstanceId(), result.get(0).getServiceInstanceId());
		assertEquals(1, result.get(1).getId());
		assertEquals(candidateList.get(1).getServiceInstanceId(), result.get(1).getServiceInstanceId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDBError() {
		final List<OrchestrationLock> candidateList = candidateList(2);

		doThrow(new HibernateException("test message")).when(lockRepo).saveAllAndFlush(eq(candidateList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(lockRepo).saveAllAndFlush(eq(candidateList));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateNullInput() {
		final List<OrchestrationLock> candidateList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(lockRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Orchestration lock list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateEmptyInput() {
		final List<OrchestrationLock> candidateList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(lockRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Orchestration lock list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInputContainsNull() {
		final List<OrchestrationLock> candidateList = candidateList(1);
		candidateList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(lockRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Orchestration lock list contains null element", ex.getMessage());
	}

	// getByServiceInstanceId()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByServiceInstanceId() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		final List<String> instanceIdList = records.stream().map(c -> c.getServiceInstanceId()).toList();

		when(lockRepo.findAllByServiceInstanceIdIn(eq(instanceIdList))).thenReturn(records);

		final List<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.getByServiceInstanceId(instanceIdList));

		verify(lockRepo).findAllByServiceInstanceIdIn(eq(instanceIdList));

		assertTrue(result.size() == 2);
		assertEquals(records.get(0).getServiceInstanceId(), result.get(0).getServiceInstanceId());
		assertEquals(records.get(1).getServiceInstanceId(), result.get(1).getServiceInstanceId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByServiceInstanceIdDBError() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		final List<String> instanceIdList = records.stream().map(c -> c.getServiceInstanceId()).toList();

		doThrow(new HibernateException("test message")).when(lockRepo).findAllByServiceInstanceIdIn(eq(instanceIdList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getByServiceInstanceId(instanceIdList));

		verify(lockRepo).findAllByServiceInstanceIdIn(eq(instanceIdList));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByServiceInstanceIdNullInput() {
		final List<String> instanceIdList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getByServiceInstanceId(instanceIdList));

		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByServiceInstanceIdEmptyInput() {
		final List<String> instanceIdList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getByServiceInstanceId(instanceIdList));

		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByServiceInstanceIdInputContainsNull() {
		final List<String> instanceIdList = new ArrayList<>();
		instanceIdList.add("some-id");
		instanceIdList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getByServiceInstanceId(instanceIdList));

		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByServiceInstanceIdInputContainsEmpty() {
		final List<String> instanceIdList = new ArrayList<>();
		instanceIdList.add("some-id");
		instanceIdList.add("");

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getByServiceInstanceId(instanceIdList));

		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id list contains null or empty element", ex.getMessage());
	}

	// getAll()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAll() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		when(lockRepo.findAll()).thenReturn(records);

		final List<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.getAll());

		verify(lockRepo).findAll();

		assertTrue(result.size() == 2);
		assertEquals(records.get(0).getId(), result.get(0).getId());
		assertEquals(records.get(1).getId(), result.get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetAllDBError() {
		candidateListToEntityList(candidateList(2));

		doThrow(new HibernateException("test message")).when(lockRepo).findAll();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getAll());

		verify(lockRepo).findAll();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	// query()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBased() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = List.of(records.getLast().getId());
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(List.of(records.getLast()));
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithJobIdFilter() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setOrchestrationJobId(records.getLast().getOrchestrationJobId());

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = List.of(records.getLast().getOrchestrationJobId());
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithJobIdFilterOneIsNotMatching1() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = List.of(records.getLast().getOrchestrationJobId());
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithJobIdFilterOneHasNoJobId() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setOrchestrationJobId(null);

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = List.of(records.getLast().getOrchestrationJobId());
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithServiceIdFilter() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setServiceInstanceId(records.getLast().getServiceInstanceId());

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = List.of(records.getLast().getServiceInstanceId());
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithServiceIdFilterOneIsNotMatching() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = List.of(records.getLast().getServiceInstanceId());
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithOwnerFilter() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setOwner(records.getLast().getOwner());

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = List.of(records.getLast().getOwner());
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryIdBasedWithOwnerFilterOneIsNotMatching() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = records.stream().map(r -> r.getId()).toList();
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = List.of(records.getLast().getOwner());
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllById(eq(ids))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo).findAllById(eq(ids));
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryJobBased() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = List.of(records.getLast().getOrchestrationJobId());
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByOrchestrationJobIdIn(eq(orchestrationJobIds))).thenReturn(List.of(records.getLast()));
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo).findAllByOrchestrationJobIdIn(eq(orchestrationJobIds));
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryJobBasedWithServiceIdFilter() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setServiceInstanceId(records.getLast().getServiceInstanceId());

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = records.stream().map(r -> r.getOrchestrationJobId()).toList();
		final List<String> serviceInstanceIds = List.of(records.getLast().getServiceInstanceId());
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByOrchestrationJobIdIn(eq(orchestrationJobIds))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo).findAllByOrchestrationJobIdIn(eq(orchestrationJobIds));
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryJobBasedWithServiceIdFilterOneIsNotMatching() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = records.stream().map(r -> r.getOrchestrationJobId()).toList();
		final List<String> serviceInstanceIds = List.of(records.getLast().getServiceInstanceId());
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByOrchestrationJobIdIn(eq(orchestrationJobIds))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo).findAllByOrchestrationJobIdIn(eq(orchestrationJobIds));
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryJobBasedWithOwnerFilter() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setOwner(records.getLast().getOwner());

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = records.stream().map(r -> r.getOrchestrationJobId()).toList();
		final List<String> serviceInstanceIds = null;
		final List<String> owners = List.of(records.getLast().getOwner());
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByOrchestrationJobIdIn(eq(orchestrationJobIds))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo).findAllByOrchestrationJobIdIn(eq(orchestrationJobIds));
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryJobBasedWithOwnerFilterOneIsNotMatching() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = records.stream().map(r -> r.getOrchestrationJobId()).toList();
		final List<String> serviceInstanceIds = null;
		final List<String> owners = List.of(records.getLast().getOwner());
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByOrchestrationJobIdIn(eq(orchestrationJobIds))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo).findAllByOrchestrationJobIdIn(eq(orchestrationJobIds));
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceBased() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = List.of(records.getLast().getServiceInstanceId());
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByServiceInstanceIdIn(eq(serviceInstanceIds))).thenReturn(List.of(records.getLast()));
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo).findAllByServiceInstanceIdIn(eq(serviceInstanceIds));
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryServiceBasedWithOwner() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setOwner(records.getLast().getOwner());

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = records.stream().map(r -> r.getServiceInstanceId()).toList();
		final List<String> owners = List.of(records.getLast().getOwner());
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByServiceInstanceIdIn(eq(serviceInstanceIds))).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo).findAllByServiceInstanceIdIn(eq(serviceInstanceIds));
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryOwnerBased() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = List.of(records.getLast().getOwner());
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAllByOwnerIn(eq(owners))).thenReturn(List.of(records.getLast()));
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo).findAllByOwnerIn(eq(owners));
		verify(lockRepo, never()).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoBase() {
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(lockRepo.findAll()).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(records));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 2);
		assertTrue(idListCaptor.getValue().contains(records.getFirst().getId()));
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(records.getFirst().getId(), result.getContent().getFirst().getId());
		assertEquals(records.getLast().getId(), result.getContent().getLast().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoBaseWithExpiresBeforeFilter() {
		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = Utilities.utcNow();
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setExpiresAt(expiresBefore.plusSeconds(5));
		records.getLast().setExpiresAt(expiresBefore.minusSeconds(5));

		when(lockRepo.findAll()).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoBaseWithExpiresAfterFilter() {
		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = Utilities.utcNow();
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setExpiresAt(expiresAfter.minusSeconds(5));
		records.getLast().setExpiresAt(expiresAfter.plusSeconds(5));

		when(lockRepo.findAll()).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoBaseWithExpiresBetweenFilter1() {
		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = Utilities.utcNow().plusSeconds(10);
		final ZonedDateTime expiresAfter = Utilities.utcNow();
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setExpiresAt(expiresAfter.minusSeconds(5));
		records.getLast().setExpiresAt(expiresAfter.plusSeconds(5));

		when(lockRepo.findAll()).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoBaseWithExpiresBetweenFilter2() {
		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = Utilities.utcNow().plusSeconds(10);
		final ZonedDateTime expiresAfter = Utilities.utcNow();
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setExpiresAt(expiresAfter.plusSeconds(11));
		records.getLast().setExpiresAt(expiresAfter.plusSeconds(5));

		when(lockRepo.findAll()).thenReturn(records);
		when(lockRepo.findAllByIdIn(anyCollection(), eq(pageRequest))).thenReturn(new PageImpl<OrchestrationLock>(List.of(records.getLast())));

		final Page<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo).findAll();
		verify(lockRepo).findAllByIdIn(idListCaptor.capture(), eq(pageRequest));

		assertTrue(idListCaptor.getValue().size() == 1);
		assertTrue(idListCaptor.getValue().contains(records.getLast().getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(records.getLast().getId(), result.getContent().getFirst().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDBError() {
		candidateListToEntityList(candidateList(2));

		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = PageRequest.of(0, 10);

		doThrow(new HibernateException("test message")).when(lockRepo).findAll();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo).findAll();
		verify(lockRepo, never()).findAllByIdIn(anyCollection(), any());

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNullFilter() {
		final OrchestrationLockFilter filter = null;
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo, never()).findAllByIdIn(anyCollection(), any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("filter is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNullPage() {
		final List<Long> ids = null;
		final List<String> orchestrationJobIds = null;
		final List<String> serviceInstanceIds = null;
		final List<String> owners = null;
		final ZonedDateTime expiresBefore = null;
		final ZonedDateTime expiresAfter = null;
		final OrchestrationLockFilter filter = new OrchestrationLockFilter(ids, orchestrationJobIds, serviceInstanceIds, owners, expiresBefore, expiresAfter);
		final PageRequest pageRequest = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(filter, pageRequest));

		verify(lockRepo, never()).findAllById(anyList());
		verify(lockRepo, never()).findAllByOrchestrationJobIdIn(anyList());
		verify(lockRepo, never()).findAllByServiceInstanceIdIn(anyList());
		verify(lockRepo, never()).findAllByOwnerIn(anyList());
		verify(lockRepo, never()).findAll();
		verify(lockRepo, never()).findAllByIdIn(anyCollection(), any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("pagination is null", ex.getMessage());
	}

	// changeExpiresAtByOrchestrationJobIdAndServiceInstanceId()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceId() {
		final OrchestrationLock record = candidate(1, Utilities.utcNow());
		record.setTemporary(true);

		final String orchestrationJobId = record.getOrchestrationJobId();
		final String serviceInstanceId = record.getServiceInstanceId();
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		when(lockRepo.findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId))).thenReturn(Optional.of(record));

		final Optional<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo).findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId));
		verify(lockRepo).saveAndFlush(eq(record));

		assertEquals(record.getId(), result.get().getId());
		assertEquals(newExpiry, result.get().getExpiresAt());
		assertFalse(result.get().isTemporary());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdNullExpiry() {
		final OrchestrationLock record = candidate(1, Utilities.utcNow());
		record.setTemporary(true);

		final String orchestrationJobId = record.getOrchestrationJobId();
		final String serviceInstanceId = record.getServiceInstanceId();
		final ZonedDateTime newExpiry = null;
		final boolean newTemporary = false;

		when(lockRepo.findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId))).thenReturn(Optional.of(record));

		final Optional<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo).findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId));
		verify(lockRepo).saveAndFlush(eq(record));

		assertEquals(record.getId(), result.get().getId());
		assertNull(result.get().getExpiresAt());
		assertFalse(result.get().isTemporary());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdNotFound() {
		final String orchestrationJobId = "job-id";
		final String serviceInstanceId = "instance-id";
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		when(lockRepo.findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId))).thenReturn(Optional.empty());

		final Optional<OrchestrationLock> result = assertDoesNotThrow(() -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo).findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId));
		verify(lockRepo, never()).saveAndFlush(any());

		assertTrue(result.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdDBError() {
		final String orchestrationJobId = "job-id";
		final String serviceInstanceId = "instance-id";
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		doThrow(new HibernateException("test message")).when(lockRepo).findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo).findByOrchestrationJobIdAndServiceInstanceId(eq(orchestrationJobId), eq(serviceInstanceId));
		verify(lockRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdNullJobId() {
		final String orchestrationJobId = null;
		final String serviceInstanceId = "instance-id";
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo, never()).findByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString());
		verify(lockRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Orchestration job id is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdEmptyJobId() {
		final String orchestrationJobId = " ";
		final String serviceInstanceId = "instance-id";
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo, never()).findByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString());
		verify(lockRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Orchestration job id is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdNullServiceId() {
		final String orchestrationJobId = "job-id";
		final String serviceInstanceId = null;
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo, never()).findByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString());
		verify(lockRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testChangeExpiresAtByOrchestrationJobIdAndServiceInstanceIdEmptyServiceId() {
		final String orchestrationJobId = "job-id";
		final String serviceInstanceId = " ";
		final ZonedDateTime newExpiry = Utilities.utcNow().plusHours(2);
		final boolean newTemporary = false;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(orchestrationJobId, serviceInstanceId, newExpiry, newTemporary));

		verify(lockRepo, never()).findByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString());
		verify(lockRepo, never()).saveAndFlush(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id is empty", ex.getMessage());
	}

	// deleteInBatch

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatch() {
		final List<Long> idList = List.of(1L, 2L);

		assertDoesNotThrow(() -> dbService.deleteInBatch(idList));

		verify(lockRepo).deleteAllByIdInBatch(eq(idList));
		verify(lockRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchDBError() {
		final List<Long> idList = List.of(1L, 2L);

		doThrow(new HibernateException("test message")).when(lockRepo).deleteAllByIdInBatch(eq(idList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(lockRepo).deleteAllByIdInBatch(eq(idList));
		verify(lockRepo, never()).flush();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchNullInput() {
		final List<Long> idList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(lockRepo, never()).deleteAllByIdInBatch(anyIterable());
		verify(lockRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchEmptyInput() {
		final List<Long> idList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(lockRepo, never()).deleteAllByIdInBatch(anyIterable());
		verify(lockRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchInputContainsNull() {
		final List<Long> idList = new ArrayList<>(2);
		idList.add(1L);
		idList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(lockRepo, never()).deleteAllByIdInBatch(anyIterable());
		verify(lockRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Id list is contains null element", ex.getMessage());
	}

	// deleteInBatchByExpiredBefore

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBefore() {
		final ZonedDateTime threshold = Utilities.utcNow();
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setExpiresAt(threshold.plusSeconds(3));
		records.getLast().setExpiresAt(threshold.minusSeconds(3));

		final List<OrchestrationLock> recordsFound = records.stream().filter(r -> r.getExpiresAt().isBefore(threshold)).toList();

		when(lockRepo.findAllByExpiresAtBefore(eq(threshold))).thenReturn(recordsFound);

		assertDoesNotThrow(() -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(lockRepo).findAllByExpiresAtBefore(eq(threshold));
		verify(lockRepo).deleteAllInBatch(eq(recordsFound));
		verify(lockRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeNoHits() {
		final ZonedDateTime threshold = Utilities.utcNow();
		final List<OrchestrationLock> records = candidateListToEntityList(candidateList(2));
		records.getFirst().setExpiresAt(threshold);
		records.getLast().setExpiresAt(threshold.plusSeconds(3));

		final List<OrchestrationLock> recordsFound = records.stream().filter(r -> r.getExpiresAt().isBefore(threshold)).toList();

		when(lockRepo.findAllByExpiresAtBefore(eq(threshold))).thenReturn(recordsFound);

		assertDoesNotThrow(() -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(lockRepo).findAllByExpiresAtBefore(eq(threshold));
		verify(lockRepo, never()).deleteAllInBatch(anyIterable());
		verify(lockRepo, never()).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeDBError() {
		final ZonedDateTime threshold = Utilities.utcNow();

		doThrow(new HibernateException("test message")).when(lockRepo).findAllByExpiresAtBefore(eq(threshold));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(lockRepo).findAllByExpiresAtBefore(eq(threshold));
		verify(lockRepo, never()).deleteAllInBatch(anyIterable());
		verify(lockRepo, never()).flush();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeNullInput() {
		final ZonedDateTime threshold = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(lockRepo, never()).findAllByExpiresAtBefore(any());
		verify(lockRepo, never()).deleteAllInBatch(anyIterable());
		verify(lockRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("time is null", ex.getMessage());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationLock candidate(final int num, final ZonedDateTime expiresAt) {
		return new OrchestrationLock("orch-job-id-" + num, "service-instance-id" + num, "OwnerSystem" + num, null, false);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationLock candidate(final int num) {
		return candidate(num, null);
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationLock> candidateList(final int num) {
		final List<OrchestrationLock> candidates = new ArrayList<>(num);
		for (int i = 0; i < num; ++i) {
			candidates.add(candidate(i));
		}
		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationLock> candidateListToEntityList(final List<OrchestrationLock> candidates) {
		for (int i = 0; i < candidates.size(); ++i) {
			candidates.get(i).setId(i);
		}
		return new ArrayList<OrchestrationLock>(candidates);
	}
}
