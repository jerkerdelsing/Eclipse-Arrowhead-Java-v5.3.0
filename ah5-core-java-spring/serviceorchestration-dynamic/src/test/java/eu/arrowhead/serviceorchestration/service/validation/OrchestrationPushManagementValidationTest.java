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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationSubscriptionQueryRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationPushTriggerValidation;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationSubscriptionValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationPushManagementValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationPushManagementValidation validator;

	@Mock
	private OrchestrationSubscriptionValidation orchSubsValidator;

	@Mock
	private OrchestrationPushTriggerValidation orchPushTriggerValidator;

	@Mock
	private PageValidator pageValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	//=================================================================================================
	// members

	// validateAndNormalizePushSubscribeService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeServiceNullList() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePushSubscribeService(null, "test.origin"));

		assertEquals("Subscription request list is empty", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeServiceCallOrchestrationSubscriptionValidation() {
		final OrchestrationSubscription item1 = new OrchestrationSubscription(null, null);
		final OrchestrationSubscription item2 = new OrchestrationSubscription(null, null);
		final String origin = "test.origin";

		doNothing().when(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(any(), anyString());

		assertDoesNotThrow(() -> validator.validateAndNormalizePushSubscribeService(List.of(item1, item2), origin));

		verify(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(eq(item1), eq(origin));
		verify(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(eq(item2), eq(origin));
	}

	// validateAndNormalizePushTriggerService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeServiceCallOrchestrationPushTriggerValidation() {
		final OrchestrationPushTrigger dto = new OrchestrationPushTrigger(null, null);
		final String origin = "test.origin";

		doNothing().when(orchPushTriggerValidator).validateAndNormalizeOrchestrationPushTrigger(any(), anyString());

		assertDoesNotThrow(() -> validator.validateAndNormalizePushTriggerService(dto, origin));

		verify(orchPushTriggerValidator).validateAndNormalizeOrchestrationPushTrigger(eq(dto), eq(origin));
	}

	// validateAndNormalizeRequesterSystem

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterSystemCallSystemNameNormalizerAndSystemNameValidator() {
		final String requester = "TestManager";
		final String origin = "test.origin";

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester);
		doNothing().when(systemNameValidator).validateSystemName(eq(requester));

		assertDoesNotThrow(() -> validator.validateAndNormalizeRequesterSystem(requester, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameValidator).validateSystemName(eq(requester));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterSystemCallAndCatchSystemNameValidator() {
		final String requester = "TestManager";
		final String origin = "test.origin";

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(requester));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeRequesterSystem(requester, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameValidator).validateSystemName(eq(requester));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	// validateAndNormalizePublishUnsubscribeService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePublishUnsubscribeServiceEmptyList() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePublishUnsubscribeService(List.of(), "test.origin"));

		assertEquals("ID list is empty", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePublishUnsubscribeServiceContainsEmptyId() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePublishUnsubscribeService(List.of("bc600e60-d3a5-4569-8407-74f04ee701f2", ""), "test.origin"));

		assertEquals("ID list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePublishUnsubscribeServiceInvalidId() {
		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePublishUnsubscribeService(List.of("not-uuid"), "test.origin"));

		assertEquals("Invalid subscription ID: not-uuid", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePublishUnsubscribeServiceValidIdWithTrim() {
		final List<String> result = assertDoesNotThrow(() -> validator.validateAndNormalizePublishUnsubscribeService(List.of(" bc600e60-d3a5-4569-8407-74f04ee701f2 "), "test.origin"));

		assertEquals("bc600e60-d3a5-4569-8407-74f04ee701f2", result.get(0));
	}

	// validateAndNormalizeQueryPushSubscriptionsService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceNullPayload() {
		final OrchestrationSubscriptionQueryRequestDTO result = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryPushSubscriptionsService(null, "test.origin"));

		verify(pageValidator, never()).validatePageParameter(any(), anyList(), anyString());
		verify(systemNameNormalizer, never()).normalize(anyString());
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(systemNameValidator, never()).validateSystemName(anyString());
		verify(serviceDefNameValidator, never()).validateServiceDefinitionName(anyString());

		assertNotNull(result);
		assertTrue(result.pagination() == null);
		assertTrue(result.ownerSystems().size() == 0);
		assertTrue(result.targetSystems().size() == 0);
		assertTrue(result.serviceDefinitions().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceContainsEmptyOwner() {
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(new OrchestrationSubscriptionQueryRequestDTO(null, List.of("TestManager", ""), null, null), "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());

		assertEquals("Owner system list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceContainsEmptyTarget() {
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(new OrchestrationSubscriptionQueryRequestDTO(null, null, List.of("TestConsumer", ""), null), "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());

		assertEquals("Target system list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceContainsEmptyServiceDefinition() {
		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(new OrchestrationSubscriptionQueryRequestDTO(null, null, null, List.of("testService", "")), "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());

		assertEquals("Service definition list contains empty element", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceCallAndCatchSystemNameValidatorOnOwner() {
		final String owner = "Test-Manager";

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(systemNameNormalizer.normalize(eq(owner))).thenReturn(owner);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(owner));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(new OrchestrationSubscriptionQueryRequestDTO(null, List.of(owner), null, null), "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(systemNameNormalizer).normalize(eq(owner));
		verify(systemNameValidator).validateSystemName(eq(owner));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceCallAndCatchSystemNameValidatorOnTarget() {
		final String target = "Test-Consumer";

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(systemNameNormalizer.normalize(eq(target))).thenReturn(target);
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(target));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(new OrchestrationSubscriptionQueryRequestDTO(null, null, List.of(target), null), "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(systemNameNormalizer).normalize(eq(target));
		verify(systemNameValidator).validateSystemName(eq(target));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceCallAndCatchSystemNameValidatorOnServiceDef() {
		final String serviceDef = "test-service";

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(serviceDefNameNormalizer.normalize(eq(serviceDef))).thenReturn(serviceDef);
		doThrow(new InvalidParameterException("test message")).when(serviceDefNameValidator).validateServiceDefinitionName(eq(serviceDef));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeQueryPushSubscriptionsService(new OrchestrationSubscriptionQueryRequestDTO(null, null, null, List.of(serviceDef)), "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(serviceDefNameNormalizer).normalize(eq(serviceDef));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq(serviceDef));

		assertEquals("test message", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	///-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeQueryPushSubscriptionsServiceEverythingIsValid() {
		final String owner = "TestManager";
		final String target = "TestConsumer";
		final String serviceDef = "testService";
		final OrchestrationSubscriptionQueryRequestDTO dto = new OrchestrationSubscriptionQueryRequestDTO(null, List.of(owner), List.of(target), List.of(serviceDef));

		doNothing().when(pageValidator).validatePageParameter(any(), anyList(), anyString());
		when(systemNameNormalizer.normalize(eq(owner))).thenReturn(owner);
		when(systemNameNormalizer.normalize(eq(target))).thenReturn(target);
		when(serviceDefNameNormalizer.normalize(eq(serviceDef))).thenReturn(serviceDef);

		final OrchestrationSubscriptionQueryRequestDTO result = assertDoesNotThrow(() -> validator.validateAndNormalizeQueryPushSubscriptionsService(dto, "test.origin"));

		verify(pageValidator).validatePageParameter(any(), anyList(), anyString());
		verify(systemNameNormalizer).normalize(eq(owner));
		verify(systemNameValidator).validateSystemName(eq(owner));
		verify(systemNameNormalizer).normalize(eq(target));
		verify(systemNameValidator).validateSystemName(eq(target));
		verify(serviceDefNameNormalizer).normalize(eq(serviceDef));
		verify(serviceDefNameValidator).validateServiceDefinitionName(eq(serviceDef));

		assertNotNull(result);
		assertEquals(owner, result.ownerSystems().get(0));
		assertEquals(target, result.targetSystems().get(0));
		assertEquals(serviceDef, result.serviceDefinitions().get(0));
	}
}
