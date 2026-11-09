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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.validation.utils.OrchestrationValidation;

@ExtendWith(MockitoExtension.class)
public class OrchestrationServiceValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationServiceValidation validator;

	@Mock
	private OrchestrationValidation orchValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRequesterOk() {
		when(orchValidator.validateAndNormalizeSystemName(eq("TestSystem"), eq("test origin"))).thenReturn("TestSystem");

		final String result = validator.validateAndNormalizeRequester("TestSystem", "test origin");

		assertEquals("TestSystem", result);
		verify(orchValidator).validateAndNormalizeSystemName("TestSystem", "test origin");
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
	public void testValidateAndNormalizePullOk() {
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, List.of("Provider1"));
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(serviceReq, null, null, null);
		final SimpleOrchestrationRequest expected = new SimpleOrchestrationRequest("testService", List.of("Provider1"), Map.of(), null);

		when(orchValidator.validateAndNormalizeOrchestrationRequest(eq(dto), eq("test origin"))).thenReturn(expected);

		final SimpleOrchestrationRequest result = validator.validateAndNormalizePull(dto, "test origin");

		assertEquals(expected, result);
		verify(orchValidator).validateAndNormalizeOrchestrationRequest(dto, "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePullNullDto() {

		final SimpleOrchestrationRequest expected = new SimpleOrchestrationRequest(null, null, null, null);

		when(orchValidator.validateAndNormalizeOrchestrationRequest(eq(null), eq("test origin"))).thenReturn(expected);

		final SimpleOrchestrationRequest result = validator.validateAndNormalizePull(null, "test origin");
		assertNotNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePullThrowsInvalidParameterException() {
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO("inv@lid-service", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(serviceReq, null, null, null);

		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizeOrchestrationRequest(eq(dto), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePull(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizeOrchestrationRequest(dto, "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeOk() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080", "method", "POST", "path", "/notify"));
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, List.of("Provider1"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(serviceReq, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TestConsumer", orchRequest, notifyInterface, 60L);

		final SimpleOrchestrationRequest simpleOrchReq = new SimpleOrchestrationRequest("teStservice", List.of("Provider1"), null, null);
		final SimpleOrchestrationSubscriptionRequest expected = new SimpleOrchestrationSubscriptionRequest("TestConsumer", simpleOrchReq, notifyInterface, 60L);

		when(orchValidator.validateAndNormalizePushSubscribe(eq(dto), eq("TestConsumer"), eq("test origin"))).thenReturn(expected);

		final SimpleOrchestrationSubscriptionRequest result = validator.validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin");

		assertNotNull(result);
		assertEquals(expected, result);
		verify(orchValidator).validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeNullDto() {
		doThrow(new InvalidParameterException("Request payload is missing", "test origin")).when(orchValidator).validateAndNormalizePushSubscribe(eq(null), any(String.class), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(null, "TestConsumer", "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizePushSubscribe(null, "TestConsumer", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeThrowsInvalidParameterException() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TestConsumer", orchRequest, notifyInterface, 60L);

		doThrow(new InvalidParameterException("Notify properties has no port property", "test origin")).when(orchValidator).validateAndNormalizePushSubscribe(eq(dto), eq("TestConsumer"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin"));
		assertEquals("Notify properties has no port property", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeTargetSystemDifferentFromRequester() {
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080", "method", "POST", "path", "/notify"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("DifferentConsumer", orchRequest, notifyInterface, 60L);

		doThrow(new InvalidParameterException("Target system cannot be different than the requester system", "test origin")).when(orchValidator).validateAndNormalizePushSubscribe(eq(dto), eq("TestConsumer"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin"));
		assertEquals("Target system cannot be different than the requester system", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeOk() {

		final UUID expected = UUID.fromString("11111111-1111-1111-1111-111111111111");

		when(orchValidator.validateAndNormalizeUUID(eq("11111111-1111-1111-1111-111111111111"), eq("test origin"))).thenReturn(expected);

		final UUID result = validator.validateAndNormalizePushUnsubscribe("11111111-1111-1111-1111-111111111111", "test origin");

		assertEquals(expected, result);
		verify(orchValidator).validateAndNormalizeUUID("11111111-1111-1111-1111-111111111111", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeInvalidUuid() {
		doThrow(new InvalidParameterException("Invalid parameter", "test origin")).when(orchValidator).validateAndNormalizeUUID(eq("6-7"), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushUnsubscribe("6-7", "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizeUUID("6-7", "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeNullUuid() {
		doThrow(new InvalidParameterException("UUID is missing", "test origin")).when(orchValidator).validateAndNormalizeUUID(eq(null), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushUnsubscribe(null, "test origin"));
		assertEquals("UUID is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizeUUID(null, "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushUnsubscribeEmptyUuid() {
		doThrow(new InvalidParameterException("UUID is missing", "test origin")).when(orchValidator).validateAndNormalizeUUID(eq(""), eq("test origin"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushUnsubscribe("", "test origin"));
		assertEquals("UUID is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		verify(orchValidator).validateAndNormalizeUUID("", "test origin");
	}
}
