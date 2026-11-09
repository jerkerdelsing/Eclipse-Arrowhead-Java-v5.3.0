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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.repository.OrchestrationStoreRepository;

@ExtendWith(MockitoExtension.class)
public class SimpleStoreDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SimpleStoreDbService dbService;

	@Mock
	private OrchestrationStoreRepository storeRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkOk() {
		final OrchestrationSimpleStoreRequestDTO dto1 = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|serviceDef1|1.0.0", 1);
		final OrchestrationSimpleStoreRequestDTO dto2 = new OrchestrationSimpleStoreRequestDTO("Consumer2", "Provider2|serviceDef2|1.0.0", 2);
		final List<OrchestrationSimpleStoreRequestDTO> candidates = List.of(dto1, dto2);

		when(storeRepo.findByConsumerAndServiceDefinitionAndPriority(any(String.class), any(String.class), anyInt())).thenReturn(Optional.empty());
		when(storeRepo.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<OrchestrationStore> result = dbService.createBulk(candidates, "Requester");

		assertEquals(2, result.size());
		verify(storeRepo, times(2)).findByConsumerAndServiceDefinitionAndPriority(any(String.class), any(String.class), anyInt());
		verify(storeRepo).saveAllAndFlush(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingEntryThrowsInvalidParameterException() {
		final OrchestrationSimpleStoreRequestDTO dto = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|serviceDef1|1.0.0", 1);
		final List<OrchestrationSimpleStoreRequestDTO> candidates = List.of(dto);

		final OrchestrationStore existingStore = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Requester");
		when(storeRepo.findByConsumerAndServiceDefinitionAndPriority("Consumer1", "serviceDef1", 1)).thenReturn(Optional.of(existingStore));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> dbService.createBulk(candidates, "Requester"));
		assertEquals("There is already an existing entity with consumer name: Consumer1, service definition: serviceDef1, priority: 1", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkDatabaseErrorThrowsInternalServerError() {
		final OrchestrationSimpleStoreRequestDTO dto = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|serviceDef1|1.0.0", 1);
		final List<OrchestrationSimpleStoreRequestDTO> candidates = List.of(dto);

		when(storeRepo.findByConsumerAndServiceDefinitionAndPriority("Consumer1", "serviceDef1", 1)).thenReturn(Optional.empty());
		when(storeRepo.saveAllAndFlush(anyList())).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.createBulk(candidates, "Requester"));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersNoFiltersOk() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");
		final Page<OrchestrationStore> page = new PageImpl<>(List.of(store));

		when(storeRepo.findAll(pagination)).thenReturn(page);

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, null, null, null, null, null, null);

		assertEquals(1, result.getContent().size());
		verify(storeRepo).findAll(pagination);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithIdsOk() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), null, null, null, null, null, null);

		assertEquals(1, result.getContent().size());
		verify(storeRepo).findAllById(List.of(id));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithConsumerNamesAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllByConsumerIn(List.of("Consumer1"))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, List.of("Consumer1"), null, null, null, null, null);

		assertEquals(1, result.getContent().size());
		assertEquals("Consumer1", result.getContent().get(0).getConsumer());
		verify(storeRepo).findAllByConsumerIn(List.of("Consumer1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithIdsConsumerNamesButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), List.of("Consumer2"), null, null, null, null, null);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithServiceDefinitionsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllByServiceDefinitionIn(List.of("serviceDef1"))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, null, List.of("serviceDef1"), null, null, null, null);

		assertEquals(1, result.getContent().size());
		assertEquals("serviceDef1", result.getContent().get(0).getServiceDefinition());
		verify(storeRepo).findAllByServiceDefinitionIn(List.of("serviceDef1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithIdsAndServiceDefinitionsButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), null, List.of("serviceDef2"), null, null, null, null);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithServiceInstanceIdsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllByServiceInstanceIdIn(List.of("Provider1|serviceDef1|1.0.0"))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, null, null, List.of("Provider1|serviceDef1|1.0.0"), null, null, null);

		assertEquals(1, result.getContent().size());
		assertEquals("Provider1|serviceDef1|1.0.0", result.getContent().get(0).getServiceInstanceId());
		verify(storeRepo).findAllByServiceInstanceIdIn(List.of("Provider1|serviceDef1|1.0.0"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithServiceInstanceIdsButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), null, null, List.of("Provider2|serviceDef1|1.0.0"), null, null, null);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithCreatedByAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllByCreatedBy("Creator")).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, null, null, null, null, null, "Creator");

		assertEquals(1, result.getContent().size());
		assertEquals("Creator", result.getContent().get(0).getCreatedBy());
		verify(storeRepo).findAllByCreatedBy("Creator");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithCreatedByButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), null, null, null, null, null, "Creator2");

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithMinPriorityAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store1 = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 5, "Creator");
		final OrchestrationStore store2 = new OrchestrationStore("Consumer2", "serviceDef2", "Provider2|serviceDef2|1.0.0", 2, "Creator");

		when(storeRepo.findAll()).thenReturn(List.of(store1, store2));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store1)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, null, null, null, 3, null, null);

		assertEquals(1, result.getContent().size());
		verify(storeRepo).findAll();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithMinPriorityButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer2", "serviceDef2", "Provider2|serviceDef2|1.0.0", 2, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), null, null, null, 3, null, null);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithMaxPriorityAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final OrchestrationStore store1 = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 5, "Creator");
		final OrchestrationStore store2 = new OrchestrationStore("Consumer2", "serviceDef2", "Provider2|serviceDef2|1.0.0", 2, "Creator");

		when(storeRepo.findAll()).thenReturn(List.of(store1, store2));
		when(storeRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(store2)));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, null, null, null, null, null, 3, null);

		assertEquals(1, result.getContent().size());
		verify(storeRepo).findAll();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithMaxPriorityButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final UUID id = UUID.randomUUID();
		final OrchestrationStore store = new OrchestrationStore("Consumer2", "serviceDef2", "Provider2|serviceDef2|1.0.0", 5, "Creator");

		when(storeRepo.findAllById(List.of(id))).thenReturn(List.of(store));
		when(storeRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<OrchestrationStore> result = dbService.getPageByFilters(pagination, List.of(id), null, null, null, null, 3, null);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersDatabaseErrorThrowsInternalServerError() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(storeRepo.findAll(pagination)).thenThrow(new ArrowheadException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.getPageByFilters(pagination, null, null, null, null, null, null, null));

		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByConsumerOk() {
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllByConsumerOrderByPriorityAsc("Consumer1")).thenReturn(List.of(store));

		final List<OrchestrationStore> result = dbService.getByConsumer("Consumer1");

		assertEquals(1, result.size());
		assertEquals("Consumer1", result.get(0).getConsumer());
		verify(storeRepo).findAllByConsumerOrderByPriorityAsc("Consumer1");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByConsumerDatabaseErrorThrowsInternalServerError() {
		when(storeRepo.findAllByConsumerOrderByPriorityAsc("Consumer1")).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.getByConsumer("Consumer1"));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByConsumerAndServiceDefinitionOk() {
		final OrchestrationStore store = new OrchestrationStore("Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 1, "Creator");

		when(storeRepo.findAllByConsumerAndServiceDefinitionOrderByPriorityAsc("Consumer1", "serviceDef1")).thenReturn(List.of(store));

		final List<OrchestrationStore> result = dbService.getByConsumerAndServiceDefinition("Consumer1", "serviceDef1");

		assertEquals(1, result.size());
		assertEquals("Consumer1", result.get(0).getConsumer());
		assertEquals("serviceDef1", result.get(0).getServiceDefinition());
		verify(storeRepo).findAllByConsumerAndServiceDefinitionOrderByPriorityAsc("Consumer1", "serviceDef1");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByConsumerAndServiceDefinitionDatabaseErrorThrowsInternalServerError() {
		when(storeRepo.findAllByConsumerAndServiceDefinitionOrderByPriorityAsc("Consumer1", "serviceDef1")).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.getByConsumerAndServiceDefinition("Consumer1", "serviceDef1"));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrioritiesOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final Map<UUID, Integer> priorityCandidates = Map.of(id1, 1, id2, 2);

		final OrchestrationStore store1 = createStoreWithId(id1, "Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 3, "Creator");
		final OrchestrationStore store2 = createStoreWithId(id2, "Consumer1", "serviceDef1", "Provider2|serviceDef2|1.0.0", 4, "Creator");

		when(storeRepo.findAllById(priorityCandidates.keySet())).thenReturn(List.of(store1, store2));
		when(storeRepo.findAllByConsumerAndServiceDefinition("Consumer1", "serviceDef1")).thenReturn(List.of(store1, store2));
		when(storeRepo.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<OrchestrationStore> result = dbService.setPriorities(priorityCandidates, "Requester");

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(1, result.stream().filter(s -> s.getId().equals(id1)).findFirst().get().getPriority());
		assertEquals(2, result.stream().filter(s -> s.getId().equals(id2)).findFirst().get().getPriority());
		verify(storeRepo).findAllById(priorityCandidates.keySet());
		verify(storeRepo).findAllByConsumerAndServiceDefinition("Consumer1", "serviceDef1");
		verify(storeRepo).saveAllAndFlush(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrioritiesNotExistingUuidThrowsInvalidParameterException() {
		final UUID id = UUID.randomUUID();
		final Map<UUID, Integer> priorityCandidates = Map.of(id, 1);

		when(storeRepo.findAllById(priorityCandidates.keySet())).thenReturn(List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> dbService.setPriorities(priorityCandidates, "Requester"));
		assertEquals("Not existing UUID: " + id.toString(), ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrioritiesDifferentRuleSetThrowsInvalidParameterException() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final Map<UUID, Integer> priorityCandidates = Map.of(id1, 1, id2, 2);

		final OrchestrationStore store1 = createStoreWithId(id1, "Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 3, "Creator");
		final OrchestrationStore store2 = createStoreWithId(id2, "Consumer2", "serviceDef2", "Provider2|serviceDef2|1.0.0", 4, "Creator");

		when(storeRepo.findAllById(priorityCandidates.keySet())).thenReturn(List.of(store1, store2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> dbService.setPriorities(priorityCandidates, "Requester"));
		assertEquals("Subscription ids should belong to the same rule set (same consumer and service definition)", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrioritiesDuplicatePrioritiesThrowsInvalidParameterException() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final Map<UUID, Integer> priorityCandidates = Map.of(id1, 1, id2, 1);

		final OrchestrationStore store1 = createStoreWithId(id1, "Consumer1", "serviceDef1", "Provider1|serviceDef1|1.0.0", 3, "Creator");
		final OrchestrationStore store2 = createStoreWithId(id2, "Consumer1", "serviceDef1", "Provider2|serviceDef2|1.0.0", 4, "Creator");

		when(storeRepo.findAllById(priorityCandidates.keySet())).thenReturn(List.of(store1, store2));
		when(storeRepo.findAllByConsumerAndServiceDefinition("Consumer1", "serviceDef1")).thenReturn(List.of(store1, store2));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> dbService.setPriorities(priorityCandidates, "Requester"));
		assertEquals("Conflicting rules, the combination of the following fields should be unique: Consumer1, service definition: serviceDef1, priority: 1", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSetPrioritiesDatabaseErrorThrowsInternalServerError() {
		final UUID id = UUID.randomUUID();
		final Map<UUID, Integer> priorityCandidates = Map.of(id, 1);

		when(storeRepo.findAllById(priorityCandidates.keySet())).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.setPriorities(priorityCandidates, "Requester"));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteBulkOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final List<UUID> uuids = List.of(id1, id2);

		dbService.deleteBulk(uuids);

		verify(storeRepo).deleteAllById(uuids);
		verify(storeRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteBulkDatabaseErrorThrowsInternalServerError() {
		final List<UUID> uuids = List.of(UUID.randomUUID());

		doThrow(new RuntimeException("DB error")).when(storeRepo).deleteAllById(uuids);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.deleteBulk(uuids));
		assertEquals("Database operation error", ex.getMessage());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationStore createStoreWithId(final UUID id, final String consumer, final String serviceDef, final String instanceId, final int priority, final String createdBy) {
		final OrchestrationStore store = new OrchestrationStore(consumer, serviceDef, instanceId, priority, createdBy);
		try {
			final java.lang.reflect.Field idField = OrchestrationStore.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(store, id);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return store;
	}
}
