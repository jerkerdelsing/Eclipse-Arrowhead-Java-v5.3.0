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
package eu.arrowhead.serviceorchestration.service.dto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.arrowhead.dto.enums.OrchestrationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.dto.OrchestrationJobDTO;
import eu.arrowhead.dto.OrchestrationLockListResponseDTO;
import eu.arrowhead.dto.OrchestrationLockResponseDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionResponseDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;

@SuppressWarnings("checkstyle:MagicNumberCheck")
@ExtendWith(MockitoExtension.class)
public class DTOConverterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DTOConverter converter;

	@Spy
	private ObjectMapper mapper;

	//=================================================================================================
	// methods

	// convertSubscriptionListToDTO()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDTO() {
		final List<Subscription> records = List.of(subscription(), subscription());
		final int count = 5;

		OrchestrationSubscriptionListResponseDTO result = assertDoesNotThrow(() -> converter.convertSubscriptionListToDTO(records, count));

		assertEquals(count, result.count());
		assertEquals(records.get(0).getId().toString(), result.entries().get(0).id());
		assertEquals(records.get(1).getId().toString(), result.entries().get(1).id());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDTONullInput() {
		final List<Subscription> records = null;
		final int count = 5;

		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertSubscriptionListToDTO(records, count));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscriptions list is null", ex.getMessage());
	}

	// convertSubscriptionToDTO()

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertSubscriptionToDTO() throws JsonMappingException, JsonProcessingException {
		final Subscription record = subscription();

		final OrchestrationSubscriptionResponseDTO result = assertDoesNotThrow(() -> converter.convertSubscriptionToDTO(record));

		verify(mapper).readValue(eq(record.getOrchestrationRequest()), eq(OrchestrationRequestDTO.class));
		verify(mapper).readValue(eq(record.getNotifyProperties()), any(TypeReference.class));

		assertEquals(record.getId().toString(), result.id());
		assertEquals(record.getOwnerSystem(), result.ownerSystemName());
		assertEquals(record.getTargetSystem(), result.targetSystemName());
		assertEquals(record.getOrchestrationRequest(), Utilities.toJson(result.orchestrationRequest()));
		assertEquals(record.getNotifyProtocol(), result.notifyInterface().protocol());
		assertEquals(record.getNotifyProperties(), Utilities.toJson(result.notifyInterface().properties()));
		assertEquals(record.getExpiresAt(), Utilities.parseUTCStringToZonedDateTime(result.expiredAt()));
		assertEquals(record.getCreatedAt(), Utilities.parseUTCStringToZonedDateTime(result.createdAt()));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertSubscriptionToDTONullInput() throws JsonMappingException, JsonProcessingException {
		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertSubscriptionToDTO(null));

		verify(mapper, never()).readValue(anyString(), any(Class.class));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("subscription is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConvertSubscriptionToDTOInvalidOrchestrationRequestString() throws JsonMappingException, JsonProcessingException {
		final Subscription record = subscription();

		doThrow(JsonProcessingException.class).when(mapper).readValue(eq(record.getOrchestrationRequest()), eq(OrchestrationRequestDTO.class));

		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertSubscriptionToDTO(record));

		verify(mapper).readValue(eq(record.getOrchestrationRequest()), eq(OrchestrationRequestDTO.class));
		verify(mapper, never()).readValue(anyString(), any(TypeReference.class));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("DTOconverter.createOrchestrationRequestDTO failed. Error: N/A", ex.getMessage());
	}

	// convertOrchestrationJobListToDTO()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobListToDTO() {
		final List<OrchestrationJob> records = List.of(orchestrationJob(), orchestrationJob());

		final OrchestrationPushJobListResponseDTO result = assertDoesNotThrow(() -> converter.convertOrchestrationJobListToDTO(records));

		assertEquals(records.get(0).getId().toString(), result.jobs().get(0).id());
		assertEquals(records.get(1).getId().toString(), result.jobs().get(1).id());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobListToDTONullInput() {
		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertOrchestrationJobListToDTO(null));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job list is null", ex.getMessage());
	}

	// convertOrchestrationJobPageToHistoryDTO()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobPageToHistoryDTO() {
		final PageImpl<OrchestrationJob> page = new PageImpl<OrchestrationJob>(List.of(orchestrationJob(), orchestrationJob()), PageRequest.of(0, 10), 10L);

		final OrchestrationHistoryResponseDTO result = assertDoesNotThrow(() -> converter.convertOrchestrationJobPageToHistoryDTO(page));

		assertEquals(page.getTotalElements(), result.count());
		assertEquals(page.getContent().get(0).getId().toString(), result.entries().get(0).id());
		assertEquals(page.getContent().get(1).getId().toString(), result.entries().get(1).id());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobPageToHistoryDTONullInput() {
		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertOrchestrationJobPageToHistoryDTO(null));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("page is null", ex.getMessage());
	}

	// convertOrchestrationJobToDTO()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobToDTO() {
		final OrchestrationJob record = orchestrationJob();

		final OrchestrationJobDTO result = assertDoesNotThrow(() -> converter.convertOrchestrationJobToDTO(record));

		assertEquals(record.getId().toString(), result.id());
		assertEquals(record.getStatus().toString(), result.status());
		assertEquals(record.getType().toString(), result.type());
		assertEquals(record.getRequesterSystem(), result.requesterSystem());
		assertEquals(record.getTargetSystem(), result.targetSystem());
		assertEquals(record.getServiceDefinition(), result.serviceDefinition());
		assertEquals(record.getSubscriptionId(), result.subscriptionId());
		assertEquals(record.getMessage(), result.message());
		assertEquals(record.getCreatedAt(), Utilities.parseUTCStringToZonedDateTime(result.createdAt()));
		assertEquals(record.getStartedAt(), Utilities.parseUTCStringToZonedDateTime(result.startedAt()));
		assertEquals(record.getFinishedAt(), Utilities.parseUTCStringToZonedDateTime(result.finishedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobToDTONullInput() {
		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertOrchestrationJobToDTO(null));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("job is null", ex.getMessage());
	}

	// convertOrchestrationLockListToDTO()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationLockListToDTO() {
		final List<OrchestrationLock> records = List.of(orchestrationLock(), orchestrationLock());
		records.getLast().setId(78);
		final int count = 5;

		final OrchestrationLockListResponseDTO result = assertDoesNotThrow(() -> converter.convertOrchestrationLockListToDTO(records, count));

		assertEquals(count, result.count());
		assertEquals(records.get(0).getId(), result.entries().get(0).id());
		assertEquals(records.get(1).getId(), result.entries().get(1).id());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationLockListToDTONullInput() {
		final List<OrchestrationLock> records = null;
		final int count = 5;

		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertOrchestrationLockListToDTO(records, count));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("lock list is null", ex.getMessage());
	}

	// convertOrchestrationLockToDTO()

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationLockToDTO() {
		final OrchestrationLock record = orchestrationLock();

		final OrchestrationLockResponseDTO result = assertDoesNotThrow(() -> converter.convertOrchestrationLockToDTO(record));

		assertEquals(record.getId(), result.id());
		assertEquals(record.getServiceInstanceId(), result.serviceInstanceId());
		assertEquals(record.getOwner(), result.owner());
		assertEquals(record.getOrchestrationJobId(), result.orchestrationJobId());
		assertEquals(record.getExpiresAt(), Utilities.parseUTCStringToZonedDateTime(result.expiresAt()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationLockToDTONullInput() {
		final Throwable ex = assertThrows(Throwable.class, () -> converter.convertOrchestrationLockToDTO(null));

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("lock is null", ex.getMessage());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationServiceRequirementDTO orchestrationServiceRequirementDTO() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");

		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		metadataReq.put("abc", "xyz");

		return new OrchestrationServiceRequirementDTO(
				"serviceDefinition",
				List.of("operation-1", "operation-2"),
				List.of("1.2.3"),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()),
				List.of(metadataReq),
				List.of("generic_http"),
				List.of("IPV4"),
				List.of(interfacePropsReq),
				List.of("CERTIFICATE"),
				List.of("PreferredProvider"));
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationRequestDTO orchestrationRequestDTO() {
		return new OrchestrationRequestDTO(orchestrationServiceRequirementDTO(), Map.of("MATCHMAKING", true), List.of(new QoSRequirementDTO("test-qos", "FILTER", Map.of("k", "v"))), 1000);
	}

	//-------------------------------------------------------------------------------------------------
	private Subscription subscription() {
		return new Subscription(UUID.randomUUID(), "OwerSystem", "TargetSystem", "serviceDefinitin", Utilities.utcNow(), "MQTT", Utilities.toJson(Map.of("topic", "test/topic")), Utilities.toJson(orchestrationRequestDTO()));
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationJob orchestrationJob() {
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, "RequesterSystem", "targetSystem", "serviceDefinition", UUID.randomUUID().toString());
		job.setMessage("test message");
		job.setStatus(OrchestrationJobStatus.DONE);
		job.setCreatedAt(Utilities.utcNow().minusSeconds(7));
		job.setStartedAt(Utilities.utcNow().minusSeconds(5));
		job.setFinishedAt(Utilities.utcNow().minusSeconds(1));
		return job;
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationLock orchestrationLock() {
		final OrchestrationLock lock = new OrchestrationLock("instance-id", "OwnerSystem", Utilities.utcNow().plusSeconds(10));
		lock.setId(99);
		lock.setOrchestrationJobId("job-id");
		return lock;
	}
}
