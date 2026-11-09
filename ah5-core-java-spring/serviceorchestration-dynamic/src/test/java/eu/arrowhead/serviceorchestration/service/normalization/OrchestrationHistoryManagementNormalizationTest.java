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
package eu.arrowhead.serviceorchestration.service.normalization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class OrchestrationHistoryManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationHistoryManagementNormalization normalization;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Captor
	private ArgumentCaptor<String> serviceDefCaptor;

	@Captor
	private ArgumentCaptor<String> sysNameCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationHistoryQueryRequestDTO() {
		final OrchestrationHistoryQueryRequestDTO requestDTO = new OrchestrationHistoryQueryRequestDTO(new PageDTO(10, 20, "ASC", "id"), List.of(" id1 ", " id2 "), List.of(" pending ", " in_progress "), " pull ",
				List.of(" RequesterA ", " RequesterB "), List.of(" TargetA ", " TargetB "), List.of(" serviceA ", " serviceB "), List.of(" subscriptID1 ", " subscriptID2 "));

		when(systemNameNormalizer.normalize(eq(requestDTO.requesterSystems().get(0)))).thenReturn(requestDTO.requesterSystems().get(0).trim());
		when(systemNameNormalizer.normalize(eq(requestDTO.requesterSystems().get(1)))).thenReturn(requestDTO.requesterSystems().get(1).trim());
		when(systemNameNormalizer.normalize(eq(requestDTO.targetSystems().get(0)))).thenReturn(requestDTO.targetSystems().get(0).trim());
		when(systemNameNormalizer.normalize(eq(requestDTO.targetSystems().get(1)))).thenReturn(requestDTO.targetSystems().get(1).trim());
		when(serviceDefNameNormalizer.normalize(eq(requestDTO.serviceDefinitions().get(0)))).thenReturn(requestDTO.serviceDefinitions().get(0).trim());
		when(serviceDefNameNormalizer.normalize(eq(requestDTO.serviceDefinitions().get(1)))).thenReturn(requestDTO.serviceDefinitions().get(1).trim());

		final OrchestrationHistoryQueryRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationHistoryQueryRequestDTO(requestDTO));

		verify(systemNameNormalizer, times(4)).normalize(sysNameCaptor.capture());
		verify(serviceDefNameNormalizer, times(2)).normalize(serviceDefCaptor.capture());

		assertEquals(requestDTO.requesterSystems().get(0), sysNameCaptor.getAllValues().get(0));
		assertEquals(requestDTO.requesterSystems().get(1), sysNameCaptor.getAllValues().get(1));
		assertEquals(requestDTO.targetSystems().get(0), sysNameCaptor.getAllValues().get(2));
		assertEquals(requestDTO.targetSystems().get(1), sysNameCaptor.getAllValues().get(3));
		assertEquals(requestDTO.serviceDefinitions().get(0), serviceDefCaptor.getAllValues().get(0));
		assertEquals(requestDTO.serviceDefinitions().get(1), serviceDefCaptor.getAllValues().get(1));

		assertEquals(requestDTO.pagination().page(), result.pagination().page());
		assertEquals(requestDTO.pagination().size(), result.pagination().size());
		assertEquals(requestDTO.pagination().direction(), result.pagination().direction());
		assertEquals(requestDTO.pagination().sortField(), result.pagination().sortField());
		assertEquals(requestDTO.ids().get(0).trim(), result.ids().get(0));
		assertEquals(requestDTO.ids().get(1).trim(), result.ids().get(1));
		assertEquals(requestDTO.statuses().get(0).toUpperCase().trim(), result.statuses().get(0));
		assertEquals(requestDTO.statuses().get(1).toUpperCase().trim(), result.statuses().get(1));
		assertEquals(requestDTO.type().toUpperCase().trim(), result.type());
		assertEquals(requestDTO.requesterSystems().get(0).trim(), result.requesterSystems().get(0));
		assertEquals(requestDTO.requesterSystems().get(1).trim(), result.requesterSystems().get(1));
		assertEquals(requestDTO.targetSystems().get(0).trim(), result.targetSystems().get(0));
		assertEquals(requestDTO.targetSystems().get(1).trim(), result.targetSystems().get(1));
		assertEquals(requestDTO.serviceDefinitions().get(0).trim(), result.serviceDefinitions().get(0));
		assertEquals(requestDTO.serviceDefinitions().get(1).trim(), result.serviceDefinitions().get(1));
		assertEquals(requestDTO.subscriptionIds().get(0).trim(), result.subscriptionIds().get(0));
		assertEquals(requestDTO.subscriptionIds().get(1).trim(), result.subscriptionIds().get(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationHistoryQueryRequestDTOEmptyInput() {
		final OrchestrationHistoryQueryRequestDTO requestDTO = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, null, null, null);

		final OrchestrationHistoryQueryRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationHistoryQueryRequestDTO(requestDTO));

		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer, never()).normalize(anyString());

		assertNull(result.pagination());
		assertTrue(result.ids().size() == 0);
		assertTrue(result.statuses().size() == 0);
		assertNull(result.type());
		assertTrue(result.requesterSystems().size() == 0);
		assertTrue(result.targetSystems().size() == 0);
		assertTrue(result.serviceDefinitions().size() == 0);
		assertTrue(result.subscriptionIds().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationHistoryQueryRequestDTONullInput() {
		final OrchestrationHistoryQueryRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationHistoryQueryRequestDTO(null));

		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer, never()).normalize(anyString());

		assertNull(result.pagination());
		assertTrue(result.ids().size() == 0);
		assertTrue(result.statuses().size() == 0);
		assertNull(result.type());
		assertTrue(result.requesterSystems().size() == 0);
		assertTrue(result.targetSystems().size() == 0);
		assertTrue(result.serviceDefinitions().size() == 0);
		assertTrue(result.subscriptionIds().size() == 0);
	}
}
