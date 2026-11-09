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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationPushManagementServiceValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationPushManagementServiceValidation validator;

	@Mock
	private OrchestrationValidation orchValidator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator sysNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private SystemNameNormalizer sysNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	//=================================================================================================
	// methods

	// Tests for validateAndNormalizeRequester

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOk() {
		when(orchValidator.validateAndNormalizeSystemName(eq("TemperatureManager"), eq("test origin"))).thenReturn("TemperatureManager");

		final String result = validator.validateAndNormalizeRequester("TemperatureManager", "test origin");

		assertEquals("TemperatureManager", result);
		verify(orchValidator).validateAndNormalizeSystemName("TemperatureManager", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterThrowsInvalidParameterException() {
		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizeSystemName(any(String.class), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRequester("inv@lid", "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizeSystemName("inv@lid", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeBulkOk() {

		final OrchestrationSubscriptionListRequestDTO dto = new OrchestrationSubscriptionListRequestDTO(List.of());

		final List<SimpleOrchestrationSubscriptionRequest> expected = List.of();
		when(orchValidator.validateAndNormalizePushSubscribeBulk(dto, "test origin")).thenReturn(expected);

		final List<SimpleOrchestrationSubscriptionRequest> result = validator.validateAndNormalizePushSubscribeBulk(dto, "test origin");
		assertEquals(expected, result);
		verify(orchValidator).validateAndNormalizePushSubscribeBulk(dto, "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeBulkThrowsInvalidParameterException() {
		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizePushSubscribeBulk(eq(null), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizePushSubscribeBulk(null, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizePushSubscribeBulk(null, "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerNullDto() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(null, "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerTargetSystemsContainsNull() {

		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(Arrays.asList("system1", null), null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertEquals("Target system list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerTargetSystemsContainsEmpty() {

		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(Arrays.asList("system1", ""), null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertEquals("Target system list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerDuplicateTargetSystems() {
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of("System1", "System1"), null);

		when(orchValidator.validateAndNormalizeSystemName("System1", "test origin")).thenReturn("System1");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertTrue(ex.getMessage().contains("Duplicated target system name"));
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerInvalidTargetSystemName() {
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of("inv@lid"), null);

		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizeSystemName(eq("inv@lid"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerSubscriptionIdsContainsNull() {
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(null, Arrays.asList("11111111-1111-1111-1111-111111111111", null));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertEquals("Subscription id list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerSubscriptionIdsContainsEmpty() {
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(null, Arrays.asList("11111111-1111-1111-1111-111111111111", ""));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertEquals("Subscription id list contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerDuplicateSubscriptionIds() {

		final UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(null, List.of("11111111-1111-1111-1111-111111111111", "11111111-1111-1111-1111-111111111111"));

		when(orchValidator.validateAndNormalizeUUID("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(uuid);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertTrue(ex.getMessage().contains("Duplicated subscription id"));
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerInvalidSubscriptionId() {

		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(null, List.of("6-7"));

		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizeUUID(eq("6-7"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushTrigger(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerWithTargetSystemsOk() {
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of("System1", "System2"), null);

		when(orchValidator.validateAndNormalizeSystemName("System1", "test origin")).thenReturn("System1");
		when(orchValidator.validateAndNormalizeSystemName("System2", "test origin")).thenReturn("System2");

		final NormalizedOrchestrationPushTrigger result = validator.validateAndNormalizePushTrigger(dto, "test origin");

		assertEquals(2, result.getTargetSystems().size());
		assertTrue(result.getTargetSystems().contains("System1"));
		assertTrue(result.getTargetSystems().contains("System2"));
		assertNull(result.getSubscriptionIds());
		verify(orchValidator, times(2)).validateAndNormalizeSystemName(any(String.class), eq("test origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerWithSubscriptionIdsOk() {
		final UUID uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
		final UUID uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(null,
				List.of("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222"));

		when(orchValidator.validateAndNormalizeUUID("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(uuid1);
		when(orchValidator.validateAndNormalizeUUID("22222222-2222-2222-2222-222222222222", "test origin")).thenReturn(uuid2);

		final NormalizedOrchestrationPushTrigger result = validator.validateAndNormalizePushTrigger(dto, "test origin");

		assertNull(result.getTargetSystems());
		assertEquals(2, result.getSubscriptionIds().size());
		assertTrue(result.getSubscriptionIds().contains(uuid1));
		assertTrue(result.getSubscriptionIds().contains(uuid2));
		verify(orchValidator, times(2)).validateAndNormalizeUUID(any(String.class), eq("test origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushTriggerWithBothFieldsOk() {
		final UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
		final OrchestrationPushTriggerDTO dto = new OrchestrationPushTriggerDTO(List.of("System1"), List.of("11111111-1111-1111-1111-111111111111"));

		when(orchValidator.validateAndNormalizeSystemName("System1", "test origin")).thenReturn("System1");
		when(orchValidator.validateAndNormalizeUUID("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(uuid);

		final NormalizedOrchestrationPushTrigger result = validator.validateAndNormalizePushTrigger(dto, "test origin");

		assertEquals(1, result.getTargetSystems().size());
		assertEquals("System1", result.getTargetSystems().get(0));
		assertEquals(1, result.getSubscriptionIds().size());
		assertEquals(uuid, result.getSubscriptionIds().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceNullDto() {

		final OrchestrationSubscriptionQueryRequestDTO result = validator.validateAndNormalizeQueryPushSubscriptionsService(null, "test origin");

		assertNotNull(result);
		assertNull(result.pagination());
		assertTrue(result.ownerSystems().isEmpty());
		assertTrue(result.targetSystems().isEmpty());
		assertTrue(result.serviceDefinitions().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceOwnerSystemListContainsEmptyElement() {

		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, List.of("System1", ""), null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Owner system list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceOwnerSystemsContainsNull() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, Arrays.asList("System1", null), null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Owner system list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceTargetSystemsContainsEmpty() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, null, List.of("System1", ""), null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Target system list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceTargetSystemsContainsNull() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, null, Arrays.asList("System1", null), null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Target system list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceServiceDefinitionsContainsEmpty() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, null, null, List.of("serviceDef1", ""));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Service definition list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceServiceDefinitionsContainsNull() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, null, null, Arrays.asList("serviceDef1", null));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Service definition list contains empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceInvalidOwnerSystemName() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, List.of("inv@lid"), null, null);

		when(sysNameNormalizer.normalize("inv@lid")).thenReturn("inv@lid");
		doThrow(new InvalidParameterException("Invalid parameter")).when(sysNameValidator).validateSystemName("inv@lid");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceInvalidTargetSystemName() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, null, List.of("inv@lid"), null);

		when(sysNameNormalizer.normalize("inv@lid")).thenReturn("inv@lid");
		doThrow(new InvalidParameterException("Invalid parameter")).when(sysNameValidator).validateSystemName("inv@lid");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceInvalidServiceDefinitionName() {
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, null, null, List.of("inv@lid-service"));

		when(serviceDefNameNormalizer.normalize("inv@lid-service")).thenReturn("inv@lid-service");
		doThrow(new InvalidParameterException("Invalid parameter")).when(serviceDefNameValidator).validateServiceDefinitionName("inv@lid-service");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceWithPaginationOk() {
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(pagination, null, null, null);

		final OrchestrationSubscriptionQueryRequestDTO result = validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin");

		assertEquals(pagination, result.pagination());
		verify(pageValidator).validatePageParameter(eq(pagination), eq(Subscription.SORTABLE_FIELDS_BY), eq("test origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceWithAllFieldsOk() {
		final PageDTO pagination = new PageDTO(0, 10, "id", "ASC");
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(
				pagination, List.of("OwnerSystem"), List.of("TargetSystem"), List.of("serviceDef"));

		when(sysNameNormalizer.normalize("OwnerSystem")).thenReturn("OwnerSystem");
		when(sysNameNormalizer.normalize("TargetSystem")).thenReturn("TargetSystem");
		when(serviceDefNameNormalizer.normalize("serviceDef")).thenReturn("serviceDef");

		final OrchestrationSubscriptionQueryRequestDTO result = validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test origin");

		assertNotNull(result);
		assertEquals(pagination, result.pagination());
		assertEquals(1, result.ownerSystems().size());
		assertEquals("OwnerSystem", result.ownerSystems().get(0));
		assertEquals(1, result.targetSystems().size());
		assertEquals("TargetSystem", result.targetSystems().get(0));
		assertEquals(1, result.serviceDefinitions().size());
		assertEquals("serviceDef", result.serviceDefinitions().get(0));

		verify(pageValidator).validatePageParameter(eq(pagination), eq(Subscription.SORTABLE_FIELDS_BY), eq("test origin"));
		verify(sysNameValidator, times(2)).validateSystemName(any(String.class));
		verify(serviceDefNameValidator).validateServiceDefinitionName("serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeNullList() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushUnsubscribe(null, "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeEmptyList() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushUnsubscribe(List.of(), "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeInvalidUuid() {
		final List<String> ids = List.of("6-7");

		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizeUUID(eq("6-7"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushUnsubscribe(ids, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeOk() {
		final UUID uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
		final UUID uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
		final List<String> ids = List.of("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222");

		when(orchValidator.validateAndNormalizeUUID("11111111-1111-1111-1111-111111111111", "test origin")).thenReturn(uuid1);
		when(orchValidator.validateAndNormalizeUUID("22222222-2222-2222-2222-222222222222", "test origin")).thenReturn(uuid2);

		final List<UUID> result = validator.validateAndNormalizePushUnsubscribe(ids, "test origin");

		assertEquals(2, result.size());
		assertEquals(uuid1, result.get(0));
		assertEquals(uuid2, result.get(1));
		verify(orchValidator, times(2)).validateAndNormalizeUUID(any(String.class), eq("test origin"));
	}
}
