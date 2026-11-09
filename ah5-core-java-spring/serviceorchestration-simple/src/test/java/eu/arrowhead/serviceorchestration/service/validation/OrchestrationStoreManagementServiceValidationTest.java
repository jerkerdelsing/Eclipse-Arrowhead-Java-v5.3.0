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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationSimpleStoreQueryRequest;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationStoreManagementServiceNormalization;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class OrchestrationStoreManagementServiceValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationStoreManagementServiceValidation validator;

	@Mock
	private OrchestrationStoreManagementServiceNormalization normalizer;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	// Tests for validateAndNormalizeCreateBulk

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkPayloadIsNull() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(null, "test origin"));
		assertEquals("Request payload is null", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkPayloadIsEmpty() {

		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Request payload is empty", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkCandidatesAreNull() {
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Request payload is empty", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkCandidatesContainsNull() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(Arrays.asList(candidate, null));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Request payload contains null element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkMissingPriority() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", null);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Priority is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkZeroPriority() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", 0);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Priority should be positive", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkNegativePriority() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", -5);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Priority should be positive", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkInvalidSystemName() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Inv@lid", "Provider1|service1|1.0.0", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		when(normalizer.normalizeCreate(any(OrchestrationSimpleStoreRequestDTO.class))).thenReturn(candidate);
		doThrow(new InvalidParameterException("Invalid parameter")).when(systemNameValidator).validateSystemName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkInvalidServiceInstanceId() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "inv@lidInstanceId", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		when(normalizer.normalizeCreate(any(OrchestrationSimpleStoreRequestDTO.class))).thenReturn(candidate);
		doThrow(new InvalidParameterException("Invalid parameter")).when(serviceInstanceIdValidator).validateServiceInstanceIdentifier(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkDuplicateCandidates() {
		final OrchestrationSimpleStoreRequestDTO candidate1 = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", 1);
		final OrchestrationSimpleStoreRequestDTO candidate2 = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|2.0.0", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate1, candidate2));

		when(normalizer.normalizeCreate(candidate1)).thenReturn(candidate1);
		when(normalizer.normalizeCreate(candidate2)).thenReturn(candidate2);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeCreateBulk(dto, "test origin"));
		assertEquals("Conflicting rules, the combination of the following fields should be unique: Consumer1, service definition: service1, priority: 1", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeCreateBulkOk() {
		final OrchestrationSimpleStoreRequestDTO candidate1 = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|servicedef1|1.0.0", 1);
		final OrchestrationSimpleStoreRequestDTO candidate2 = new OrchestrationSimpleStoreRequestDTO("Consumer2", "Provider2|servicedef2|1.0.0", 2);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate1, candidate2));

		when(normalizer.normalizeCreate(candidate1)).thenReturn(candidate1);
		when(normalizer.normalizeCreate(candidate2)).thenReturn(candidate2);

		final List<OrchestrationSimpleStoreRequestDTO> actual = validator.validateAndNormalizeCreateBulk(dto, "test origin");

		assertNotNull(actual);
		assertEquals(2, actual.size());
		verify(normalizer, times(2)).normalizeCreate(any(OrchestrationSimpleStoreRequestDTO.class));
		verify(systemNameValidator, times(2)).validateSystemName(any(String.class));
		verify(serviceInstanceIdValidator, times(2)).validateServiceInstanceIdentifier(any(String.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryNullDto() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(null, "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryNoFilterFields() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, null, null, null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("At least one of the following fields must be specified: ids, consumerNames, serviceDefinitions, serviceInstanceIds, createdBy", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidUuid() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, List.of("6-7"), null, null, null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid UUID: 6-7", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryIdsContainsNullOrEmpty() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, Arrays.asList("11111111-1111-1111-1111-111111111111", ""), null, null, null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Id list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryConsumerNamesContainsNullOrEmpty() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, List.of("Consumer1", ""), null, null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Consumer name list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceDefinitionsContainsNullOrEmpty() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, null, Arrays.asList("serviceDef1", null), null, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Service definition name list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryServiceInstanceIdsContainsNullOrEmpty() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, null, null, Arrays.asList("instanceId1", ""), null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Service instance id list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidMinPriority() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, List.of("consumer1"), null, null, 0, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid minimum priority: should be a positive integer", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidMaxPriority() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, List.of("consumer1"), null, null, null, -1, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid maximum priority: should be a positive integer", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryMinPriorityGreaterThanMaxPriority() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, List.of("consumer1"), null, null, 10, 5, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Minimum priority should not be greater than maximum priority", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidConsumerName() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, List.of("inv@lid-consumer"), null, null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest normalizedDto = new NormalizedOrchestrationSimpleStoreQueryRequest(
				null, null, List.of("inv@lid-consumer"), null, null, null, null, null);

		when(normalizer.normalizeQuery(dto)).thenReturn(normalizedDto);
		doThrow(new InvalidParameterException("Invalid parameter")).when(systemNameValidator).validateSystemName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidServiceDefinition() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, null, List.of("inv@lid-serviceDef"), null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest normalizedDto = new NormalizedOrchestrationSimpleStoreQueryRequest(null, null, null, List.of("inv@lid-serviceDef"), null, null, null, null);

		when(normalizer.normalizeQuery(dto)).thenReturn(normalizedDto);
		doThrow(new InvalidParameterException("Invalid parameter")).when(serviceDefNameValidator).validateServiceDefinitionName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidServiceInstanceId() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(
				null, null, null, null, List.of("invälid-instance-id"), null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest normalizedDto = new NormalizedOrchestrationSimpleStoreQueryRequest(
				null, null, null, null, List.of("invälid-instance-id"), null, null, null);

		when(normalizer.normalizeQuery(dto)).thenReturn(normalizedDto);
		doThrow(new InvalidParameterException("Invalid parameter")).when(serviceInstanceIdValidator).validateServiceInstanceIdentifier(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryInvalidCreatedBy() {
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, null, null, null, null, null, null, "inv@lid-creator");

		final NormalizedOrchestrationSimpleStoreQueryRequest normalizedDto = new NormalizedOrchestrationSimpleStoreQueryRequest(null, null, null, null, null, null, null, "inv@lid-creator");

		when(normalizer.normalizeQuery(dto)).thenReturn(normalizedDto);
		doThrow(new InvalidParameterException("Invalid parameter")).when(systemNameValidator).validateSystemName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQuery(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryWithIdsOk() {
		final UUID uuid = UUID.randomUUID();
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(null, List.of(uuid.toString()), null, null, null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest normalizedDto = new NormalizedOrchestrationSimpleStoreQueryRequest(null, List.of(uuid), null, null, null, null, null, null);

		when(normalizer.normalizeQuery(dto)).thenReturn(normalizedDto);

		final NormalizedOrchestrationSimpleStoreQueryRequest result = validator.validateAndNormalizeQuery(dto, "test origin");

		assertEquals(1, result.ids().size());
		assertEquals(uuid, result.ids().get(0));
		verify(pageValidator).validatePageParameter(eq(null), eq(OrchestrationStore.SORTABLE_FIELDS_BY), eq("test origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryWithAllFieldsOk() {
		final UUID uuid = UUID.randomUUID();
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(
				pagination,
				List.of(uuid.toString()),
				List.of("consumer1"),
				List.of("servicedef1"),
				List.of("instanceId1"),
				1,
				10,
				"creator1");

		final NormalizedOrchestrationSimpleStoreQueryRequest normalizedDto = new NormalizedOrchestrationSimpleStoreQueryRequest(
				pagination,
				List.of(uuid),
				List.of("consumer1"),
				List.of("servicedef1"),
				List.of("instanceId1"),
				1,
				10,
				"creator1");

		when(normalizer.normalizeQuery(dto)).thenReturn(normalizedDto);

		final NormalizedOrchestrationSimpleStoreQueryRequest result = validator.validateAndNormalizeQuery(dto, "test origin");

		assertNotNull(result);
		verify(pageValidator).validatePageParameter(eq(pagination), eq(OrchestrationStore.SORTABLE_FIELDS_BY), eq("test origin"));
		verify(systemNameValidator, times(2)).validateSystemName(any(String.class));
		verify(serviceDefNameValidator).validateServiceDefinitionName(any(String.class));
		verify(serviceInstanceIdValidator).validateServiceInstanceIdentifier(any(String.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePriorityRequestDTONullDto() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePriorityRequestDTO(null, "test origin"));
		assertEquals("Priority map is null", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePriorityRequestDTOInvalidUuid() {
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put("6-7", 1);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePriorityRequestDTO(dto, "test origin"));
		assertEquals("Invalid UUID: 6-7", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePriorityRequestDTONullPriorityValue() {
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put("11111111-1111-1111-1111-111111111111", null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePriorityRequestDTO(dto, "test origin"));
		assertEquals("Priority map contains null value", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePriorityRequestDTOZeroPriority() {
		final UUID uuid = UUID.randomUUID();
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put(uuid.toString(), 0);

		when(normalizer.normalizePriorityRequestDTO(dto)).thenReturn(Map.of(uuid, 0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePriorityRequestDTO(dto, "test origin"));
		assertEquals("Invalid priority: 0, should be positive", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePriorityRequestDTONegativePriority() {
		final UUID uuid = UUID.randomUUID();
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put(uuid.toString(), -5);

		when(normalizer.normalizePriorityRequestDTO(dto)).thenReturn(Map.of(uuid, -5));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePriorityRequestDTO(dto, "test origin"));
		assertEquals("Invalid priority: -5, should be positive", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePriorityRequestDTOOk() {
		final UUID uuid1 = UUID.randomUUID();
		final UUID uuid2 = UUID.randomUUID();
		final PriorityRequestDTO dto = new PriorityRequestDTO();
		dto.put(uuid1.toString(), 1);
		dto.put(uuid2.toString(), 2);

		when(normalizer.normalizePriorityRequestDTO(dto)).thenReturn(Map.of(uuid1, 1, uuid2, 2));

		final Map<UUID, Integer> result = validator.validateAndNormalizePriorityRequestDTO(dto, "test origin");

		assertEquals(2, result.size());
		assertEquals(1, result.get(uuid1));
		assertEquals(2, result.get(uuid2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveEmptyList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemove(List.of(), "test origin"));
		assertEquals("UUIDs are missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveNullList() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemove(null, "test origin"));
		assertEquals("UUIDs are missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveInvalidUuid() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRemove(List.of("6-7"), "test origin"));
		assertTrue(ex.getMessage().contains("Invalid UUID"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRemoveOk() {
		final UUID uuid1 = UUID.randomUUID();
		final UUID uuid2 = UUID.randomUUID();
		final List<String> uuids = List.of(uuid1.toString(), uuid2.toString());

		when(normalizer.normalizeRemove(uuids)).thenReturn(List.of(uuid1, uuid2));

		final List<UUID> result = validator.validateAndNormalizeRemove(uuids, "test origin");

		assertEquals(2, result.size());
		assertTrue(result.contains(uuid1));
		assertTrue(result.contains(uuid2));
		verify(normalizer).normalizeRemove(uuids);
	}
}
