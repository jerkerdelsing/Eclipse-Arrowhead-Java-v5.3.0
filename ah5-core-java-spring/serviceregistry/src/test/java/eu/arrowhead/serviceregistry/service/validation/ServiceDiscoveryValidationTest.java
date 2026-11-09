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
package eu.arrowhead.serviceregistry.service.validation;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.MetadataValidation;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.common.service.validation.version.VersionValidator;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.service.normalization.ServiceDiscoveryNormalization;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceValidator;

@SuppressWarnings("checkstyle:magicnumber")
@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceDiscoveryValidation validator;

	@Mock
	private ServiceDiscoveryNormalization normalizer;

	@Mock
	private VersionValidator versionValidator;

	@Mock
	private InterfaceValidator interfaceValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Mock
	private SystemNameValidator systemNameValidator;

	@Mock
	private ServiceInstanceIdentifierValidator serviceInstanceIdentifierValidator;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";
	private static final String testExpirationDate = "2025-11-04T01:53:02Z";
	private static final ServiceInstanceInterfaceRequestDTO testIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

	// expected error messages
	private static final String MISSING_PAYLOAD = "Request payload is missing";
	private static final String MISSING_SYSTEM_NAME = "System name is empty";
	private static final String MISSING_SERVICE_DEFINITION = "Service definition name is empty";
	private static final String INVALID_FORMAT = "Expiration time has an invalid time format";
	private static final String INVALID_DATE = "Expiration time is in the past";
	private static final String MISSING_INTF_LIST = "Service interface list is empty";
	private static final String MISSING_TEMPLATE_NAME = "Interface template name is missing";
	private static final String MISSING_POLICY = "Interface policy is missing";
	private static final String INVALID_POLICY = "Invalid interface policy";
	private static final String MISSING_PROPERTIES = "Interface properties are missing";
	private static final String EXCLUSIVITY_VALUE_ERROR = "allowExclusivity metadata must have the string representation of an integer value.";
	private static final String MISSING_MANDATORI_FIELD = "One of the following filters must be used: 'instanceIds', 'providerNames', 'serviceDefinitionNames'";
	private static final String INVALID_INSTANCE_ID_LIST = "Instance id list contains null or empty element";
	private static final String INVALID_PROVIDER_NAME_LIST = "Provider name list contains null or empty element";
	private static final String INVALID_SERVICE_DEFINITION_LIST = "Service definition name list contains null or empty element";
	private static final String INVALID_VERSION_LIST = "Version list contains null or empty element";
	private static final String INVALID_ALIVES_AT = "Alive time has an invalid time format";
	private static final String INVALID_METADATA_REQUIREMENT_LIST = "Metadata requirements list contains null element";
	private static final String INVALID_ADDRESS_TYPE_LIST = "Address type list contains null or empty element";
	private static final String INVALID_ADDRESS_TYPE_PREFIX = "Address type list contains invalid element: ";
	private static final String INVALID_INTERFACE_TEMPLATE_LIST = "Interface template list contains null or empty element";
	private static final String INVALID_INTF_PROPERTY_REQUIREMENT_LIST = "Interface property requirements list contains null element";
	private static final String INVALID_POLICY_LIST = "Policy list contains null or empty element";
	private static final String INVALID_POLICY_PREFIX = "Policy list contains invalid element: ";
	private static final String MISSING_SERVICE_INSTANCE_ID = "Service instance ID is missing";

	//=================================================================================================
	// methods

	// REGISTER SERVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceMissingPayload() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceEmptySystemName() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(EMPTY, "alertService", "1.0.0", testExpirationDate, Map.of(), List.of(testIntf));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals(MISSING_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceEmptyServiceDefinition() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", EMPTY, "1.0.0", testExpirationDate, Map.of(), List.of(testIntf));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals(MISSING_SERVICE_DEFINITION, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceValidateExpiration() {

		assertAll("Validate expiration",

				// dto with empty expiration
				() -> {
					final ServiceInstanceRequestDTO dtoEmptyExpiration = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", EMPTY, Map.of(), List.of(testIntf));
					final ServiceInstanceRequestDTO expected = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", "", Map.of(), List.of());
					when(normalizer.normalizeServiceInstanceRequestDTO(dtoEmptyExpiration)).thenReturn(expected);

					final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dtoEmptyExpiration, "test origin"));
					assertEquals(expected, normalized);
				},

				// invalid expiration format
				() -> {
					final ServiceInstanceRequestDTO dtoInvalidFormat = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", "2030-11-04", Map.of(), List.of(testIntf));
					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidFormat.expiresAt())).thenThrow(DateTimeException.class);

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoInvalidFormat, "test origin"));
					assertEquals(INVALID_FORMAT, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidFormat.expiresAt()));
				},

				// invalid date
				() -> {
					final ServiceInstanceRequestDTO dtoInvalidDate = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", "1999-11-04T01:53:02Z", Map.of(), List.of(testIntf));
					resetUtilitiesMock();
					utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidDate.expiresAt())).thenReturn(ZonedDateTime.of(1999, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoInvalidDate, "test origin"));
					assertEquals(INVALID_DATE, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime(dtoInvalidDate.expiresAt()));

				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceInvalidMetadata() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("in.door", true), List.of(testIntf));
		final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
		metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(dto.metadata())).thenThrow(new InvalidParameterException("Metadata validation error"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals("Metadata validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(dto.metadata()));
		metadataValidationMock.close();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceMissingInterfaceList() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		assertEquals(MISSING_INTF_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceInvalidInterface() {
		assertAll("Validate interface list",

				// template name is missing
				() -> {
					final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO(EMPTY, "http", "NONE", Map.of("accessPort", 8080));
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
					assertEquals(MISSING_TEMPLATE_NAME, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
				},

				// protocol is empty
				() -> {
					final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", EMPTY, "NONE", Map.of("accessPort", 8080));
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

					final ServiceInstanceInterfaceRequestDTO normalizedIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "", "NONE", Map.of("port", 8080));
					final ServiceInstanceRequestDTO expected = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(normalizedIntf));

					when(normalizer.normalizeServiceInstanceRequestDTO(dto)).thenReturn(expected);
					when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(expected.interfaces())).thenReturn(expected.interfaces());

					final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
					assertEquals(expected, normalized);
				},

				// policy is missing
				() -> {
					resetUtilitiesMock();
					final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", EMPTY, Map.of("accessPort", 8080));
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
					assertEquals(MISSING_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
				},

				// invalid policy
				() -> {
					resetUtilitiesMock();
					final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "MAGIC_TOKEN_AUTH", Map.of("accessPort", 8080));
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
					assertEquals(INVALID_POLICY, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEnumValue("MAGIC_TOKEN_AUTH", ServiceInterfacePolicy.class));
				},

				// missing properties
				() -> {
					resetUtilitiesMock();
					final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of());
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
					assertEquals(MISSING_PROPERTIES, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(Map.of()), atLeastOnce());
				},
				// invalid properties
				() -> {
					resetUtilitiesMock();
					final ServiceInstanceInterfaceRequestDTO invalidIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("intf.port", 8080));
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of(), List.of(invalidIntf));
					final MockedStatic<MetadataValidation> metadataValidationMock = mockStatic(MetadataValidation.class);
					metadataValidationMock.when(() -> MetadataValidation.validateMetadataKey(Map.of("intf.port", 8080))).thenThrow(new InvalidParameterException("Metadata validation error"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
					assertEquals("Metadata validation error", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					metadataValidationMock.verify(() -> MetadataValidation.validateMetadataKey(Map.of("intf.port", 8080)));
					metadataValidationMock.close();
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceNotOfferablePolicy() {
		final ServiceInstanceInterfaceRequestDTO testIntf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "TRANSLATION_BRIDGE_TOKEN_AUTH", Map.of("accessPort", 8080));
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("frequency", "400"), List.of(testIntf));
		when(normalizer.normalizeServiceInstanceRequestDTO(dto)).thenReturn(dto);
		when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dto.interfaces())).thenReturn(dto.interfaces());
		utilitiesMock.when(() -> Utilities.isEnumValue("TRANSLATION_BRIDGE_TOKEN_AUTH", ServiceInterfacePolicy.class)).thenReturn(true);

		final ArrowheadException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dto, "test origin"));

		assertEquals("Invalid interface policy: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());

		verify(normalizer).normalizeServiceInstanceRequestDTO(dto);
		verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
		verify(serviceDefNameValidator, times(1)).validateServiceDefinitionName("temperatureInfo");
		verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
		verify(interfaceValidator, times(1)).validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(testIntf));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceExclusivityValueValidation() {

		assertAll(

				// invalid value
				() -> {
					final ServiceInstanceRequestDTO dtoInvalidValue = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("allowExclusivity", true), List.of(testIntf));
					when(normalizer.normalizeServiceInstanceRequestDTO(dtoInvalidValue)).thenReturn(dtoInvalidValue);
					when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dtoInvalidValue.interfaces())).thenReturn(dtoInvalidValue.interfaces());

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoInvalidValue, "test origin"));
					assertEquals(EXCLUSIVITY_VALUE_ERROR, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// value is not represented as a string
				() -> {
					final ServiceInstanceRequestDTO dtoValueNotString = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("allowExclusivity", 100), List.of(testIntf));
					when(normalizer.normalizeServiceInstanceRequestDTO(dtoValueNotString)).thenReturn(dtoValueNotString);
					when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dtoValueNotString.interfaces())).thenReturn(dtoValueNotString.interfaces());

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRegisterService(dtoValueNotString, "test origin"));
					assertEquals(EXCLUSIVITY_VALUE_ERROR, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
				},

				// valid value
				() -> {
					final ServiceInstanceRequestDTO dtoOk = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("allowExclusivity", "100"), List.of(testIntf));
					when(normalizer.normalizeServiceInstanceRequestDTO(dtoOk)).thenReturn(dtoOk);
					when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dtoOk.interfaces())).thenReturn(dtoOk.interfaces());

					final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dtoOk, "test origin"));
					assertEquals(dtoOk, normalized);
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterServiceOk() {
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("TemperatureProvider", "temperatureInfo", "1.0.0", testExpirationDate, Map.of("frequency", "400"), List.of(testIntf));
		when(normalizer.normalizeServiceInstanceRequestDTO(dto)).thenReturn(dto);
		when(interfaceValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(dto.interfaces())).thenReturn(dto.interfaces());

		final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRegisterService(dto, "test origin"));
		verify(normalizer).normalizeServiceInstanceRequestDTO(dto);
		verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
		verify(serviceDefNameValidator, times(1)).validateServiceDefinitionName("temperatureInfo");
		verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
		verify(interfaceValidator, times(1)).validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(testIntf));
		assertEquals(normalized, dto);
	}

	// LOOKUP SERVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceMissingPayload() {
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(null, "test origin"));
		assertEquals(MISSING_PAYLOAD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceMissingField() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of(),
				List.of(),
				List.of(),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(MISSING_MANDATORI_FIELD, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidInstanceIdList() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<String> invalidInstanceIds = new ArrayList<String>(2);
		invalidInstanceIds.add("TemperatureProvider|temperatureInfo|1.0.0");
		invalidInstanceIds.add(EMPTY);
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(invalidInstanceIds)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				invalidInstanceIds,
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_INSTANCE_ID_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(invalidInstanceIds));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidProviderNameList() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<String> invalidProviderNames = new ArrayList<String>(2);
		invalidProviderNames.add("TemperatureProvider");
		invalidProviderNames.add(EMPTY);
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(invalidProviderNames)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				invalidProviderNames,
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_PROVIDER_NAME_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(invalidProviderNames));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidServiceDefinitionList() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<String> invalidServiceDefinitions = new ArrayList<String>(2);
		invalidServiceDefinitions.add("temperatureInfo");
		invalidServiceDefinitions.add(EMPTY);
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(invalidServiceDefinitions)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				invalidServiceDefinitions,
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_SERVICE_DEFINITION_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(invalidServiceDefinitions));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidVersionList() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<String> invalidVersions = new ArrayList<String>(2);
		invalidVersions.add("2.0.0");
		invalidVersions.add(EMPTY);
		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(invalidVersions)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				invalidVersions,
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_VERSION_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(invalidVersions));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidAliveTime() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime("2025-11-04")).thenThrow(DateTimeException.class);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_ALIVES_AT, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.parseUTCStringToZonedDateTime("2025-11-04"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidMetadataRequirements() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final List<MetadataRequirementDTO> invalidMetadataRequirements = new ArrayList<MetadataRequirementDTO>(2);
		invalidMetadataRequirements.add(requirement);
		invalidMetadataRequirements.add(null);

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		utilitiesMock.when(() -> Utilities.containsNull(invalidMetadataRequirements)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				invalidMetadataRequirements,
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_METADATA_REQUIREMENT_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNull(invalidMetadataRequirements));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidAddressTypeList() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(
				// empty element
				() -> {
					final List<String> invalidAddressTypes = new ArrayList<String>(2);
					invalidAddressTypes.add("IPV4");
					invalidAddressTypes.add(EMPTY);

					final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							invalidAddressTypes,
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
					assertEquals(INVALID_ADDRESS_TYPE_LIST, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
				},

				// invalid element
				() -> {

					final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV5"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
					assertEquals(INVALID_ADDRESS_TYPE_PREFIX + "IPV5", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEnumValue("IPV5", AddressType.class));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidInterfaceTemplateList() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<String> invalidIntfTemplateList = new ArrayList<String>(2);
		invalidIntfTemplateList.add("generic_http");
		invalidIntfTemplateList.add(EMPTY);

		utilitiesMock.when(() -> Utilities.containsNullOrEmpty(invalidIntfTemplateList)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				invalidIntfTemplateList,
				List.of(intfReq),
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_INTERFACE_TEMPLATE_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNullOrEmpty(invalidIntfTemplateList));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidInterfacePropRequirementList() {

		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final List<MetadataRequirementDTO> invalidIntfRequirements = new ArrayList<MetadataRequirementDTO>(2);
		invalidIntfRequirements.add(intfReq);
		invalidIntfRequirements.add(null);

		utilitiesMock.when(() -> Utilities.containsNull(invalidIntfRequirements)).thenReturn(true);

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(requirement),
				List.of("IPV4"),
				List.of("generic_http"),
				invalidIntfRequirements,
				List.of("NONE"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals(INVALID_INTF_PROPERTY_REQUIREMENT_LIST, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
		utilitiesMock.verify(() -> Utilities.containsNull(invalidIntfRequirements));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testLookupServiceInvalidPolicyList() {
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(

				// empty element
				() -> {
					final List<String> invalidPolicies = new ArrayList<String>(2);
					invalidPolicies.add("NONE");
					invalidPolicies.add(EMPTY);

					final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							invalidPolicies);

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
					assertEquals(INVALID_POLICY_LIST, ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY), atLeastOnce());
				},

				// invalid element
				() -> {

					final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("MAGIC_TOKEN_AUTH"));

					final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
					assertEquals(INVALID_POLICY_PREFIX + "MAGIC_TOKEN_AUTH", ex.getMessage());
					assertEquals("test origin", ex.getOrigin());
					utilitiesMock.verify(() -> Utilities.isEnumValue("MAGIC_TOKEN_AUTH", ServiceInterfacePolicy.class));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupServiceOk() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(

				// nothing is null
				() -> {
					final ServiceInstanceLookupRequestDTO dtoNothingEmpty = new ServiceInstanceLookupRequestDTO(
							List.of("TemperatureProvider|temperatureInfo|1.0.0"),
							List.of("TemperatureProvider"),
							List.of("temperatureInfo"),
							List.of("1.0.0"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					when(normalizer.normalizeServiceInstanceLookupRequestDTO(dtoNothingEmpty)).thenReturn(dtoNothingEmpty);

					final ServiceInstanceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupService(dtoNothingEmpty, "test origin"));
					assertEquals(dtoNothingEmpty, normalized);
					verify(normalizer, times(1)).normalizeServiceInstanceLookupRequestDTO(dtoNothingEmpty);
					verify(serviceInstanceIdentifierValidator, times(1)).validateServiceInstanceIdentifier("TemperatureProvider|temperatureInfo|1.0.0");
					verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
					verify(serviceDefNameValidator, times(1)).validateServiceDefinitionName("temperatureInfo");
					verify(versionValidator, times(1)).validateNormalizedVersion("1.0.0");
					verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName("generic_http");
				},

				// empty cases 1
				() -> {
					final ServiceInstanceLookupRequestDTO dtoAlmostEmpty = new ServiceInstanceLookupRequestDTO(List.of(), List.of(), List.of("temperatureInfo"), List.of(), EMPTY, List.of(),
							List.of(), List.of(), List.of(), List.of());

					Mockito.reset(normalizer);
					when(normalizer.normalizeServiceInstanceLookupRequestDTO(dtoAlmostEmpty)).thenReturn(dtoAlmostEmpty);

					final ServiceInstanceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupService(dtoAlmostEmpty, "test origin"));
					assertEquals(dtoAlmostEmpty, normalized);
				},

				// empty cases 2
				() -> {

					final ServiceInstanceLookupRequestDTO dtoEmptyInstanceId = new ServiceInstanceLookupRequestDTO(
							List.of(),
							List.of("TemperatureProvider"),
							List.of(),
							List.of("1.0.0"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					Mockito.reset(normalizer, serviceInstanceIdentifierValidator);
					when(normalizer.normalizeServiceInstanceLookupRequestDTO(dtoEmptyInstanceId)).thenReturn(dtoEmptyInstanceId);

					final ServiceInstanceLookupRequestDTO normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeLookupService(dtoEmptyInstanceId, "test origin"));
					assertEquals(dtoEmptyInstanceId, normalized);
					verify(serviceInstanceIdentifierValidator, never()).validateServiceInstanceIdentifier(anyString());
				}

		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupServiceThrowsException() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("-TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("NONE"));

		when(normalizer.normalizeServiceInstanceLookupRequestDTO(dto)).thenReturn(dto);
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("-TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeLookupServiceNotOfferablePolicy() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
				List.of("TemperatureProvider|temperatureInfo|1.0.0"),
				List.of("-TemperatureProvider"),
				List.of("temperatureInfo"),
				List.of("1.0.0"),
				"2025-11-04T01:53:02Z",
				List.of(metadataReq),
				List.of("IPV4"),
				List.of("generic_http"),
				List.of(intfReq),
				List.of("TRANSLATION_BRIDGE_TOKEN_AUTH"));

		when(normalizer.normalizeServiceInstanceLookupRequestDTO(dto)).thenReturn(dto);
		utilitiesMock.when(() -> Utilities.isEnumValue("TRANSLATION_BRIDGE_TOKEN_AUTH", ServiceInterfacePolicy.class)).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeLookupService(dto, "test origin"));
		assertEquals("Invalid interface policy: TRANSLATION_BRIDGE_TOKEN_AUTH", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	// REVOKE SERVICE

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRevokeServiceSystemNameMissing() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeService(EMPTY, "TemperatureProvider|temperatureInfo|1.0.0", "test origin"));
		assertEquals(MISSING_SYSTEM_NAME, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRevokeServiceInstanceIdMissing() {

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeService("TemperatureProvider", EMPTY, "test origin"));
		assertEquals(MISSING_SERVICE_INSTANCE_ID, ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeRevokeServiceOk() {

		when(normalizer.normalizeSystemName("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(normalizer.normalizeServiceInstanceId("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");

		final Entry<String, String> normalized = assertDoesNotThrow(() -> validator.validateAndNormalizeRevokeService("TemperatureProvider", "TemperatureProvider|temperatureInfo|1.0.0", "test origin"));
		assertEquals("TemperatureProvider", normalized.getKey());
		assertEquals("TemperatureProvider|temperatureInfo|1.0.0", normalized.getValue());
		verify(normalizer, times(1)).normalizeSystemName("TemperatureProvider");
		verify(normalizer, times(1)).normalizeServiceInstanceId("TemperatureProvider|temperatureInfo|1.0.0");
		verify(systemNameValidator, times(1)).validateSystemName("TemperatureProvider");
		verify(serviceInstanceIdentifierValidator, times(1)).validateServiceInstanceIdentifier("TemperatureProvider|temperatureInfo|1.0.0");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateRevokeServiceThrowsException() {

		when(normalizer.normalizeSystemName("TemperatureProvider")).thenReturn("TemperatureProvider");
		when(normalizer.normalizeServiceInstanceId("TemperatureProvider|temperatureInfo|1.0.0")).thenReturn("TemperatureProvider|temperatureInfo|1.0.0");
		doThrow(new InvalidParameterException("Validation error")).when(systemNameValidator).validateSystemName("TemperatureProvider");

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalizeRevokeService("TemperatureProvider", "TemperatureProvider|temperatureInfo|1.0.0", "test origin"));
		assertEquals("Validation error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	private static void initializeUtilitiesMock() {
		createUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	private void resetUtilitiesMockBeforeEach() {
		resetUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	private void resetUtilitiesMock() {
		if (utilitiesMock != null) {
			utilitiesMock.close();
		}
		createUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	private static void createUtilitiesMock() {
		utilitiesMock = mockStatic(Utilities.class);

		// mock common cases
		utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(Map.of())).thenReturn(true);
		utilitiesMock.when(() -> Utilities.parseUTCStringToZonedDateTime(testExpirationDate)).thenReturn(ZonedDateTime.of(2025, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")));
		utilitiesMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.of(2025, 8, 4, 1, 53, 2, 0, ZoneId.of("UTC"))); // fictive date of testing
		utilitiesMock.when(() -> Utilities.isEnumValue("NONE", ServiceInterfacePolicy.class)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEnumValue("IPV4", AddressType.class)).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
