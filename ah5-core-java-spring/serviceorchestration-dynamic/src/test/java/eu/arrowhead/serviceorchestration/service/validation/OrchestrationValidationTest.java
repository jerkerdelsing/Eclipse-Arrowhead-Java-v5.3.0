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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationFormValidation;
import eu.arrowhead.serviceorchestration.service.model.validation.OrchestrationSubscriptionValidation;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationServiceNormalization;

@ExtendWith(MockitoExtension.class)
public class OrchestrationValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationValidation validator;

	@Mock
	private OrchestrationServiceNormalization normalization;

	@Mock
	private OrchestrationFormValidation orchFormValidator;

	@Mock
	private OrchestrationSubscriptionValidation orchSubsValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	//=================================================================================================
	// methods

	// validateAndNormalizePullService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePullServiceCallOrchestrationFormValidation() {
		final OrchestrationForm form = new OrchestrationForm(null, null);
		final String origin = "test.origin";

		doNothing().when(orchFormValidator).validateAndNormalizeOrchestrationForm(eq(form), anyBoolean(), eq(origin));

		assertDoesNotThrow(() -> validator.validateAndNormalizePullService(form, origin));

		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(eq(form), eq(false), eq(origin));
	}

	// validateAndNormalizePushSubscribeService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeServiceTargetAndRequesterSystemsAreDifferent() {
		final OrchestrationSubscriptionRequestDTO requestDTO = new OrchestrationSubscriptionRequestDTO("SystemTarget", null, null, null);
		final OrchestrationSubscription subscriptionModel = new OrchestrationSubscription("SystemRequester", requestDTO);
		final String origin = "test.origin";

		doNothing().when(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(eq(subscriptionModel), eq(origin));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePushSubscribeService(subscriptionModel, origin));

		verify(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(eq(subscriptionModel), eq(origin));

		assertEquals("Target system cannot be different than the requester system", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeServiceValid() {
		final OrchestrationSubscriptionRequestDTO requestDTO = new OrchestrationSubscriptionRequestDTO("SystemTarget", null, null, null);
		final OrchestrationSubscription subscriptionModel = new OrchestrationSubscription("SystemTarget", requestDTO);
		final String origin = "test.origin";

		doNothing().when(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(eq(subscriptionModel), eq(origin));

		assertDoesNotThrow(() -> validator.validateAndNormalizePushSubscribeService(subscriptionModel, origin));

		verify(orchSubsValidator).validateAndNormalizeOrchestrationSubscription(eq(subscriptionModel), eq(origin));
	}

	// validateAndNormalizePushUnsubscribeService

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeServiceEmptyRequester() {
		final String requester = "";
		final String subsId = "7de68755-4922-4266-8eb5-8f4c1fad1cda";
		final String origin = "test.origin";

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePushUnsubscribeService(requester, subsId, origin));

		verify(normalization, never()).normalizePushUnsubscribe(anyString(), anyString());
		verify(systemNameValidator, never()).validateSystemName(anyString());

		assertEquals("Requester system is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeServiceEmptyId() {
		final String requester = "TestSystem";
		final String subsId = "";
		final String origin = "test.origin";

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePushUnsubscribeService(requester, subsId, origin));

		verify(normalization, never()).normalizePushUnsubscribe(anyString(), anyString());
		verify(systemNameValidator, never()).validateSystemName(anyString());

		assertEquals("Subscription id is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeServiceInvalidId() {
		final String requester = "TestSystem";
		final String subsId = "not-uuid";
		final String origin = "test.origin";

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePushUnsubscribeService(requester, subsId, origin));

		verify(normalization, never()).normalizePushUnsubscribe(anyString(), anyString());
		verify(systemNameValidator, never()).validateSystemName(anyString());

		assertEquals("Invalid subscription id", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeServiceCallAndCatchSystemNameValidator() {
		final String requester = "Test-system";
		final String subsId = "7de68755-4922-4266-8eb5-8f4c1fad1cda";
		final String origin = "test.origin";

		when(normalization.normalizePushUnsubscribe(eq(requester), eq(subsId))).thenReturn(Pair.of(requester, UUID.fromString(subsId)));
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(requester));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizePushUnsubscribeService(requester, subsId, origin));

		verify(normalization).normalizePushUnsubscribe(eq(requester), eq(subsId));
		verify(systemNameValidator).validateSystemName(eq(requester));

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeServiceEverythingIsValid() {
		final String requester = "TestSystem";
		final String subsId = "7de68755-4922-4266-8eb5-8f4c1fad1cda";
		final String origin = "test.origin";

		when(normalization.normalizePushUnsubscribe(eq(requester), eq(subsId))).thenReturn(Pair.of(requester, UUID.fromString(subsId)));
		doNothing().when(systemNameValidator).validateSystemName(eq(requester));

		assertDoesNotThrow(() -> validator.validateAndNormalizePushUnsubscribeService(requester, subsId, origin));

		verify(normalization).normalizePushUnsubscribe(eq(requester), eq(subsId));
		verify(systemNameValidator).validateSystemName(eq(requester));
	}
}
