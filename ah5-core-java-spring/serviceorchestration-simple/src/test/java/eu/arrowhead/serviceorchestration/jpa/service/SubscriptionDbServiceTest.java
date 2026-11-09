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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Collection;
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

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.repository.SubscriptionRepository;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;

@ExtendWith(MockitoExtension.class)
public class SubscriptionDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SubscriptionDbService dbService;

	@Mock
	private SubscriptionRepository subscriptionRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOk() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080"));
		final SimpleOrchestrationRequest orchRequest = new SimpleOrchestrationRequest("serviceDef1", List.of("Provider1"), null, null);
		final SimpleOrchestrationSubscriptionRequest candidate = new SimpleOrchestrationSubscriptionRequest("TargetSystem", orchRequest, notifyInterface, 60L);
		final List<SimpleOrchestrationSubscriptionRequest> candidates = List.of(candidate);

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(any(), any(), any())).thenReturn(Optional.empty());
		when(subscriptionRepo.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<Subscription> result = dbService.create(candidates, "RequesterSystem");

		assertEquals(1, result.size());
		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition("RequesterSystem", "TargetSystem", "serviceDef1");
		verify(subscriptionRepo).saveAllAndFlush(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateWithExistingSubscription() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080"));
		final SimpleOrchestrationRequest orchRequest = new SimpleOrchestrationRequest("serviceDef1", List.of("Provider1"), null, null);
		final SimpleOrchestrationSubscriptionRequest candidate = new SimpleOrchestrationSubscriptionRequest("TargetSystem", orchRequest, notifyInterface, 60L);
		final List<SimpleOrchestrationSubscriptionRequest> candidates = List.of(candidate);

		final UUID existingId = UUID.randomUUID();
		final Subscription existingSubscription = new Subscription("RequesterSystem", "TargetSystem", "serviceDef1", null, "HTTPS", "{}", "{}");
		try {
			final java.lang.reflect.Field idField = Subscription.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(existingSubscription, existingId);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition("RequesterSystem", "TargetSystem", "serviceDef1")).thenReturn(Optional.of(existingSubscription));
		when(subscriptionRepo.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<Subscription> result = dbService.create(candidates, "RequesterSystem");

		assertNotNull(result);
		verify(subscriptionRepo).deleteAllById(List.of(existingId));
		verify(subscriptionRepo, times(1)).flush();
		verify(subscriptionRepo).saveAllAndFlush(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateWithNullDurationOk() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080"));
		final SimpleOrchestrationRequest orchRequest = new SimpleOrchestrationRequest("serviceDef1", List.of("Provider1"), null, null);
		final SimpleOrchestrationSubscriptionRequest candidate = new SimpleOrchestrationSubscriptionRequest("TargetSystem", orchRequest, notifyInterface, null);
		final List<SimpleOrchestrationSubscriptionRequest> candidates = List.of(candidate);

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(any(), any(), any())).thenReturn(Optional.empty());
		when(subscriptionRepo.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		final List<Subscription> result = dbService.create(candidates, "RequesterSystem");

		assertEquals(1, result.size());
		assertEquals(null, result.get(0).getExpiresAt());
		verify(subscriptionRepo).saveAllAndFlush(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateThrowsInternalServerError() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080"));
		final SimpleOrchestrationRequest orchRequest = new SimpleOrchestrationRequest("serviceDef1", List.of("Provider1"), null, null);
		final SimpleOrchestrationSubscriptionRequest candidate = new SimpleOrchestrationSubscriptionRequest("TargetSystem", orchRequest, notifyInterface, 60L);
		final List<SimpleOrchestrationSubscriptionRequest> candidates = List.of(candidate);

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(any(), any(), any())).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.create(candidates, "RequesterSystem"));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByOwnerTargetServiceDefOk() {
		final Subscription subscription = new Subscription("Owner", "Target", "serviceDef", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition("Owner", "Target", "serviceDef")).thenReturn(Optional.of(subscription));

		final Optional<Subscription> result = dbService.get("Owner", "Target", "serviceDef");

		assertTrue(result.isPresent());
		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition("Owner", "Target", "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByOwnerTargetServiceDefThrowsInternalServerError() {
		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition("Owner", "Target", "serviceDef")).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.get("Owner", "Target", "serviceDef"));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdOk() {
		final UUID id = UUID.randomUUID();
		final Subscription subscription = new Subscription("Owner", "Target", "serviceDef", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findById(id)).thenReturn(Optional.of(subscription));

		final Optional<Subscription> result = dbService.get(id);

		assertTrue(result.isPresent());
		verify(subscriptionRepo).findById(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdDatabaseErrorThrowsInternalServerError() {
		final UUID id = UUID.randomUUID();

		when(subscriptionRepo.findById(id)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.get(id));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdsOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final List<UUID> ids = List.of(id1, id2);
		final Subscription subscription1 = new Subscription("Owner1", "Target1", "serviceDef1", null, "HTTPS", "{}", "{}");
		final Subscription subscription2 = new Subscription("Owner2", "Target2", "serviceDef2", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findAllById(ids)).thenReturn(List.of(subscription1, subscription2));

		final List<Subscription> result = dbService.get(ids);

		assertEquals(2, result.size());
		verify(subscriptionRepo).findAllById(ids);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByIdsDatabaseErrorThrowsInternalServerError() {
		final List<UUID> ids = List.of(UUID.randomUUID());

		when(subscriptionRepo.findAllById(ids)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.get(ids));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdOk() {
		final UUID id = UUID.randomUUID();

		when(subscriptionRepo.existsById(id)).thenReturn(true);

		final boolean result = dbService.deleteById(id);

		assertTrue(result);
		verify(subscriptionRepo).existsById(id);
		verify(subscriptionRepo).deleteById(id);
		verify(subscriptionRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdNotExistingId() {
		final UUID id = UUID.randomUUID();

		when(subscriptionRepo.existsById(id)).thenReturn(false);

		final boolean result = dbService.deleteById(id);

		assertFalse(result);
		verify(subscriptionRepo).existsById(id);
		verify(subscriptionRepo, never()).deleteById(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdDatabaseErrorThrowsInternalServerError() {
		final UUID id = UUID.randomUUID();

		when(subscriptionRepo.existsById(id)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.deleteById(id));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryWithoutFiltersOk() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final Subscription subscription = new Subscription("Owner", "Target", "serviceDef", null, "HTTPS", "{}", "{}");
		final Page<Subscription> page = new PageImpl<>(List.of(subscription));

		when(subscriptionRepo.findAll(pagination)).thenReturn(page);

		final Page<Subscription> result = dbService.query(null, null, null, pagination);

		assertEquals(1, result.getContent().size());
		verify(subscriptionRepo).findAll(pagination);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByOwnerSystemsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final Subscription subscription = new Subscription("Owner1", "Target", "serviceDef", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findAllByOwnerSystemIn(List.of("Owner1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(subscription)));

		final Page<Subscription> result = dbService.query(List.of("Owner1"), null, null, pagination);

		assertEquals(1, result.getContent().size());
		assertEquals("Owner1", result.getContent().get(0).getOwnerSystem());
		verify(subscriptionRepo).findAllByOwnerSystemIn(List.of("Owner1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByOwnerSystemsButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(subscriptionRepo.findAllByOwnerSystemIn(List.of("Owner1"))).thenReturn(List.of());
		when(subscriptionRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<Subscription> result = dbService.query(List.of("Owner1"), null, null, pagination);

		assertEquals(0, result.getContent().size());
		verify(subscriptionRepo).findAllByOwnerSystemIn(List.of("Owner1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByTargetSystemsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final Subscription subscription = new Subscription("Owner", "Target1", "serviceDef", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findAllByTargetSystemIn(List.of("Target1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(subscription)));

		final Page<Subscription> result = dbService.query(null, List.of("Target1"), null, pagination);

		assertEquals(1, result.getContent().size());
		assertEquals("Target1", result.getContent().get(0).getTargetSystem());
		verify(subscriptionRepo).findAllByTargetSystemIn(List.of("Target1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByTargetSystemsOkButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(subscriptionRepo.findAllByTargetSystemIn(List.of("Target1"))).thenReturn(List.of());
		when(subscriptionRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<Subscription> result = dbService.query(null, List.of("Target1"), null, pagination);

		assertEquals(0, result.getContent().size());
		verify(subscriptionRepo).findAllByTargetSystemIn(List.of("Target1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByServiceDefinitionsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final Subscription subscription = new Subscription("Owner", "Target", "serviceDef1", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findAllByServiceDefinitionIn(List.of("serviceDef1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(subscription)));

		final Page<Subscription> result = dbService.query(null, null, List.of("serviceDef1"), pagination);

		assertEquals(1, result.getContent().size());
		assertEquals("serviceDef1", result.getContent().get(0).getServiceDefinition());
		verify(subscriptionRepo).findAllByServiceDefinitionIn(List.of("serviceDef1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByServiceDefinitionsButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(subscriptionRepo.findAllByServiceDefinitionIn(List.of("serviceDef1"))).thenReturn(List.of());
		when(subscriptionRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<Subscription> result = dbService.query(null, null, List.of("serviceDef1"), pagination);

		assertEquals(0, result.getContent().size());
		verify(subscriptionRepo).findAllByServiceDefinitionIn(List.of("serviceDef1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByEverythingButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(subscriptionRepo.findAllByOwnerSystemIn(List.of("Owner1"))).thenReturn(List.of());
		when(subscriptionRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<Subscription> result = dbService.query(List.of("Owner1"), List.of("Target1"), List.of("serviceDef1"), pagination);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByEverythingAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);
		final Subscription subscription = new Subscription("Owner1", "Target1", "serviceDef", null, "HTTPS", "{}", "{}");

		when(subscriptionRepo.findAllByOwnerSystemIn(List.of("Owner1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(subscription)));

		final Page<Subscription> result = dbService.query(List.of("Owner1"), List.of("Target1"), List.of("serviceDef1"), pagination);

		assertEquals(1, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByTargetAndServiceDefinitonsButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		final Subscription subscription = new Subscription("Owner1", "Target1", "serviceDef2", null, "HTTPS", "{}", "{}");
		when(subscriptionRepo.findAllByTargetSystemIn(List.of("Target1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<Subscription> result = dbService.query(null, List.of("Target1"), List.of("serviceDef1"), pagination);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByTargetAndServiceDefinitonsAndMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		final Subscription subscription = new Subscription("Owner1", "Target1", "serviceDef1", null, "HTTPS", "{}", "{}");
		when(subscriptionRepo.findAllByTargetSystemIn(List.of("Target1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(anyList(), eq(pagination))).thenReturn(new PageImpl<>(List.of(subscription)));

		final Page<Subscription> result = dbService.query(null, List.of("Target1"), List.of("serviceDef1"), pagination);

		assertEquals(1, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryByOwnerAndTargetButNoMatch() {
		final PageRequest pagination = PageRequest.of(0, 10);

		final Subscription subscription = new Subscription("Owner1", "Target1", "serviceDef1", null, "HTTPS", "{}", "{}");
		when(subscriptionRepo.findAllByOwnerSystemIn(List.of("Owner1"))).thenReturn(List.of(subscription));
		when(subscriptionRepo.findAllByIdIn(argThat(list -> list.size() == 0), eq(pagination))).thenReturn(new PageImpl<>(List.of()));

		final Page<Subscription> result = dbService.query(List.of("Owner1"), List.of("Target2"), null, pagination);

		assertEquals(0, result.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDatabaseErrorThrowsInternalServerError() {
		final PageRequest pagination = PageRequest.of(0, 10);

		when(subscriptionRepo.findAll(pagination)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.query(null, null, null, pagination));
		assertEquals("Database operation error", ex.getMessage());
	}

	//=================================================================================================
	// Tests for deleteInBatch

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchOk() {
		final UUID id1 = UUID.randomUUID();
		final UUID id2 = UUID.randomUUID();
		final Collection<UUID> ids = List.of(id1, id2);

		dbService.deleteInBatch(ids);

		verify(subscriptionRepo).deleteAllByIdInBatch(ids);
		verify(subscriptionRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchDatabaseErrorThrowsInternalServerError() {
		final Collection<UUID> ids = List.of(UUID.randomUUID());

		doThrow(new RuntimeException("DB error")).when(subscriptionRepo).deleteAllByIdInBatch(ids);

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.deleteInBatch(ids));
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeOk() {
		final ZonedDateTime time = ZonedDateTime.now();
		final Subscription subscription = new Subscription("Owner", "Target", "serviceDef", time.minusHours(1), "HTTPS", "{}", "{}");

		when(subscriptionRepo.findAllByExpiresAtBefore(time)).thenReturn(List.of(subscription));

		dbService.deleteInBatchByExpiredBefore(time);

		verify(subscriptionRepo).findAllByExpiresAtBefore(time);
		verify(subscriptionRepo).deleteAllInBatch(List.of(subscription));
		verify(subscriptionRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeNoExpiredSubscriptions() {
		final ZonedDateTime time = ZonedDateTime.now();

		when(subscriptionRepo.findAllByExpiresAtBefore(time)).thenReturn(List.of());

		dbService.deleteInBatchByExpiredBefore(time);

		verify(subscriptionRepo).findAllByExpiresAtBefore(time);
		verify(subscriptionRepo, never()).deleteAllInBatch(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeDatabaseErrorThrowsInternalServerError() {
		final ZonedDateTime time = ZonedDateTime.now();

		when(subscriptionRepo.findAllByExpiresAtBefore(time)).thenThrow(new RuntimeException("DB error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> dbService.deleteInBatchByExpiredBefore(time));
		assertEquals("Database operation error", ex.getMessage());
	}
}
