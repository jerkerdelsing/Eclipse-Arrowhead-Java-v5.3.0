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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationHistoryQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.enums.OrchestrationType;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationJobQueryRequest;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationHistoryManagementNormalization;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
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
	private OrchestrationValidation orchestrationValidator;

	@Mock
	private OrchestrationHistoryManagementNormalization normalization;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceNullDto() {
		final NormalizedOrchestrationJobQueryRequest expected = new NormalizedOrchestrationJobQueryRequest(null, List.of(), List.of(), null, List.of(), List.of(), List.of(), List.of());

		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(null), eq("test origin"))).thenReturn(expected);

		final NormalizedOrchestrationJobQueryRequest result = validator.validateAndNormalizeQueryService(null, "test origin");

		assertEquals(expected, result);
		verify(normalization).normalizeOrchestrationHistoryQueryRequestDTO(null, "test origin");
		verify(pageValidator, never()).validatePageParameter(any(), any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithInvalidId() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, List.of("invalid-uuid"), null, null, null, null, null, null);

		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchestrationValidator).validateUUID(eq("invalid-uuid"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchestrationValidator).validateUUID("invalid-uuid", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithInvalidStatus() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, List.of("INVALID_STATUS"), null, null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Invalid status: INVALID_STATUS", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithStatusesContainsNull() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, Arrays.asList("PENDING", null), null, null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Status list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithInvalidType() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, null, "INVALID_TYPE", null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Invalid type: INVALID_TYPE", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithRequesterSystemsContainsNull() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, Arrays.asList("System1", null), null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Requester system list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithTargetSystemsContainsNull() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, Arrays.asList("System1", null), null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Target system list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithServiceDefinitionsContainsNull() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(
				null, null, null, null, null, null, Arrays.asList("serviceDef1", null), null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Service definition list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithInvalidRequesterSystemName() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, List.of("inv@lid"), null, null, null);
		final NormalizedOrchestrationJobQueryRequest normalized = new NormalizedOrchestrationJobQueryRequest(null, List.of(), List.of(), null, List.of("inv@lid"), List.of(), List.of(), List.of());

		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto), eq("test origin"))).thenReturn(normalized);
		doThrow(new InvalidParameterException("Invalid parameter")).when(systemNameValidator).validateSystemName("inv@lid");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithInvalidTargetSystemName() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, List.of("inv@lid"), null, null);
		final NormalizedOrchestrationJobQueryRequest normalized = new NormalizedOrchestrationJobQueryRequest(null, List.of(), List.of(), null, List.of(), List.of("inv@lid"), List.of(), List.of());

		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto), eq("test origin"))).thenReturn(normalized);
		doThrow(new InvalidParameterException("Invalid parameter")).when(systemNameValidator).validateSystemName("inv@lid");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithInvalidServiceDefinitionName() {
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(null, null, null, null, null, null, List.of("inv@lid-service"), null);
		final NormalizedOrchestrationJobQueryRequest normalized = new NormalizedOrchestrationJobQueryRequest(null, List.of(), List.of(), null, List.of(), List.of(), List.of("inv@lid-service"), List.of());

		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto), eq("test origin"))).thenReturn(normalized);
		doThrow(new InvalidParameterException("Invalid parameter")).when(serviceDefNameValidator).validateServiceDefinitionName("inv@lid-service");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceWithAllFieldsOk() {
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
		final UUID subscriptionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
		final OrchestrationHistoryQueryRequestDTO dto = new OrchestrationHistoryQueryRequestDTO(
				pagination,
				List.of("11111111-1111-1111-1111-111111111111"),
				List.of("PENDING"),
				"PULL",
				List.of("RequesterSystem"),
				List.of("TargetSystem"),
				List.of("serviceDef"),
				List.of("22222222-2222-2222-2222-222222222222"));

		final NormalizedOrchestrationJobQueryRequest expected = new NormalizedOrchestrationJobQueryRequest(
				PageRequest.of(0, 10),
				List.of(uuid),
				List.of(OrchestrationJobStatus.PENDING),
				OrchestrationType.PULL,
				List.of("RequesterSystem"),
				List.of("TargetSystem"),
				List.of("serviceDef"),
				List.of(subscriptionId));

		when(normalization.normalizeOrchestrationHistoryQueryRequestDTO(eq(dto), eq("test origin"))).thenReturn(expected);

		final NormalizedOrchestrationJobQueryRequest result = validator.validateAndNormalizeQueryService(dto, "test origin");

		assertNotNull(result);
		verify(pageValidator).validatePageParameter(eq(pagination), eq(OrchestrationJob.SORTABLE_FIELDS_BY), eq("test origin"));
		verify(orchestrationValidator, times(2)).validateUUID("11111111-1111-1111-1111-111111111111", "test origin");
		verify(systemNameValidator).validateSystemName("RequesterSystem");
		verify(systemNameValidator).validateSystemName("TargetSystem");
		verify(serviceDefNameValidator).validateServiceDefinitionName("serviceDef");
	}
}
