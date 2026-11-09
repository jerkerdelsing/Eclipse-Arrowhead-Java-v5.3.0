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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationHistoryManagementNormalization;

@ExtendWith(MockitoExtension.class)
public class OrchestrationHistoryManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationHistoryManagementValidation validator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private OrchestrationHistoryManagementNormalization normalization;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceNullDTO() {
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(any()))
				.thenReturn(new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of()));

		assertDoesNotThrow(() -> validator.validateAndNormalizeQueryService(null, "test.origin"));

		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyId() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of("id1", "", "id3"), List.of(), null, List.of(), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationHistoryQueryRequestDTO(any());

		assertEquals("ID list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyStatus() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of("PENDING", "", "IN_PROGRESS"), null, List.of(), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationHistoryQueryRequestDTO(any());

		assertEquals("Status list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyRequesterSystem() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of("sys1", "", "sys3"), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationHistoryQueryRequestDTO(any());

		assertEquals("Requester system list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyTargetSystem() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of("sys1", "", "sys3"), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationHistoryQueryRequestDTO(any());

		assertEquals("Target system list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptyServiceDefinition() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of(), List.of("service1", "", "service3"), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationHistoryQueryRequestDTO(any());

		assertEquals("Service definition list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceContainsEmptySubscriptionID() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of("id1", "", "id3"));
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization, never()).normalizeOrchestrationHistoryQueryRequestDTO(any());

		assertEquals("Subscription ID list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidId() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of("not-uuid"), List.of(), null, List.of(), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));

		assertEquals("Invalid id: not-uuid", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidStatus() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of("not-a-status"), null, List.of(), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));

		assertEquals("Invalid status: not-a-status", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidType() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), "not-a-type", List.of(), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));

		assertEquals("Invalid type: not-a-type", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidSubscriptionId() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of("not-uuid"));
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));

		assertEquals("Invalid id: not-uuid", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceNormalizedNameValidations() {
		final String reqSys1 = "ReqSys1";
		final String reqSys2 = "ReqSys2";
		final String targetSys1 = "TargetSys1";
		final String targetSys2 = "TargetSys2";
		final String serviceDef1 = "serviceDef1";
		final String serviceDef2 = "serviceDef2";
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(reqSys1, reqSys2), List.of(targetSys1, targetSys2), List.of(serviceDef1, serviceDef2), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);

		assertDoesNotThrow(() -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));
		verify(systemNameValidator).validateSystemName(eq(reqSys1));
		verify(systemNameValidator).validateSystemName(eq(reqSys2));
		verify(systemNameValidator).validateSystemName(eq(targetSys1));
		verify(systemNameValidator).validateSystemName(eq(targetSys2));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq(serviceDef1));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq(serviceDef2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidSytemName() {
		final String sysName = "sys-name";
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(sysName), List.of(), List.of(), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(sysName));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));
		verify(systemNameValidator).validateSystemName(eq(sysName));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInvalidServiceDefinitionName() {
		final String serviceName = "service-name";
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of(), List.of(), null, List.of(), List.of(), List.of(serviceName), List.of());
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);
		doThrow(new InvalidParameterException("test message")).when(serviceDefNameValidator).validateServiceDefinitionName(serviceName);

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(eq(dto));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq(serviceName));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceEverythingIsValid() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of("9e9803b1-4433-4acf-875b-2dda484bec00"), List.of("PENDING"), "PUSH", List.of("ReqSys"), List.of("TargetSys"), List.of("serviceDef"),
				List.of("cc541986-4530-4313-860e-d529abece31b"));
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto))).thenReturn(dto);
		doNothing().when(systemNameValidator).validateSystemName(anyString());
		doNothing().when(serviceDefNameValidator).validateServiceDefinitionName(anyString());

		final OrchestrationHistoryQueryRequestDTO result = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(any());
		verify(systemNameValidator, times(2)).validateSystemName(anyString());
		verify(serviceDefNameValidator).validateServiceDefinitionName(anyString());

		assertEquals(dto, result);
	}
}
