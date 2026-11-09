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
package eu.arrowhead.serviceorchestration.service.model.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationPushTriggerDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationPushTrigger;

@ExtendWith(MockitoExtension.class)
public class OrchestrationPushTriggerValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationPushTriggerValidation validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Captor
	private ArgumentCaptor<OrchestrationPushTrigger> triggerCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationPushTrigger1() {
		final String origin = "test.origin";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger("RequesterSystem", new OrchestrationPushTriggerDTO(List.of("TargetSys"), List.of("2a1417e6-9faf-4ef3-93b1-6d726bdc714b")));

		assertDoesNotThrow(() -> validator.validateOrchestrationPushTrigger(trigger, origin));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationPushTrigger2() {
		final String origin = "test.origin";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger("RequesterSystem", new OrchestrationPushTriggerDTO(List.of(), List.of()));

		assertDoesNotThrow(() -> validator.validateOrchestrationPushTrigger(trigger, origin));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationPushTriggerNullRequest() {
		final String origin = "test.origin";

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationPushTrigger(null, origin));

		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationPushTriggerMissingRerquester() {
		final String origin = "test.origin";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger("", new OrchestrationPushTriggerDTO(List.of(), List.of()));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationPushTrigger(trigger, origin));

		assertEquals("Requester system is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationPushTriggerEmptyTargetSystem() {
		final String origin = "test.origin";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger("RequesterSystem", new OrchestrationPushTriggerDTO(List.of("TargetSys1", ""), List.of()));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationPushTrigger(trigger, origin));

		assertEquals("Target system list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationPushTriggerEmptyID() {
		final String origin = "test.origin";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger("RequesterSystem", new OrchestrationPushTriggerDTO(List.of(), List.of("2a1417e6-9faf-4ef3-93b1-6d726bdc714b", "")));

		final Throwable ex = assertThrows(Throwable.class, () -> validator.validateOrchestrationPushTrigger(trigger, origin));

		assertEquals("Subscription id list contains empty element", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAnNormalizeOrchestrationPushTrigger1() {
		final String origin = "test.origin";
		final String requester = " RequesterSystem ";
		final String target = " TargetSystem ";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger(requester, new OrchestrationPushTriggerDTO(List.of(target), List.of("2a1417e6-9faf-4ef3-93b1-6d726bdc714b")));

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());
		when(systemNameNormalizer.normalize(eq(target))).thenReturn(target.trim());

		assertDoesNotThrow(() -> validator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameNormalizer).normalize(eq(target));
		verify(systemNameValidator).validateSystemName(eq(requester.trim()));
		verify(systemNameValidator).validateSystemName(eq(target.trim()));

		assertEquals(requester.trim(), trigger.getRequesterSystem());
		assertEquals(target.trim(), trigger.getTargetSystems().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAnNormalizeOrchestrationPushTrigger2() {
		final String origin = "test.origin";
		final String requester = " RequesterSystem ";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger(requester, new OrchestrationPushTriggerDTO(List.of(), List.of()));

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());

		assertDoesNotThrow(() -> validator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameValidator).validateSystemName(eq(requester.trim()));

		assertEquals(requester.trim(), trigger.getRequesterSystem());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAnNormalizeOrchestrationPushTriggerInvalidRequester() {
		final String origin = "test.origin";
		final String requester = " requester.system ";
		final String target = " TargetSystem ";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger(requester, new OrchestrationPushTriggerDTO(List.of(target), List.of("2a1417e6-9faf-4ef3-93b1-6d726bdc714b")));

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(requester.trim()));

		Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameNormalizer, never()).normalize(eq(target));
		verify(systemNameValidator).validateSystemName(eq(requester.trim()));
		verify(systemNameValidator, never()).validateSystemName(eq(target.trim()));

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAnNormalizeOrchestrationPushTriggerInvalidTarget() {
		final String origin = "test.origin";
		final String requester = " RequesterSystem ";
		final String target = " target.system ";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger(requester, new OrchestrationPushTriggerDTO(List.of(target), List.of("2a1417e6-9faf-4ef3-93b1-6d726bdc714b")));

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());
		when(systemNameNormalizer.normalize(eq(target))).thenReturn(target.trim());
		doNothing().when(systemNameValidator).validateSystemName(eq(requester.trim()));
		doThrow(new InvalidParameterException("test message")).when(systemNameValidator).validateSystemName(eq(target.trim()));

		Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameNormalizer).normalize(eq(target));
		verify(systemNameValidator).validateSystemName(eq(requester.trim()));
		verify(systemNameValidator).validateSystemName(eq(target.trim()));

		assertEquals("test message", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAnNormalizeOrchestrationPushTriggerInvalidIDt() {
		final String origin = "test.origin";
		final String requester = " RequesterSystem ";
		final String target = " Target System ";
		final OrchestrationPushTrigger trigger = new OrchestrationPushTrigger(requester, new OrchestrationPushTriggerDTO(List.of(target), List.of("not-uuid")));

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());
		when(systemNameNormalizer.normalize(eq(target))).thenReturn(target.trim());

		Throwable ex = assertThrows(Throwable.class, () -> validator.validateAndNormalizeOrchestrationPushTrigger(trigger, origin));

		verify(systemNameNormalizer).normalize(eq(requester));
		verify(systemNameNormalizer).normalize(eq(target));
		verify(systemNameValidator).validateSystemName(eq(requester.trim()));
		verify(systemNameValidator).validateSystemName(eq(target.trim()));

		assertEquals("Invalid subscription id: not-uuid", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}
}
