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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationLockRequestDTO;
import eu.arrowhead.dto.PageDTO;

@ExtendWith(MockitoExtension.class)
public class OrchestrationLockManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationLockManagementNormalization normalization;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdNormalizer;

	@Captor
	private ArgumentCaptor<String> instanceIdCaptor;

	@Captor
	private ArgumentCaptor<String> systemNameCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationLockListRequestDTO() {
		final OrchestrationLockRequestDTO lockRequestDTO1 = new OrchestrationLockRequestDTO("TestProviderA|testService|1.0.0", "TestManagerC", Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()));
		final OrchestrationLockRequestDTO lockRequestDTO2 = new OrchestrationLockRequestDTO("TestProviderB|testService|1.0.0", "TestManagerD", Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()));
		final OrchestrationLockListRequestDTO requestDTOList = new OrchestrationLockListRequestDTO(List.of(lockRequestDTO1, lockRequestDTO2));

		when(systemNameNormalizer.normalize(eq(lockRequestDTO1.owner()))).thenReturn(lockRequestDTO1.owner());
		when(systemNameNormalizer.normalize(eq(lockRequestDTO2.owner()))).thenReturn(lockRequestDTO2.owner());
		when(serviceInstanceIdNormalizer.normalize(eq(lockRequestDTO1.serviceInstanceId()))).thenReturn(lockRequestDTO1.serviceInstanceId());
		when(serviceInstanceIdNormalizer.normalize(eq(lockRequestDTO2.serviceInstanceId()))).thenReturn(lockRequestDTO2.serviceInstanceId());

		final OrchestrationLockListRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationLockListRequestDTO(requestDTOList));

		verify(systemNameNormalizer, times(2)).normalize(systemNameCaptor.capture());
		verify(serviceInstanceIdNormalizer, times(2)).normalize(instanceIdCaptor.capture());

		assertEquals(lockRequestDTO1.serviceInstanceId(), instanceIdCaptor.getAllValues().get(0));
		assertEquals(lockRequestDTO2.serviceInstanceId(), instanceIdCaptor.getAllValues().get(1));
		assertEquals(lockRequestDTO1.owner(), systemNameCaptor.getAllValues().get(0));
		assertEquals(lockRequestDTO2.owner(), systemNameCaptor.getAllValues().get(1));

		assertEquals(lockRequestDTO1.serviceInstanceId(), result.locks().get(0).serviceInstanceId());
		assertEquals(lockRequestDTO2.serviceInstanceId(), result.locks().get(1).serviceInstanceId());
		assertEquals(lockRequestDTO1.owner(), result.locks().get(0).owner());
		assertEquals(lockRequestDTO2.owner(), result.locks().get(1).owner());
		assertEquals(lockRequestDTO1.expiresAt(), result.locks().get(0).expiresAt());
		assertEquals(lockRequestDTO2.expiresAt(), result.locks().get(1).expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationLockListRequestDTONullInput() {
		final Throwable ex = assertThrows(Throwable.class, () -> normalization.normalizeOrchestrationLockListRequestDTO(null));

		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(serviceInstanceIdNormalizer, never()).normalize(anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationLockQueryRequestDTO() {
		final OrchestrationLockQueryRequestDTO requestDTO = new OrchestrationLockQueryRequestDTO(new PageDTO(6, 50, "ASC", "id"), List.of(105L), List.of(" jobId1 ", " jobId2 "), List.of(" instanceId1 ", " instanceId2 "),
				List.of(" TestManagerA ", " TestManagerB "), " " + Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()) + " ", " " + Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow()) + " ");

		when(systemNameNormalizer.normalize(eq(requestDTO.owners().get(0)))).thenReturn(requestDTO.owners().get(0).trim());
		when(systemNameNormalizer.normalize(eq(requestDTO.owners().get(1)))).thenReturn(requestDTO.owners().get(1).trim());
		when(serviceInstanceIdNormalizer.normalize(eq(requestDTO.serviceInstanceIds().get(0)))).thenReturn(requestDTO.serviceInstanceIds().get(0).trim());
		when(serviceInstanceIdNormalizer.normalize(eq(requestDTO.serviceInstanceIds().get(1)))).thenReturn(requestDTO.serviceInstanceIds().get(1).trim());

		final OrchestrationLockQueryRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationLockQueryRequestDTO(requestDTO));

		verify(systemNameNormalizer, times(2)).normalize(systemNameCaptor.capture());
		verify(serviceInstanceIdNormalizer, times(2)).normalize(instanceIdCaptor.capture());

		assertEquals(requestDTO.owners().get(0), systemNameCaptor.getAllValues().get(0));
		assertEquals(requestDTO.owners().get(1), systemNameCaptor.getAllValues().get(1));
		assertEquals(requestDTO.serviceInstanceIds().get(0), instanceIdCaptor.getAllValues().get(0));
		assertEquals(requestDTO.serviceInstanceIds().get(1), instanceIdCaptor.getAllValues().get(1));

		assertEquals(requestDTO.pagination().page(), result.pagination().page());
		assertEquals(requestDTO.pagination().size(), result.pagination().size());
		assertEquals(requestDTO.pagination().direction(), result.pagination().direction());
		assertEquals(requestDTO.pagination().sortField(), result.pagination().sortField());
		assertEquals(requestDTO.ids().get(0), result.ids().get(0));
		assertEquals(requestDTO.orchestrationJobIds().get(0).trim(), result.orchestrationJobIds().get(0));
		assertEquals(requestDTO.orchestrationJobIds().get(1).trim(), result.orchestrationJobIds().get(1));
		assertEquals(requestDTO.serviceInstanceIds().get(0).trim(), result.serviceInstanceIds().get(0));
		assertEquals(requestDTO.serviceInstanceIds().get(1).trim(), result.serviceInstanceIds().get(1));
		assertEquals(requestDTO.owners().get(0).trim(), result.owners().get(0));
		assertEquals(requestDTO.owners().get(1).trim(), result.owners().get(1));
		assertEquals(requestDTO.expiresBefore().trim(), result.expiresBefore());
		assertEquals(requestDTO.expiresAfter().trim(), result.expiresAfter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationLockQueryRequestDTONullInput() {
		final OrchestrationLockQueryRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationLockQueryRequestDTO(null));

		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(serviceInstanceIdNormalizer, never()).normalize(anyString());

		assertNull(result.pagination());
		assertNull(result.ids());
		assertNull(result.orchestrationJobIds());
		assertNull(result.serviceInstanceIds());
		assertNull(result.owners());
		assertNull(result.expiresBefore());
		assertNull(result.expiresAfter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationLockQueryRequestDTOEmptyInput() {
		final OrchestrationLockQueryRequestDTO requestDTO = new OrchestrationLockQueryRequestDTO(null, null, null, null, null, null, null);

		final OrchestrationLockQueryRequestDTO result = assertDoesNotThrow(() -> normalization.normalizeOrchestrationLockQueryRequestDTO(requestDTO));

		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(serviceInstanceIdNormalizer, never()).normalize(anyString());

		assertNull(result.pagination());
		assertTrue(result.ids().size() == 0);
		assertTrue(result.orchestrationJobIds().size() == 0);
		assertTrue(result.serviceInstanceIds().size() == 0);
		assertNull(result.expiresBefore());
		assertNull(result.expiresAfter());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInstanceIds() {
		final List<String> instanceIds = List.of(" jobId1 ", " jobId2 ");

		when(serviceInstanceIdNormalizer.normalize(eq(instanceIds.get(0)))).thenReturn(instanceIds.get(0).trim());
		when(serviceInstanceIdNormalizer.normalize(eq(instanceIds.get(1)))).thenReturn(instanceIds.get(1).trim());

		final List<String> result = assertDoesNotThrow(() -> normalization.normalizeServiceInstanceIds(instanceIds));

		verify(serviceInstanceIdNormalizer, times(2)).normalize(instanceIdCaptor.capture());

		assertEquals(instanceIds.get(0), instanceIdCaptor.getAllValues().get(0));
		assertEquals(instanceIds.get(1), instanceIdCaptor.getAllValues().get(1));

		assertEquals(instanceIds.get(0).trim(), result.get(0));
		assertEquals(instanceIds.get(1).trim(), result.get(1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInstanceIdsEmptyInput() {
		final List<String> instanceIds = List.of();

		final Throwable ex = assertThrows(Throwable.class, () -> normalization.normalizeServiceInstanceIds(instanceIds));

		verify(serviceInstanceIdNormalizer, never()).normalize(anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("Service instance id list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemName() {
		final String sysName = " TestProvider ";

		when(systemNameNormalizer.normalize(eq(sysName))).thenReturn(sysName.trim());

		final String result = assertDoesNotThrow(() -> normalization.normalizeSystemName(sysName));

		verify(systemNameNormalizer).normalize(systemNameCaptor.capture());

		assertEquals(sysName, systemNameCaptor.getValue());
		assertEquals(sysName.trim(), result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemNameEmpty() {
		final String sysName = " ";

		final Throwable ex = assertThrows(Throwable.class, () -> normalization.normalizeSystemName(sysName));

		verify(systemNameNormalizer, never()).normalize(anyString());

		assertTrue(ex instanceof IllegalArgumentException);
		assertEquals("System name is empty", ex.getMessage());
	}
}
