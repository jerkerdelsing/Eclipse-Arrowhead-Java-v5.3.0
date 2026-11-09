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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationSubscription;

@ExtendWith(MockitoExtension.class)
public class OrchestrationSubscriptionValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationSubscriptionValidation validation;

	@Mock
	private OrchestrationFormValidation orchFormValidator;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Captor
	private ArgumentCaptor<OrchestrationForm> orchFormCaptor;
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscription() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("foo", "bar")), null));

		assertDoesNotThrow(() -> validation.validateOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));

		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), orchFormCaptor.getValue().getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), orchFormCaptor.getValue().getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), orchFormCaptor.getValue().getTargetSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscriptionNullRequest() {
		final String origin = "test.origin";
		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateOrchestrationSubscription(null, origin));

		verify(orchFormValidator, never()).validateOrchestrationForm(any(), anyString());

		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscriptionZeroDuration() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("foo", "bar")), 0L));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator, never()).validateOrchestrationForm(any(), anyString());

		assertEquals("Subscription duration must be greater than 0", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscriptionMissingNotifyProtocolMissing() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("", Map.of("foo", "bar")), null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator, never()).validateOrchestrationForm(any(), anyString());

		assertEquals("Notify protocol is missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscriptionMissingNotifyPropsMissing() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of()), null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator, never()).validateOrchestrationForm(any(), anyString());

		assertEquals("Notify properties are missing", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscriptionMissingNotifyPropsWithEmptyKey() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("", "bar")), 10L));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator, never()).validateOrchestrationForm(any(), anyString());

		assertEquals("Notify properties contains empty key", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOrchestrationSubscriptionMissingNotifyPropsWithEmptyValue() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("foo", "")), null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator, never()).validateOrchestrationForm(any(), anyString());

		assertEquals("Notify properties contains empty value", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithMQTTNotify() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("topic", "test/topic")), null));

		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		assertDoesNotThrow(() -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithMQTTSNotify() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtts", Map.of("topic", "test/topic")), null));

		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		assertDoesNotThrow(() -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithMQTTNotifyButNotEnabled() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("topic", "test/topic")), null));

		when(sysInfo.isMqttApiEnabled()).thenReturn(false);

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("MQTT notify protocol required, but MQTT is not enabled", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithMQTTNotifyButMissingTopic() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO, new OrchestrationNotifyInterfaceDTO("mqtt", Map.of("not-topic", "test/topic")), null));

		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify properties has no topic member", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotify() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "12345", "method", "post", "path", "/foo/bar")),
						null));

		assertDoesNotThrow(() -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPSNotify() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("https", Map.of("address", "localhost", "port", "12345", "method", "put", "path", "/foo/bar")),
						null));

		assertDoesNotThrow(() -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButMissingAddress() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("port", "12345", "method", "post", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify properties has no address property", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButMissingPort() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "method", "post", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify properties has no port property", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButInvalidPortLess() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "0", "method", "post", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify port is out of the valid range", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButInvalidPortMore() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "65536", "method", "post", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify port is out of the valid range", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButPortNotNumber() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "foo", "method", "post", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify port is not a number", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButMissingMethod() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "12345", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify properties has no method property", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButInvalidMethod() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "12345", "method", "GET", "path", "/foo/bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Unsupported notify HTTP method: GET", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButMissingPath() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("http", Map.of("address", "localhost", "port", "12345", "method", "patch")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Notify properties has no path property", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationSubscriptionWithHTTPNotifyButUnsupportedProtocol() {
		final String origin = "test.origin";
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, origin, null, null, null, null, null, null), null, null, null);
		final OrchestrationSubscription subscription = new OrchestrationSubscription("RequesterSystem",
				new OrchestrationSubscriptionRequestDTO("TargetSystem", orchestrationRequestDTO,
						new OrchestrationNotifyInterfaceDTO("coap", Map.of("foo", "bar")),
						null));

		final Throwable ex = assertThrows(Throwable.class, () -> validation.validateAndNormalizeOrchestrationSubscription(subscription, origin));

		verify(orchFormValidator).validateOrchestrationForm(orchFormCaptor.capture(), eq(origin));
		verify(orchFormValidator).validateAndNormalizeOrchestrationForm(orchFormCaptor.capture(), eq(true), eq(origin));
		verify(sysInfo, never()).isMqttApiEnabled();

		final OrchestrationForm validateInput = orchFormCaptor.getAllValues().get(0);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), validateInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), validateInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), validateInput.getTargetSystemName());

		final OrchestrationForm normalizeInput = orchFormCaptor.getAllValues().get(1);
		assertEquals(subscription.getOrchestrationForm().getServiceDefinition(), normalizeInput.getServiceDefinition());
		assertEquals(subscription.getOrchestrationForm().getRequesterSystemName(), normalizeInput.getRequesterSystemName());
		assertEquals(subscription.getOrchestrationForm().getTargetSystemName(), normalizeInput.getTargetSystemName());

		assertEquals("Unsupported notify protocol: COAP", ex.getMessage());
		assertEquals(origin, ((InvalidParameterException) ex).getOrigin());
	}
}
