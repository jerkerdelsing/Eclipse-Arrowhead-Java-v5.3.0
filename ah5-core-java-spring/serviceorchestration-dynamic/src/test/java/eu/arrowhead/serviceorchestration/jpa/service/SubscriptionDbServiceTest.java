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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.repository.SubscriptionRepository;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;

@SuppressWarnings("checkstyle:MagicNumberCheck")
@ExtendWith(MockitoExtension.class)
public class SubscriptionDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SubscriptionDbService dbService;

	@Mock
	private SubscriptionRepository subscriptionRepo;

	@Captor
	private ArgumentCaptor<List<Subscription>> subscriptionListCaptor;

	@Captor
	private ArgumentCaptor<List<UUID>> uuidListCaptor;

	//=================================================================================================
	// methods

	// create()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreate() {
		final List<OrchestrationSubscription> candidateList = candidateList(2);

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
		when(subscriptionRepo.saveAllAndFlush(anyIterable())).thenReturn(convertCandidatesToEntiyList(candidateList));

		final List<Subscription> result = assertDoesNotThrow(() -> dbService.create(candidateList));

		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq("RequesterSystem0"), eq("TargetSystem0"), eq("testService0"));
		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq("RequesterSystem1"), eq("TargetSystem1"), eq("testService1"));
		verify(subscriptionRepo, never()).deleteAllById(anyIterable());
		verify(subscriptionRepo).flush();
		verify(subscriptionRepo).saveAllAndFlush(subscriptionListCaptor.capture());

		final List<Subscription> saveAndFlushInput = subscriptionListCaptor.getValue();
		assertTrue(saveAndFlushInput.size() == 2);
		assertEquals(candidateList.get(0).getOrchestrationForm().getRequesterSystemName(), saveAndFlushInput.get(0).getOwnerSystem());
		assertEquals(candidateList.get(0).getOrchestrationForm().getTargetSystemName(), saveAndFlushInput.get(0).getTargetSystem());
		assertEquals(candidateList.get(0).getOrchestrationForm().getServiceDefinition(), saveAndFlushInput.get(0).getServiceDefinition());
		assertEquals(candidateList.get(0).getNotifyProtocol(), saveAndFlushInput.get(0).getNotifyProtocol());
		assertEquals(Utilities.toJson(candidateList.get(0).getNotifyProperties()), saveAndFlushInput.get(0).getNotifyProperties());
		assertEquals(Utilities.toJson(candidateList.get(0).getOrchestrationForm().extractOrchestrationRequestDTO()), saveAndFlushInput.get(0).getOrchestrationRequest());

		assertEquals(candidateList.get(1).getOrchestrationForm().getRequesterSystemName(), saveAndFlushInput.get(1).getOwnerSystem());
		assertEquals(candidateList.get(1).getOrchestrationForm().getTargetSystemName(), saveAndFlushInput.get(1).getTargetSystem());
		assertEquals(candidateList.get(1).getOrchestrationForm().getServiceDefinition(), saveAndFlushInput.get(1).getServiceDefinition());
		assertEquals(candidateList.get(1).getNotifyProtocol(), saveAndFlushInput.get(1).getNotifyProtocol());
		assertEquals(Utilities.toJson(candidateList.get(1).getNotifyProperties()), saveAndFlushInput.get(1).getNotifyProperties());
		assertEquals(Utilities.toJson(candidateList.get(0).getOrchestrationForm().extractOrchestrationRequestDTO()), saveAndFlushInput.get(0).getOrchestrationRequest());

		assertTrue(result.size() == 2);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateOneAlreadyExists() {
		final List<OrchestrationSubscription> candidateList = candidateList(2, true);
		final Subscription existing = convertCandidateToEntiy(candidateList.get(1));

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString())).thenReturn(Optional.empty()).thenReturn(Optional.of(existing));
		when(subscriptionRepo.saveAllAndFlush(anyIterable())).thenReturn(convertCandidatesToEntiyList(candidateList));

		final List<Subscription> result = assertDoesNotThrow(() -> dbService.create(candidateList));

		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq("RequesterSystem0"), eq("TargetSystem0"), eq("testService0"));
		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq("RequesterSystem1"), eq("TargetSystem1"), eq("testService1"));
		verify(subscriptionRepo).deleteAllById(uuidListCaptor.capture());
		verify(subscriptionRepo).flush();
		verify(subscriptionRepo).saveAllAndFlush(subscriptionListCaptor.capture());

		final List<UUID> deleteAllByIdInput = uuidListCaptor.getValue();
		assertTrue(deleteAllByIdInput.size() == 1);
		assertEquals(existing.getId(), deleteAllByIdInput.get(0));

		final List<Subscription> saveAndFlushInput = subscriptionListCaptor.getValue();
		assertTrue(saveAndFlushInput.size() == 2);
		assertEquals(candidateList.get(0).getOrchestrationForm().getRequesterSystemName(), saveAndFlushInput.get(0).getOwnerSystem());
		assertEquals(candidateList.get(0).getOrchestrationForm().getTargetSystemName(), saveAndFlushInput.get(0).getTargetSystem());
		assertEquals(candidateList.get(0).getOrchestrationForm().getServiceDefinition(), saveAndFlushInput.get(0).getServiceDefinition());
		assertEquals(candidateList.get(0).getNotifyProtocol(), saveAndFlushInput.get(0).getNotifyProtocol());
		assertEquals(Utilities.toJson(candidateList.get(0).getNotifyProperties()), saveAndFlushInput.get(0).getNotifyProperties());
		assertEquals(Utilities.toJson(candidateList.get(0).getOrchestrationForm().extractOrchestrationRequestDTO()), saveAndFlushInput.get(0).getOrchestrationRequest());

		assertEquals(candidateList.get(1).getOrchestrationForm().getRequesterSystemName(), saveAndFlushInput.get(1).getOwnerSystem());
		assertEquals(candidateList.get(1).getOrchestrationForm().getTargetSystemName(), saveAndFlushInput.get(1).getTargetSystem());
		assertEquals(candidateList.get(1).getOrchestrationForm().getServiceDefinition(), saveAndFlushInput.get(1).getServiceDefinition());
		assertEquals(candidateList.get(1).getNotifyProtocol(), saveAndFlushInput.get(1).getNotifyProtocol());
		assertEquals(Utilities.toJson(candidateList.get(1).getNotifyProperties()), saveAndFlushInput.get(1).getNotifyProperties());
		assertEquals(Utilities.toJson(candidateList.get(0).getOrchestrationForm().extractOrchestrationRequestDTO()), saveAndFlushInput.get(0).getOrchestrationRequest());

		assertTrue(result.size() == 2);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateDBError() {
		final List<OrchestrationSubscription> candidateList = candidateList(2);

		doThrow(new HibernateException("test message")).when(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq("RequesterSystem0"), eq("TargetSystem0"), eq("testService0"));
		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq("RequesterSystem1"), eq("TargetSystem1"), eq("testService1"));
		verify(subscriptionRepo, never()).deleteAllById(anyIterable());
		verify(subscriptionRepo, never()).flush();
		verify(subscriptionRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateNullInput() {
		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(null));

		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());
		verify(subscriptionRepo, never()).deleteAllById(anyIterable());
		verify(subscriptionRepo, never()).flush();
		verify(subscriptionRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateEmptyInput() {
		final List<OrchestrationSubscription> candidateList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());
		verify(subscriptionRepo, never()).deleteAllById(anyIterable());
		verify(subscriptionRepo, never()).flush();
		verify(subscriptionRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateInputContainsNoll() {
		final List<OrchestrationSubscription> candidateList = candidateList(2);
		candidateList.set(1, null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.create(candidateList));

		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());
		verify(subscriptionRepo, never()).deleteAllById(anyIterable());
		verify(subscriptionRepo, never()).flush();
		verify(subscriptionRepo, never()).saveAllAndFlush(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription candidate list contains null element", ex.getMessage());
	}

	// get(final UUID id)

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getById() {
		final Subscription subscriptionRecord = convertCandidateToEntiy(candidate(1, null));
		final UUID id = subscriptionRecord.getId();

		when(subscriptionRepo.findById(eq(id))).thenReturn(Optional.of(subscriptionRecord));

		final Optional<Subscription> result = assertDoesNotThrow(() -> dbService.get(id));

		verify(subscriptionRepo).findById(eq(id));

		assertTrue(result.isPresent());
		assertEquals(id, result.get().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdDBError() {
		final Subscription subscriptionRecord = convertCandidateToEntiy(candidate(1, null));
		final UUID id = subscriptionRecord.getId();

		doThrow(new HibernateException("test message")).when(subscriptionRepo).findById(eq(id));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(id));

		verify(subscriptionRepo).findById(eq(id));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdNullInput() {
		final UUID id = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(id));

		verify(subscriptionRepo, never()).findById(any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id is null", ex.getMessage());
	}

	// get(final List<UUID> ids)

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdList() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		final List<UUID> idList = subscriptionRecords.stream().map(r -> r.getId()).toList();

		when(subscriptionRepo.findAllById(eq(idList))).thenReturn(subscriptionRecords);

		final List<Subscription> result = assertDoesNotThrow(() -> dbService.get(idList));

		verify(subscriptionRepo).findAllById(eq(idList));

		assertTrue(result.size() == 2);
		assertEquals(idList.get(0), result.get(0).getId());
		assertEquals(idList.get(1), result.get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdListDBError() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		final List<UUID> idList = subscriptionRecords.stream().map(r -> r.getId()).toList();

		doThrow(new HibernateException("test message")).when(subscriptionRepo).findAllById(eq(idList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(idList));

		verify(subscriptionRepo).findAllById(eq(idList));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdListNullInput() {
		final List<UUID> idList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(idList));

		verify(subscriptionRepo, never()).findAllById(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdListEmptyInput() {
		final List<UUID> idList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(idList));

		verify(subscriptionRepo, never()).findAllById(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByIdListInputContainsNull() {
		final List<UUID> idList = new ArrayList<UUID>(2);
		idList.add(UUID.randomUUID());
		idList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(idList));

		verify(subscriptionRepo, never()).findAllById(anyIterable());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id list contains null element", ex.getMessage());
	}

	// get(final String ownerSystem, final String targetSystem, final String serviceDefinition)

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByTriplet() {
		final Subscription subscriptionRecord = convertCandidateToEntiy(candidate(1, null));
		final String ownerSystem = subscriptionRecord.getOwnerSystem();
		final String targetSystem = subscriptionRecord.getTargetSystem();
		final String serviceDef = subscriptionRecord.getServiceDefinition();

		when(subscriptionRepo.findByOwnerSystemAndTargetSystemAndServiceDefinition(eq(ownerSystem), eq(targetSystem), eq(serviceDef))).thenReturn(Optional.of(subscriptionRecord));

		final Optional<Subscription> result = assertDoesNotThrow(() -> dbService.get(ownerSystem, targetSystem, serviceDef));

		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq(ownerSystem), eq(targetSystem), eq(serviceDef));

		assertTrue(result.isPresent());
		assertEquals(subscriptionRecord.getId(), result.get().getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByTripletDBError() {
		final Subscription subscriptionRecord = convertCandidateToEntiy(candidate(1, null));
		final String ownerSystem = subscriptionRecord.getOwnerSystem();
		final String targetSystem = subscriptionRecord.getTargetSystem();
		final String serviceDef = subscriptionRecord.getServiceDefinition();

		doThrow(new HibernateException("test message")).when(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq(ownerSystem), eq(targetSystem), eq(serviceDef));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(ownerSystem, targetSystem, serviceDef));

		verify(subscriptionRepo).findByOwnerSystemAndTargetSystemAndServiceDefinition(eq(ownerSystem), eq(targetSystem), eq(serviceDef));

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByTripletNullOwnerInput() {
		final String ownerSystem = null;
		final String targetSystem = "TargetSystem";
		final String serviceDef = "testService";

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(ownerSystem, targetSystem, serviceDef));

		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("ownerSystem is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByTripletNullTargetInput() {
		final String ownerSystem = "OwnerSystem";
		final String targetSystem = null;
		final String serviceDef = "testService";

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(ownerSystem, targetSystem, serviceDef));

		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("targetSystem is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getByTripletNullServiceDefInput() {
		final String ownerSystem = "OwnerSystem";
		final String targetSystem = "TargetSystem";
		final String serviceDef = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.get(ownerSystem, targetSystem, serviceDef));

		verify(subscriptionRepo, never()).findByOwnerSystemAndTargetSystemAndServiceDefinition(anyString(), anyString(), anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("serviceDefinition is empty", ex.getMessage());
	}

	// getAll

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getAll() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));

		when(subscriptionRepo.findAll()).thenReturn(subscriptionRecords);

		final List<Subscription> result = assertDoesNotThrow(() -> dbService.getAll());

		verify(subscriptionRepo).findAll();

		assertTrue(result.size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getAllDBError() {
		doThrow(new HibernateException("test message")).when(subscriptionRepo).findAll();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.getAll());

		verify(subscriptionRepo).findAll();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	// query()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryOwnerBased() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setOwnerSystem(subscriptionRecords.get(1).getOwnerSystem());
		final List<String> ownersSystems = List.of(subscriptionRecords.get(1).getOwnerSystem());
		final List<String> targetSystems = List.of(subscriptionRecords.get(1).getTargetSystem());
		final List<String> serviceDefinitions = List.of(subscriptionRecords.get(1).getServiceDefinition());
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByOwnerSystemIn(eq(ownersSystems))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(List.of(subscriptionRecords.get(1))));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo).findByOwnerSystemIn(eq(ownersSystems));
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryOwnerBasedOnly() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		final List<String> ownersSystems = List.of(subscriptionRecords.get(1).getOwnerSystem());
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByOwnerSystemIn(eq(ownersSystems))).thenReturn(List.of(subscriptionRecords.getLast()));
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(List.of(subscriptionRecords.get(1))));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo).findByOwnerSystemIn(eq(ownersSystems));
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryOwnerBasedAndSameTarget() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setOwnerSystem(subscriptionRecords.get(1).getOwnerSystem());
		subscriptionRecords.get(0).setTargetSystem(subscriptionRecords.get(1).getTargetSystem());
		final List<String> ownersSystems = List.of(subscriptionRecords.get(1).getOwnerSystem());
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByOwnerSystemIn(eq(ownersSystems))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo).findByOwnerSystemIn(eq(ownersSystems));
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(0).getId()));
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryOwnerBasedAndSameService() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setOwnerSystem(subscriptionRecords.get(1).getOwnerSystem());
		subscriptionRecords.get(0).setServiceDefinition(subscriptionRecords.get(1).getServiceDefinition());
		final List<String> ownersSystems = List.of(subscriptionRecords.get(1).getOwnerSystem());
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByOwnerSystemIn(eq(ownersSystems))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo).findByOwnerSystemIn(eq(ownersSystems));
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(0).getId()));
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryTargetBased() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setTargetSystem(subscriptionRecords.get(1).getTargetSystem());
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of(subscriptionRecords.get(1).getTargetSystem());
		final List<String> serviceDefinitions = List.of(subscriptionRecords.get(1).getServiceDefinition());
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByTargetSystemIn(eq(targetSystems))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(List.of(subscriptionRecords.get(1))));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo).findByTargetSystemIn(eq(targetSystems));
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryTargetBasedOnly() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of(subscriptionRecords.get(1).getTargetSystem());
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByTargetSystemIn(eq(targetSystems))).thenReturn(List.of(subscriptionRecords.getLast()));
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(List.of(subscriptionRecords.get(1))));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo).findByTargetSystemIn(eq(targetSystems));
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryTargetBasedAndSameOwner() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setOwnerSystem(subscriptionRecords.get(1).getOwnerSystem());
		subscriptionRecords.get(0).setTargetSystem(subscriptionRecords.get(1).getTargetSystem());
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of(subscriptionRecords.get(1).getTargetSystem());
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByTargetSystemIn(eq(targetSystems))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo).findByTargetSystemIn(eq(targetSystems));
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(0).getId()));
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryTargetBasedAndSameService() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setTargetSystem(subscriptionRecords.get(1).getTargetSystem());
		subscriptionRecords.get(0).setServiceDefinition(subscriptionRecords.get(1).getServiceDefinition());
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of(subscriptionRecords.get(1).getTargetSystem());
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByTargetSystemIn(eq(targetSystems))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo).findByTargetSystemIn(eq(targetSystems));
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(0).getId()));
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryServiceBasedOnly() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setServiceDefinition(subscriptionRecords.get(1).getServiceDefinition());
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of(subscriptionRecords.get(1).getServiceDefinition());
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByServiceDefinitionIn(eq(serviceDefinitions))).thenReturn(List.of(subscriptionRecords.getLast()));
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(List.of(subscriptionRecords.get(1))));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo).findByServiceDefinitionIn(eq(serviceDefinitions));
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 1);
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryServiceAndSameOwner() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setOwnerSystem(subscriptionRecords.get(1).getOwnerSystem());
		subscriptionRecords.get(0).setServiceDefinition(subscriptionRecords.get(1).getServiceDefinition());
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of(subscriptionRecords.get(1).getServiceDefinition());
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByServiceDefinitionIn(eq(serviceDefinitions))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo).findByServiceDefinitionIn(eq(serviceDefinitions));
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(0).getId()));
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryServiceAndSameTarget() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setTargetSystem(subscriptionRecords.get(1).getTargetSystem());
		subscriptionRecords.get(0).setServiceDefinition(subscriptionRecords.get(1).getServiceDefinition());
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of(subscriptionRecords.get(1).getServiceDefinition());
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findByServiceDefinitionIn(eq(serviceDefinitions))).thenReturn(subscriptionRecords);
		when(subscriptionRepo.findByIdIn(anyList(), eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo).findByServiceDefinitionIn(eq(serviceDefinitions));
		verify(subscriptionRepo, never()).findAll(eq(pageRequest));
		verify(subscriptionRepo).findByIdIn(uuidListCaptor.capture(), eq(pageRequest));

		assertTrue(uuidListCaptor.getValue().size() == 2);
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(0).getId()));
		assertTrue(uuidListCaptor.getValue().contains(subscriptionRecords.get(1).getId()));

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryNoBase() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findAll(eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo).findAll(eq(pageRequest));
		verify(subscriptionRepo, never()).findByIdIn(anyList(), any());

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryNoBaseNullFilters() {
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		final List<String> ownersSystems = null;
		final List<String> targetSystems = null;
		final List<String> serviceDefinitions = null;
		final PageRequest pageRequest = PageRequest.of(0, 10);

		when(subscriptionRepo.findAll(eq(pageRequest))).thenReturn(new PageImpl<Subscription>(subscriptionRecords));

		final Page<Subscription> result = assertDoesNotThrow(() -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo).findAll(eq(pageRequest));
		verify(subscriptionRepo, never()).findByIdIn(anyList(), any());

		assertTrue(result.getContent().size() == 2);
		assertEquals(subscriptionRecords.get(0).getId(), result.getContent().get(0).getId());
		assertEquals(subscriptionRecords.get(1).getId(), result.getContent().get(1).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryDBError() {
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = PageRequest.of(0, 10);

		doThrow(new HibernateException("test message")).when(subscriptionRepo).findAll(eq(pageRequest));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo).findAll(eq(pageRequest));
		verify(subscriptionRepo, never()).findByIdIn(anyList(), any());

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void queryNullPageRequest() {
		final List<String> ownersSystems = List.of();
		final List<String> targetSystems = List.of();
		final List<String> serviceDefinitions = List.of();
		final PageRequest pageRequest = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.query(ownersSystems, targetSystems, serviceDefinitions, pageRequest));

		verify(subscriptionRepo, never()).findByOwnerSystemIn(anyList());
		verify(subscriptionRepo, never()).findByTargetSystemIn(anyList());
		verify(subscriptionRepo, never()).findByServiceDefinitionIn(anyList());
		verify(subscriptionRepo, never()).findAll(any(PageRequest.class));
		verify(subscriptionRepo, never()).findByIdIn(anyList(), any());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("pagination is null", ex.getMessage());
	}

	// deleteById

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteById() {
		final UUID id = UUID.randomUUID();

		when(subscriptionRepo.existsById(eq(id))).thenReturn(true);

		final Boolean result = assertDoesNotThrow(() -> dbService.deleteById(id));

		verify(subscriptionRepo).existsById(eq(id));
		verify(subscriptionRepo).deleteById(eq(id));
		verify(subscriptionRepo).flush();

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdNotExists() {
		final UUID id = UUID.randomUUID();

		when(subscriptionRepo.existsById(eq(id))).thenReturn(false);

		final Boolean result = assertDoesNotThrow(() -> dbService.deleteById(id));

		verify(subscriptionRepo).existsById(eq(id));
		verify(subscriptionRepo, never()).deleteById(any());
		verify(subscriptionRepo, never()).flush();

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdDBErroe() {
		final UUID id = UUID.randomUUID();

		doThrow(new HibernateException("test exception")).when(subscriptionRepo).existsById(eq(id));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteById(id));

		verify(subscriptionRepo).existsById(eq(id));
		verify(subscriptionRepo, never()).deleteById(any());
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	// deleteInBatch

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatch() {
		final List<UUID> idList = List.of(UUID.randomUUID(), UUID.randomUUID());

		assertDoesNotThrow(() -> dbService.deleteInBatch(idList));

		verify(subscriptionRepo).deleteAllByIdInBatch(eq(idList));
		verify(subscriptionRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchDBError() {
		final List<UUID> idList = List.of(UUID.randomUUID(), UUID.randomUUID());

		doThrow(new HibernateException("test message")).when(subscriptionRepo).deleteAllByIdInBatch(eq(idList));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(subscriptionRepo).deleteAllByIdInBatch(eq(idList));
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchNullInput() {
		final List<UUID> idList = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(subscriptionRepo, never()).deleteAllByIdInBatch(anyList());
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchEmptyInput() {
		final List<UUID> idList = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(subscriptionRepo, never()).deleteAllByIdInBatch(anyList());
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchContainsNullInput() {
		final List<UUID> idList = new ArrayList<UUID>();
		idList.add(UUID.randomUUID());
		idList.add(null);

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatch(idList));

		verify(subscriptionRepo, never()).deleteAllByIdInBatch(anyList());
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription id list contains null element", ex.getMessage());
	}

	// deleteInBatchByExpiredBefore

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBefore() {
		final ZonedDateTime threshold = Utilities.utcNow();
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setExpiresAt(threshold.plusSeconds(3));
		subscriptionRecords.get(1).setExpiresAt(threshold.minusSeconds(3));
		final List<Subscription> expiredRecords = subscriptionRecords.stream().filter(r -> r.getExpiresAt().isBefore(threshold)).toList();

		when(subscriptionRepo.findAllByExpiresAtBefore(eq(threshold))).thenReturn(expiredRecords);

		assertDoesNotThrow(() -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(subscriptionRepo).findAllByExpiresAtBefore(eq(threshold));
		verify(subscriptionRepo).deleteAllInBatch(eq(expiredRecords));
		verify(subscriptionRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeNoHits() {
		final ZonedDateTime threshold = Utilities.utcNow();
		final List<Subscription> subscriptionRecords = convertCandidatesToEntiyList(candidateList(2));
		subscriptionRecords.get(0).setExpiresAt(threshold.plusSeconds(3));
		subscriptionRecords.get(1).setExpiresAt(threshold);
		final List<Subscription> expiredRecords = subscriptionRecords.stream().filter(r -> r.getExpiresAt().isBefore(threshold)).toList();

		when(subscriptionRepo.findAllByExpiresAtBefore(eq(threshold))).thenReturn(expiredRecords);

		assertDoesNotThrow(() -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(subscriptionRepo).findAllByExpiresAtBefore(eq(threshold));
		verify(subscriptionRepo, never()).deleteAllInBatch(anyIterable());
		verify(subscriptionRepo, never()).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeDBError() {
		final ZonedDateTime threshold = Utilities.utcNow();

		doThrow(new HibernateException("test message")).when(subscriptionRepo).findAllByExpiresAtBefore(eq(threshold));

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(subscriptionRepo).findAllByExpiresAtBefore(eq(threshold));
		verify(subscriptionRepo, never()).deleteAllInBatch(anyIterable());
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof InternalServerError);
		assertEquals("Database operation error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteInBatchByExpiredBeforeNullInput() {
		final ZonedDateTime threshold = null;

		final Throwable ex = assertThrows(Throwable.class, () -> dbService.deleteInBatchByExpiredBefore(threshold));

		verify(subscriptionRepo, never()).findAllByExpiresAtBefore(any());
		verify(subscriptionRepo, never()).deleteAllInBatch(anyIterable());
		verify(subscriptionRepo, never()).flush();

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("time is null", ex.getMessage());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationNotifyInterfaceDTO orchestrationNotifyInterfaceDTO(final int num) {
		return new OrchestrationNotifyInterfaceDTO("MQTT" + num, Map.of("foo" + num, "bar" + num));
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationServiceRequirementDTO orchestrationServiceRequirementDTO(final int num) {
		return new OrchestrationServiceRequirementDTO("testService" + num, null, List.of("1.1." + num), null, null, null, null, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationRequestDTO orchestrationRequestDTO(final int num) {
		return new OrchestrationRequestDTO(orchestrationServiceRequirementDTO(num), null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSubscriptionRequestDTO orchestrationSubscriptionRequestDTO(final int num, final Long duration) {
		return new OrchestrationSubscriptionRequestDTO("TargetSystem" + num, orchestrationRequestDTO(num), orchestrationNotifyInterfaceDTO(num), duration);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationSubscription candidate(final int num, final Long duration) {
		return new OrchestrationSubscription("RequesterSystem" + num, orchestrationSubscriptionRequestDTO(num, duration));
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationSubscription> candidateList(final int num, final boolean hasDuration) {
		final List<OrchestrationSubscription> candidates = new ArrayList<>(num);
		for (int i = 0; i < num; ++i) {
			candidates.add(candidate(i, hasDuration ? i * 10L : null));
		}
		return candidates;
	}

	//-------------------------------------------------------------------------------------------------
	private List<OrchestrationSubscription> candidateList(final int num) {
		return candidateList(num, false);
	}

	//-------------------------------------------------------------------------------------------------
	private Subscription convertCandidateToEntiy(final OrchestrationSubscription candidate) {
		return new Subscription(
				UUID.randomUUID(),
				candidate.getOrchestrationForm().getRequesterSystemName(),
				candidate.getOrchestrationForm().getTargetSystemName(),
				candidate.getOrchestrationForm().getServiceDefinition(),
				candidate.getDuration() == null ? null : Utilities.utcNow().plusSeconds(candidate.getDuration()),
				candidate.getNotifyProtocol(),
				Utilities.toJson(candidate.getNotifyProperties()),
				Utilities.toJson(candidate.getOrchestrationForm().extractOrchestrationRequestDTO()));
	}

	//-------------------------------------------------------------------------------------------------
	private List<Subscription> convertCandidatesToEntiyList(final List<OrchestrationSubscription> candidateList) {
		return candidateList.stream().map(candidate -> convertCandidateToEntiy(candidate)).toList();
	}
}