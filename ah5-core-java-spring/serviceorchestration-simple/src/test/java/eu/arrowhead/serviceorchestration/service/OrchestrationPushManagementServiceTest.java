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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationJobDTO;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionResponseDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationPushManagementServiceValidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class OrchestrationPushManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationPushManagementService service;

	@Mock
	private OrchestrationPushManagementServiceValidation validator;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private PageService pageService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private BlockingQueue<UUID> pushOrchJobQueue;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeOk() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO subscriptionRequest = new OrchestrationSubscriptionRequestDTO("Consumer1", orchestrationRequest, intfDTO, 30L);
		final OrchestrationSubscriptionListRequestDTO dto = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionRequest));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final SimpleOrchestrationSubscriptionRequest normalizedSubscriptionRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, normalizedIntfDTO, 30L);
		final Subscription subscription = new Subscription(
				"Consumer2",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(normalizedIntfDTO),
				Utilities.toJson(normalizedOrchRequest));
		final OrchestrationSubscriptionResponseDTO responseEntry = new OrchestrationSubscriptionResponseDTO(
				"11111111-1111-1111-1111-111111111111",
				"Consumer2",
				"Consumer1",
				orchestrationRequest,
				normalizedIntfDTO,
				"2026-01-30T01:53:02Z",
				"2026-01-30T01:23:02Z"
				);
		final OrchestrationSubscriptionListResponseDTO expected = new OrchestrationSubscriptionListResponseDTO(List.of(responseEntry), 1);

		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushSubscribeBulk(dto, "test origin")).thenReturn(List.of(normalizedSubscriptionRequest));
		when(subscriptionDbService.create(List.of(normalizedSubscriptionRequest), "Consumer2")).thenReturn(List.of(subscription));
		when(dtoConverter.convertSubscriptionListToDTO(List.of(subscription), 1)).thenReturn(expected);

		final OrchestrationSubscriptionListResponseDTO actual = service.pushSubscribe("Consumer2", dto, "test origin");
		assertEquals(expected, actual);
		verify(subscriptionDbService).create(List.of(normalizedSubscriptionRequest), "Consumer2");

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushSubscribeThrowsInternalServerError() {

		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("GENERIC_HTTP", Map.of("port", "4040"));
		final OrchestrationSubscriptionRequestDTO subscriptionRequest = new OrchestrationSubscriptionRequestDTO("Consumer1", orchestrationRequest, intfDTO, 30L);
		final OrchestrationSubscriptionListRequestDTO dto = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionRequest));
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final SimpleOrchestrationSubscriptionRequest normalizedSubscriptionRequest = new SimpleOrchestrationSubscriptionRequest("Consumer1", normalizedOrchRequest, normalizedIntfDTO, 30L);


		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushSubscribeBulk(dto, "test origin")).thenReturn(List.of(normalizedSubscriptionRequest));
		when(subscriptionDbService.create(List.of(normalizedSubscriptionRequest), "Consumer2")).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.pushSubscribe("Consumer2", dto, "test origin"));
		assertEquals("test origin", ex.getOrigin());
		assertEquals("Internal error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushTriggerNoIdNoTargetExistingJob() {

		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of());
		final NormalizedOrchestrationPushTrigger normalizedTrigger = new NormalizedOrchestrationPushTrigger(List.of(), List.of());
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest simpleOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final Subscription subscription = new Subscription(
				"Consumer2",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(simpleOrchRequest));
		final OrchestrationJob existingJob = new OrchestrationJob(OrchestrationType.PUSH, "Consumer2", "Consumer1", "serivce1", subscription.getId().toString());
		final OrchestrationJobDTO jobDTO = new OrchestrationJobDTO(
				existingJob.getId().toString(),
				OrchestrationJobStatus.PENDING.toString(),
				OrchestrationType.PUSH.toString(),
				"Consumer2",
				"Consumer1",
				"service1",
				subscription.getId().toString(),
				"",
				"2026-01-30T01:53:02Z",
				"2026-01-30T01:53:02Z",
				null);
		final OrchestrationPushJobListResponseDTO expected = new OrchestrationPushJobListResponseDTO(List.of(jobDTO));

		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushTrigger(dto, "test origin")).thenReturn(normalizedTrigger);
		when(subscriptionDbService.query(List.of("Consumer2"), List.of(), List.of(), PageRequest.of(0, Integer.MAX_VALUE))).thenReturn(new PageImpl<>(List.of(subscription)));
		when(orchJobDbService.query(argThat(queryRequest -> queryRequest.getSubscriptionIds().equals(List.of(subscription.getId()))))).thenReturn(new PageImpl<>(List.of(existingJob)));
		when(dtoConverter.convertOrchestrationJobListToDTO(List.of(existingJob))).thenReturn(expected);

		final OrchestrationPushJobListResponseDTO actual = service.pushTrigger("Consumer2", dto, "test origin");

		assertEquals(expected, actual);
		verify(orchJobDbService).create(List.of());
		verify(pushOrchJobQueue).addAll(List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushTriggerNoIdNoTargetNoExistingJob() {

		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of());
		final NormalizedOrchestrationPushTrigger normalizedTrigger = new NormalizedOrchestrationPushTrigger(List.of(), List.of());
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest simpleOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final Subscription subscription = new Subscription(
				"Consumer2",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(simpleOrchRequest));
		final OrchestrationJob newJob = new OrchestrationJob(OrchestrationType.PUSH, "Consumer2", "Consumer1", "serivce1", subscription.getId().toString());
		final OrchestrationJobDTO jobDTO = new OrchestrationJobDTO(
				newJob.getId().toString(),
				OrchestrationJobStatus.PENDING.toString(),
				OrchestrationType.PUSH.toString(),
				"Consumer2",
				"Consumer1",
				"service1",
				subscription.getId().toString(),
				"",
				"2026-01-30T01:53:02Z",
				"2026-01-30T01:53:02Z",
				null);
		final OrchestrationPushJobListResponseDTO expected = new OrchestrationPushJobListResponseDTO(List.of(jobDTO));

		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushTrigger(dto, "test origin")).thenReturn(normalizedTrigger);
		when(subscriptionDbService.query(List.of("Consumer2"), List.of(), List.of(), PageRequest.of(0, Integer.MAX_VALUE))).thenReturn(new PageImpl<>(List.of(subscription)));
		when(orchJobDbService.query(argThat(queryRequest -> queryRequest.getSubscriptionIds().equals(List.of(subscription.getId()))))).thenReturn(new PageImpl<>(List.of()));
		when(orchJobDbService.create(anyList())).thenReturn(List.of(newJob));
		when(dtoConverter.convertOrchestrationJobListToDTO(List.of(newJob))).thenReturn(expected);

		final OrchestrationPushJobListResponseDTO actual = service.pushTrigger("Consumer2", dto, "test origin");

		assertEquals(expected, actual);
		verify(orchJobDbService).create(argThat(list ->
		list.size() == 1
		&& list.get(0).getStatus().equals(OrchestrationJobStatus.PENDING)
		&& list.get(0).getType().equals(OrchestrationType.PUSH)
		&& list.get(0).getRequesterSystem().equals("Consumer2")
		&& list.get(0).getTargetSystem().equals("Consumer1")
		&& list.get(0).getServiceDefinition().equals("service1")));
		verify(pushOrchJobQueue).addAll(List.of(newJob.getId()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushTriggerIdsAreNotEmptyNoExistingJob() {

		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest simpleOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final Subscription subscription = new Subscription(
				"Consumer2",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(simpleOrchRequest));
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of(), List.of(subscription.getId().toString()));
		final NormalizedOrchestrationPushTrigger normalizedTrigger = new NormalizedOrchestrationPushTrigger(List.of(), List.of(subscription.getId()));
		final OrchestrationJob newJob = new OrchestrationJob(OrchestrationType.PUSH, "Consumer2", "Consumer1", "serivce1", subscription.getId().toString());
		final OrchestrationJobDTO jobDTO = new OrchestrationJobDTO(
				newJob.getId().toString(),
				OrchestrationJobStatus.PENDING.toString(),
				OrchestrationType.PUSH.toString(),
				"Consumer2",
				"Consumer1",
				"service1",
				subscription.getId().toString(),
				"",
				"2026-01-30T01:53:02Z",
				"2026-01-30T01:53:02Z",
				null);
		final OrchestrationPushJobListResponseDTO expected = new OrchestrationPushJobListResponseDTO(List.of(jobDTO));

		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushTrigger(dto, "test origin")).thenReturn(normalizedTrigger);
		when(subscriptionDbService.get(List.of(subscription.getId()))).thenReturn(List.of(subscription));
		when(orchJobDbService.query(argThat(queryRequest -> queryRequest.getSubscriptionIds().equals(List.of(subscription.getId()))))).thenReturn(new PageImpl<>(List.of()));
		when(orchJobDbService.create(anyList())).thenReturn(List.of(newJob));
		when(dtoConverter.convertOrchestrationJobListToDTO(List.of(newJob))).thenReturn(expected);

		final OrchestrationPushJobListResponseDTO actual = service.pushTrigger("Consumer2", dto, "test origin");

		assertEquals(expected, actual);
		verify(orchJobDbService).create(argThat(list ->
		list.size() == 1
		&& list.get(0).getStatus().equals(OrchestrationJobStatus.PENDING)
		&& list.get(0).getType().equals(OrchestrationType.PUSH)
		&& list.get(0).getRequesterSystem().equals("Consumer2")
		&& list.get(0).getTargetSystem().equals("Consumer1")
		&& list.get(0).getServiceDefinition().equals("service1")));
		verify(pushOrchJobQueue).addAll(List.of(newJob.getId()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushTriggerTargetsAreNotEmptyNoExistingJob() {

		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of("Consumer1"), List.of());
		final NormalizedOrchestrationPushTrigger normalizedTrigger = new NormalizedOrchestrationPushTrigger(List.of("Consumer1"), List.of());
		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest simpleOrchRequest = new SimpleOrchestrationRequest("service1", List.of(), Map.of("MATCHMAKING", true), Set.of());
		final Subscription subscription = new Subscription(
				"Consumer2",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-30T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(simpleOrchRequest));
		final OrchestrationJob newJob = new OrchestrationJob(OrchestrationType.PUSH, "Consumer2", "Consumer1", "serivce1", subscription.getId().toString());
		final OrchestrationJobDTO jobDTO = new OrchestrationJobDTO(
				newJob.getId().toString(),
				OrchestrationJobStatus.PENDING.toString(),
				OrchestrationType.PUSH.toString(),
				"Consumer2",
				"Consumer1",
				"service1",
				subscription.getId().toString(),
				"",
				"2026-01-30T01:53:02Z",
				"2026-01-30T01:53:02Z",
				null);
		final OrchestrationPushJobListResponseDTO expected = new OrchestrationPushJobListResponseDTO(List.of(jobDTO));

		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushTrigger(dto, "test origin")).thenReturn(normalizedTrigger);
		when(subscriptionDbService.query(List.of(), List.of("Consumer1"), List.of(), PageRequest.of(0, Integer.MAX_VALUE))).thenReturn(new PageImpl<>(List.of(subscription)));
		when(orchJobDbService.query(argThat(queryRequest -> queryRequest.getSubscriptionIds().equals(List.of(subscription.getId()))))).thenReturn(new PageImpl<>(List.of()));
		when(orchJobDbService.create(anyList())).thenReturn(List.of(newJob));
		when(dtoConverter.convertOrchestrationJobListToDTO(List.of(newJob))).thenReturn(expected);

		final OrchestrationPushJobListResponseDTO actual = service.pushTrigger("Consumer2", dto, "test origin");

		assertEquals(expected, actual);
		verify(orchJobDbService).create(argThat(list ->
		list.size() == 1
		&& list.get(0).getStatus().equals(OrchestrationJobStatus.PENDING)
		&& list.get(0).getType().equals(OrchestrationType.PUSH)
		&& list.get(0).getRequesterSystem().equals("Consumer2")
		&& list.get(0).getTargetSystem().equals("Consumer1")
		&& list.get(0).getServiceDefinition().equals("service1")));
		verify(pushOrchJobQueue).addAll(List.of(newJob.getId()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushTriggerThrowsInternalServerError() {
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of("Consumer1"), List.of());
		final NormalizedOrchestrationPushTrigger normalizedTrigger = new NormalizedOrchestrationPushTrigger(List.of("Consumer1"), List.of());

		when(validator.validateAndNormalizeRequester("Consumer2", "test origin")).thenReturn("Consumer2");
		when(validator.validateAndNormalizePushTrigger(dto, "test origin")).thenReturn(normalizedTrigger);
		when(subscriptionDbService.query(List.of(), List.of("Consumer1"), List.of(), PageRequest.of(0, Integer.MAX_VALUE))).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.pushTrigger("Consumer2", dto, "test origin"));
		assertEquals("test origin", ex.getOrigin());
		assertEquals("Internal error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeOk() {

		when(validator.validateAndNormalizeRequester("Provider1", "test origin")).thenReturn("Provider1");
		when(validator.validateAndNormalizePushUnsubscribe(List.of("11111111-1111-1111-1111-111111111111"), "test origin")).thenReturn(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")));

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
		existingSubscription.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		when(subscriptionDbService.get(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")))).thenReturn(List.of(existingSubscription));

		service.pushUnsubscribe("Provider1", List.of("11111111-1111-1111-1111-111111111111"), "test origin");
		verify(subscriptionDbService).deleteInBatch(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeThrowsForbiddenException() {

		when(validator.validateAndNormalizeRequester("Provider1", "test origin")).thenReturn("Provider1");
		when(validator.validateAndNormalizePushUnsubscribe(List.of("11111111-1111-1111-1111-111111111111"), "test origin")).thenReturn(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")));

		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest(null, null, null, null);

		final Subscription existingSubscription = new Subscription(
				"Provider2",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-27T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(normalizedOrchRequest));
		existingSubscription.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		when(subscriptionDbService.get(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")))).thenReturn(List.of(existingSubscription));

		final ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.pushUnsubscribe("Provider1", List.of("11111111-1111-1111-1111-111111111111"), "test origin"));
		assertEquals("test origin", ex.getOrigin());
		assertEquals("11111111-1111-1111-1111-111111111111 is not owned by the requester", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPushUnsubscribeThrowsInternalServerError() {

		when(validator.validateAndNormalizeRequester("Provider1", "test origin")).thenReturn("Provider1");
		when(validator.validateAndNormalizePushUnsubscribe(List.of("11111111-1111-1111-1111-111111111111"), "test origin")).thenReturn(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")));

		when(subscriptionDbService.get(List.of(UUID.fromString("11111111-1111-1111-1111-111111111111")))).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.pushUnsubscribe("Provider1", List.of("11111111-1111-1111-1111-111111111111"), "test origin"));
		assertEquals("test origin", ex.getOrigin());
		assertEquals("Internal error", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPushSubscriptionsOk() {

		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(new PageDTO(0, 5, "ASC", "id"), List.of("Consumer1"), List.of(), List.of());
		final PageRequest pageRequest = PageRequest.of(0, 5, Direction.ASC, "id");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("service1", null, null, null, null, null, null, null, null, null);

		final OrchestrationNotifyInterfaceDTO intfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final SimpleOrchestrationRequest normalizedOrchRequest = new SimpleOrchestrationRequest(null, null, null, null);
		final OrchestrationNotifyInterfaceDTO normalizedIntfDTO = new OrchestrationNotifyInterfaceDTO("generic_http", Map.of("port", "4040"));
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO(requirementDTO, Map.of("MATCHMAKING", true), List.of(), null);
		final Subscription existingSubscription = new Subscription(
				"Provider1",
				"Consumer1",
				"service1",
				ZonedDateTime.parse("2026-01-27T01:53:02Z"),
				"generic_http",
				Utilities.toJson(intfDTO),
				Utilities.toJson(normalizedOrchRequest));
		final OrchestrationSubscriptionResponseDTO responseEntry = new OrchestrationSubscriptionResponseDTO(
				"11111111-1111-1111-1111-111111111111",
				"Consumer2",
				"Consumer1",
				orchestrationRequest,
				normalizedIntfDTO,
				"2026-01-30T01:53:02Z",
				"2026-01-30T01:23:02Z"
				);
		final OrchestrationSubscriptionListResponseDTO expected = new OrchestrationSubscriptionListResponseDTO(List.of(responseEntry), 1);
		when(pageService.getPageRequest(eq(new PageDTO(0, 5, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);
		when(validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin")).thenReturn(dto);
		when(subscriptionDbService.query(List.of("Consumer1"), List.of(), List.of(), pageRequest)).thenReturn(new PageImpl<>(List.of(existingSubscription)));
		when(dtoConverter.convertSubscriptionListToDTO(List.of(existingSubscription), 1)).thenReturn(expected);

		final OrchestrationSubscriptionListResponseDTO actual = service.queryPushSubscriptions(dto, "test origin");
		assertEquals(expected, actual);
		verify(subscriptionDbService).query(List.of("Consumer1"), List.of(), List.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPushSubscriptionsThrowsInternalServerError() {

		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(new PageDTO(0, 5, "ASC", "id"), List.of("Consumer1"), List.of(), List.of());
		final PageRequest pageRequest = PageRequest.of(0, 5, Direction.ASC, "id");

		when(pageService.getPageRequest(eq(new PageDTO(0, 5, "ASC", "id")), any(), any(), any(), eq("test origin"))).thenReturn(pageRequest);
		when(validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin")).thenReturn(dto);
		when(subscriptionDbService.query(List.of("Consumer1"), List.of(), List.of(), pageRequest)).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.queryPushSubscriptions(dto, "test origin"));
		assertEquals("test origin", ex.getOrigin());
		assertEquals("Internal error", ex.getMessage());
	}
}
