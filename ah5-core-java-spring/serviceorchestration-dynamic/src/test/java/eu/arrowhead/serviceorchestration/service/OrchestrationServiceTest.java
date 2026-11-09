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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationFromContextValidation;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationService orchestrationService;

	@Mock
	private LocalServiceOrchestration localOrch;

	@Mock
	private InterCloudServiceOrchestration interCloudOrch;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private OrchestrationValidation validator;

	@Mock
	private OrchestrationFromContextValidation formContextValidator;

	@Captor
	private ArgumentCaptor<OrchestrationForm> orchFormCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationJob>> orchJobListCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationSubscription> orchSubscriptionCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationSubscription>> orchSubscriptionListCaptor;

	@Captor
	private ArgumentCaptor<UUID> uuidCaptor;

	//=================================================================================================
	// method

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPull() {
		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, requesterSystem, requesterSystem, "testService", null);
		job.setId(UUID.randomUUID());
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.create(anyList())).thenReturn(List.of(job));
		when(localOrch.doLocalServiceOrchestration(eq(job.getId()), any())).thenReturn(responseDTO);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestrationService.pull(requesterSystem, requestDTO, origin));

		verify(validator).validateAndNormalizePullService(orchFormCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchFormCaptor.capture(), eq(origin));
		verify(orchJobDbService).create(orchJobListCaptor.capture());
		verify(localOrch).doLocalServiceOrchestration(eq(job.getId()), orchFormCaptor.capture());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());

		assertEquals(job.getType(), orchJobListCaptor.getValue().get(0).getType());
		assertEquals(job.getRequesterSystem(), orchJobListCaptor.getValue().get(0).getRequesterSystem());
		assertEquals(job.getTargetSystem(), orchJobListCaptor.getValue().get(0).getTargetSystem());
		assertEquals(job.getServiceDefinition(), orchJobListCaptor.getValue().get(0).getServiceDefinition());
		assertNull(orchJobListCaptor.getValue().get(0).getSubscriptionId());

		final OrchestrationForm validatorInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(requestDTO.serviceRequirement().serviceDefinition(), validatorInput.getServiceDefinition());
		assertEquals(requesterSystem, validatorInput.getRequesterSystemName());
		assertEquals(requesterSystem, validatorInput.getTargetSystemName());

		final OrchestrationForm contextValidatorInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(requestDTO.serviceRequirement().serviceDefinition(), contextValidatorInput.getServiceDefinition());
		assertEquals(requesterSystem, contextValidatorInput.getRequesterSystemName());
		assertEquals(requesterSystem, contextValidatorInput.getTargetSystemName());

		final OrchestrationForm orchProcessInput = orchFormCaptor.getAllValues().get(2);
		assertEquals(requestDTO.serviceRequirement().serviceDefinition(), orchProcessInput.getServiceDefinition());
		assertEquals(requesterSystem, orchProcessInput.getRequesterSystemName());
		assertEquals(requesterSystem, orchProcessInput.getTargetSystemName());

		assertEquals(responseDTO.warnings().get(0), result.warnings().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPullWithOnlyIntercloud() {
		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null),
				Map.of(OrchestrationFlag.ONLY_INTERCLOUD.name(), true), null, null);
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, requesterSystem, requesterSystem, "testService", null);
		job.setId(UUID.randomUUID());
		final OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.create(anyList())).thenReturn(List.of(job));
		when(interCloudOrch.doInterCloudServiceOrchestration(eq(job.getId()), any())).thenReturn(responseDTO);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestrationService.pull(requesterSystem, requestDTO, origin));

		verify(validator).validateAndNormalizePullService(orchFormCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchFormCaptor.capture(), eq(origin));
		verify(orchJobDbService).create(orchJobListCaptor.capture());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch).doInterCloudServiceOrchestration(eq(job.getId()), orchFormCaptor.capture());

		assertEquals(job.getType(), orchJobListCaptor.getValue().get(0).getType());
		assertEquals(job.getRequesterSystem(), orchJobListCaptor.getValue().get(0).getRequesterSystem());
		assertEquals(job.getTargetSystem(), orchJobListCaptor.getValue().get(0).getTargetSystem());
		assertEquals(job.getServiceDefinition(), orchJobListCaptor.getValue().get(0).getServiceDefinition());
		assertNull(orchJobListCaptor.getValue().get(0).getSubscriptionId());

		final OrchestrationForm validatorInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(requestDTO.serviceRequirement().serviceDefinition(), validatorInput.getServiceDefinition());
		assertEquals(requesterSystem, validatorInput.getRequesterSystemName());
		assertEquals(requesterSystem, validatorInput.getTargetSystemName());

		final OrchestrationForm contextValidatorInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(requestDTO.serviceRequirement().serviceDefinition(), contextValidatorInput.getServiceDefinition());
		assertEquals(requesterSystem, contextValidatorInput.getRequesterSystemName());
		assertEquals(requesterSystem, contextValidatorInput.getTargetSystemName());

		final OrchestrationForm orchProcessInput = orchFormCaptor.getAllValues().get(2);
		assertEquals(requestDTO.serviceRequirement().serviceDefinition(), orchProcessInput.getServiceDefinition());
		assertEquals(requesterSystem, orchProcessInput.getRequesterSystemName());
		assertEquals(requesterSystem, orchProcessInput.getTargetSystemName());

		assertEquals(responseDTO.warnings().get(0), result.warnings().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushSubscribe() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(orchestrationService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final String serviceDef = "testService";
		final OrchestrationRequestDTO orchRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null);
		final OrchestrationNotifyInterfaceDTO notifyInterfaceDTO = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar"));
		final OrchestrationSubscriptionRequestDTO requestDTO = new OrchestrationSubscriptionRequestDTO(requesterSystem, orchRequestDTO, notifyInterfaceDTO, null);
		final Subscription subscription = new Subscription();
		subscription.setId(UUID.randomUUID());
		subscription.setOwnerSystem(requesterSystem);
		subscription.setTargetSystem(requesterSystem);

		when(subscriptionDbService.get(eq(requesterSystem), eq(requesterSystem), eq(serviceDef))).thenReturn(Optional.empty());
		when(subscriptionDbService.create(anyList())).thenReturn(List.of(subscription));

		final Pair<Boolean, String> result = assertDoesNotThrow(() -> orchestrationService.pushSubscribe(requesterSystem, requestDTO, true, origin));

		verify(validator).validateAndNormalizePushSubscribeService(orchSubscriptionCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchFormCaptor.capture(), eq(origin));
		verify(subscriptionDbService).get(eq(requesterSystem), eq(requesterSystem), eq(serviceDef));
		verify(subscriptionDbService).create(orchSubscriptionListCaptor.capture());
		verify(orchJobDbService).create(orchJobListCaptor.capture());
		verify(jobQueue).add(uuidCaptor.capture());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());

		assertEquals(requesterSystem, orchSubscriptionCaptor.getValue().getOrchestrationForm().getRequesterSystemName());
		assertEquals(requesterSystem, orchSubscriptionCaptor.getValue().getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, orchSubscriptionCaptor.getValue().getOrchestrationForm().getServiceDefinition());
		assertEquals(notifyInterfaceDTO.protocol(), orchSubscriptionCaptor.getValue().getNotifyProtocol());

		assertEquals(requesterSystem, orchFormCaptor.getValue().getRequesterSystemName());
		assertEquals(requesterSystem, orchFormCaptor.getValue().getTargetSystemName());
		assertEquals(serviceDef, orchFormCaptor.getValue().getServiceDefinition());

		assertEquals(requesterSystem, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(requesterSystem, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals(OrchestrationType.PUSH, orchJobListCaptor.getValue().get(0).getType());
		assertEquals(requesterSystem, orchJobListCaptor.getValue().get(0).getRequesterSystem());
		assertEquals(requesterSystem, orchJobListCaptor.getValue().get(0).getTargetSystem());
		assertEquals(serviceDef, orchJobListCaptor.getValue().get(0).getServiceDefinition());
		assertEquals(subscription.getId().toString(), orchJobListCaptor.getValue().get(0).getSubscriptionId());

		assertEquals(orchJobListCaptor.getValue().get(0).getId(), uuidCaptor.getValue());

		assertFalse(result.getLeft());
		assertEquals(subscription.getId(), UUID.fromString(result.getRight()));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushSubscribeWithoutTrigger() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(orchestrationService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final String serviceDef = "testService";
		final OrchestrationRequestDTO orchRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null);
		final OrchestrationNotifyInterfaceDTO notifyInterfaceDTO = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar"));
		final OrchestrationSubscriptionRequestDTO requestDTO = new OrchestrationSubscriptionRequestDTO(requesterSystem, orchRequestDTO, notifyInterfaceDTO, null);
		final Subscription subscription = new Subscription();
		subscription.setId(UUID.randomUUID());
		subscription.setOwnerSystem(requesterSystem);
		subscription.setTargetSystem(requesterSystem);

		when(subscriptionDbService.get(eq(requesterSystem), eq(requesterSystem), eq(serviceDef))).thenReturn(Optional.empty());
		when(subscriptionDbService.create(anyList())).thenReturn(List.of(subscription));

		final Pair<Boolean, String> result = assertDoesNotThrow(() -> orchestrationService.pushSubscribe(requesterSystem, requestDTO, false, origin));

		verify(validator).validateAndNormalizePushSubscribeService(orchSubscriptionCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchFormCaptor.capture(), eq(origin));
		verify(subscriptionDbService).get(eq(requesterSystem), eq(requesterSystem), eq(serviceDef));
		verify(subscriptionDbService).create(orchSubscriptionListCaptor.capture());
		verify(orchJobDbService, never()).create(any());
		verify(jobQueue, never()).add(any());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());

		assertEquals(requesterSystem, orchSubscriptionCaptor.getValue().getOrchestrationForm().getRequesterSystemName());
		assertEquals(requesterSystem, orchSubscriptionCaptor.getValue().getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, orchSubscriptionCaptor.getValue().getOrchestrationForm().getServiceDefinition());
		assertEquals(notifyInterfaceDTO.protocol(), orchSubscriptionCaptor.getValue().getNotifyProtocol());

		assertEquals(requesterSystem, orchFormCaptor.getValue().getRequesterSystemName());
		assertEquals(requesterSystem, orchFormCaptor.getValue().getTargetSystemName());
		assertEquals(serviceDef, orchFormCaptor.getValue().getServiceDefinition());

		assertEquals(requesterSystem, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(requesterSystem, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getServiceDefinition());

		assertFalse(result.getLeft());
		assertEquals(subscription.getId(), UUID.fromString(result.getRight()));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testPushSubscribeWithOverride() {
		final BlockingQueue<UUID> jobQueue = Mockito.mock(BlockingQueue.class);
		ReflectionTestUtils.setField(orchestrationService, "pushOrchJobQueue", jobQueue);

		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final String serviceDef = "testService";
		final OrchestrationRequestDTO orchRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null), null, null, null);
		final OrchestrationNotifyInterfaceDTO notifyInterfaceDTO = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("foo", "bar"));
		final OrchestrationSubscriptionRequestDTO requestDTO = new OrchestrationSubscriptionRequestDTO(null, orchRequestDTO, notifyInterfaceDTO, null);
		final Subscription subscription = new Subscription();
		subscription.setId(UUID.randomUUID());
		subscription.setOwnerSystem(requesterSystem);
		subscription.setTargetSystem(requesterSystem);

		when(subscriptionDbService.get(eq(requesterSystem), eq(requesterSystem), eq(serviceDef))).thenReturn(Optional.of(subscription));
		when(subscriptionDbService.create(anyList())).thenReturn(List.of(subscription));

		final Pair<Boolean, String> result = assertDoesNotThrow(() -> orchestrationService.pushSubscribe(requesterSystem, requestDTO, true, origin));

		verify(validator).validateAndNormalizePushSubscribeService(orchSubscriptionCaptor.capture(), eq(origin));
		verify(formContextValidator).validate(orchFormCaptor.capture(), eq(origin));
		verify(subscriptionDbService).get(eq(requesterSystem), eq(requesterSystem), eq(serviceDef));
		verify(subscriptionDbService).create(orchSubscriptionListCaptor.capture());
		verify(orchJobDbService).create(orchJobListCaptor.capture());
		verify(jobQueue).add(uuidCaptor.capture());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());

		assertEquals(requesterSystem, orchSubscriptionCaptor.getValue().getOrchestrationForm().getRequesterSystemName());
		assertEquals(requesterSystem, orchSubscriptionCaptor.getValue().getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, orchSubscriptionCaptor.getValue().getOrchestrationForm().getServiceDefinition());
		assertEquals(notifyInterfaceDTO.protocol(), orchSubscriptionCaptor.getValue().getNotifyProtocol());

		assertEquals(requesterSystem, orchFormCaptor.getValue().getRequesterSystemName());
		assertEquals(requesterSystem, orchFormCaptor.getValue().getTargetSystemName());
		assertEquals(serviceDef, orchFormCaptor.getValue().getServiceDefinition());

		assertEquals(requesterSystem, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getRequesterSystemName());
		assertEquals(requesterSystem, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getTargetSystemName());
		assertEquals(serviceDef, orchSubscriptionListCaptor.getValue().get(0).getOrchestrationForm().getServiceDefinition());

		assertEquals(OrchestrationType.PUSH, orchJobListCaptor.getValue().get(0).getType());
		assertEquals(requesterSystem, orchJobListCaptor.getValue().get(0).getRequesterSystem());
		assertEquals(requesterSystem, orchJobListCaptor.getValue().get(0).getTargetSystem());
		assertEquals(serviceDef, orchJobListCaptor.getValue().get(0).getServiceDefinition());
		assertEquals(subscription.getId().toString(), orchJobListCaptor.getValue().get(0).getSubscriptionId());

		assertEquals(orchJobListCaptor.getValue().get(0).getId(), uuidCaptor.getValue());

		assertTrue(result.getLeft());
		assertEquals(subscription.getId(), UUID.fromString(result.getRight()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribe() {
		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final String subscriptionId = UUID.randomUUID().toString();
		final Subscription subscription = new Subscription();
		subscription.setId(UUID.fromString(subscriptionId));
		subscription.setOwnerSystem(requesterSystem);
		subscription.setTargetSystem(requesterSystem);

		when(validator.validateAndNormalizePushUnsubscribeService(eq(requesterSystem), eq(subscriptionId), eq(origin))).thenReturn(Pair.of(requesterSystem, UUID.fromString(subscriptionId)));
		when(subscriptionDbService.get(eq(UUID.fromString(subscriptionId)))).thenReturn(Optional.of(subscription));

		final Boolean result = assertDoesNotThrow(() -> orchestrationService.pushUnsubscribe(requesterSystem, subscriptionId, origin));

		verify(validator).validateAndNormalizePushUnsubscribeService(eq(requesterSystem), eq(subscriptionId), eq(origin));
		verify(subscriptionDbService).get(eq(UUID.fromString(subscriptionId)));
		verify(subscriptionDbService).deleteById(eq(UUID.fromString(subscriptionId)));

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeNotFound() {
		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final String subscriptionId = UUID.randomUUID().toString();

		when(validator.validateAndNormalizePushUnsubscribeService(eq(requesterSystem), eq(subscriptionId), eq(origin))).thenReturn(Pair.of(requesterSystem, UUID.fromString(subscriptionId)));
		when(subscriptionDbService.get(eq(UUID.fromString(subscriptionId)))).thenReturn(Optional.empty());

		final Boolean result = assertDoesNotThrow(() -> orchestrationService.pushUnsubscribe(requesterSystem, subscriptionId, origin));

		verify(validator).validateAndNormalizePushUnsubscribeService(eq(requesterSystem), eq(subscriptionId), eq(origin));
		verify(subscriptionDbService).get(eq(UUID.fromString(subscriptionId)));
		verify(subscriptionDbService, never()).deleteById(any());

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeNotOwner() {
		final String origin = "test.origin";
		final String requesterSystem = "RequesterSystem";
		final String subscriptionId = UUID.randomUUID().toString();
		final Subscription subscription = new Subscription();
		subscription.setId(UUID.fromString(subscriptionId));
		subscription.setOwnerSystem("OwnerSystem");
		subscription.setTargetSystem("TargetSystem");

		when(validator.validateAndNormalizePushUnsubscribeService(eq(requesterSystem), eq(subscriptionId), eq(origin))).thenReturn(Pair.of(requesterSystem, UUID.fromString(subscriptionId)));
		when(subscriptionDbService.get(eq(UUID.fromString(subscriptionId)))).thenReturn(Optional.of(subscription));

		final Throwable ex = assertThrows(Throwable.class, () -> orchestrationService.pushUnsubscribe(requesterSystem, subscriptionId, origin));

		verify(validator).validateAndNormalizePushUnsubscribeService(eq(requesterSystem), eq(subscriptionId), eq(origin));
		verify(subscriptionDbService).get(eq(UUID.fromString(subscriptionId)));
		verify(subscriptionDbService, never()).deleteById(any());

		assertEquals("RequesterSystem is not the subscription owner", ex.getMessage());
		assertEquals(origin, ((ForbiddenException) ex).getOrigin());
	}
}
