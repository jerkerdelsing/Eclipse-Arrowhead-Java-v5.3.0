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

package eu.arrowhead.serviceorchestration.service.utils;

import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ServiceOrchestrationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceOrchestration serviceOrchestration;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private SimpleStoreDbService storeDbService;


	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOrchestrateWithoutServiceDef() {

		when(orchJobDbService.setStatus(any(), any(), any())).thenReturn(any());


		final OrchestrationStore entry1 = new OrchestrationStore("CarConsumer1", "carService1", "CarManager|carService1|4.0.0", 2, "Sysop");
		final OrchestrationStore entry2 = new OrchestrationStore("CarConsumer1", "carService2", "CarManager|carService2|5.0.0", 4, "Sysop");
		final OrchestrationStore entry3 = new OrchestrationStore("CarConsumer1", "carService2", "CarManager|carService2|4.0.0", 6, "Sysop");
		final OrchestrationStore entry4 = new OrchestrationStore("CarConsumer1", "carService1", "CarProvider|carService1|5.0.0", 3, "Sysop");

		// needed for equals
		entry1.setId(UUID.randomUUID());
		entry2.setId(UUID.randomUUID());
		entry3.setId(UUID.randomUUID());
		entry4.setId(UUID.randomUUID());

		when(storeDbService.getByConsumer("CarConsumer1")).thenReturn(List.of(entry1, entry4, entry2, entry3));

		Assertions.assertAll(
				// no matchmaking, no preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), false);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(null, List.of(), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(4, result.size());
					assertEquals(entry1, result.get(0));
					assertEquals(entry4, result.get(1));
					assertEquals(entry2, result.get(2));
					assertEquals(entry3, result.get(3));
				},

				//  matchmaking, no preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), true);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(null, List.of(), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(2, result.size());
					assertEquals(entry1, result.get(0));
					assertEquals(entry2, result.get(1));
				},

				//  no matchmaking, preferred but not only
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), false);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(null, List.of("CarProvider"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(4, result.size());
					assertEquals(entry4, result.get(0));
					assertEquals(entry1, result.get(1));
					assertEquals(entry2, result.get(2));
					assertEquals(entry3, result.get(3));
				},

				//  matchmaking, preferred but not only
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), true);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(null, List.of("CarProvider"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(2, result.size());
					assertEquals(entry4, result.get(0));
					assertEquals(entry2, result.get(1));
				},
				//  no matchmaking, only preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), true, OrchestrationFlag.MATCHMAKING.toString(), false);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(null, List.of("CarManager"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(3, result.size());
					assertEquals(entry1, result.get(0));
					assertEquals(entry2, result.get(1));
					assertEquals(entry3, result.get(2));
				},
				//  matchmaking, only preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), true, OrchestrationFlag.MATCHMAKING.toString(), true);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest(null, List.of("CarManager"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(2, result.size());
					assertEquals(entry1, result.get(0));
					assertEquals(entry2, result.get(1));
				}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOrchestrateWithServiceDef() {

		final OrchestrationStore entry1 = new OrchestrationStore("CarConsumer1", "carService1", "CarManager|carService1|4.0.0", 2, "Sysop");
		final OrchestrationStore entry2 = new OrchestrationStore("CarConsumer1", "carService1", "CarManager|carService2|5.0.0", 4, "Sysop");
		final OrchestrationStore entry3 = new OrchestrationStore("CarConsumer1", "carService1", "CarProvider|carService1|5.0.0", 3, "Sysop");

		// needed for equals
		entry1.setId(UUID.randomUUID());
		entry2.setId(UUID.randomUUID());
		entry3.setId(UUID.randomUUID());

		when(storeDbService.getByConsumerAndServiceDefinition("CarConsumer1", "carService1")).thenReturn(List.of(entry1, entry3, entry2));

		Assertions.assertAll(
				// no matchmaking, no preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), false);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest("carService1", List.of(), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(3, result.size());
					assertEquals(entry1, result.get(0));
					assertEquals(entry3, result.get(1));
					assertEquals(entry2, result.get(2));
				},

				//  matchmaking, no preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), true);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest("carService1", List.of(), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(1, result.size());
					assertEquals(entry1, result.getFirst());
				},

				//  no matchmaking, preferred but not only
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), false);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest("carService1", List.of("CarProvider"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(3, result.size());
					assertEquals(entry3, result.get(0));
					assertEquals(entry1, result.get(1));
					assertEquals(entry2, result.get(2));
				},

				//  matchmaking, preferred but not only
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), false, OrchestrationFlag.MATCHMAKING.toString(), true);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest("carService1", List.of("CarProvider"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(1, result.size());
					assertEquals(entry3, result.getFirst());
				},
				//  no matchmaking, only preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), true, OrchestrationFlag.MATCHMAKING.toString(), false);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest("carService1", List.of("CarManager"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(2, result.size());
					assertEquals(entry1, result.get(0));
					assertEquals(entry2, result.get(1));
				},
				//  matchmaking, only preferred
				() -> {
					final Map<String, Boolean> orchestrationFlags = Map.of(OrchestrationFlag.ONLY_PREFERRED.toString(), true, OrchestrationFlag.MATCHMAKING.toString(), true);
					final SimpleOrchestrationRequest request = new SimpleOrchestrationRequest("carService1", List.of("CarProvider"), orchestrationFlags, null);

					final List<OrchestrationStore> result = serviceOrchestration.orchestrate(null, "CarConsumer1", request);
					assertEquals(1, result.size());
					assertEquals(entry3, result.getFirst());
				}
		);
	}

}
