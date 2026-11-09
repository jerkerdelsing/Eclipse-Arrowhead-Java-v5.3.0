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
package eu.arrowhead.serviceorchestration.service.validation.utils;

import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionListRequestDTO;
import eu.arrowhead.dto.OrchestrationSubscriptionRequestDTO;
import eu.arrowhead.dto.QoSRequirementDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.serviceorchestration.SimpleStoreServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationRequest;
import eu.arrowhead.serviceorchestration.service.model.SimpleOrchestrationSubscriptionRequest;
import eu.arrowhead.serviceorchestration.service.normalization.utils.OrchestrationNormalization;

@ExtendWith(MockitoExtension.class)
public class OrchestrationValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationValidation validator;

	@Mock
	private OrchestrationNormalization normalizer;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private SimpleStoreServiceOrchestrationSystemInfo sysInfo;

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameOk() {
		when(systemNameNormalizer.normalize(any(String.class))).thenReturn("Consumer");
		final String actual = validator.validateAndNormalizeSystemName("CONSUMER", "test origin");

		assertEquals("Consumer", actual);
		verify(systemNameNormalizer).normalize("CONSUMER");
		verify(systemNameValidator).validateSystemName("Consumer");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeSystemNameThrowsInvalidParameterException() {
		when(systemNameNormalizer.normalize(any(String.class))).thenReturn("Consumer");
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeSystemName("CONSUMER", "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUUIDOk() {
		when(normalizer.normalizeUUID(any(String.class))).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		final UUID actual = validator.validateAndNormalizeUUID(" 11111111-1111-1111-1111-111111111111\n ", "test origin");
		assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), actual);
		verify(normalizer).normalizeUUID(" 11111111-1111-1111-1111-111111111111\n ");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUUIDMissing() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUUID("", "test origin"));
		assertEquals("UUID is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeUUIDInvalid() {
		try (MockedStatic<Utilities> mocked = Mockito.mockStatic(Utilities.class)) {
			mocked.when(() -> Utilities.isEmpty("6-7")).thenReturn(false);
			mocked.when(() -> Utilities.isUUID("6-7")).thenReturn(false);

			final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeUUID("6-7", "test origin"));
			assertEquals("Invalid UUID id", ex.getMessage());
			assertEquals("test origin", ex.getOrigin());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUUIDOk() {
		try (MockedStatic<Utilities> mocked = Mockito.mockStatic(Utilities.class)) {
			mocked.when(() -> Utilities.isEmpty("")).thenReturn(true);
			mocked.when(() -> Utilities.isEmpty("test origin")).thenReturn(false);
			mocked.when(() -> Utilities.isUUID("11111111-1111-1111-1111-111111111111")).thenReturn(true);

			validator.validateUUID("11111111-1111-1111-1111-111111111111", "test origin");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUUIDMissing() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateUUID("", "test origin"));
		assertEquals("UUID is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateUUIDInvalid() {
		try (MockedStatic<Utilities> mocked = Mockito.mockStatic(Utilities.class)) {
			mocked.when(() -> Utilities.isEmpty("test origin")).thenReturn(false);
			mocked.when(() -> Utilities.isUUID("invalid-uuid")).thenReturn(false);

			final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateUUID("6-7", "test origin"));
			assertEquals("Invalid UUID id", ex.getMessage());
			assertEquals("test origin", ex.getOrigin());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestNullDto() {
		final SimpleOrchestrationRequest actual = validator.validateAndNormalizeOrchestrationRequest(null, "test origin");
		assertNotNull(actual);
	}


	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestCheckWarnings() {
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(
				"serviceDef",
				List.of("get"),
				List.of("1.0.0"),
				"2026-12-31T23:59:59Z",
				List.of(new MetadataRequirementDTO()),
				List.of("generic_http"),
				List.of("IPv4"),
				List.of(new MetadataRequirementDTO()),
				List.of("NONE"),
				List.of("Provider"));
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(
				requirementDTO,
				Map.of("ONLY_EXCLUSIVE", true),
				List.of(new QoSRequirementDTO("basic-device-kpi", "FILTER", Map.of("maxLatencyMs", 100))),
				30);

		final SimpleOrchestrationRequest actual = validator.validateAndNormalizeOrchestrationRequest(dto, "test origin");
		assertNotNull(actual.getWarnings());
		assertEquals(2, actual.getWarnings().size());
		assertTrue(actual.getWarnings().stream().anyMatch(w -> w.contains(
				"ignoredFields: [operations, versions, alivesAt, metadataRequirements, interfaceTemplateNames, interfaceAddressTypes, interfacePropertyRequirements, securityPolicies, qualityRequirements, exclusivityDuration]")));
		assertTrue(actual.getWarnings().stream().anyMatch(w -> w.contains(
				"ignoredFlags: [ONLY_EXCLUSIVE]")));
		}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestSupportedFlagsOk() {
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(null, Map.of("MATCHMAKING", true, "ONLY_PREFERRED", false), null, null);

		final SimpleOrchestrationRequest actual = validator.validateAndNormalizeOrchestrationRequest(dto, "test origin");

		assertEquals(true, actual.getOrchestrationFlags().get("MATCHMAKING"));
		assertEquals(false, actual.getOrchestrationFlags().get("ONLY_PREFERRED"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestPreferredProvidersContainsEmpty() {
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(
				"serviceDef",
				null, null, null, null, null, null, null, null,
				java.util.Arrays.asList("Provider1", ""));
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, null, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeOrchestrationRequest(dto, "test origin"));
		assertEquals("Preferred providers contains null or empty element", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestOrchestrationFlagsContainsNull() {
		final Map<String, Boolean> flags = new HashMap<>();
		flags.put("MATCHMAKING", null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(null, flags, null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeOrchestrationRequest(dto, "test origin"));
		assertEquals("Orchestration flag map contains null value", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestOnlyPreferredFlagButNoPreferredProviders() {

		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(null, Map.of("ONLY_PREFERRED", true), null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeOrchestrationRequest(dto, "test origin"));
		assertEquals("ONLY_PREFERRED flag is present, but no preferred provider is defined", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestInvalidOrchestrationFlag() {

		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(null, Map.of("INVALID_FLAG", true), null, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeOrchestrationRequest(dto, "test origin"));
		assertTrue(ex.getMessage().contains("Invalid orchestration flag"));
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestInvalidServiceDefinition() {
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO("inv@lid-service", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, null, null, null);

		doThrow(new InvalidParameterException("Invalid parameter")).when(serviceDefNameValidator).validateServiceDefinitionName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeOrchestrationRequest(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestInvalidPreferredProvider() {
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(null, null, null, null, null, null, null, null, null, List.of("Inv@lidProvider"));
		final OrchestrationRequestDTO dto = new OrchestrationRequestDTO(requirementDTO, null, null, null);

		doThrow(new InvalidParameterException("Invalid parameter")).when(systemNameValidator).validateSystemName(any(String.class));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeOrchestrationRequest(dto, "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeOrchestrationRequestEmptyOriginThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> validator.validateAndNormalizeOrchestrationRequest(null, ""));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeMissingPayload() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(null, "TemperatureConsumer", "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeNotifyProtocolMissing() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("", Map.of("port", "4040"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer", orchRequest, notifyInterface, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify protocol is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeNotifyPropertiesMissing() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", null);
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer", orchRequest, notifyInterface, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties are missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeNotifyPropertiesEmptyKey() {

		final Map<String, String> properties = new HashMap<>();
		properties.put("", "value");
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", properties);
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer", orchRequest, notifyInterface, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties contains empty key", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeNotifyPropertiesEmptyValue() {

		final Map<String, String> properties = new HashMap<>();
		properties.put("key", "");
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", properties);
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer", orchRequest, notifyInterface, null);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties contains empty value", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeInvalidDuration() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("port", "8080"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer", orchRequest, notifyInterface, -1L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Subscription duration must be greater than 0", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}


	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeTargetSystemDifferentFromRequester() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("port", "8080"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TemperatureProvider", orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Target system cannot be different than the requester system", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeUnsupportedNotifyProtocol() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("magic_http", Map.of("address", "magic.com"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertTrue(ex.getMessage().contains("Unsupported notify protocol: magic_http"));
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpMissingAddress() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("port", "8080"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties has no address property", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpMissingPort() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties has no port property", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpPortNotANumber() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost", "port", "not.a.number"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify port is not a number", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpPortTooBig() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost", "port", "99999"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify port is out of the valid range", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpPortTooSmall() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost", "port", "0"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify port is out of the valid range", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpMissingMethod() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost", "port", "8080"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties has no method property", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpUnsupportedMethod() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost", "port", "8080", "method", "PUTPUT"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Unsupported notify HTTP method: PUTPUT", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeHttpMissingPath() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTP", Map.of("address", "localhost", "port", "8080", "method", "POST"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties has no path property", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeMqttButNotEnabled() {

		when(sysInfo.isMqttApiEnabled()).thenReturn(false);

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("topic", "test"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("MQTT notify protocol required, but MQTT is not enabled", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeMqttsButNotEnabled() {

		when(sysInfo.isMqttApiEnabled()).thenReturn(false);

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTTS", Map.of("topic", "test"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("MQTT notify protocol required, but MQTT is not enabled", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeMqttMissingProperties() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of());
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties are missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeMqttMissingTopic() {

		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("port", "1337"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TemperatureConsumer", "test origin"));
		assertEquals("Notify properties has no topic member", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeThrowsInvalidParameterException() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080", "method", "PUT", "path", "/notify"));
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO(
				"service1", null, null, null, null, null, null, null, null, List.of("Provider1"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(serviceReq, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TestConsumer", orchRequest, notifyInterface, 60L);

		when(systemNameNormalizer.normalize(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

		doThrow(new InvalidParameterException("Validation error")).when(serviceDefNameValidator).validateServiceDefinitionName(any(String.class));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribePreferredFlagIsSetButNoPreferredProviders() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080", "method", "PATCH", "path", "/notify"));
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO(
				"service1", null, null, null, null, null, null, null, null, List.of());
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(serviceReq, Map.of("MATCHMAKING", true, "ONLY_PREFERRED", true), null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TestConsumer", orchRequest, notifyInterface, 60L);

		when(systemNameNormalizer.normalize(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin"));
		assertEquals("ONLY_PREFERRED flag is present, but no preferred provider is defined", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeOk() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("HTTPS", Map.of("address", "localhost", "port", "8080", "method", "POST", "path", "/notify"));
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO(
				"service1", null, null, null, null, null, null, null, null, List.of("Provider1"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(serviceReq, null, null, null);
		final OrchestrationSubscriptionRequestDTO dto = new OrchestrationSubscriptionRequestDTO("TestConsumer", orchRequest, notifyInterface, 60L);

		when(systemNameNormalizer.normalize(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final SimpleOrchestrationSubscriptionRequest expected = new SimpleOrchestrationSubscriptionRequest("TestConsumer", new SimpleOrchestrationRequest("service1", List.of("Provider1"), null, null), notifyInterface, 60L);
		final SimpleOrchestrationSubscriptionRequest actual = validator.validateAndNormalizePushSubscribe(dto, "TestConsumer", "test origin");

		assertEquals(expected.getTargetSystemName(), actual.getTargetSystemName());
		assertEquals(expected.getOrchestrationRequest().getOrchestrationFlags(), actual.getOrchestrationRequest().getOrchestrationFlags());
		assertEquals(expected.getOrchestrationRequest().getPreferredProviders(), actual.getOrchestrationRequest().getPreferredProviders());
		assertEquals(expected.getOrchestrationRequest().getServiceDefinition(), actual.getOrchestrationRequest().getServiceDefinition());
		assertEquals(expected.getOrchestrationRequest().getWarnings(), actual.getOrchestrationRequest().getWarnings());
		assertEquals(expected.getNotifyInterface(), actual.getNotifyInterface());
		assertEquals(expected.getDuration(), actual.getDuration());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeBulkMissingPayload() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribeBulk(null, "test origin"));
		assertEquals("Request payload is missing", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeBulkTargetSystemNameMissing() {

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("topic", "test"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(null, null, null, null);
		final OrchestrationSubscriptionRequestDTO subscriptionDto = new OrchestrationSubscriptionRequestDTO(null, orchRequest, notifyInterface, 60L);
		final OrchestrationSubscriptionListRequestDTO dto = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionDto));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribeBulk(dto, "test origin"));
		assertEquals("Target system name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeBulkDuplicatedSubscription() {
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("topic", "test"));
		final OrchestrationServiceRequirementDTO serviceRequirement = new OrchestrationServiceRequirementDTO(
				"service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(serviceRequirement, null, null, null);
		final OrchestrationSubscriptionRequestDTO subscriptionDTO = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer1", orchRequest, notifyInterface, 60L);
		final OrchestrationSubscriptionListRequestDTO dto = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionDTO, subscriptionDTO));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizePushSubscribeBulk(dto, "test origin"));
		assertTrue(ex.getMessage().contains("Duplicated subscription request"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePushSubscribeBulkOk() {
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);

		final OrchestrationServiceRequirementDTO serviceRequirement = new OrchestrationServiceRequirementDTO(
				"service1", null, null, null, null, null, null, null, null, null);
		final OrchestrationNotifyInterfaceDTO notifyInterface = new OrchestrationNotifyInterfaceDTO("MQTT", Map.of("topic", "test"));
		final OrchestrationRequestDTO orchRequest = new OrchestrationRequestDTO(serviceRequirement, null, null, null);
		final OrchestrationSubscriptionRequestDTO subscriptionDTO = new OrchestrationSubscriptionRequestDTO("TemperatureConsumer1", orchRequest, notifyInterface, 60L);
		final OrchestrationSubscriptionListRequestDTO dto = new OrchestrationSubscriptionListRequestDTO(List.of(subscriptionDTO));

		final SimpleOrchestrationSubscriptionRequest expected = new SimpleOrchestrationSubscriptionRequest("TemperatureConsumer1", new SimpleOrchestrationRequest("service1", null, null, null), notifyInterface, 60L);
		final List<SimpleOrchestrationSubscriptionRequest> actual = validator.validateAndNormalizePushSubscribeBulk(dto, "test origin");

		assertEquals(1, actual.size());
		final SimpleOrchestrationSubscriptionRequest actualElement = actual.get(0);
		assertEquals(expected.getTargetSystemName(), actualElement.getTargetSystemName());
		assertEquals(expected.getOrchestrationRequest().getServiceDefinition(), actualElement.getOrchestrationRequest().getServiceDefinition());
		assertEquals(expected.getOrchestrationRequest().getPreferredProviders(), actualElement.getOrchestrationRequest().getPreferredProviders());
		assertEquals(expected.getOrchestrationRequest().getOrchestrationFlags(), actualElement.getOrchestrationRequest().getOrchestrationFlags());
		assertEquals(expected.getOrchestrationRequest().getWarnings(), actualElement.getOrchestrationRequest().getWarnings());
		assertEquals(expected.getNotifyInterface(), actualElement.getNotifyInterface());
		assertEquals(expected.getDuration(), actualElement.getDuration());

		verify(normalizer).normalizeSubscribe(actualElement);
		verify(systemNameValidator).validateSystemName("TemperatureConsumer1");

	}
}
