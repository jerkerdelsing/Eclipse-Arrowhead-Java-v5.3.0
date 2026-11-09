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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationJobFilter;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationPushManagementValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationPushManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationPushManagementService pushService;

	@Mock
	private OrchestrationFromContextValidation formContextValidator;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Spy
	private DTOConverter dtoConverter;

	@Mock
	private OrchestrationPushManagementValidation validator;

	@Mock
	private PageService pageService;

	@Captor
	private ArgumentCaptor<List<String>> stringListCaptor;

	@Captor
	private ArgumentCaptor<PageRequest> pageCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationSubscription>> subscriptionListCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationJob>> jobListCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationForm> orchestrationFormCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationJobFilter> jobFilterCaptor;

	@Captor
	private ArgumentCaptor<Collection<UUID>> uuidCollectionCaptor;

	@Captor
	private ArgumentCaptor<List<UUID>> uuidListCaptor;

	//=================================================================================================
	// method

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		ReflectionTestUtils.setField(dtoConverter, "mapper", new ObjectMapper());
	}

	// pushSubscribe

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribe() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "testService";
		final OrchestrationSubscriptionRequestDTO subscriptionRequestDTO = new OrchestrationSubscriptionRequestDTO(targetSystem,
				new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null),
				new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar")), null);
		final OrchestrationSubscriptionListRequestDTO requestDTO = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionRequestDTO));
		final Subscription subscriptionRecord = new Subscription(UUID.randomUUID(), requesterSystem, targetSystem, serviceDef, null, subscriptionRequestDTO.notifyInterface().protocol(),
				Utilities.toJson(subscriptionRequestDTO.notifyInterface().properties()), Utilities.toJson(subscriptionRequestDTO.orchestrationRequest()));
		final List<Subscription> subscriptionRecordList = List.of(subscriptionRecord);

		when(subscriptionDbService.create(anyList())).thenReturn(subscriptionRecordList);

		final OrchestrationSubscriptionListResponseDTO result = assertDoesNotThrow(() -> pushService.pushSubscribe(requesterSystem, requestDTO, origin));

		verify(validator).validateAndNormalizePushSubscribeService(subscriptionListCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchestrationFormCaptor.capture(), eq(origin));
		verify(subscriptionDbService).create(subscriptionListCaptor.capture());
		verify(dtoConverter).convertSubscriptionListToDTO(eq(subscriptionRecordList), eq(1L));

		final List<OrchestrationSubscription> contextValidatorInput = subscriptionListCaptor.getAllValues().get(0);
		assertEquals(requesterSystem, contextValidatorInput.get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(targetSystem, contextValidatorInput.get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, contextValidatorInput.get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals(requesterSystem, orchestrationFormCaptor.getValue().getRequesterSystemName());
		assertEquals(targetSystem, orchestrationFormCaptor.getValue().getTargetSystemName());
		assertEquals(serviceDef, orchestrationFormCaptor.getValue().getServiceDefinition());

		final List<OrchestrationSubscription> createInput = subscriptionListCaptor.getAllValues().get(1);
		assertEquals(requesterSystem, createInput.get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(targetSystem, createInput.get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, createInput.get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals(requesterSystem, result.entries().get(0).ownerSystemName());
		assertEquals(targetSystem, result.entries().get(0).targetSystemName());
		assertEquals(serviceDef, result.entries().get(0).orchestrationRequest().serviceRequirement().serviceDefinition());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeNullDTO() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";

		doThrow(new InvalidParameterException("test message", origin)).when(validator).validateAndNormalizePushSubscribeService(anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushSubscribe(requesterSystem, null, origin));

		verify(validator).validateAndNormalizePushSubscribeService(subscriptionListCaptor.capture(), eq(origin));
		verify(formContextValidator, never()).validate(any(), anyString());
		verify(subscriptionDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertSubscriptionListToDTO(anyList(), anyLong());

		assertTrue(subscriptionListCaptor.getValue().size() == 0);
		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeEmptyList() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final OrchestrationSubscriptionListRequestDTO subscriptionRequestDTO = new OrchestrationSubscriptionListRequestDTO(List.of());

		doThrow(new InvalidParameterException("test message", origin)).when(validator).validateAndNormalizePushSubscribeService(anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushSubscribe(requesterSystem, subscriptionRequestDTO, origin));

		verify(validator).validateAndNormalizePushSubscribeService(subscriptionListCaptor.capture(), eq(origin));
		verify(formContextValidator, never()).validate(any(), anyString());
		verify(subscriptionDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertSubscriptionListToDTO(anyList(), anyLong());

		assertTrue(subscriptionListCaptor.getValue().size() == 0);
		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeDBError() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "testService";
		final OrchestrationSubscriptionRequestDTO subscriptionRequestDTO = new OrchestrationSubscriptionRequestDTO(targetSystem,
				new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null),
				new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar")), null);
		final OrchestrationSubscriptionListRequestDTO requestDTO = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionRequestDTO));

		doThrow(new InternalServerError("test message")).when(subscriptionDbService).create(anyList());

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushSubscribe(requesterSystem, requestDTO, origin));

		verify(validator).validateAndNormalizePushSubscribeService(subscriptionListCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchestrationFormCaptor.capture(), eq(origin));
		verify(subscriptionDbService).create(subscriptionListCaptor.capture());
		verify(dtoConverter, never()).convertSubscriptionListToDTO(anyList(), anyLong());

		final List<OrchestrationSubscription> contextValidatorInput = subscriptionListCaptor.getAllValues().get(0);
		assertEquals(requesterSystem, contextValidatorInput.get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(targetSystem, contextValidatorInput.get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, contextValidatorInput.get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals(requesterSystem, orchestrationFormCaptor.getValue().getRequesterSystemName());
		assertEquals(targetSystem, orchestrationFormCaptor.getValue().getTargetSystemName());
		assertEquals(serviceDef, orchestrationFormCaptor.getValue().getServiceDefinition());

		final List<OrchestrationSubscription> createInput = subscriptionListCaptor.getAllValues().get(1);
		assertEquals(requesterSystem, createInput.get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(targetSystem, createInput.get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, createInput.get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeDuplicate() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "testService";
		final OrchestrationSubscriptionRequestDTO subscriptionRequestDTO1 = new OrchestrationSubscriptionRequestDTO(targetSystem,
				new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null),
				new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar")), null);
		final OrchestrationSubscriptionRequestDTO subscriptionRequestDTO2 = new OrchestrationSubscriptionRequestDTO(targetSystem,
				new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null),
				new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar")), null);
		final OrchestrationSubscriptionListRequestDTO requestDTO = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionRequestDTO1, subscriptionRequestDTO2));

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushSubscribe(requesterSystem, requestDTO, origin));

		verify(validator).validateAndNormalizePushSubscribeService(subscriptionListCaptor.capture(), eq(origin));
		verify(formContextValidator, never()).validate(any(), anyString());
		verify(subscriptionDbService, never()).create(anyList());
		verify(dtoConverter, never()).convertSubscriptionListToDTO(anyList(), anyLong());

		final List<OrchestrationSubscription> contextValidatorInput = subscriptionListCaptor.getAllValues().get(0);
		assertEquals(requesterSystem, contextValidatorInput.get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(targetSystem, contextValidatorInput.get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, contextValidatorInput.get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals("Duplicate subscription request for: TestManager#TargetSystem#testService", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	// pushTrigger

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushTriggerWithSystemName() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(pushService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(targetSystem), List.of());
		final Subscription subscriptionRecord = new Subscription(UUID.randomUUID(), requesterSystem, targetSystem, serviceDef, null, null, null, null);
		final PageImpl<Subscription> subscriptionRecordPage = new PageImpl<Subscription>(List.of(subscriptionRecord));
		final OrchestrationJob jobRecord = new OrchestrationJob(OrchestrationType.PUSH, requesterSystem, targetSystem, serviceDef, subscriptionRecord.getId().toString());
		jobRecord.setId(UUID.randomUUID());
		final List<OrchestrationJob> jobRecordList = List.of(jobRecord);

		when(subscriptionDbService.query(anyList(), anyList(), anyList(), any())).thenReturn(subscriptionRecordPage);
		when(orchJobDbService.query(any(), any())).thenReturn(new PageImpl<OrchestrationJob>(List.of()));
		when(orchJobDbService.create(anyList())).thenReturn(jobRecordList);

		final OrchestrationPushJobListResponseDTO result = assertDoesNotThrow(() -> pushService.pushTrigger(requesterSystem, dto, origin));

		verify(subscriptionDbService).query(stringListCaptor.capture(), stringListCaptor.capture(), stringListCaptor.capture(), pageCaptor.capture());
		verify(subscriptionDbService, never()).get(anyList());
		verify(orchJobDbService).query(jobFilterCaptor.capture(), pageCaptor.capture());
		verify(orchJobDbService).create(jobListCaptor.capture());
		verify(jobQueue).addAll(uuidCollectionCaptor.capture());
		verify(dtoConverter).convertOrchestrationJobListToDTO(anyList());

		assertEquals(targetSystem, stringListCaptor.getAllValues().get(1).get(0));
		assertTrue(stringListCaptor.getAllValues().get(0).size() == 0);
		assertTrue(stringListCaptor.getAllValues().get(2).size() == 0);

		final PageRequest subsQueryPageReq = pageCaptor.getAllValues().get(0);
		assertEquals(0, subsQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, subsQueryPageReq.getPageSize());

		assertTrue(jobFilterCaptor.getValue().getIds().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getStatuses().size() == 2);
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.PENDING));
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.IN_PROGRESS));
		assertEquals(OrchestrationType.PUSH, jobFilterCaptor.getValue().getType());
		assertTrue(jobFilterCaptor.getValue().getRequesterSystems().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getTargetSystems().size() == 0);
		assertNull(jobFilterCaptor.getValue().getServiceDefinitions());
		assertTrue(jobFilterCaptor.getValue().getSubscriptionIds().size() == 1);
		assertEquals(subscriptionRecord.getId().toString(), jobFilterCaptor.getValue().getSubscriptionIds().get(0));

		final PageRequest jobQueryPageReq = pageCaptor.getAllValues().get(1);
		assertEquals(0, jobQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, jobQueryPageReq.getPageSize());
		assertTrue(jobQueryPageReq.getSort().getOrderFor(OrchestrationJob.DEFAULT_SORT_FIELD).isDescending());

		final List<OrchestrationJob> jobCreateInput = jobListCaptor.getValue();
		assertEquals(subscriptionRecord.getId().toString(), jobCreateInput.get(0).getSubscriptionId());
		assertEquals(requesterSystem, jobCreateInput.get(0).getRequesterSystem());
		assertEquals(targetSystem, jobCreateInput.get(0).getTargetSystem());
		assertEquals(serviceDef, jobCreateInput.get(0).getServiceDefinition());
		assertEquals(OrchestrationType.PUSH, jobCreateInput.get(0).getType());

		assertTrue(uuidCollectionCaptor.getValue().size() == 1);
		assertTrue(uuidCollectionCaptor.getValue().contains(jobRecord.getId()));

		assertEquals(jobRecord.getId().toString(), result.jobs().get(0).id());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushTriggerWithSubscriptionId() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(pushService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String subScriptionId = UUID.randomUUID().toString();
		final String serviceDef = "serviceDef";
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of(subScriptionId));
		final Subscription subscriptionRecord = new Subscription(UUID.fromString(subScriptionId), requesterSystem, targetSystem, serviceDef, null, null, null, null);
		final List<Subscription> subscriptionRecordList = List.of(subscriptionRecord);
		final OrchestrationJob jobRecord = new OrchestrationJob(OrchestrationType.PUSH, requesterSystem, targetSystem, serviceDef, subscriptionRecord.getId().toString());
		jobRecord.setId(UUID.randomUUID());
		final List<OrchestrationJob> jobRecordList = List.of(jobRecord);

		when(subscriptionDbService.get(uuidListCaptor.capture())).thenReturn(subscriptionRecordList);
		when(orchJobDbService.query(any(), any())).thenReturn(new PageImpl<OrchestrationJob>(List.of()));
		when(orchJobDbService.create(anyList())).thenReturn(jobRecordList);

		final OrchestrationPushJobListResponseDTO result = assertDoesNotThrow(() -> pushService.pushTrigger(requesterSystem, dto, origin));

		verify(subscriptionDbService).get(uuidListCaptor.capture());
		verify(subscriptionDbService, never()).query(anyList(), anyList(), anyList(), any());
		verify(orchJobDbService).query(jobFilterCaptor.capture(), pageCaptor.capture());
		verify(orchJobDbService).create(jobListCaptor.capture());
		verify(jobQueue).addAll(uuidCollectionCaptor.capture());
		verify(dtoConverter).convertOrchestrationJobListToDTO(anyList());

		assertEquals(subScriptionId, uuidListCaptor.getValue().get(0).toString());

		final PageRequest subsQueryPageReq = pageCaptor.getValue();
		assertEquals(0, subsQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, subsQueryPageReq.getPageSize());

		assertTrue(jobFilterCaptor.getValue().getIds().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getStatuses().size() == 2);
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.PENDING));
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.IN_PROGRESS));
		assertEquals(OrchestrationType.PUSH, jobFilterCaptor.getValue().getType());
		assertTrue(jobFilterCaptor.getValue().getRequesterSystems().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getTargetSystems().size() == 0);
		assertNull(jobFilterCaptor.getValue().getServiceDefinitions());
		assertTrue(jobFilterCaptor.getValue().getSubscriptionIds().size() == 1);
		assertEquals(subscriptionRecord.getId().toString(), jobFilterCaptor.getValue().getSubscriptionIds().get(0));

		final List<OrchestrationJob> jobCreateInput = jobListCaptor.getValue();
		assertEquals(subscriptionRecord.getId().toString(), jobCreateInput.get(0).getSubscriptionId());
		assertEquals(requesterSystem, jobCreateInput.get(0).getRequesterSystem());
		assertEquals(targetSystem, jobCreateInput.get(0).getTargetSystem());
		assertEquals(serviceDef, jobCreateInput.get(0).getServiceDefinition());
		assertEquals(OrchestrationType.PUSH, jobCreateInput.get(0).getType());

		assertTrue(uuidCollectionCaptor.getValue().size() == 1);
		assertTrue(uuidCollectionCaptor.getValue().contains(jobRecord.getId()));

		assertEquals(jobRecord.getId().toString(), result.jobs().get(0).id());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushTriggerWithEmpty() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(pushService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of());
		final Subscription subscriptionRecord = new Subscription(UUID.randomUUID(), requesterSystem, targetSystem, serviceDef, null, null, null, null);
		final PageImpl<Subscription> subscriptionRecordPage = new PageImpl<Subscription>(List.of(subscriptionRecord));
		final OrchestrationJob jobRecord = new OrchestrationJob(OrchestrationType.PUSH, requesterSystem, targetSystem, serviceDef, subscriptionRecord.getId().toString());
		jobRecord.setId(UUID.randomUUID());
		final List<OrchestrationJob> jobRecordList = List.of(jobRecord);

		when(subscriptionDbService.query(anyList(), anyList(), anyList(), any())).thenReturn(subscriptionRecordPage);
		when(orchJobDbService.query(any(), any())).thenReturn(new PageImpl<OrchestrationJob>(List.of()));
		when(orchJobDbService.create(anyList())).thenReturn(jobRecordList);

		final OrchestrationPushJobListResponseDTO result = assertDoesNotThrow(() -> pushService.pushTrigger(requesterSystem, dto, origin));

		verify(subscriptionDbService).query(stringListCaptor.capture(), stringListCaptor.capture(), stringListCaptor.capture(), pageCaptor.capture());
		verify(subscriptionDbService, never()).get(anyList());
		verify(orchJobDbService).query(jobFilterCaptor.capture(), pageCaptor.capture());
		verify(orchJobDbService).create(jobListCaptor.capture());
		verify(jobQueue).addAll(uuidCollectionCaptor.capture());
		verify(dtoConverter).convertOrchestrationJobListToDTO(anyList());

		assertEquals(requesterSystem, stringListCaptor.getAllValues().get(0).get(0));
		assertTrue(stringListCaptor.getAllValues().get(1).size() == 0);
		assertTrue(stringListCaptor.getAllValues().get(2).size() == 0);

		final PageRequest subsQueryPageReq = pageCaptor.getAllValues().get(0);
		assertEquals(0, subsQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, subsQueryPageReq.getPageSize());

		assertTrue(jobFilterCaptor.getValue().getIds().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getStatuses().size() == 2);
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.PENDING));
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.IN_PROGRESS));
		assertEquals(OrchestrationType.PUSH, jobFilterCaptor.getValue().getType());
		assertTrue(jobFilterCaptor.getValue().getRequesterSystems().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getTargetSystems().size() == 0);
		assertNull(jobFilterCaptor.getValue().getServiceDefinitions());
		assertTrue(jobFilterCaptor.getValue().getSubscriptionIds().size() == 1);
		assertEquals(subscriptionRecord.getId().toString(), jobFilterCaptor.getValue().getSubscriptionIds().get(0));

		final PageRequest jobQueryPageReq = pageCaptor.getAllValues().get(1);
		assertEquals(0, jobQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, jobQueryPageReq.getPageSize());
		assertTrue(jobQueryPageReq.getSort().getOrderFor(OrchestrationJob.DEFAULT_SORT_FIELD).isDescending());

		final List<OrchestrationJob> jobCreateInput = jobListCaptor.getValue();
		assertEquals(subscriptionRecord.getId().toString(), jobCreateInput.get(0).getSubscriptionId());
		assertEquals(requesterSystem, jobCreateInput.get(0).getRequesterSystem());
		assertEquals(targetSystem, jobCreateInput.get(0).getTargetSystem());
		assertEquals(serviceDef, jobCreateInput.get(0).getServiceDefinition());
		assertEquals(OrchestrationType.PUSH, jobCreateInput.get(0).getType());

		assertTrue(uuidCollectionCaptor.getValue().size() == 1);
		assertTrue(uuidCollectionCaptor.getValue().contains(jobRecord.getId()));

		assertEquals(jobRecord.getId().toString(), result.jobs().get(0).id());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushTriggerExistingJob() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(pushService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of());
		final Subscription subscriptionRecord = new Subscription(UUID.randomUUID(), requesterSystem, targetSystem, serviceDef, null, null, null, null);
		final PageImpl<Subscription> subscriptionRecordPage = new PageImpl<Subscription>(List.of(subscriptionRecord));
		final OrchestrationJob jobRecord = new OrchestrationJob(OrchestrationType.PUSH, requesterSystem, targetSystem, serviceDef, subscriptionRecord.getId().toString());
		jobRecord.setId(UUID.randomUUID());
		final List<OrchestrationJob> jobRecordList = List.of(jobRecord);

		when(subscriptionDbService.query(anyList(), anyList(), anyList(), any())).thenReturn(subscriptionRecordPage);
		when(orchJobDbService.query(any(), any())).thenReturn(new PageImpl<OrchestrationJob>(jobRecordList));
		when(orchJobDbService.create(anyList())).thenReturn(List.of());

		final OrchestrationPushJobListResponseDTO result = assertDoesNotThrow(() -> pushService.pushTrigger(requesterSystem, dto, origin));

		verify(subscriptionDbService).query(stringListCaptor.capture(), stringListCaptor.capture(), stringListCaptor.capture(), pageCaptor.capture());
		verify(subscriptionDbService, never()).get(anyList());
		verify(orchJobDbService).query(jobFilterCaptor.capture(), pageCaptor.capture());
		verify(orchJobDbService).create(jobListCaptor.capture());
		verify(jobQueue).addAll(uuidCollectionCaptor.capture());
		verify(dtoConverter).convertOrchestrationJobListToDTO(anyList());

		assertEquals(requesterSystem, stringListCaptor.getAllValues().get(0).get(0));
		assertTrue(stringListCaptor.getAllValues().get(1).size() == 0);
		assertTrue(stringListCaptor.getAllValues().get(2).size() == 0);

		final PageRequest subsQueryPageReq = pageCaptor.getAllValues().get(0);
		assertEquals(0, subsQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, subsQueryPageReq.getPageSize());

		assertTrue(jobFilterCaptor.getValue().getIds().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getStatuses().size() == 2);
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.PENDING));
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.IN_PROGRESS));
		assertEquals(OrchestrationType.PUSH, jobFilterCaptor.getValue().getType());
		assertTrue(jobFilterCaptor.getValue().getRequesterSystems().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getTargetSystems().size() == 0);
		assertNull(jobFilterCaptor.getValue().getServiceDefinitions());
		assertTrue(jobFilterCaptor.getValue().getSubscriptionIds().size() == 1);
		assertEquals(subscriptionRecord.getId().toString(), jobFilterCaptor.getValue().getSubscriptionIds().get(0));

		final PageRequest jobQueryPageReq = pageCaptor.getAllValues().get(1);
		assertEquals(0, jobQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, jobQueryPageReq.getPageSize());
		assertTrue(jobQueryPageReq.getSort().getOrderFor(OrchestrationJob.DEFAULT_SORT_FIELD).isDescending());

		final List<OrchestrationJob> jobCreateInput = jobListCaptor.getValue();
		assertTrue(jobCreateInput.size() == 0);
		assertTrue(uuidCollectionCaptor.getValue().size() == 0);

		assertEquals(jobRecord.getId().toString(), result.jobs().get(0).id());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushTriggerDBError() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(pushService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of());
		final Subscription subscriptionRecord = new Subscription(UUID.randomUUID(), requesterSystem, targetSystem, serviceDef, null, null, null, null);
		final PageImpl<Subscription> subscriptionRecordPage = new PageImpl<Subscription>(List.of(subscriptionRecord));
		final OrchestrationJob jobRecord = new OrchestrationJob(OrchestrationType.PUSH, requesterSystem, targetSystem, serviceDef, subscriptionRecord.getId().toString());
		jobRecord.setId(UUID.randomUUID());

		when(subscriptionDbService.query(anyList(), anyList(), anyList(), any())).thenReturn(subscriptionRecordPage);
		when(orchJobDbService.query(any(), any())).thenReturn(new PageImpl<OrchestrationJob>(List.of()));
		doThrow(new InternalServerError("test message")).when(orchJobDbService).create(anyList());

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushTrigger(requesterSystem, dto, origin));

		verify(subscriptionDbService).query(stringListCaptor.capture(), stringListCaptor.capture(), stringListCaptor.capture(), pageCaptor.capture());
		verify(subscriptionDbService, never()).get(anyList());
		verify(orchJobDbService).query(jobFilterCaptor.capture(), pageCaptor.capture());
		verify(orchJobDbService).create(jobListCaptor.capture());
		verify(jobQueue, never()).addAll(anyCollection());
		verify(dtoConverter, never()).convertOrchestrationJobListToDTO(anyList());

		assertEquals(requesterSystem, stringListCaptor.getAllValues().get(0).get(0));
		assertTrue(stringListCaptor.getAllValues().get(1).size() == 0);
		assertTrue(stringListCaptor.getAllValues().get(2).size() == 0);

		final PageRequest subsQueryPageReq = pageCaptor.getAllValues().get(0);
		assertEquals(0, subsQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, subsQueryPageReq.getPageSize());

		assertTrue(jobFilterCaptor.getValue().getIds().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getStatuses().size() == 2);
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.PENDING));
		assertTrue(jobFilterCaptor.getValue().getStatuses().contains(OrchestrationJobStatus.IN_PROGRESS));
		assertEquals(OrchestrationType.PUSH, jobFilterCaptor.getValue().getType());
		assertTrue(jobFilterCaptor.getValue().getRequesterSystems().size() == 0);
		assertTrue(jobFilterCaptor.getValue().getTargetSystems().size() == 0);
		assertNull(jobFilterCaptor.getValue().getServiceDefinitions());
		assertTrue(jobFilterCaptor.getValue().getSubscriptionIds().size() == 1);
		assertEquals(subscriptionRecord.getId().toString(), jobFilterCaptor.getValue().getSubscriptionIds().get(0));

		final PageRequest jobQueryPageReq = pageCaptor.getAllValues().get(1);
		assertEquals(0, jobQueryPageReq.getPageNumber());
		assertEquals(Integer.MAX_VALUE, jobQueryPageReq.getPageSize());
		assertTrue(jobQueryPageReq.getSort().getOrderFor(OrchestrationJob.DEFAULT_SORT_FIELD).isDescending());

		final List<OrchestrationJob> jobCreateInput = jobListCaptor.getValue();
		assertEquals(subscriptionRecord.getId().toString(), jobCreateInput.get(0).getSubscriptionId());
		assertEquals(requesterSystem, jobCreateInput.get(0).getRequesterSystem());
		assertEquals(targetSystem, jobCreateInput.get(0).getTargetSystem());
		assertEquals(serviceDef, jobCreateInput.get(0).getServiceDefinition());
		assertEquals(OrchestrationType.PUSH, jobCreateInput.get(0).getType());

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}

	// pushUnsubscribe

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribe() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final List<String> ids = List.of(UUID.randomUUID().toString());
		final Subscription subscriptionRecord = new Subscription(UUID.fromString(ids.get(0)), requesterSystem, targetSystem, serviceDef, null, null, null, null);
		final List<Subscription> subscriptionRecordList = List.of(subscriptionRecord);

		when(validator.validateAndNormalizeRequesterSystem(eq(requesterSystem), eq(origin))).thenReturn(requesterSystem);
		when(validator.validateAndNormalizePublishUnsubscribeService(eq(ids), eq(origin))).thenReturn(ids);
		when(subscriptionDbService.get(anyList())).thenReturn(subscriptionRecordList);

		assertDoesNotThrow(() -> pushService.pushUnsubscribe(requesterSystem, ids, origin));

		verify(validator).validateAndNormalizeRequesterSystem(eq(requesterSystem), eq(origin));
		verify(validator).validateAndNormalizePublishUnsubscribeService(eq(ids), eq(origin));
		verify(subscriptionDbService).get(uuidListCaptor.capture());
		verify(subscriptionDbService).deleteInBatch(uuidCollectionCaptor.capture());

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertEquals(UUID.fromString(ids.get(0)), uuidListCaptor.getValue().get(0));
		assertTrue(uuidCollectionCaptor.getValue().size() == 1);
		assertTrue(uuidCollectionCaptor.getValue().contains(UUID.fromString(ids.get(0))));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeDifferentOwner() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final List<String> ids = List.of(UUID.randomUUID().toString());
		final Subscription subscriptionRecord = new Subscription(UUID.fromString(ids.get(0)), "OtherSystem", targetSystem, serviceDef, null, null, null, null);
		final List<Subscription> subscriptionRecordList = List.of(subscriptionRecord);

		when(validator.validateAndNormalizeRequesterSystem(eq(requesterSystem), eq(origin))).thenReturn(requesterSystem);
		when(validator.validateAndNormalizePublishUnsubscribeService(eq(ids), eq(origin))).thenReturn(ids);
		when(subscriptionDbService.get(anyList())).thenReturn(subscriptionRecordList);

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushUnsubscribe(requesterSystem, ids, origin));

		verify(validator).validateAndNormalizeRequesterSystem(eq(requesterSystem), eq(origin));
		verify(validator).validateAndNormalizePublishUnsubscribeService(eq(ids), eq(origin));
		verify(subscriptionDbService).get(uuidListCaptor.capture());
		verify(subscriptionDbService, never()).deleteInBatch(anyCollection());

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertEquals(UUID.fromString(ids.get(0)), uuidListCaptor.getValue().get(0));

		assertEquals(ids.get(0) + " is not owned by the requester", ex.getMessage());
		assertEquals(origin, ((ForbiddenException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeDBError() {
		final String origin = "test.origin";
		final String requesterSystem = "TestManager";
		final List<String> ids = List.of(UUID.randomUUID().toString());

		when(validator.validateAndNormalizeRequesterSystem(eq(requesterSystem), eq(origin))).thenReturn(requesterSystem);
		when(validator.validateAndNormalizePublishUnsubscribeService(eq(ids), eq(origin))).thenReturn(ids);
		doThrow(new InternalServerError("test message")).when(subscriptionDbService).get(anyList());
		final Throwable ex = assertThrows(Throwable.class, () -> pushService.pushUnsubscribe(requesterSystem, ids, origin));

		verify(validator).validateAndNormalizeRequesterSystem(eq(requesterSystem), eq(origin));
		verify(validator).validateAndNormalizePublishUnsubscribeService(eq(ids), eq(origin));
		verify(subscriptionDbService).get(uuidListCaptor.capture());
		verify(subscriptionDbService, never()).deleteInBatch(anyCollection());

		assertTrue(uuidListCaptor.getValue().size() == 1);
		assertEquals(UUID.fromString(ids.get(0)), uuidListCaptor.getValue().get(0));

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}

	// queryPushSubscriptions

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPushSubscriptions() {
		final String origin = "test.origin";
		final String ownerSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final OrchestrationSubscriptionQueryRequestDTO requestDTO = new OrchestrationSubscriptionQueryRequestDTO(null, List.of(ownerSystem), List.of(targetSystem), List.of(serviceDef));
		final Subscription subscriptionRecord = new Subscription(UUID.randomUUID(), ownerSystem, targetSystem, serviceDef, null, "MQTT",
				Utilities.toJson(Map.of("foo", "bar")), Utilities.toJson(new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null)));
		final PageImpl<Subscription> subscriptionRecordPage = new PageImpl<Subscription>(List.of(subscriptionRecord));

		when(validator.validateAndNormalizeQueryPushSubscriptionsService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(pageService.getPageRequest(isNull(), eq(Direction.DESC), eq(Subscription.SORTABLE_FIELDS_BY), eq(Subscription.DEFAULT_SORT_FIELD), eq(origin))).thenReturn(PageRequest.of(0, 1));
		when(subscriptionDbService.query(eq(requestDTO.ownerSystems()), eq(requestDTO.targetSystems()), eq(requestDTO.serviceDefinitions()), any())).thenReturn(subscriptionRecordPage);

		final OrchestrationSubscriptionListResponseDTO result = assertDoesNotThrow(() -> pushService.queryPushSubscriptions(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryPushSubscriptionsService(eq(requestDTO), eq(origin));
		verify(pageService).getPageRequest(isNull(), eq(Direction.DESC), eq(Subscription.SORTABLE_FIELDS_BY), eq(Subscription.DEFAULT_SORT_FIELD), eq(origin));
		verify(subscriptionDbService).query(eq(requestDTO.ownerSystems()), eq(requestDTO.targetSystems()), eq(requestDTO.serviceDefinitions()), any());
		verify(dtoConverter).convertSubscriptionListToDTO(eq(subscriptionRecordPage.getContent()), eq(1L));

		assertTrue(result.entries().size() == 1);
		assertEquals(subscriptionRecord.getId().toString(), result.entries().get(0).id());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPushSubscriptionsDBError() {
		final String origin = "test.origin";
		final String ownerSystem = "TestManager";
		final String targetSystem = "TargetSystem";
		final String serviceDef = "serviceDef";
		final OrchestrationSubscriptionQueryRequestDTO requestDTO = new OrchestrationSubscriptionQueryRequestDTO(null, List.of(ownerSystem), List.of(targetSystem), List.of(serviceDef));

		when(validator.validateAndNormalizeQueryPushSubscriptionsService(eq(requestDTO), eq(origin))).thenReturn(requestDTO);
		when(pageService.getPageRequest(isNull(), eq(Direction.DESC), eq(Subscription.SORTABLE_FIELDS_BY), eq(Subscription.DEFAULT_SORT_FIELD), eq(origin))).thenReturn(PageRequest.of(0, 1));
		doThrow(new InternalServerError("test message")).when(subscriptionDbService).query(eq(requestDTO.ownerSystems()), eq(requestDTO.targetSystems()), eq(requestDTO.serviceDefinitions()), any());

		final Throwable ex = assertThrows(Throwable.class, () -> pushService.queryPushSubscriptions(requestDTO, origin));

		verify(validator).validateAndNormalizeQueryPushSubscriptionsService(eq(requestDTO), eq(origin));
		verify(pageService).getPageRequest(isNull(), eq(Direction.DESC), eq(Subscription.SORTABLE_FIELDS_BY), eq(Subscription.DEFAULT_SORT_FIELD), eq(origin));
		verify(subscriptionDbService).query(eq(requestDTO.ownerSystems()), eq(requestDTO.targetSystems()), eq(requestDTO.serviceDefinitions()), any());
		verify(dtoConverter, never()).convertSubscriptionListToDTO(anyList(), anyLong());

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InternalServerError) ex).getOrigin());
	}
}
