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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.dto.OrchestrationHistoryResponseDTO;
import eu.arrowhead.dto.OrchestrationPushJobListResponseDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationResultDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListResponseDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionResponseDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class DTOConverterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DTOConverter dtoConverter;

	@Spy
	private ObjectMapper mapper = new ObjectMapper();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStoreEntityListToResponseListDTOOk() {
		final OrchestrationStore store1 = createOrchestrationStore("Consumer1", "serviceDef1", "Provider|serviceDef1|1.0.0", 1);
		final OrchestrationStore store2 = createOrchestrationStore("Consumer2", "serviceDef2", "Provider|serviceDef2|1.0.0", 2);
		final List<OrchestrationStore> entities = List.of(store1, store2);

		final OrchestrationSimpleStoreListResponseDTO result = dtoConverter.convertStoreEntityListToResponseListDTO(entities);

		assertNotNull(result);
		assertEquals(2, result.count());
		assertEquals(2, result.entries().size());
		assertEquals("Consumer1", result.entries().get(0).consumer());
		assertEquals("serviceDef1", result.entries().get(0).serviceDefinition());
		assertEquals("Provider|serviceDef1|1.0.0", result.entries().get(0).serviceInstanceId());
		assertEquals(1, result.entries().get(0).priority());
		assertEquals("Consumer2", result.entries().get(1).consumer());
		assertEquals("serviceDef2", result.entries().get(1).serviceDefinition());
		assertEquals("Provider|serviceDef2|1.0.0", result.entries().get(1).serviceInstanceId());
		assertEquals(2, result.entries().get(1).priority());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStoreEntityListToResponseListDTOEmptyList() {
		final List<OrchestrationStore> entities = List.of();

		final OrchestrationSimpleStoreListResponseDTO result = dtoConverter.convertStoreEntityListToResponseListDTO(entities);

		assertNotNull(result);
		assertEquals(0, result.count());
		assertTrue(result.entries().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStoreEntityPageToResponseListDTOOk() {
		final OrchestrationStore store = createOrchestrationStore("Consumer1", "serviceDef1", "Provider|serviceDef1|1.0.0", 1);
		final Page<OrchestrationStore> page = new PageImpl<>(List.of(store), PageRequest.of(0, 10), 20);

		final OrchestrationSimpleStoreListResponseDTO result = dtoConverter.convertStoreEntityPageToResponseListTO(page);

		assertEquals(20, result.count());
		assertEquals(1, result.entries().size());
		final OrchestrationSimpleStoreResponseDTO entry = result.entries().get(0);
		assertEquals("Consumer1", entry.consumer());
		assertEquals("serviceDef1", entry.serviceDefinition());
		assertEquals("Provider|serviceDef1|1.0.0", entry.serviceInstanceId());
		assertEquals(1, entry.priority());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStoreEntityPageToResponseListDTOEmptyPage() {
		final Page<OrchestrationStore> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

		final OrchestrationSimpleStoreListResponseDTO result = dtoConverter.convertStoreEntityPageToResponseListTO(page);

		assertNotNull(result);
		assertEquals(0, result.count());
		assertTrue(result.entries().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStoreEntitiesToOrchestrationResponseDTOOk() {
		final OrchestrationStore store = createOrchestrationStore("Consumer1", "serviceDef1", "Provider|serviceDef1|1.0.0", 1);
		final List<OrchestrationStore> entities = List.of(store);

		final OrchestrationResponseDTO result = dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(entities, new HashSet<>());

		assertNotNull(result);
		assertEquals(1, result.results().size());
		final OrchestrationResultDTO resultDto = result.results().get(0);
		assertEquals("Provider|serviceDef1|1.0.0", resultDto.serviceInstanceId());
		assertEquals("LOCAL", resultDto.cloudIdentitifer());
		assertEquals("Provider", resultDto.providerName());
		assertEquals("serviceDef1", resultDto.serviceDefinitition());
		assertEquals("1.0.0", resultDto.version());
		assertNull(result.warnings());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertStoreEntitiesToOrchestrationResponseDTOWithWarnings() {
		final OrchestrationStore store = createOrchestrationStore("Consumer", "serviceDef", "Provider|serviceDef1|1.0.0", 1);
		final List<OrchestrationStore> entities = List.of(store);
		final Set<String> warnings = Set.of("warning1", "warning2");

		final OrchestrationResponseDTO result = dtoConverter.convertStoreEntitiesToOrchestrationResponseDTO(entities, warnings);

		assertNotNull(result.warnings());
		assertEquals(2, result.warnings().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDTOOk() {
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\",\"preferredProviders\":[\"Provider1\"]}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\"}";
		final Subscription subscription = createSubscription("Owner1", "Target1", "serviceDef1", orchestrationRequest, notifyProperties);
		final List<Subscription> subscriptions = List.of(subscription);

		final OrchestrationSubscriptionListResponseDTO result = dtoConverter.convertSubscriptionListToDTO(subscriptions, 10);

		assertNotNull(result);
		assertEquals(10, result.count());
		assertEquals(1, result.entries().size());
		final OrchestrationSubscriptionResponseDTO entry = result.entries().get(0);
		assertEquals("Owner1", entry.ownerSystemName());
		assertEquals("Target1", entry.targetSystemName());
		assertEquals("testService", entry.orchestrationRequest().serviceRequirement().serviceDefinition());
		assertEquals(1, entry.orchestrationRequest().serviceRequirement().preferredProviders().size());
		assertEquals("Provider1", entry.orchestrationRequest().serviceRequirement().preferredProviders().get(0));
		assertEquals("localhost", entry.notifyInterface().properties().get("address"));
		assertEquals("8080", entry.notifyInterface().properties().get("port"));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDtoPreferredProvidersIsNull() {
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\",\"preferredProviders\":null}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\"}";
		final Subscription subscription = createSubscription("Owner1", "Target1", "serviceDef1", orchestrationRequest, notifyProperties);
		final List<Subscription> subscriptions = List.of(subscription);

		final OrchestrationSubscriptionListResponseDTO result = dtoConverter.convertSubscriptionListToDTO(subscriptions, 10);

		assertEquals(10, result.count());
		assertEquals(1, result.entries().size());
		final OrchestrationSubscriptionResponseDTO entry = result.entries().get(0);
		assertEquals("testService", entry.orchestrationRequest().serviceRequirement().serviceDefinition());
		assertNull(entry.orchestrationRequest().serviceRequirement().preferredProviders());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDtoServiceDefinitionIsNull() {
		final String orchestrationRequest = "{\"serviceDefinition\":null,\"preferredProviders\":[\"Provider1\"]}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\"}";
		final Subscription subscription = createSubscription("Owner1", "Target1", "serviceDef1", orchestrationRequest, notifyProperties);
		final List<Subscription> subscriptions = List.of(subscription);

		final OrchestrationSubscriptionListResponseDTO result = dtoConverter.convertSubscriptionListToDTO(subscriptions, 10);

		assertEquals(10, result.count());
		assertEquals(1, result.entries().size());
		final OrchestrationSubscriptionResponseDTO entry = result.entries().get(0);
		assertEquals("Provider1", entry.orchestrationRequest().serviceRequirement().preferredProviders().get(0));
		assertNull(entry.orchestrationRequest().serviceRequirement().serviceDefinition());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDtoServiceDefinitionIsNullPreferredProvidersIsNull() {
		final String orchestrationRequest = "{\"serviceDefinition\":null,\"preferredProviders\":null}";
		final String notifyProperties = "{\"address\":\"localhost\",\"port\":\"8080\"}";
		final Subscription subscription = createSubscription("Owner1", "Target1", "serviceDef1", orchestrationRequest, notifyProperties);
		final List<Subscription> subscriptions = List.of(subscription);

		final OrchestrationSubscriptionListResponseDTO result = dtoConverter.convertSubscriptionListToDTO(subscriptions, 10);

		assertEquals(10, result.count());
		assertEquals(1, result.entries().size());
		final OrchestrationSubscriptionResponseDTO entry = result.entries().get(0);
		assertNull(entry.orchestrationRequest().serviceRequirement());

	}
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDTOWithInvalidOrchestrationRequestThrowsIllegalArgumentException() {
		final Subscription subscription = createSubscription("owner", "target", "serviceDef", "invalid-json", "{\"port\":\"8080\"}");
		final List<Subscription> subscriptions = List.of(subscription);

		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> dtoConverter.convertSubscriptionListToDTO(subscriptions, 1));
		assertTrue(ex.getMessage().contains("DTOconverter.createOrchestrationRequestDTO failed. Error: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSubscriptionListToDTOWithInvalidNotifyPropertiesThrowsIllegalArgumentException() {
		final String orchestrationRequest = "{\"serviceDefinition\":\"testService\"}";
		final Subscription subscription = createSubscription("owner", "target", "serviceDef", orchestrationRequest, "invalid-json");
		final List<Subscription> subscriptions = List.of(subscription);

		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> dtoConverter.convertSubscriptionListToDTO(subscriptions, 1));
		assertTrue(ex.getMessage().contains("DTOconverter.createOrchestrationNotifyInterfaceDTO failed. Error: "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobListToDTOOk() {
		final OrchestrationJob job1 = createOrchestrationJob("Requester1", "Target1", "serviceDef1", OrchestrationJobStatus.PENDING);
		final OrchestrationJob job2 = createOrchestrationJob("Requester2", "Target2", "serviceDef2", OrchestrationJobStatus.DONE);
		final List<OrchestrationJob> jobs = List.of(job1, job2);

		final OrchestrationPushJobListResponseDTO result = dtoConverter.convertOrchestrationJobListToDTO(jobs);

		assertEquals(2, result.jobs().size());
		assertEquals("PENDING", result.jobs().get(0).status());
		assertEquals("DONE", result.jobs().get(1).status());
	}


	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertOrchestrationJobPageToHistoryDTOOk() {
		final OrchestrationJob job = createOrchestrationJob("Requester1", "Target1", "serviceDef1", OrchestrationJobStatus.PENDING);
		final Page<OrchestrationJob> page = new PageImpl<>(List.of(job), PageRequest.of(0, 10), 50);

		final OrchestrationHistoryResponseDTO result = dtoConverter.convertOrchestrationJobPageToHistoryDTO(page);

		assertNotNull(result);
		assertEquals(50, result.count());
		assertEquals(1, result.entries().size());
		assertEquals("Requester1", result.entries().get(0).requesterSystem());
		assertEquals("Target1", result.entries().get(0).targetSystem());
		assertEquals("serviceDef1", result.entries().get(0).serviceDefinition());
		assertEquals("PENDING", result.entries().get(0).status());
		assertNotNull(result.entries().get(0).createdAt());
		assertNotNull(result.entries().get(0).startedAt());
		assertNotNull(result.entries().get(0).finishedAt());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationStore createOrchestrationStore(final String consumer, final String serviceDefinition, final String serviceInstanceId, final int priority) {
		final OrchestrationStore store = new OrchestrationStore(consumer, serviceDefinition, serviceInstanceId, priority, "Creator");
		setOrchestrationStoreId(store, UUID.randomUUID());
		store.onCreate();
		return store;
	}

	//-------------------------------------------------------------------------------------------------
	private Subscription createSubscription(final String ownerSystem, final String targetSystem, final String serviceDefinition, final String orchestrationRequest,
			final String notifyProperties) {
		final Subscription subscription = new Subscription(ownerSystem, targetSystem, serviceDefinition, ZonedDateTime.now().plusHours(1), "HTTPS", notifyProperties, orchestrationRequest);
		setSubscriptionId(subscription, UUID.randomUUID());
		return subscription;
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationJob createOrchestrationJob(final String requesterSystem, final String targetSystem, final String serviceDefinition, final OrchestrationJobStatus status) {
		final OrchestrationJob job = new OrchestrationJob(OrchestrationType.PULL, requesterSystem, targetSystem, serviceDefinition, UUID.randomUUID().toString());
		job.setStatus(status);
		job.setCreatedAt(ZonedDateTime.now());
		job.setStartedAt(ZonedDateTime.now().plusSeconds(30));
		job.setFinishedAt(ZonedDateTime.now().plusMinutes(1));
		return job;
	}

	//-------------------------------------------------------------------------------------------------
	private void setOrchestrationStoreId(final OrchestrationStore store, final UUID id) {
		try {
			final java.lang.reflect.Field idField = OrchestrationStore.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(store, id);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void setSubscriptionId(final Subscription subscription, final UUID id) {
		try {
			final java.lang.reflect.Field idField = Subscription.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(subscription, id);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
}
