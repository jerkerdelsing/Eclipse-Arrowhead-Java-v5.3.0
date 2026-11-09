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
package eu.arrowhead.serviceorchestration.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationLockRequestDTO;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationLockManagementNormalization;

@ExtendWith(MockitoExtension.class)
public class OrchestrationLockManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationLockManagementValidation validator;

	@Mock
	private OrchestrationLockManagementNormalization normalization;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdValidator;

	@Mock
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	// validateAndNormalizeCreateService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceNullRequestPayload() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(null, "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Request payload is null", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceContainsNullDTO() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", expirationStr));
		dtoList.add(null);
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(new OrchestrationLockListRequestDTO(dtoList), "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Request payload contains null element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceMissingServiceInstanceId() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider|testService|1.0.0", "TestManager", expirationStr));
		dtoList.add(new OrchestrationLockRequestDTO("", "TestManager", expirationStr));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(new OrchestrationLockListRequestDTO(dtoList), "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Service instance id is missing", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceMissingOwner() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider1|testService|1.0.0", "TestManager", expirationStr));
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider2|testService|1.0.0", "", expirationStr));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(new OrchestrationLockListRequestDTO(dtoList), "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Owner is missing", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceExpirationIsInThePast() {
		final String expirationStrFuture = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final String expirationStrPast = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().minusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider1|testService|1.0.0", "TestManager", expirationStrFuture));
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider2|testService|1.0.0", "TestManager", expirationStrPast));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(new OrchestrationLockListRequestDTO(dtoList), "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Expires at is in the past: " + expirationStrPast, ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceInvalidExpirationTimeFormat() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider1|testService|1.0.0", "TestManager", expirationStr));
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider2|testService|1.0.0", "TestManager", "2025-03-08 00:00:00"));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(new OrchestrationLockListRequestDTO(dtoList), "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Invalid expires at format: 2025-03-08 00:00:00", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceMissingExpirationTime() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider1|testService|1.0.0", "TestManager", expirationStr));
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider2|testService|1.0.0", "TestManager", ""));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(new OrchestrationLockListRequestDTO(dtoList), "test.origin"));

		verify(normalization, never()).normalizeOrchestrationLockListRequestDTO(any());

		assertEquals("Expiration time is missing", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceCallAndCatchServiceInstanceIdValidator() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		final String notServiceId = "not-a-service-id";
		dtoList.add(new OrchestrationLockRequestDTO(notServiceId, "TestManager", expirationStr));
		final OrchestrationLockListRequestDTO dto = new OrchestrationLockListRequestDTO(dtoList);

		when(normalization.normalizeOrchestrationLockListRequestDTO(eq(dto))).thenReturn(dto);
		doThrow(new InvalidParameterException("test message")).when(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(notServiceId));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(dto, "test.origin"));

		verify(normalization).normalizeOrchestrationLockListRequestDTO(eq(dto));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(notServiceId));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceCallAndCatchSystemNameValidator() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		final String invalidSysName = "invalid-sys-name";
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider1|testService|1.0.0", invalidSysName, expirationStr));
		final OrchestrationLockListRequestDTO dto = new OrchestrationLockListRequestDTO(dtoList);

		when(normalization.normalizeOrchestrationLockListRequestDTO(eq(dto))).thenReturn(dto);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(invalidSysName));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeCreateService(dto, "test.origin"));

		verify(normalization).normalizeOrchestrationLockListRequestDTO(eq(dto));
		verify(systemNameValidator).validateSystemName(eq(invalidSysName));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateServiceEverythingIsValid() {
		final String expirationStr = Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusMinutes(10));
		final List<OrchestrationLockRequestDTO> dtoList = new ArrayList<>();
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider1|testService|1.0.0", "TestManager", expirationStr));
		dtoList.add(new OrchestrationLockRequestDTO("TestProvider2|testService|1.0.0", "TestManager", expirationStr));
		final OrchestrationLockListRequestDTO dto = new OrchestrationLockListRequestDTO(dtoList);

		when(normalization.normalizeOrchestrationLockListRequestDTO(eq(dto))).thenReturn(dto);

		final OrchestrationLockListRequestDTO result = validator.validateAndNormalizeCreateService(dto, "test.origin");

		verify(normalization).normalizeOrchestrationLockListRequestDTO(eq(dto));
		verify(serviceInstanceIdValidator, times(2)).validateServiceInstanceIdentifier(anyString());
		verify(systemNameValidator, times(2)).validateSystemName(anyString());

		assertEquals(dto, result);
	}

	// validateAndNormalizeQueryService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceNullDTO() {
		final OrchestrationLockQueryRequestDTO normalized = new OrchestrationLockQueryRequestDTO(null, null, null, null, null, null, null);
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(isNull())).thenReturn(normalized);

		final OrchestrationLockQueryRequestDTO result = validator.validateAndNormalizeQueryService(null, "test.origin");

		verify(pageValidator, never()).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(isNull());

		assertEquals(normalized, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsNullId() {
		final List<Long> ids = new ArrayList<>();
		ids.add(1L);
		ids.add(null);

		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, ids, null, null, null, null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationLockQueryRequestDTO(any());

		assertEquals("ID list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyJobId() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, List.of("1be77208-e779-4dc5-845f-7e4b4842dfa7", ""), null, null, null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationLockQueryRequestDTO(any());

		assertEquals("Orchestration job id list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyServiceInstanceId() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, null, List.of("TestProvider1|testService|1.0.0", ""), null, null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationLockQueryRequestDTO(any());

		assertEquals("Service instance id list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyOwner() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, null, null, List.of("TestManager", ""), null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationLockQueryRequestDTO(any());

		assertEquals("Owner list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidJobId() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, List.of("1be77208-e779-4dc5-845f-7e4b4842dfa7", "not-uuid"), null, null, null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(eq(dto));

		assertEquals("Invalid orchestration job id: not-uuid", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceCallAndCatchServiceInstanceIdValidator() {
		final String invalidServiceInstanceId = "invalid-service-instance-id";
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, null, List.of(invalidServiceInstanceId), null, null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(eq(dto))).thenReturn(dto);
		doThrow(new InvalidParameterException("test message")).when(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(invalidServiceInstanceId));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(eq(dto));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(invalidServiceInstanceId));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceCallAndCatchSystemNameValidator() {
		final String invalidSysName = "invalid-system-id";
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, null, null, List.of(invalidSysName), null, null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(eq(dto))).thenReturn(dto);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(invalidSysName));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(eq(dto));
		verify(systemNameValidator).validateSystemName(eq(invalidSysName));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidExpiresBefore() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, null, null, null, "2025-03-08 00:00:00", null);

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(eq(dto));

		assertEquals("Invalid expires before: 2025-03-08 00:00:00", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidExpiresAfter() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, null, null, null, null, null, "2025-03-08 00:00:00");

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(eq(dto));

		assertEquals("Invalid expires after: 2025-03-08 00:00:00", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceEverythingIsValid() {
		final OrchestrationLockQueryRequestDTO dto = new OrchestrationLockQueryRequestDTO(null, List.of(1L), List.of("9e037c52-599a-4b3f-8027-a49a8b4a8aa4"), List.of("TestProvider1|testService|1.0.0"),
				List.of("TestManager"), "2025-03-08T00:00:00Z", "2025-03-06T00:00:00Z");

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationLockQueryRequestDTO(eq(dto))).thenReturn(dto);

		final OrchestrationLockQueryRequestDTO result = validator.validateAndNormalizeQueryService(dto, "test.origin");

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationLockQueryRequestDTO(eq(dto));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(anyString());
		verify(systemNameValidator).validateSystemName(anyString());

		assertEquals(dto, result);
	}

	//validateRemoveService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceEmptyOwner() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeRemoveService("", List.of("TestProvider1|testService|1.0.0"), "test.origin"));

		verify(normalization, never()).normalizeSystemName(anyString());
		verify(normalization, never()).normalizeServiceInstanceIds(anyList());

		assertEquals("Owner is missing", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceEmptyServiceInstanceIdList() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeRemoveService("TestManager", List.of(), "test.origin"));

		verify(normalization, never()).normalizeSystemName(anyString());
		verify(normalization, never()).normalizeServiceInstanceIds(anyList());

		assertEquals("Service instance id list empty", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceContainsEmptyServiceInstanceId() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeRemoveService("TestManager", List.of("TestProvider1|testService|1.0.0", ""), "test.origin"));

		verify(normalization, never()).normalizeSystemName(anyString());
		verify(normalization, never()).normalizeServiceInstanceIds(anyList());

		assertEquals("Service instance id list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceCallAndCatchSystemNameValidator() {
		final String owner = "TestManager";
		final List<String> instances = List.of("TestProvider1|testService|1.0.0");

		when(normalization.normalizeSystemName(eq(owner))).thenReturn(owner);
		when(normalization.normalizeServiceInstanceIds(eq(instances))).thenReturn(instances);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(owner));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeRemoveService(owner, instances, "test.origin"));

		verify(normalization).normalizeSystemName(eq(owner));
		verify(normalization).normalizeServiceInstanceIds(eq(instances));
		verify(systemNameValidator).validateSystemName(eq(owner));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceCallAndCatchServiceInstanceIdValidator() {
		final String owner = "TestManager";
		final List<String> instances = List.of("TestProvider1|testService|1.0.0");

		when(normalization.normalizeSystemName(eq(owner))).thenReturn(owner);
		when(normalization.normalizeServiceInstanceIds(eq(instances))).thenReturn(instances);
		doThrow(new InvalidParameterException("test message")).when(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(instances.get(0)));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeRemoveService(owner, instances, "test.origin"));

		verify(normalization).normalizeSystemName(eq(owner));
		verify(normalization).normalizeServiceInstanceIds(eq(instances));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(instances.get(0)));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRemoveServiceEverythingIsValid() {
		final String owner = "TestManager";
		final List<String> instances = List.of("TestProvider1|testService|1.0.0", "TestProvider2|testService|1.0.0");

		when(normalization.normalizeSystemName(eq(owner))).thenReturn(owner);
		when(normalization.normalizeServiceInstanceIds(eq(instances))).thenReturn(instances);
		doNothing().when(systemNameValidator).validateSystemName(anyString());
		doNothing().when(serviceInstanceIdValidator).validateServiceInstanceIdentifier(anyString());

		final Pair<String, List<String>> result = validator.validateAndNormalizeRemoveService(owner, instances, "test.origin");

		verify(normalization).normalizeSystemName(eq(owner));
		verify(normalization).normalizeServiceInstanceIds(eq(instances));
		verify(systemNameValidator).validateSystemName(eq(owner));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(instances.get(0)));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(eq(instances.get(1)));

		assertEquals(owner, result.getLeft());
		assertEquals(instances, result.getRight());
	}
}
