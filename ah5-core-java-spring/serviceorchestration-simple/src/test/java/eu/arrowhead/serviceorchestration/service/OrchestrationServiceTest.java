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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.utils.ServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationServiceValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationService service;

	@Mock
	private OrchestrationServiceValidation validator;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Mock
	private ServiceOrchestration serviceOrchestration;

	@Mock
	private BlockingQueue<UUID> pushOrchJobQueue;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPullOk() {
		AtomicReference<UUID> jobID = new AtomicReference<>();

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final SimpleOrchestrationRequest normalized = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final OrchestrationStore result = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 1, null);
		final OrchestrationResultDTO resultDTO = new OrchestrationResultDTO("Provider1|service1|1.0.0", "LOCAL", "Provider1", "service1", "1.0.0", null, null, null, null, null);
		OrchestrationResponseDTO responseDTO = new OrchestrationResponseDTO(List.of(resultDTO), List.of());

		when(validator.validateAndNormalizeRequester("Consumer1", "test origin")).thenReturn("Consumer1");
		when(validator.validateAndNormalizePull(dto, "test origin")).thenReturn(normalized);
		when(orchJobDbService.create(any())).thenAnswer(Invocation -> {
			final List<OrchestrationJob> jobs = Invocation.getArgument(0);
			final OrchestrationJob job = jobs.getFirst();
			jobID.set(job.getId());
			return List.of(job);
		});
		when(serviceOrchestration.orchestrate(any(), eq("Consumer1"), eq(normalized))).thenReturn(List.of(result));
		when(dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(List.of(result), Set.of())).thenReturn(responseDTO);

		final OrchestrationResponseDTO actual = service.pull("Consumer1", dto, "test origin");
		assertEquals(responseDTO, actual);
		verify(orchJobDbService).create(argThat(jobList ->
						jobList.size() == 1
						&& jobList.getFirst().getType() == OrchestrationType.PULL
						&& Objects.equals(jobList.getFirst().getRequesterSystem(), "Consumer1")
						&& Objects.equals(jobList.getFirst().getTargetSystem(), "Consumer1")
						&& Objects.equals(jobList.getFirst().getServiceDefinition(), "service1")));
		verify(serviceOrchestration).orchestrate(jobID.get(), "Consumer1", normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPullThrowsInternalServerError() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final SimpleOrchestrationRequest normalized = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());

		when(validator.validateAndNormalizeRequester("Consumer1", "test origin")).thenReturn("Consumer1");
		when(validator.validateAndNormalizePull(dto, "test origin")).thenReturn(normalized);
		when(orchJobDbService.create(any())).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.pull("Consumer1", dto, "test origin"));
		assertEquals(InternalServerError.class, ex.getClass());
		assertEquals("Internal error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeOverrideExistingRecordNoTrigger() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("Consumer1", requestDTO, normalizedIntfDTO, 30L);
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final SimpleOrchestrationSubscriptionRequest simpleOrchRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, intfDTO, 30L);
		final Subscription existingSubscription = new Subscription(
				"Provider1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-27T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		final Subscription createdSubscription = new Subscription(
				"Consumer1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		createdSubscription.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		when(validator.validateAndNormalizePushSubscribe(dto, "Consumer1", "test origin")).thenReturn(simpleOrchRequest);
		when(subscriptionDbService.get("Consumer1", "Consumer1", "service1")).thenReturn(Optional.of(existingSubscription));
		when(subscriptionDbService.create(List.of(simpleOrchRequest), "Consumer1")).thenReturn(List.of(createdSubscription));

		final Pair<Boolean, String> expected = Pair.of(true, "11111111-1111-1111-1111-111111111111");
		final Pair<Boolean, String> actual = service.pushSubscribe("Consumer1", dto, false, "test origin");
		assertEquals(expected, actual);

		verify(subscriptionDbService).get("Consumer1", "Consumer1", "service1");
		verify(subscriptionDbService).create(List.of(simpleOrchRequest), "Consumer1");
		verify(orchJobDbService, never()).create(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeDoTriggerNotOverrideServiceRequirementNotNull() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("Consumer1", requestDTO, normalizedIntfDTO, 30L);
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final SimpleOrchestrationSubscriptionRequest simpleOrchRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, intfDTO, 30L);
		final Subscription createdSubscription = new Subscription(
				"Consumer1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		createdSubscription.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		when(validator.validateAndNormalizePushSubscribe(dto, "Consumer1", "test origin")).thenReturn(simpleOrchRequest);
		when(subscriptionDbService.get("Consumer1", "Consumer1", "service1")).thenReturn(Optional.empty());
		when(subscriptionDbService.create(List.of(simpleOrchRequest), "Consumer1")).thenReturn(List.of(createdSubscription));
		when(orchJobDbService.create(argThat(list -> list.get(0).getType().equals(OrchestrationType.PUSH)
				&& list.get(0).getRequesterSystem().equals("Consumer1")
				&& list.get(0).getTargetSystem().equals("Consumer1")
				&& list.get(0).getServiceDefinition().equals("service1")))).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(pushOrchJobQueue.add(any())).thenReturn(true);

		final Pair<Boolean, String> expected = Pair.of(false, "11111111-1111-1111-1111-111111111111");
		final Pair<Boolean, String> actual = service.pushSubscribe("Consumer1", dto, true, "test origin");
		assertEquals(expected, actual);

		verify(subscriptionDbService).get("Consumer1", "Consumer1", "service1");
		verify(subscriptionDbService).create(List.of(simpleOrchRequest), "Consumer1");
		@SuppressWarnings("unchecked")
		final ArgumentCaptor<List<OrchestrationJob>> dbCaptor = ArgumentCaptor.forClass(List.class);
		verify(orchJobDbService).create(dbCaptor.capture());
		verify(pushOrchJobQueue).add(dbCaptor.getValue().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeDoTriggerNotOverrideServiceRequirementIsNull() {

		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(null, Map.of("MATCHMAKING", true), List.of(), null);
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("Consumer1", requestDTO, normalizedIntfDTO, 30L);
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest(null, List.of(), Map.of("MATCHMAKING", true), Set.of());
		final SimpleOrchestrationSubscriptionRequest simpleOrchRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, intfDTO, 30L);
		final Subscription createdSubscription = new Subscription(
				"Consumer1",
				"Consumer1",
				null,
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		createdSubscription.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		when(validator.validateAndNormalizePushSubscribe(dto, "Consumer1", "test origin")).thenReturn(simpleOrchRequest);
		when(subscriptionDbService.get("Consumer1", "Consumer1", null)).thenReturn(Optional.empty());
		when(subscriptionDbService.create(List.of(simpleOrchRequest), "Consumer1")).thenReturn(List.of(createdSubscription));
		when(orchJobDbService.create(argThat(list -> list.get(0).getType().equals(OrchestrationType.PUSH)
				&& list.get(0).getRequesterSystem().equals("Consumer1")
				&& list.get(0).getTargetSystem().equals("Consumer1")
				&& list.get(0).getServiceDefinition() == null))).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(pushOrchJobQueue.add(any())).thenReturn(true);

		final Pair<Boolean, String> expected = Pair.of(false, "11111111-1111-1111-1111-111111111111");
		final Pair<Boolean, String> actual = service.pushSubscribe("Consumer1", dto, true, "test origin");
		assertEquals(expected, actual);

		verify(subscriptionDbService).get("Consumer1", "Consumer1", null);
		verify(subscriptionDbService).create(List.of(simpleOrchRequest), "Consumer1");
		@SuppressWarnings("unchecked")
		final ArgumentCaptor<List<OrchestrationJob>> dbCaptor = ArgumentCaptor.forClass(List.class);
		verify(orchJobDbService).create(dbCaptor.capture());
		verify(pushOrchJobQueue).add(dbCaptor.getValue().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeDoTriggerNotOverrideOrchestrationRequestIsNull() {

		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("Consumer1", null, normalizedIntfDTO, 30L);
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest(null, null, null, null);
		final SimpleOrchestrationSubscriptionRequest simpleOrchRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, intfDTO, 30L);
		final Subscription createdSubscription = new Subscription(
				"Consumer1",
				"Consumer1",
				null,
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		createdSubscription.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		when(validator.validateAndNormalizePushSubscribe(dto, "Consumer1", "test origin")).thenReturn(simpleOrchRequest);
		when(subscriptionDbService.get("Consumer1", "Consumer1", null)).thenReturn(Optional.empty());
		when(subscriptionDbService.create(List.of(simpleOrchRequest), "Consumer1")).thenReturn(List.of(createdSubscription));
		when(orchJobDbService.create(argThat(list -> list.get(0).getType().equals(OrchestrationType.PUSH)
				&& list.get(0).getRequesterSystem().equals("Consumer1")
				&& list.get(0).getTargetSystem().equals("Consumer1")
				&& list.get(0).getServiceDefinition() == null))).thenAnswer(Invocation -> Invocation.getArgument(0));
		when(pushOrchJobQueue.add(any())).thenReturn(true);

		final Pair<Boolean, String> expected = Pair.of(false, "11111111-1111-1111-1111-111111111111");
		final Pair<Boolean, String> actual = service.pushSubscribe("Consumer1", dto, true, "test origin");
		assertEquals(expected, actual);

		verify(subscriptionDbService).get("Consumer1", "Consumer1", null);
		verify(subscriptionDbService).create(List.of(simpleOrchRequest), "Consumer1");
		@SuppressWarnings("unchecked")
		final ArgumentCaptor<List<OrchestrationJob>> dbCaptor = ArgumentCaptor.forClass(List.class);
		verify(orchJobDbService).create(dbCaptor.capture());
		verify(pushOrchJobQueue).add(dbCaptor.getValue().get(0).getId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeExistingSubscription() {

		when(validator.validateAndNormalizeRequester("Provider1", "test origin")).thenReturn("Provider1");
		when(validator.validateAndNormalizePushUnsubscribe("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		when(subscriptionDbService.deleteById(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(true);

		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest(null, null, null, null);

		final Subscription existingSubscription = new Subscription(
				"Provider1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-27T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(normalizedOrchRequest));
		when(subscriptionDbService.get(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(Optional.of(existingSubscription));

		assertTrue(service.pushUnsubscribe("Provider1", "11111111-1111-1111-1111-111111111111", "test origin"));
		verify(subscriptionDbService).deleteById(UUID.fromString("11111111-1111-1111-1111-111111111111"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeNotExistingSubscription() {
		when(validator.validateAndNormalizeRequester("Provider1", "test origin")).thenReturn("Provider1");
		when(validator.validateAndNormalizePushUnsubscribe("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		when(subscriptionDbService.get(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(Optional.empty());

		assertTrue(!service.pushUnsubscribe("Provider1", "11111111-1111-1111-1111-111111111111", "test origin"));
		verify(subscriptionDbService, never()).deleteById(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeThrowsForbiddenException() {

		when(validator.validateAndNormalizeRequester("Provider1", "test origin")).thenReturn("Provider1");
		when(validator.validateAndNormalizePushUnsubscribe("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest(null, null, null, null);

		final Subscription existingSubscription = new Subscription(
				"Consumer1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-27T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(normalizedOrchRequest));
		when(subscriptionDbService.get(UUID.fromString("11111111-1111-1111-1111-111111111111"))).thenReturn(Optional.of(existingSubscription));

		final ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.pushUnsubscribe("Provider1", "11111111-1111-1111-1111-111111111111", "test origin"));
		assertEquals("test origin", ex.getOrigin());
		assertEquals("Provider1 is not the subscription owner", ex.getMessage());
	}
}
