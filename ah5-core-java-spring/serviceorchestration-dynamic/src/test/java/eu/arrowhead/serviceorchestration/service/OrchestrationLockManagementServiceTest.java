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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockListResponseDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationLockRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationLockFilter;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationLockManagementValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationLockManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationLockManagementService lockService;

	@Mock
	private OrchestrationLockManagementValidation validator;

	@Mock
	private OrchestrationLockDbService lockDbService;

	@Mock
	private PageService pageService;

	@Spy
	private DTOConverter dtoConverter;

	@Captor
	private ArgumentCaptor<List<String>> stringListCaptor;

	@Captor
	private ArgumentCaptor<List<Long>> longListCaptor;

	@Captor
	private ArgumentCaptor<Collection<Long>> longCollectionCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationLock>> lockListCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationLockFilter> lockFilterCaptor;

	//=================================================================================================
	// method

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreate() {
		final String origin = "test.origin";
		final OrchestrationLockRequestDTO lockRequestDTO = new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", null);
		final OrchestrationLockListRequestDTO requestDTO = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO));
		final OrchestrationLock lockRecord = new OrchestrationLock(lockRequestDTO.serviceInstanceId(), lockRequestDTO.owner(), null);
		lockRecord.setId(1);
		final List<OrchestrationLock> lockRecordList = List.of(lockRecord);

		when(validator.validateAndNormalizeCreateService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(lockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(lockDbService.create(anyList())).thenReturn(lockRecordList);

		final OrchestrationLockListResponseDTO result = assertDoesNotThrow(() -> lockService.create(requestDTO, origin));

		verify(validator).validateAndNormalizeCreateService(eq(requestDTO), eq(origin));
		verify(lockDbService).getByServiceInstanceId(stringListCaptor.capture());
		verify(lockDbService, never()).deleteInBatch(anyList());
		verify(lockDbService).create(lockListCaptor.capture());
		verify(dtoConverter).convertOrchestrationLockListToDTO(eq(lockRecordList), eq(1L));

		assertEquals(lockRequestDTO.serviceInstanceId(), stringListCaptor.getValue().get(0));
		assertEquals(lockRequestDTO.serviceInstanceId(), lockListCaptor.getValue().get(0).getServiceInstanceId());
		assertEquals(lockRequestDTO.owner(), lockListCaptor.getValue().get(0).getOwner());
		assertEquals(lockRecord.getId(), result.entries().get(0).id());
		assertEquals(lockRecord.getServiceInstanceId(), result.entries().get(0).serviceInstanceId());
		assertEquals(lockRecord.getOwner(), result.entries().get(0).owner());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExisting() {
		final String origin = "test.origin";
		final OrchestrationLockRequestDTO lockRequestDTO = new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", null);
		final OrchestrationLockListRequestDTO requestDTO = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO));
		final OrchestrationLock existinglockRecord = new OrchestrationLock(lockRequestDTO.serviceInstanceId(), lockRequestDTO.owner(), null);
		existinglockRecord.setId(1);

		when(validator.validateAndNormalizeCreateService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(lockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of(existinglockRecord));

		final Throwable ex = assertThrows(Throwable.class, () -> lockService.create(requestDTO, origin));

		verify(validator).validateAndNormalizeCreateService(eq(requestDTO), eq(origin));
		verify(lockDbService).getByServiceInstanceId(stringListCaptor.capture());
		verify(lockDbService, never()).deleteInBatch(anyList());
		verify(lockDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertOrchestrationLockListToDTO(anyList(), anyLong());

		assertEquals(lockRequestDTO.serviceInstanceId(), stringListCaptor.getValue().get(0));
		assertEquals("Already locked: TestProvider|testService|1.0.0", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExisting2() {
		final String origin = "test.origin";
		final OrchestrationLockRequestDTO lockRequestDTO = new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", null);
		final OrchestrationLockListRequestDTO requestDTO = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO));
		final OrchestrationLock existinglockRecord = new OrchestrationLock(lockRequestDTO.serviceInstanceId(), lockRequestDTO.owner(), Utilities.utcNow().plusMinutes(1));
		existinglockRecord.setId(1);
		existinglockRecord.setOrchestrationJobId((UUID.randomUUID().toString()));

		when(validator.validateAndNormalizeCreateService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(lockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of(existinglockRecord));

		final Throwable ex = assertThrows(Throwable.class, () -> lockService.create(requestDTO, origin));

		verify(validator).validateAndNormalizeCreateService(eq(requestDTO), eq(origin));
		verify(lockDbService).getByServiceInstanceId(stringListCaptor.capture());
		verify(lockDbService, never()).deleteInBatch(anyList());
		verify(lockDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertOrchestrationLockListToDTO(anyList(), anyLong());

		assertEquals(lockRequestDTO.serviceInstanceId(), stringListCaptor.getValue().get(0));
		assertEquals("Already locked: TestProvider|testService|1.0.0", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExisting3() {
		final String origin = "test.origin";
		final OrchestrationLockRequestDTO lockRequestDTO = new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", null);
		final OrchestrationLockListRequestDTO requestDTO = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO));
		final OrchestrationLock existinglockRecord = new OrchestrationLock(lockRequestDTO.serviceInstanceId(), lockRequestDTO.owner(), null);
		existinglockRecord.setId(1);
		existinglockRecord.setOrchestrationJobId((UUID.randomUUID().toString()));

		when(validator.validateAndNormalizeCreateService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(lockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of(existinglockRecord));

		final Throwable ex = assertThrows(Throwable.class, () -> lockService.create(requestDTO, origin));

		verify(validator).validateAndNormalizeCreateService(eq(requestDTO), eq(origin));
		verify(lockDbService).getByServiceInstanceId(stringListCaptor.capture());
		verify(lockDbService, never()).deleteInBatch(anyList());
		verify(lockDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertOrchestrationLockListToDTO(anyList(), anyLong());

		assertEquals(lockRequestDTO.serviceInstanceId(), stringListCaptor.getValue().get(0));
		assertEquals("Already locked: TestProvider|testService|1.0.0", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExistingButExpired() {
		final String origin = "test.origin";
		final OrchestrationLockRequestDTO lockRequestDTO = new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", null);
		final OrchestrationLockListRequestDTO requestDTO = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO));
		final OrchestrationLock existinglockRecord = new OrchestrationLock(lockRequestDTO.serviceInstanceId(), lockRequestDTO.owner(), Utilities.utcNow().minusMinutes(1));
		existinglockRecord.setId(1);
		existinglockRecord.setOrchestrationJobId(UUID.randomUUID().toString());
		final OrchestrationLock lockRecord = new OrchestrationLock(lockRequestDTO.serviceInstanceId(), lockRequestDTO.owner(), null);
		lockRecord.setId(2);
		final List<OrchestrationLock> lockRecordList = List.of(lockRecord);

		when(validator.validateAndNormalizeCreateService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(lockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of(existinglockRecord));
		when(lockDbService.create(anyList())).thenReturn(lockRecordList);

		final OrchestrationLockListResponseDTO result = assertDoesNotThrow(() -> lockService.create(requestDTO, origin));

		verify(validator).validateAndNormalizeCreateService(eq(requestDTO), eq(origin));
		verify(lockDbService).getByServiceInstanceId(stringListCaptor.capture());
		verify(lockDbService).deleteInBatch(longListCaptor.capture());
		verify(lockDbService).create(lockListCaptor.capture());
		verify(dtoConverter).convertOrchestrationLockListToDTO(eq(lockRecordList), eq(1L));

		assertEquals(lockRequestDTO.serviceInstanceId(), stringListCaptor.getValue().get(0));
		assertEquals(existinglockRecord.getId(), longListCaptor.getValue().get(0));
		assertEquals(lockRequestDTO.serviceInstanceId(), lockListCaptor.getValue().get(0).getServiceInstanceId());
		assertEquals(lockRequestDTO.owner(), lockListCaptor.getValue().get(0).getOwner());
		assertEquals(lockRecord.getId(), result.entries().get(0).id());
		assertEquals(lockRecord.getServiceInstanceId(), result.entries().get(0).serviceInstanceId());
		assertEquals(lockRecord.getOwner(), result.entries().get(0).owner());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDBException() {
		final String origin = "test.origin";
		final OrchestrationLockRequestDTO lockRequestDTO = new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", null);
		final OrchestrationLockListRequestDTO requestDTO = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO));

		when(validator.validateAndNormalizeCreateService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		doThrow(new InternalServerError("test message")).when(lockDbService).getByServiceInstanceId(anyList());

		final Throwable ex = assertThrows(Throwable.class, () -> lockService.create(requestDTO, origin));

		verify(validator).validateAndNormalizeCreateService(eq(requestDTO), eq(origin));
		verify(lockDbService).getByServiceInstanceId(stringListCaptor.capture());
		verify(lockDbService, never()).deleteInBatch(anyList());
		verify(lockDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertOrchestrationLockListToDTO(anyList(), anyLong());

		assertEquals(lockRequestDTO.serviceInstanceId(), stringListCaptor.getValue().get(0));
		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuery() {
		final String origin = "test.orign";
		final OrchestrationLockQueryRequestDTO requestDTO = new OrchestrationLockQueryRequestDTO(null, null, null, List.of("TestProvider|testService|1.0.0"), null, null, null);
		final OrchestrationLock lockRecord = new OrchestrationLock("TestProvider|testService|1.0.0", "TestManager", null);
		lockRecord.setId(1);
		final PageImpl<OrchestrationLock> lockPage = new PageImpl<OrchestrationLock>(List.of(lockRecord));

		when(validator.validateAndNormalizeQueryService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(pageService.getPageRequest(any(), any(), anyList(), anyString(), eq(origin))).thenReturn(PageRequest.of(0, 1));
		when(lockDbService.query(any(), any())).thenReturn(lockPage);

		final OrchestrationLockListResponseDTO result = assertDoesNotThrow(() -> lockService.query(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryService(eq(requestDTO), eq(origin));
		verify(pageService).getPageRequest(any(), eq(Direction.DESC), eq(OrchestrationLock.SORTABLE_FIELDS_BY), eq(OrchestrationLock.DEFAULT_SORT_FIELD), eq(origin));
		verify(lockDbService).query(lockFilterCaptor.capture(), any(PageRequest.class));
		verify(dtoConverter).convertOrchestrationLockListToDTO(eq(lockPage.getContent()), eq(lockPage.getTotalElements()));

		assertEquals("TestProvider|testService|1.0.0", lockFilterCaptor.getValue().getServiceInstanceIds().get(0));
		assertEquals("TestProvider|testService|1.0.0", result.entries().get(0).serviceInstanceId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDBException() {
		final String origin = "test.orign";
		final OrchestrationLockQueryRequestDTO requestDTO = new OrchestrationLockQueryRequestDTO(null, null, null, List.of("TestProvider|testService|1.0.0"), null, null, null);
		final OrchestrationLock lockRecord = new OrchestrationLock("TestProvider|testService|1.0.0", "TestManager", null);
		lockRecord.setId(1);

		when(validator.validateAndNormalizeQueryService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(pageService.getPageRequest(any(), any(), anyList(), anyString(), eq(origin))).thenReturn(PageRequest.of(0, 1));
		doThrow(new InternalServerError("test message")).when(lockDbService).query(any(), any());

		final Throwable ex = assertThrows(Throwable.class, () -> lockService.query(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryService(eq(requestDTO), eq(origin));
		verify(pageService).getPageRequest(any(), eq(Direction.DESC), eq(OrchestrationLock.SORTABLE_FIELDS_BY), eq(OrchestrationLock.DEFAULT_SORT_FIELD), eq(origin));
		verify(lockDbService).query(lockFilterCaptor.capture(), any(PageRequest.class));
		verify(dtoConverter, never()).convertOrchestrationLockListToDTO(anyList(), anyLong());

		assertEquals("TestProvider|testService|1.0.0", lockFilterCaptor.getValue().getServiceInstanceIds().get(0));
		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemove() {
		final String origin = "test.orign";
		final String owner = "TestManager";
		final List<String> instanceIds = List.of("TestProvider|testService|1.0.0");
		final OrchestrationLock lockRecord = new OrchestrationLock(instanceIds.getFirst(), owner, null);
		lockRecord.setId(1);
		final List<OrchestrationLock> lockRecordList = List.of(lockRecord);

		when(validator.validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin))).thenReturn(Pair.of(owner, instanceIds));
		when(lockDbService.getByServiceInstanceId(eq(instanceIds))).thenReturn(lockRecordList);

		assertDoesNotThrow(() -> lockService.remove(owner, instanceIds, origin));

		verify(validator).validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin));
		verify(lockDbService).getByServiceInstanceId(eq(instanceIds));
		verify(lockDbService).deleteInBatch(longCollectionCaptor.capture());

		assertTrue(longCollectionCaptor.getValue().size() == 1);
		assertTrue(longCollectionCaptor.getValue().contains(lockRecord.getId()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveNotExists() {
		final String origin = "test.orign";
		final String owner = "TestManager";
		final List<String> instanceIds = List.of("TestProvider|testService|1.0.0");

		when(validator.validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin))).thenReturn(Pair.of(owner, instanceIds));
		when(lockDbService.getByServiceInstanceId(eq(instanceIds))).thenReturn(List.of());

		assertDoesNotThrow(() -> lockService.remove(owner, instanceIds, origin));

		verify(validator).validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin));
		verify(lockDbService).getByServiceInstanceId(eq(instanceIds));
		verify(lockDbService, never()).deleteInBatch(anyCollection());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveTemporaryLock() {
		final String origin = "test.orign";
		final String owner = "TestManager";
		final List<String> instanceIds = List.of("TestProvider|testService|1.0.0");
		final OrchestrationLock lockRecord = new OrchestrationLock(instanceIds.getFirst(), owner, null);
		lockRecord.setId(1);
		lockRecord.setTemporary(true);
		final List<OrchestrationLock> lockRecordList = List.of(lockRecord);

		when(validator.validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin))).thenReturn(Pair.of(owner, instanceIds));
		when(lockDbService.getByServiceInstanceId(eq(instanceIds))).thenReturn(lockRecordList);

		assertDoesNotThrow(() -> lockService.remove(owner, instanceIds, origin));

		verify(validator).validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin));
		verify(lockDbService).getByServiceInstanceId(eq(instanceIds));
		verify(lockDbService, never()).deleteInBatch(anyCollection());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDifferentOwner() {
		final String origin = "test.orign";
		final String owner = "TestManager";
		final List<String> instanceIds = List.of("TestProvider|testService|1.0.0");
		final OrchestrationLock lockRecord = new OrchestrationLock(instanceIds.getFirst(), "OtherOne", null);
		lockRecord.setId(1);
		final List<OrchestrationLock> lockRecordList = List.of(lockRecord);

		when(validator.validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin))).thenReturn(Pair.of(owner, instanceIds));
		when(lockDbService.getByServiceInstanceId(eq(instanceIds))).thenReturn(lockRecordList);

		assertDoesNotThrow(() -> lockService.remove(owner, instanceIds, origin));

		verify(validator).validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin));
		verify(lockDbService).getByServiceInstanceId(eq(instanceIds));
		verify(lockDbService, never()).deleteInBatch(anyCollection());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveDBError() {
		final String origin = "test.orign";
		final String owner = "TestManager";
		final List<String> instanceIds = List.of("TestProvider|testService|1.0.0");

		when(validator.validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin))).thenReturn(Pair.of(owner, instanceIds));
		doThrow(new InternalServerError("test message")).when(lockDbService).getByServiceInstanceId(eq(instanceIds));

		final Throwable ex = assertThrows(Throwable.class, () -> lockService.remove(owner, instanceIds, origin));

		verify(validator).validateAndNormalizeRemoveService(eq(owner), eq(instanceIds), eq(origin));
		verify(lockDbService).getByServiceInstanceId(eq(instanceIds));
		verify(lockDbService, never()).deleteInBatch(anyCollection());

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}
}
